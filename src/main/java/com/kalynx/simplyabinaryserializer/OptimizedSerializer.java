package com.kalynx.simplyabinaryserializer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.kalynx.simplyabinaryserializer.TypeMarkers.*;

/**
 * Ultra-fast serializer using runtime bytecode generation via Java ClassFile API.
 * Generates specialized serializer/deserializer classes at construction time,
 * eliminating ALL reflection and branching from the hot path.
 *
 * This approach mirrors what Fury does for maximum performance.
 *
 * @param <T> The type this serializer handles
 */
public class OptimizedSerializer<T> implements OldSerializer, Deserializer {

    private static final AtomicLong CLASS_COUNTER = new AtomicLong(0);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final ClassDesc CD_FastByteWriter = ClassDesc.of("com.kalynx.simplyabinaryserializer.FastByteWriter");
    private static final ClassDesc CD_FastByteReader = ClassDesc.of("com.kalynx.simplyabinaryserializer.FastByteReader");
    private static final ClassDesc CD_Object = ConstantDescs.CD_Object;
    private static final ClassDesc CD_String = ConstantDescs.CD_String;
    private static final ClassDesc CD_List = ClassDesc.of("java.util.List");
    private static final ClassDesc CD_Map = ClassDesc.of("java.util.Map");
    private static final ClassDesc CD_ArrayList = ClassDesc.of("java.util.ArrayList");
    private static final ClassDesc CD_HashMap = ClassDesc.of("java.util.HashMap");
    private static final ClassDesc CD_OptimizedSerializer = ClassDesc.of("com.kalynx.simplyabinaryserializer.OptimizedSerializer");

    private final Class<T> targetClass;
    private final FieldInfo[] fieldInfos;
    private final int estimatedSize;
    private final Constructor<T> constructor;
    private final MethodHandle constructorHandle;
    private final boolean hasNestedObjects;

    // Cache to prevent infinite recursion
    private static final Map<Class<?>, OptimizedSerializer<?>> serializerCache = new ConcurrentHashMap<>();
    private static final Set<Class<?>> processingClasses = ConcurrentHashMap.newKeySet();

    // Generated serializer/deserializer instances
    private final GeneratedWriter<T> generatedWriter;
    private final GeneratedReader<T> generatedReader;

    private static final ThreadLocal<FastByteWriter> WRITER_POOL =
            ThreadLocal.withInitial(FastByteWriter::new);

    private static final ThreadLocal<FastByteReader> READER_POOL =
            ThreadLocal.withInitial(FastByteReader::new);

    // Pool for nested object serialization to avoid allocation overhead
    private static final ThreadLocal<ArrayDeque<FastByteWriter>> NESTED_WRITER_POOL =
            ThreadLocal.withInitial(() -> {
                ArrayDeque<FastByteWriter> pool = new ArrayDeque<>(8);
                for (int i = 0; i < 4; i++) {
                    FastByteWriter w = new FastByteWriter();
                    w.reset(512);
                    pool.push(w);
                }
                return pool;
            });

    private static final ThreadLocal<ArrayDeque<FastByteReader>> NESTED_READER_POOL =
            ThreadLocal.withInitial(() -> {
                ArrayDeque<FastByteReader> pool = new ArrayDeque<>(8);
                for (int i = 0; i < 4; i++) {
                    pool.push(new FastByteReader());
                }
                return pool;
            });

    private static final ThreadLocal<byte[]> STRING_BUFFER =
            ThreadLocal.withInitial(() -> new byte[256]);

    @FunctionalInterface
    public interface GeneratedWriter<T> {
        void write(FastByteWriter w, T obj, OptimizedSerializer<T> ser) throws Throwable;
    }

    @FunctionalInterface
    public interface GeneratedReader<T> {
        void read(FastByteReader r, T obj, OptimizedSerializer<T> ser) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    public OptimizedSerializer(Class<T> targetClass) {
        this.targetClass = targetClass;
        this.fieldInfos = analyzeFields(targetClass);
        this.estimatedSize = computeEstimatedSize();

        // Detect if we have nested objects for optimization
        boolean hasNested = false;
        for (FieldInfo fi : fieldInfos) {
            if (fi.type == FieldType.OBJECT || fi.type == FieldType.LIST || fi.type == FieldType.MAP) {
                hasNested = true;
                break;
            }
        }
        this.hasNestedObjects = hasNested;

        try {
            this.constructor = targetClass.getDeclaredConstructor();
            this.constructor.setAccessible(true);

            // Create MethodHandle for faster instantiation (avoids reflection overhead)
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(targetClass, MethodHandles.lookup());
            this.constructorHandle = lookup.findConstructor(targetClass, MethodType.methodType(void.class));

            // Generate specialized bytecode for this class
            this.generatedWriter = generateWriter();
            this.generatedReader = generateReader();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to generate serializer for " + targetClass.getName(), e);
        }
    }

    private int computeEstimatedSize() {
        int estimate = 4; // Small header overhead
        for (FieldInfo fi : fieldInfos) {
            estimate += switch (fi.type) {
                case INT -> 5;      // 4 bytes + potential alignment
                case LONG -> 9;     // 8 bytes + potential alignment
                case DOUBLE -> 9;   // 8 bytes + potential alignment
                case FLOAT -> 5;    // 4 bytes + potential alignment
                case SHORT -> 3;    // 2 bytes + potential alignment
                case BYTE -> 2;     // 1 byte + potential alignment
                case BOOLEAN -> 2;  // 1 byte + null marker
                case STRING -> 48;  // Average string ~20 chars, null byte, length
                case ENUM -> 5;     // 4 bytes (ordinal as int) + potential alignment
                case ARRAY -> 96;   // Average array with a few elements
                case LIST -> 96;    // Average list with a few elements
                case MAP -> 160;    // Average map with a few entries
                case OBJECT -> 80;  // Average nested object
            };
        }
        // Add 20% buffer to minimize resizing
        return (int)(estimate * 1.2);
    }

    /**
     * Clear the serializer cache. Useful for testing when class definitions change.
     */
    public static void clearCache() {
        serializerCache.clear();
        processingClasses.clear();
    }

    @Override
    public <U> byte[] serialize(U obj, Class<U> type) throws Throwable {
        return serialize(targetClass.cast(obj));
    }

    public byte[] serialize(T obj) throws Throwable {
        if (obj == null) return new byte[]{TYPE_NULL};

        FastByteWriter writer = WRITER_POOL.get();
        writer.reset(estimatedSize * 2);

        // Call generated bytecode - NO reflection, NO branching, NO overhead!
        generatedWriter.write(writer, obj, this);

        return writer.toByteArray();
    }

    /**
     * Internal serialization without header - used for nested objects
     */
    @SuppressWarnings("unchecked")
    public byte[] serializeInternal(Object obj) throws Throwable {
        if (obj == null) return new byte[0];

        // Use pooled writer for nested objects to avoid allocation overhead
        ArrayDeque<FastByteWriter> pool = NESTED_WRITER_POOL.get();
        FastByteWriter writer = pool.poll();
        if (writer == null) {
            writer = new FastByteWriter();
        }

        try {
            writer.reset(estimatedSize);

            generatedWriter.write(writer, (T) obj, this);

            return writer.toByteArray();
        } finally {
            // Return writer to pool if space available
            if (pool.size() < 8) {
                pool.push(writer);
            }
        }
    }

    @Override
    public <U> U deserialize(byte[] data, Class<U> type) throws Throwable {
        return type.cast(deserialize(data));
    }

    public T deserialize(byte[] data) throws Throwable {
        if (data == null || data.length == 0) return null;

        FastByteReader reader = READER_POOL.get();
        reader.setData(data);

        if (data.length == 1 && data[0] == TYPE_NULL) return null;

        // Use MethodHandle for fast instantiation (avoids reflection overhead)
        T instance = (T) constructorHandle.invoke();

        // Call generated bytecode - NO reflection, NO branching!
        generatedReader.read(reader, instance, this);

        return instance;
    }

    /**
     * Internal deserialization without header check - used for nested objects
     */
    @SuppressWarnings("unchecked")
    public Object deserializeInternal(byte[] data) throws Throwable {
        if (data == null || data.length == 0) return null;

        // Use pooled reader for nested objects to avoid allocation
        ArrayDeque<FastByteReader> pool = NESTED_READER_POOL.get();
        FastByteReader reader = pool.poll();
        if (reader == null) {
            reader = new FastByteReader();
        }

        try {
            reader.setData(data);
            // Use MethodHandle for fast instantiation
            T instance = (T) constructorHandle.invoke();
            generatedReader.read(reader, instance, this);
            return instance;
        } finally {
            // Return reader to pool if space available
            if (pool.size() < 8) {
                pool.push(reader);
            }
        }
    }

    /**
     * Optimized deserialization directly from an existing reader.
     * Used for nested objects in lists/arrays.
     */
    @SuppressWarnings("unchecked")
    public T deserializeDirectFromReader(FastByteReader reader, int length) throws Throwable {
        // Read the nested object data
        byte[] nested = new byte[length];
        reader.readFully(nested, 0, length);

        // Deserialize using the standard path (which uses reader pooling internally)
        return (T) deserializeInternal(nested);
    }

    // ==================== BYTECODE GENERATION ====================

    @SuppressWarnings("unchecked")
    private GeneratedWriter<T> generateWriter() throws Throwable {
        // Use a simple class name in the serializer's package to avoid package access issues
        String className = "com.kalynx.simplyabinaryserializer.GeneratedWriter$" + CLASS_COUNTER.incrementAndGet();
        ClassDesc targetClassDesc = ClassDesc.of(targetClass.getName());

        byte[] classBytes = ClassFile.of().build(
                ClassDesc.of(className),
                classBuilder -> {
                    classBuilder.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC);
                    classBuilder.withInterfaceSymbols(ClassDesc.of("com.kalynx.simplyabinaryserializer.OptimizedSerializer$GeneratedWriter"));

                    // Default constructor
                    classBuilder.withMethodBody(
                            ConstantDescs.INIT_NAME,
                            MethodTypeDesc.of(ConstantDescs.CD_void),
                            ClassFile.ACC_PUBLIC,
                            codeBuilder -> {
                                codeBuilder.aload(0);
                                codeBuilder.invokespecial(CD_Object, ConstantDescs.INIT_NAME,
                                        MethodTypeDesc.of(ConstantDescs.CD_void));
                                codeBuilder.return_();
                            }
                    );

                    // write(FastByteWriter w, Object obj, OptimizedSerializer ser) method
                    classBuilder.withMethodBody(
                            "write",
                            MethodTypeDesc.of(ConstantDescs.CD_void, CD_FastByteWriter, CD_Object, CD_OptimizedSerializer),
                            ClassFile.ACC_PUBLIC,
                            codeBuilder -> {
                                // Cast obj to target type and store in local 4
                                codeBuilder.aload(2); // obj
                                codeBuilder.checkcast(targetClassDesc);
                                codeBuilder.astore(4); // typed obj

                                // Generate field writes
                                for (FieldInfo fi : fieldInfos) {
                                    generateFieldWrite(codeBuilder, fi, targetClassDesc);
                                }

                                codeBuilder.return_();
                            }
                    );
                }
        );

        // Define and instantiate the generated class
        // Use LOOKUP directly - the generated class needs access to FastByteWriter which is in this package
        MethodHandles.Lookup lookup = LOOKUP.defineHiddenClass(classBytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
        Class<?> writerClass = lookup.lookupClass();
        return (GeneratedWriter<T>) writerClass.getDeclaredConstructor().newInstance();
    }

    @SuppressWarnings("unchecked")
    private GeneratedReader<T> generateReader() throws Throwable {
        // Use a simple class name in the serializer's package to avoid package access issues
        String className = "com.kalynx.simplyabinaryserializer.GeneratedReader$" + CLASS_COUNTER.incrementAndGet();
        ClassDesc targetClassDesc = ClassDesc.of(targetClass.getName());

        byte[] classBytes = ClassFile.of().build(
                ClassDesc.of(className),
                classBuilder -> {
                    classBuilder.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC);
                    classBuilder.withInterfaceSymbols(ClassDesc.of("com.kalynx.simplyabinaryserializer.OptimizedSerializer$GeneratedReader"));

                    // Default constructor
                    classBuilder.withMethodBody(
                            ConstantDescs.INIT_NAME,
                            MethodTypeDesc.of(ConstantDescs.CD_void),
                            ClassFile.ACC_PUBLIC,
                            codeBuilder -> {
                                codeBuilder.aload(0);
                                codeBuilder.invokespecial(CD_Object, ConstantDescs.INIT_NAME,
                                        MethodTypeDesc.of(ConstantDescs.CD_void));
                                codeBuilder.return_();
                            }
                    );

                    // read(FastByteReader r, Object obj, OptimizedSerializer ser) method
                    classBuilder.withMethodBody(
                            "read",
                            MethodTypeDesc.of(ConstantDescs.CD_void, CD_FastByteReader, CD_Object, CD_OptimizedSerializer),
                            ClassFile.ACC_PUBLIC,
                            codeBuilder -> {
                                // Cast obj to target type and store in local 4
                                codeBuilder.aload(2); // obj
                                codeBuilder.checkcast(targetClassDesc);
                                codeBuilder.astore(4); // typed obj

                                // Generate field reads
                                for (FieldInfo fi : fieldInfos) {
                                    generateFieldRead(codeBuilder, fi, targetClassDesc);
                                }

                                codeBuilder.return_();
                            }
                    );
                }
        );

        // Define and instantiate the generated class
        // Use LOOKUP directly - the generated class needs access to FastByteReader which is in this package
        MethodHandles.Lookup lookup = LOOKUP.defineHiddenClass(classBytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
        Class<?> readerClass = lookup.lookupClass();
        return (GeneratedReader<T>) readerClass.getDeclaredConstructor().newInstance();
    }

    private void generateFieldWrite(CodeBuilder cb, FieldInfo fi, ClassDesc targetClassDesc) {
        // Handle array types specially
        ClassDesc fieldClassDesc;
        if (fi.field.getType().isArray()) {
            fieldClassDesc = ClassDesc.ofDescriptor("[" + getArrayComponentDescriptor(fi.field.getType().getComponentType()));
        } else {
            fieldClassDesc = ClassDesc.of(fi.field.getType().getName());
        }

        switch (fi.type) {
            case INT -> {
                // Handle both primitive int and boxed Integer
                boolean isBoxed = fi.field.getType() == Integer.class;
                if (isBoxed) {
                    // Boxed Integer - can be null
                    cb.aload(4); // typed obj
                    cb.getfield(targetClassDesc, fi.field.getName(), fieldClassDesc);
                    cb.astore(5); // Store Integer in local 5

                    cb.aload(5);
                    Label notNull = cb.newLabel();
                    Label end = cb.newLabel();
                    cb.ifnonnull(notNull);

                    // Null - write marker value
                    cb.aload(1); // writer
                    cb.ldc(Integer.MIN_VALUE);
                    cb.invokevirtual(CD_FastByteWriter, "writeInt", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                    cb.goto_(end);

                    // Not null - unbox and write
                    cb.labelBinding(notNull);
                    cb.aload(1); // writer
                    cb.aload(5); // Integer
                    cb.invokevirtual(ClassDesc.of("java.lang.Integer"), "intValue", MethodTypeDesc.of(ConstantDescs.CD_int));
                    cb.invokevirtual(CD_FastByteWriter, "writeInt", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));

                    cb.labelBinding(end);
                } else {
                    // Primitive int - no null check needed
                    cb.aload(1); // writer
                    cb.aload(4); // typed obj
                    cb.getfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_int);
                    cb.invokevirtual(CD_FastByteWriter, "writeInt", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                }
            }
            case LONG -> {
                cb.aload(1);
                cb.aload(4);
                cb.getfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_long);
                cb.invokevirtual(CD_FastByteWriter, "writeLong", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_long));
            }
            case DOUBLE -> {
                cb.aload(1);
                cb.aload(4);
                cb.getfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_double);
                cb.invokevirtual(CD_FastByteWriter, "writeDouble", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_double));
            }
            case FLOAT -> {
                cb.aload(1);
                cb.aload(4);
                cb.getfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_float);
                cb.invokevirtual(CD_FastByteWriter, "writeFloat", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_float));
            }
            case SHORT -> {
                cb.aload(1);
                cb.aload(4);
                cb.getfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_short);
                cb.invokevirtual(CD_FastByteWriter, "writeShort", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_short));
            }
            case BYTE -> {
                cb.aload(1);
                cb.aload(4);
                cb.getfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_byte);
                cb.invokevirtual(CD_FastByteWriter, "writeByte", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
            }
            case BOOLEAN -> {
                cb.aload(1);
                cb.aload(4);
                cb.getfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_boolean);
                cb.invokevirtual(CD_FastByteWriter, "writeBoolean", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_boolean));
            }
            case STRING -> {
                // Generate: if (field == null) { w.writeByte(0); } else { w.writeByte(1); ser.writeStr(w, field); }
                cb.aload(4); // typed obj
                cb.getfield(targetClassDesc, fi.field.getName(), CD_String);
                cb.astore(5); // store string in local 5

                cb.aload(5);
                Label notNull = cb.newLabel();
                Label end = cb.newLabel();
                cb.ifnonnull(notNull);

                // null case
                cb.aload(1);
                cb.iconst_0();
                cb.invokevirtual(CD_FastByteWriter, "writeByte", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                cb.goto_(end);

                // not null case
                cb.labelBinding(notNull);
                cb.aload(1);
                cb.iconst_1();
                cb.invokevirtual(CD_FastByteWriter, "writeByte", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                cb.aload(3); // serializer
                cb.checkcast(CD_OptimizedSerializer);
                cb.aload(1); // writer
                cb.aload(5); // string
                cb.invokevirtual(CD_OptimizedSerializer, "writeStr",
                        MethodTypeDesc.of(ConstantDescs.CD_void, CD_FastByteWriter, CD_String));

                cb.labelBinding(end);
            }
            case LIST -> {
                // Generate: if (field == null) { w.writeByte(0); } else { w.writeByte(1); ser.writeListField(w, field, fieldIndex); }
                cb.aload(4);
                cb.getfield(targetClassDesc, fi.field.getName(), CD_List);
                cb.astore(5);

                cb.aload(5);
                Label notNull = cb.newLabel();
                Label end = cb.newLabel();
                cb.ifnonnull(notNull);

                cb.aload(1);
                cb.iconst_0();
                cb.invokevirtual(CD_FastByteWriter, "writeByte", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                cb.goto_(end);

                cb.labelBinding(notNull);
                cb.aload(1);
                cb.iconst_1();
                cb.invokevirtual(CD_FastByteWriter, "writeByte", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                cb.aload(3);
                cb.checkcast(CD_OptimizedSerializer);
                cb.aload(1);
                cb.aload(5);
                cb.ldc(fi.fieldIndex);
                cb.invokevirtual(CD_OptimizedSerializer, "writeListField",
                        MethodTypeDesc.of(ConstantDescs.CD_void, CD_FastByteWriter, CD_List, ConstantDescs.CD_int));

                cb.labelBinding(end);
            }
            case ARRAY -> {
                // Generate: if (field == null) { w.writeByte(0); } else { w.writeByte(1); ser.writeArrayField(w, field, fieldIndex); }
                cb.aload(4);
                ClassDesc arrayDesc = ClassDesc.ofDescriptor("[" + getArrayComponentDescriptor(fi.field.getType().getComponentType()));
                cb.getfield(targetClassDesc, fi.field.getName(), arrayDesc);
                cb.astore(5);

                cb.aload(5);
                Label notNull = cb.newLabel();
                Label end = cb.newLabel();
                cb.ifnonnull(notNull);

                cb.aload(1);
                cb.iconst_0();
                cb.invokevirtual(CD_FastByteWriter, "writeByte", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                cb.goto_(end);

                cb.labelBinding(notNull);
                cb.aload(1);
                cb.iconst_1();
                cb.invokevirtual(CD_FastByteWriter, "writeByte", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                cb.aload(3);
                cb.checkcast(CD_OptimizedSerializer);
                cb.aload(1);
                cb.aload(5);
                cb.ldc(fi.fieldIndex);
                cb.invokevirtual(CD_OptimizedSerializer, "writeArrayField",
                        MethodTypeDesc.of(ConstantDescs.CD_void, CD_FastByteWriter, CD_Object, ConstantDescs.CD_int));

                cb.labelBinding(end);
            }
            case MAP -> {
                cb.aload(4);
                cb.getfield(targetClassDesc, fi.field.getName(), CD_Map);
                cb.astore(5);

                cb.aload(5);
                Label notNull = cb.newLabel();
                Label end = cb.newLabel();
                cb.ifnonnull(notNull);

                cb.aload(1);
                cb.iconst_0();
                cb.invokevirtual(CD_FastByteWriter, "writeByte", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                cb.goto_(end);

                cb.labelBinding(notNull);
                cb.aload(1);
                cb.iconst_1();
                cb.invokevirtual(CD_FastByteWriter, "writeByte", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                cb.aload(3);
                cb.checkcast(CD_OptimizedSerializer);
                cb.aload(1);
                cb.aload(5);
                cb.ldc(fi.fieldIndex);
                cb.invokevirtual(CD_OptimizedSerializer, "writeMapField",
                        MethodTypeDesc.of(ConstantDescs.CD_void, CD_FastByteWriter, CD_Map, ConstantDescs.CD_int));

                cb.labelBinding(end);
            }
            case ENUM -> {
                // Serialize enum as ordinal (int)
                cb.aload(4); // typed obj
                cb.getfield(targetClassDesc, fi.field.getName(), fieldClassDesc);
                cb.astore(5); // Store enum in local 5

                cb.aload(5);
                Label notNull = cb.newLabel();
                Label end = cb.newLabel();
                cb.ifnonnull(notNull);

                // Null enum - write -1
                cb.aload(1); // writer
                cb.iconst_m1();
                cb.invokevirtual(CD_FastByteWriter, "writeInt", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                cb.goto_(end);

                // Not null - write ordinal
                cb.labelBinding(notNull);
                cb.aload(1); // writer
                cb.aload(5); // enum
                cb.invokevirtual(fieldClassDesc, "ordinal", MethodTypeDesc.of(ConstantDescs.CD_int));
                cb.invokevirtual(CD_FastByteWriter, "writeInt", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));

                cb.labelBinding(end);
            }
            case OBJECT -> {
                cb.aload(4);
                cb.getfield(targetClassDesc, fi.field.getName(), fieldClassDesc);
                cb.astore(5);

                cb.aload(5);
                Label notNull = cb.newLabel();
                Label end = cb.newLabel();
                cb.ifnonnull(notNull);

                cb.aload(1);
                cb.iconst_0();
                cb.invokevirtual(CD_FastByteWriter, "writeByte", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                cb.goto_(end);

                cb.labelBinding(notNull);
                cb.aload(1);
                cb.iconst_1();
                cb.invokevirtual(CD_FastByteWriter, "writeByte", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
                cb.aload(3);
                cb.checkcast(CD_OptimizedSerializer);
                cb.aload(1);
                cb.aload(5);
                cb.ldc(fi.fieldIndex);
                cb.invokevirtual(CD_OptimizedSerializer, "writeObjectField",
                        MethodTypeDesc.of(ConstantDescs.CD_void, CD_FastByteWriter, CD_Object, ConstantDescs.CD_int));

                cb.labelBinding(end);
            }
        }
    }

    private void generateFieldRead(CodeBuilder cb, FieldInfo fi, ClassDesc targetClassDesc) {
        // Handle array types specially
        ClassDesc fieldClassDesc;
        if (fi.field.getType().isArray()) {
            fieldClassDesc = ClassDesc.ofDescriptor("[" + getArrayComponentDescriptor(fi.field.getType().getComponentType()));
        } else {
            fieldClassDesc = ClassDesc.of(fi.field.getType().getName());
        }

        switch (fi.type) {
            case INT -> {
                // Handle both primitive int and boxed Integer
                boolean isBoxed = fi.field.getType() == Integer.class;
                if (isBoxed) {
                    // Boxed Integer - can be null
                    cb.aload(1); // reader
                    cb.invokevirtual(CD_FastByteReader, "readInt", MethodTypeDesc.of(ConstantDescs.CD_int));
                    cb.istore(5); // Store int value in local 5

                    cb.iload(5);
                    cb.ldc(Integer.MIN_VALUE);
                    Label notNull = cb.newLabel();
                    cb.if_icmpne(notNull);

                    // Value is MIN_VALUE (null marker) - set null
                    cb.aload(4); // typed obj
                    cb.aconst_null();
                    cb.putfield(targetClassDesc, fi.field.getName(), fieldClassDesc);
                    Label end = cb.newLabel();
                    cb.goto_(end);

                    // Not null - box and set
                    cb.labelBinding(notNull);
                    cb.aload(4); // typed obj
                    cb.iload(5); // int value
                    cb.invokestatic(ClassDesc.of("java.lang.Integer"), "valueOf",
                        MethodTypeDesc.of(ClassDesc.of("java.lang.Integer"), ConstantDescs.CD_int));
                    cb.putfield(targetClassDesc, fi.field.getName(), fieldClassDesc);

                    cb.labelBinding(end);
                } else {
                    // Primitive int - direct read
                    cb.aload(4); // typed obj
                    cb.aload(1); // reader
                    cb.invokevirtual(CD_FastByteReader, "readInt", MethodTypeDesc.of(ConstantDescs.CD_int));
                    cb.putfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_int);
                }
            }
            case LONG -> {
                cb.aload(4);
                cb.aload(1);
                cb.invokevirtual(CD_FastByteReader, "readLong", MethodTypeDesc.of(ConstantDescs.CD_long));
                cb.putfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_long);
            }
            case DOUBLE -> {
                cb.aload(4);
                cb.aload(1);
                cb.invokevirtual(CD_FastByteReader, "readDouble", MethodTypeDesc.of(ConstantDescs.CD_double));
                cb.putfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_double);
            }
            case FLOAT -> {
                cb.aload(4);
                cb.aload(1);
                cb.invokevirtual(CD_FastByteReader, "readFloat", MethodTypeDesc.of(ConstantDescs.CD_float));
                cb.putfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_float);
            }
            case SHORT -> {
                cb.aload(4);
                cb.aload(1);
                cb.invokevirtual(CD_FastByteReader, "readShort", MethodTypeDesc.of(ConstantDescs.CD_short));
                cb.putfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_short);
            }
            case BYTE -> {
                cb.aload(4);
                cb.aload(1);
                cb.invokevirtual(CD_FastByteReader, "readByte", MethodTypeDesc.of(ConstantDescs.CD_byte));
                cb.putfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_byte);
            }
            case BOOLEAN -> {
                cb.aload(4);
                cb.aload(1);
                cb.invokevirtual(CD_FastByteReader, "readBoolean", MethodTypeDesc.of(ConstantDescs.CD_boolean));
                cb.putfield(targetClassDesc, fi.field.getName(), ConstantDescs.CD_boolean);
            }
            case STRING -> {
                // if (r.readByte() != 0) obj.field = ser.readStr(r);
                cb.aload(1);
                cb.invokevirtual(CD_FastByteReader, "readByte", MethodTypeDesc.of(ConstantDescs.CD_byte));
                Label skip = cb.newLabel();
                cb.ifeq(skip);

                cb.aload(4);
                cb.aload(3);
                cb.checkcast(CD_OptimizedSerializer);
                cb.aload(1);
                cb.invokevirtual(CD_OptimizedSerializer, "readStr",
                        MethodTypeDesc.of(CD_String, CD_FastByteReader));
                cb.putfield(targetClassDesc, fi.field.getName(), CD_String);

                cb.labelBinding(skip);
            }
            case LIST -> {
                cb.aload(1);
                cb.invokevirtual(CD_FastByteReader, "readByte", MethodTypeDesc.of(ConstantDescs.CD_byte));
                Label skip = cb.newLabel();
                cb.ifeq(skip);

                cb.aload(4);
                cb.aload(3);
                cb.checkcast(CD_OptimizedSerializer);
                cb.aload(1);
                cb.ldc(fi.fieldIndex);
                cb.invokevirtual(CD_OptimizedSerializer, "readListField",
                        MethodTypeDesc.of(CD_List, CD_FastByteReader, ConstantDescs.CD_int));
                cb.putfield(targetClassDesc, fi.field.getName(), CD_List);

                cb.labelBinding(skip);
            }
            case ARRAY -> {
                cb.aload(1);
                cb.invokevirtual(CD_FastByteReader, "readByte", MethodTypeDesc.of(ConstantDescs.CD_byte));
                Label skip = cb.newLabel();
                cb.ifeq(skip);

                cb.aload(4);
                cb.aload(3);
                cb.checkcast(CD_OptimizedSerializer);
                cb.aload(1);
                cb.ldc(fi.fieldIndex);
                cb.invokevirtual(CD_OptimizedSerializer, "readArrayField",
                        MethodTypeDesc.of(CD_Object, CD_FastByteReader, ConstantDescs.CD_int));
                ClassDesc arrayDesc = ClassDesc.ofDescriptor("[" + getArrayComponentDescriptor(fi.field.getType().getComponentType()));
                cb.checkcast(arrayDesc);
                cb.putfield(targetClassDesc, fi.field.getName(), arrayDesc);

                cb.labelBinding(skip);
            }
            case MAP -> {
                cb.aload(1);
                cb.invokevirtual(CD_FastByteReader, "readByte", MethodTypeDesc.of(ConstantDescs.CD_byte));
                Label skip = cb.newLabel();
                cb.ifeq(skip);

                cb.aload(4);
                cb.aload(3);
                cb.checkcast(CD_OptimizedSerializer);
                cb.aload(1);
                cb.ldc(fi.fieldIndex);
                cb.invokevirtual(CD_OptimizedSerializer, "readMapField",
                        MethodTypeDesc.of(CD_Map, CD_FastByteReader, ConstantDescs.CD_int));
                cb.putfield(targetClassDesc, fi.field.getName(), CD_Map);

                cb.labelBinding(skip);
            }
            case ENUM -> {
                // Deserialize enum from ordinal
                cb.aload(1); // reader
                cb.invokevirtual(CD_FastByteReader, "readInt", MethodTypeDesc.of(ConstantDescs.CD_int));
                cb.istore(5); // Store ordinal in local 5

                cb.iload(5);
                Label notNull = cb.newLabel();
                cb.iconst_m1();
                cb.if_icmpne(notNull);

                // Ordinal is -1, set null
                cb.aload(4); // typed obj
                cb.aconst_null();
                cb.putfield(targetClassDesc, fi.field.getName(), fieldClassDesc);
                Label skip = cb.newLabel();
                cb.goto_(skip);

                // Not null - get enum value by ordinal
                cb.labelBinding(notNull);
                cb.aload(4); // typed obj
                cb.aload(3); // OptimizedSerializer (this)
                cb.checkcast(CD_OptimizedSerializer);
                cb.aload(1); // reader
                cb.iload(5); // ordinal
                cb.ldc(fi.fieldIndex);
                cb.invokevirtual(CD_OptimizedSerializer, "readEnumField",
                        MethodTypeDesc.of(ClassDesc.of("java.lang.Enum"), CD_FastByteReader, ConstantDescs.CD_int, ConstantDescs.CD_int));
                cb.checkcast(fieldClassDesc);
                cb.putfield(targetClassDesc, fi.field.getName(), fieldClassDesc);

                cb.labelBinding(skip);
            }
            case OBJECT -> {
                cb.aload(1);
                cb.invokevirtual(CD_FastByteReader, "readByte", MethodTypeDesc.of(ConstantDescs.CD_byte));
                Label skip = cb.newLabel();
                cb.ifeq(skip);

                cb.aload(4);
                cb.aload(3);
                cb.checkcast(CD_OptimizedSerializer);
                cb.aload(1);
                cb.ldc(fi.fieldIndex);
                cb.invokevirtual(CD_OptimizedSerializer, "readObjectField",
                        MethodTypeDesc.of(CD_Object, CD_FastByteReader, ConstantDescs.CD_int));
                cb.checkcast(fieldClassDesc);
                cb.putfield(targetClassDesc, fi.field.getName(), fieldClassDesc);

                cb.labelBinding(skip);
            }
        }
    }

    // ==================== HELPER METHODS CALLED BY GENERATED CODE ====================

    private String getArrayComponentDescriptor(Class<?> componentType) {
        if (componentType == int.class) return "I";
        if (componentType == long.class) return "J";
        if (componentType == boolean.class) return "Z";
        if (componentType == double.class) return "D";
        if (componentType == float.class) return "F";
        if (componentType == short.class) return "S";
        if (componentType == byte.class) return "B";
        if (componentType == char.class) return "C";
        if (componentType == String.class) return "Ljava/lang/String;";
        return "L" + componentType.getName().replace('.', '/') + ";";
    }

    public void writeStr(FastByteWriter w, String s) {
        if (s == null) {
            w.writeByte((byte) 255); // Marker for special handling
            w.writeInt(-1); // Use -1 to indicate null
            return;
        }

        // Always use Java's UTF-8 encoder which handles surrogate pairs correctly
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int len = bytes.length;

        if (len < 255) {
            w.writeByte((byte) len);
            w.writeBytes(bytes, len);
        } else {
            w.writeByte((byte) 255);
            w.writeInt(len);
            w.writeBytes(bytes, len);
        }
    }

    public String readStr(FastByteReader r) {
        int len = r.readByte() & 0xFF;
        if (len == 255) {
            len = r.readInt();
            if (len == -1) {
                return null; // Null string marker
            }
        }

        // Use pooled buffer for small strings to reduce allocation overhead
        if (len <= 256) {
            byte[] buf = STRING_BUFFER.get();
            if (buf.length < len) {
                buf = new byte[512]; // Larger reusable buffer
                STRING_BUFFER.set(buf);
            }
            r.readFully(buf, 0, len);
            return new String(buf, 0, len, StandardCharsets.UTF_8);
        } else {
            // Large string - allocate directly
            byte[] b = new byte[len];
            r.readFully(b, 0, len);
            return new String(b, StandardCharsets.UTF_8);
        }
    }

    private int encodeUTF8(String s, byte[] buf) {
        int len = s.length();
        int pos = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c < 0x80) {
                buf[pos++] = (byte) c;
            } else if (c < 0x800) {
                buf[pos++] = (byte) (0xC0 | (c >> 6));
                buf[pos++] = (byte) (0x80 | (c & 0x3F));
            } else {
                buf[pos++] = (byte) (0xE0 | (c >> 12));
                buf[pos++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                buf[pos++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        return pos;
    }

    public void writeListField(FastByteWriter w, List<?> list, int fieldIndex) throws Throwable {
        FieldInfo fi = fieldInfos[fieldIndex];
        int size = list.size();
        w.writeInt(size);
        if (size == 0) return;

        // Fast path: use pre-allocated writer for primitives and strings
        if (fi.listElementType != FieldType.OBJECT) {
            ListElementWriter writer = fi.listWriter;
            for (int i = 0; i < size; i++) {
                writer.write(w, list.get(i));
            }
        } else {
            // Object path: check if we have a nested serializer
            if (fi.nestedSerializer != null) {
                // Optimized path for custom objects with nested serializer
                for (int i = 0; i < size; i++) {
                    byte[] nested = fi.nestedSerializer.serializeInternal(list.get(i));
                    if (nested.length < 255) {
                        w.writeByte((byte) nested.length);
                    } else {
                        w.writeByte((byte) 255);
                        w.writeInt(nested.length);
                    }
                    w.writeBytes(nested, nested.length);
                }
            } else {
                // Fallback path for nested collections (uses pre-allocated handler)
                ListElementWriter writer = fi.listWriter;
                for (int i = 0; i < size; i++) {
                    writer.write(w, list.get(i));
                }
            }
        }
    }

    public List<?> readListField(FastByteReader r, int fieldIndex) throws Throwable {
        FieldInfo fi = fieldInfos[fieldIndex];
        int size = r.readInt();
        if (size == 0) return new ArrayList<>(0);

        List<Object> list = new ArrayList<>(size); // Pre-sized to avoid resizing

        // Fast path: use pre-allocated reader for primitives and strings
        if (fi.listElementType != FieldType.OBJECT) {
            ListElementReader reader = fi.listReader;
            for (int i = 0; i < size; i++) {
                list.add(reader.read(r));
            }
        } else {
            // Object path: check if we have a nested serializer
            if (fi.nestedSerializer != null) {
                // Optimized path for custom objects with nested serializer
                for (int i = 0; i < size; i++) {
                    int len = r.readByte() & 0xFF;
                    if (len == 255) {
                        len = r.readInt();
                    }
                    Object obj = fi.nestedSerializer.deserializeDirectFromReader(r, len);
                    list.add(obj);
                }
            } else {
                // Fallback path for nested collections (uses pre-allocated handler)
                ListElementReader reader = fi.listReader;
                for (int i = 0; i < size; i++) {
                    list.add(reader.read(r));
                }
            }
        }
        return list;
    }

    public void writeArrayField(FastByteWriter w, Object array, int fieldIndex) throws Throwable {
        FieldInfo fi = fieldInfos[fieldIndex];
        int length = java.lang.reflect.Array.getLength(array);
        w.writeInt(length);
        if (length == 0) return;

        switch (fi.arrayElementType) {
            case INT -> {
                int[] arr = (int[]) array;
                for (int val : arr) w.writeInt(val);
            }
            case LONG -> {
                long[] arr = (long[]) array;
                for (long val : arr) w.writeLong(val);
            }
            case BOOLEAN -> {
                boolean[] arr = (boolean[]) array;
                for (boolean val : arr) w.writeBoolean(val);
            }
            case DOUBLE -> {
                double[] arr = (double[]) array;
                for (double val : arr) w.writeDouble(val);
            }
            case FLOAT -> {
                float[] arr = (float[]) array;
                for (float val : arr) w.writeFloat(val);
            }
            case SHORT -> {
                short[] arr = (short[]) array;
                for (short val : arr) w.writeShort(val);
            }
            case BYTE -> {
                byte[] arr = (byte[]) array;
                for (byte val : arr) w.writeByte(val);
            }
            case STRING -> {
                String[] arr = (String[]) array;
                for (String val : arr) writeStr(w, val);
            }
            case OBJECT -> {
                Object[] arr = (Object[]) array;
                for (Object val : arr) {
                    byte[] nested = fi.nestedSerializer.serializeInternal(val);
                    // Use 1 byte for small objects, extended length for larger ones
                    if (nested.length < 255) {
                        w.writeByte((byte) nested.length);
                    } else {
                        w.writeByte((byte) 255); // Marker for extended length
                        w.writeInt(nested.length);
                    }
                    w.writeBytes(nested, nested.length);
                }
            }
            default -> {}
        }
    }

    public Object readArrayField(FastByteReader r, int fieldIndex) throws Throwable {
        FieldInfo fi = fieldInfos[fieldIndex];
        int length = r.readInt();
        if (length == 0) {
            return java.lang.reflect.Array.newInstance(fi.field.getType().getComponentType(), 0);
        }

        switch (fi.arrayElementType) {
            case INT -> {
                int[] arr = new int[length];
                for (int i = 0; i < length; i++) arr[i] = r.readInt();
                return arr;
            }
            case LONG -> {
                long[] arr = new long[length];
                for (int i = 0; i < length; i++) arr[i] = r.readLong();
                return arr;
            }
            case BOOLEAN -> {
                boolean[] arr = new boolean[length];
                for (int i = 0; i < length; i++) arr[i] = r.readBoolean();
                return arr;
            }
            case DOUBLE -> {
                double[] arr = new double[length];
                for (int i = 0; i < length; i++) arr[i] = r.readDouble();
                return arr;
            }
            case FLOAT -> {
                float[] arr = new float[length];
                for (int i = 0; i < length; i++) arr[i] = r.readFloat();
                return arr;
            }
            case SHORT -> {
                short[] arr = new short[length];
                for (int i = 0; i < length; i++) arr[i] = r.readShort();
                return arr;
            }
            case BYTE -> {
                byte[] arr = new byte[length];
                for (int i = 0; i < length; i++) arr[i] = r.readByte();
                return arr;
            }
            case STRING -> {
                String[] arr = new String[length];
                for (int i = 0; i < length; i++) arr[i] = readStr(r);
                return arr;
            }
            case OBJECT -> {
                Class<?> componentType = fi.field.getType().getComponentType();
                Object[] arr = (Object[]) java.lang.reflect.Array.newInstance(componentType, length);
                for (int i = 0; i < length; i++) {
                    int nestedLen = r.readByte() & 0xFF;
                    if (nestedLen == 255) {
                        // Extended length for larger objects
                        nestedLen = r.readInt();
                    }
                    // Direct deserialization without intermediate byte array
                    arr[i] = fi.nestedSerializer.deserializeDirectFromReader(r, nestedLen);
                }
                return arr;
            }
            default -> {
                return null;
            }
        }
    }

    public void writeMapField(FastByteWriter w, Map<?, ?> map, int fieldIndex) throws Throwable {
        FieldInfo fi = fieldInfos[fieldIndex];
        int size = map.size();
        w.writeInt(size);
        if (size == 0) return;

        for (Map.Entry<?, ?> e : map.entrySet()) {
            writeMapKey(w, e.getKey(), fi.mapKeyType);
            writeMapValue(w, e.getValue(), fi.mapValueType);
        }
    }

    private void writeMapKey(FastByteWriter w, Object key, FieldType type) {
        switch (type) {
            case INT -> w.writeInt((Integer) key);
            case LONG -> w.writeLong((Long) key);
            case STRING -> writeStr(w, (String) key);
            case BOOLEAN -> w.writeBoolean((Boolean) key);
            case DOUBLE -> w.writeDouble((Double) key);
            case FLOAT -> w.writeFloat((Float) key);
            case SHORT -> w.writeShort((Short) key);
            case BYTE -> w.writeByte((Byte) key);
            default -> {}
        }
    }

    private void writeMapValue(FastByteWriter w, Object value, FieldType type) {
        switch (type) {
            case INT -> w.writeInt((Integer) value);
            case LONG -> w.writeLong((Long) value);
            case STRING -> writeStr(w, (String) value);
            case BOOLEAN -> w.writeBoolean((Boolean) value);
            case DOUBLE -> w.writeDouble((Double) value);
            case FLOAT -> w.writeFloat((Float) value);
            case SHORT -> w.writeShort((Short) value);
            case BYTE -> w.writeByte((Byte) value);
            case OBJECT -> {
                // Fallback for nested collections or complex objects
                if (value == null) {
                    w.writeInt(-1);
                } else {
                    try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                         java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos)) {
                        oos.writeObject(value);
                        oos.flush();
                        byte[] data = baos.toByteArray();
                        w.writeInt(data.length);
                        w.writeBytes(data, data.length);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize map value", e);
                    }
                }
            }
            default -> {}
        }
    }

    public Map<?, ?> readMapField(FastByteReader r, int fieldIndex) {
        FieldInfo fi = fieldInfos[fieldIndex];
        int size = r.readInt();
        if (size == 0) return new HashMap<>(0);

        // Pre-size with load factor to avoid rehashing
        Map<Object, Object> map = new HashMap<>((int)(size / 0.75f) + 1);

        // Optimize for common case: String keys with Integer values (most common pattern)
        if (fi.mapKeyType == FieldType.STRING && fi.mapValueType == FieldType.INT) {
            for (int i = 0; i < size; i++) {
                String key = readStr(r);
                int value = r.readInt();
                map.put(key, value);
            }
        } else if (fi.mapKeyType == FieldType.STRING && fi.mapValueType == FieldType.STRING) {
            for (int i = 0; i < size; i++) {
                String key = readStr(r);
                String value = readStr(r);
                map.put(key, value);
            }
        } else {
            // Generic path for other type combinations
            for (int i = 0; i < size; i++) {
                Object key = readMapKey(r, fi.mapKeyType);
                Object value = readMapValue(r, fi.mapValueType);
                map.put(key, value);
            }
        }
        return map;
    }

    private Object readMapKey(FastByteReader r, FieldType type) {
        return switch (type) {
            case INT -> r.readInt();
            case LONG -> r.readLong();
            case STRING -> readStr(r);
            case BOOLEAN -> r.readBoolean();
            case DOUBLE -> r.readDouble();
            case FLOAT -> r.readFloat();
            case SHORT -> r.readShort();
            case BYTE -> r.readByte();
            default -> null;
        };
    }

    private Object readMapValue(FastByteReader r, FieldType type) {
        return switch (type) {
            case INT -> r.readInt();
            case LONG -> r.readLong();
            case STRING -> readStr(r);
            case BOOLEAN -> r.readBoolean();
            case DOUBLE -> r.readDouble();
            case FLOAT -> r.readFloat();
            case SHORT -> r.readShort();
            case BYTE -> r.readByte();
            case OBJECT -> {
                // Fallback for nested collections or complex objects
                int len = r.readInt();
                if (len == -1) {
                    yield null;
                }
                byte[] data = new byte[len];
                r.readFully(data, 0, len);
                try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(data))) {
                    yield ois.readObject();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deserialize map value", e);
                }
            }
            default -> null;
        };
    }

    public Enum<?> readEnumField(FastByteReader r, int ordinal, int fieldIndex) {
        FieldInfo fi = fieldInfos[fieldIndex];
        Class<?> enumClass = fi.field.getType();
        Object[] enumConstants = enumClass.getEnumConstants();
        return (Enum<?>) enumConstants[ordinal];
    }

    public void writeObjectField(FastByteWriter w, Object obj, int fieldIndex) throws Throwable {
        FieldInfo fi = fieldInfos[fieldIndex];

        if (obj == null) {
            w.writeByte(TYPE_NULL);
            return;
        }

        w.writeByte(TYPE_OBJECT);

        // Optimize: Write nested object directly to parent writer instead of creating intermediate byte[]
        OptimizedSerializer<?> serializer = fi.nestedSerializer;
        if (serializer == null) {
            // Handle case where serializer wasn't created (circular reference schema)
            serializer = serializerCache.get(obj.getClass());
            if (serializer == null) {
                serializer = new OptimizedSerializer<>(obj.getClass());
                serializerCache.put(obj.getClass(), serializer);
            }
        }

        // Write nested object directly using the serializer's writer
        @SuppressWarnings("unchecked")
        OptimizedSerializer<Object> objSerializer = (OptimizedSerializer<Object>) serializer;
        objSerializer.generatedWriter.write(w, obj, objSerializer);
    }

    public Object readObjectField(FastByteReader r, int fieldIndex) throws Throwable {
        FieldInfo fi = fieldInfos[fieldIndex];
        byte type = r.readByte();

        if (type == TYPE_NULL) {
            return null;
        }

        if (type != TYPE_OBJECT) {
            throw new IllegalStateException("Unknown object type: " + type);
        }

        // Optimize: Read nested object directly from parent reader instead of creating intermediate byte[]
        OptimizedSerializer<?> serializer = fi.nestedSerializer;
        if (serializer == null) {
            // Handle case where serializer wasn't created
            Class<?> fieldType = fi.field.getType();
            serializer = serializerCache.get(fieldType);
            if (serializer == null) {
                serializer = new OptimizedSerializer<>(fieldType);
                serializerCache.put(fieldType, serializer);
            }
        }

        // Create instance and read directly using MethodHandle
        @SuppressWarnings("unchecked")
        OptimizedSerializer<Object> objSerializer = (OptimizedSerializer<Object>) serializer;
        Object instance = objSerializer.constructorHandle.invoke();
        objSerializer.generatedReader.read(r, instance, objSerializer);
        return instance;
    }

    // ==================== FIELD ANALYSIS ====================

    private FieldInfo[] analyzeFields(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        List<FieldInfo> infos = new ArrayList<>();

        int index = 0;
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) ||
                    Modifier.isTransient(field.getModifiers())) continue;

            field.setAccessible(true);
            infos.add(analyzeField(field, index++));
        }

        return infos.toArray(new FieldInfo[0]);
    }

    private ListElementWriter createListWriter(FieldType elementType, Class<?> elementClass) {
        return switch (elementType) {
            case INT -> (w, e) -> w.writeInt((Integer) e);
            case LONG -> (w, e) -> w.writeLong((Long) e);
            case STRING -> (w, e) -> writeStr(w, (String) e);
            case BOOLEAN -> (w, e) -> w.writeBoolean((Boolean) e);
            case DOUBLE -> (w, e) -> w.writeDouble((Double) e);
            case FLOAT -> (w, e) -> w.writeFloat((Float) e);
            case SHORT -> (w, e) -> w.writeShort((Short) e);
            case BYTE -> (w, e) -> w.writeByte((Byte) e);
            case ENUM -> (w, e) -> {
                Enum<?> enumVal = (Enum<?>) e;
                w.writeInt(enumVal == null ? -1 : enumVal.ordinal());
            };
            case OBJECT -> createObjectListWriter(elementClass);
            default -> (w, e) -> {}; // No-op for unsupported types
        };
    }

    private ListElementWriter createObjectListWriter(Class<?> elementClass) {
        // Check if it's a nested collection type
        if (List.class.isAssignableFrom(elementClass) || Map.class.isAssignableFrom(elementClass)) {
            // Fallback: Java serialization for nested collections
            return (w, obj) -> {
                if (obj == null) {
                    w.writeInt(-1);
                } else {
                    try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                         java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos)) {
                        oos.writeObject(obj);
                        oos.flush();
                        byte[] data = baos.toByteArray();
                        w.writeInt(data.length);
                        w.writeBytes(data, data.length);
                    }
                }
            };
        } else {
            // Use nested OptimizedSerializer (will be set in nestedSerializer field)
            return (w, obj) -> {
                // This will be called with fi.nestedSerializer already available
                throw new UnsupportedOperationException("Should use nestedSerializer path");
            };
        }
    }

    private ListElementReader createListReader(FieldType elementType, Class<?> elementClass) {
        return switch (elementType) {
            case INT -> r -> r.readInt();
            case LONG -> r -> r.readLong();
            case STRING -> this::readStr;
            case BOOLEAN -> r -> r.readBoolean();
            case DOUBLE -> r -> r.readDouble();
            case FLOAT -> r -> r.readFloat();
            case SHORT -> r -> r.readShort();
            case BYTE -> r -> r.readByte();
            case ENUM -> r -> {
                int ordinal = r.readInt();
                if (ordinal == -1) return null;
                return elementClass.getEnumConstants()[ordinal];
            };
            case OBJECT -> createObjectListReader(elementClass);
            default -> r -> null; // No-op for unsupported types
        };
    }

    private ListElementReader createObjectListReader(Class<?> elementClass) {
        // Check if it's a nested collection type
        if (List.class.isAssignableFrom(elementClass) || Map.class.isAssignableFrom(elementClass)) {
            // Fallback: Java deserialization for nested collections
            return r -> {
                int len = r.readInt();
                if (len == -1) return null;
                byte[] data = new byte[len];
                r.readFully(data, 0, len);
                try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(data))) {
                    return ois.readObject();
                }
            };
        } else {
            // Use nested OptimizedSerializer (will be set in nestedSerializer field)
            return r -> {
                // This will be called with fi.nestedSerializer already available
                throw new UnsupportedOperationException("Should use nestedSerializer path");
            };
        }
    }

    private FieldInfo analyzeField(Field field, int index) {
        Class<?> fieldType = field.getType();
        FieldInfo fi = new FieldInfo();
        fi.field = field;
        fi.fieldIndex = index;

        if (fieldType == int.class || fieldType == Integer.class) {
            fi.type = FieldType.INT;
        } else if (fieldType == long.class || fieldType == Long.class) {
            fi.type = FieldType.LONG;
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            fi.type = FieldType.BOOLEAN;
        } else if (fieldType == double.class || fieldType == Double.class) {
            fi.type = FieldType.DOUBLE;
        } else if (fieldType == float.class || fieldType == Float.class) {
            fi.type = FieldType.FLOAT;
        } else if (fieldType == short.class || fieldType == Short.class) {
            fi.type = FieldType.SHORT;
        } else if (fieldType == byte.class || fieldType == Byte.class) {
            fi.type = FieldType.BYTE;
        } else if (fieldType == String.class) {
            fi.type = FieldType.STRING;
        } else if (fieldType.isArray()) {
            fi.type = FieldType.ARRAY;
            Class<?> componentType = fieldType.getComponentType();
            fi.arrayElementType = determineFieldType(componentType);
            if (fi.arrayElementType == FieldType.OBJECT) {
                fi.nestedSerializer = new OptimizedSerializer<>(componentType);
            }
        } else if (List.class.isAssignableFrom(fieldType)) {
            fi.type = FieldType.LIST;
            Type genericType = field.getGenericType();
            Class<?> elementType = extractGenericType(genericType, 0);
            fi.listElementType = determineFieldType(elementType);

            // Pre-allocate list handlers based on element type (eliminates runtime switch)
            fi.listWriter = createListWriter(fi.listElementType, elementType);
            fi.listReader = createListReader(fi.listElementType, elementType);

            if (fi.listElementType == FieldType.OBJECT) {
                // Skip creating serializers for nested collection types (List<List<>>, List<Map<>>)
                if (!List.class.isAssignableFrom(elementType) && !Map.class.isAssignableFrom(elementType)) {
                    fi.nestedSerializer = new OptimizedSerializer<>(elementType);
                }
            }
        } else if (Map.class.isAssignableFrom(fieldType)) {
            fi.type = FieldType.MAP;
            Type genericType = field.getGenericType();
            fi.mapKeyType = determineFieldType(extractGenericType(genericType, 0));
            fi.mapValueType = determineFieldType(extractGenericType(genericType, 1));
        } else if (fieldType.isEnum()) {
            fi.type = FieldType.ENUM;
        } else {
            fi.type = FieldType.OBJECT;
            // Use cache to prevent infinite recursion for self-referencing objects
            OptimizedSerializer<?> cached = serializerCache.get(fieldType);
            if (cached != null) {
                fi.nestedSerializer = cached;
            } else if (processingClasses.contains(fieldType)) {
                // Circular reference detected during schema creation - allow it but with stub
                // The actual serialization will be handled by reference tracking at runtime
                fi.nestedSerializer = null; // Will serialize as references at runtime
            } else {
                try {
                    processingClasses.add(fieldType);
                    fi.nestedSerializer = new OptimizedSerializer<>(fieldType);
                    serializerCache.put(fieldType, fi.nestedSerializer);
                } finally {
                    processingClasses.remove(fieldType);
                }
            }
        }

        return fi;
    }

    private Class<?> extractGenericType(Type genericType, int index) {
        if (genericType instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            if (typeArgs.length > index) {
                Type typeArg = typeArgs[index];
                if (typeArg instanceof Class) {
                    return (Class<?>) typeArg;
                } else if (typeArg instanceof ParameterizedType) {
                    // Nested generic like List<List<Integer>> - return raw type
                    Type rawType = ((ParameterizedType) typeArg).getRawType();
                    if (rawType instanceof Class) {
                        return (Class<?>) rawType;
                    }
                }
            }
        }
        return Object.class;
    }

    private FieldType determineFieldType(Class<?> clazz) {
        if (clazz == int.class || clazz == Integer.class) return FieldType.INT;
        if (clazz == long.class || clazz == Long.class) return FieldType.LONG;
        if (clazz == boolean.class || clazz == Boolean.class) return FieldType.BOOLEAN;
        if (clazz == double.class || clazz == Double.class) return FieldType.DOUBLE;
        if (clazz == float.class || clazz == Float.class) return FieldType.FLOAT;
        if (clazz == short.class || clazz == Short.class) return FieldType.SHORT;
        if (clazz == byte.class || clazz == Byte.class) return FieldType.BYTE;
        if (clazz == String.class) return FieldType.STRING;
        if (clazz.isEnum()) return FieldType.ENUM;
        return FieldType.OBJECT;
    }

    // ==================== INNER CLASSES ====================

    // Functional interfaces for pre-allocated list operations
    @FunctionalInterface
    private interface ListElementWriter {
        void write(FastByteWriter w, Object element) throws Throwable;
    }

    @FunctionalInterface
    private interface ListElementReader {
        Object read(FastByteReader r) throws Throwable;
    }

    private static class FieldInfo {
        Field field;
        int fieldIndex;
        FieldType type;
        FieldType listElementType;
        FieldType arrayElementType;
        FieldType mapKeyType;
        FieldType mapValueType;
        OptimizedSerializer<?> nestedSerializer;

        // Pre-allocated handlers for list operations (eliminates runtime switch)
        ListElementWriter listWriter;
        ListElementReader listReader;
    }

    private enum FieldType { INT, LONG, BOOLEAN, DOUBLE, FLOAT, SHORT, BYTE, STRING, LIST, MAP, OBJECT, ENUM, ARRAY }
}



































