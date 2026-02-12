package com.kalynx.simplyabinaryserializer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.kalynx.simplyabinaryserializer.TypeMarkers.*;

/**
 * Ultra-fast type-specific serializer using MethodHandle composition.
 * Generates a single composed MethodHandle for all field operations,
 * allowing the JIT to inline everything into one monomorphic call site.
 *
 * @param <T> The type this serializer handles
 */
public class TypedSerializer<T> implements Serializer, Deserializer {

    private final Class<T> targetClass;
    private final SerializationSchema schema;
    private final int estimatedSize;

    // Composed MethodHandles for zero-dispatch execution
    private final MethodHandle composedWriter;
    private final MethodHandle composedReader;

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final ThreadLocal<FastByteWriter> WRITER_POOL =
            ThreadLocal.withInitial(FastByteWriter::new);

    private static final ThreadLocal<FastByteReader> READER_POOL =
            ThreadLocal.withInitial(FastByteReader::new);

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
                for (int i = 0; i < 4; i++) pool.push(new FastByteReader());
                return pool;
            });

    private static final ThreadLocal<byte[]> STRING_BUFFER =
            ThreadLocal.withInitial(() -> new byte[256]);

    public TypedSerializer(Class<T> targetClass) {
        this.targetClass = targetClass;
        this.schema = buildSchema(targetClass);
        this.estimatedSize = computeEstimatedSize(schema);

        try {
            this.composedWriter = buildComposedWriter(schema);
            this.composedReader = buildComposedReader(schema);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to build composed MethodHandles for " + targetClass.getName(), e);
        }
    }

    private int computeEstimatedSize(SerializationSchema schema) {
        int estimate = 10;
        for (FieldSchema field : schema.fields) {
            estimate += switch (field.type) {
                case INT, FLOAT -> 6;
                case LONG, DOUBLE -> 10;
                case BOOLEAN, SHORT -> 4;
                case STRING -> 32;
                case LIST -> 64;
                case MAP -> 128;
                case OBJECT -> 64;
            };
        }
        return estimate;
    }

    @Override
    public <U> byte[] serialize(U obj, Class<U> type) throws Throwable {
        return serialize(targetClass.cast(obj));
    }

    public byte[] serialize(T obj) throws Throwable {
        if (obj == null) return new byte[]{TYPE_NULL};

        FastByteWriter writer = WRITER_POOL.get();
        writer.reset(estimatedSize * 2);
        writer.writeByte(TYPE_OBJECT_PACKED);
        writer.writeByte((byte) schema.fields.length);

        // Single monomorphic call - JIT can inline completely
        composedWriter.invokeExact(writer, obj, this);

        return writer.toByteArray();
    }

    @Override
    public <U> U deserialize(byte[] data, Class<U> type) throws Throwable {
        return type.cast(deserialize(data));
    }

    @SuppressWarnings("unchecked")
    public T deserialize(byte[] data) throws Throwable {
        if (data == null || data.length == 0) return null;

        FastByteReader reader = READER_POOL.get();
        reader.setData(data);

        byte typeMarker = reader.readByte();
        if (typeMarker == TYPE_NULL) return null;

        int fieldCount = reader.readByte() & 0xFF;
        Object instance = schema.constructor.newInstance();

        // Single monomorphic call - JIT can inline completely
        composedReader.invokeExact(reader, instance, this);

        return (T) instance;
    }

    // ==================== COMPOSED METHODHANDLE BUILDING ====================

    /**
     * Builds a single composed MethodHandle that writes all fields sequentially.
     * Signature: (FastByteWriter, Object, TypedSerializer) -> void
     */
    private MethodHandle buildComposedWriter(SerializationSchema schema) throws Throwable {
        MethodType targetType = MethodType.methodType(void.class, FastByteWriter.class, Object.class, TypedSerializer.class);

        if (schema.fields.length == 0) {
            return MethodHandles.empty(targetType);
        }

        // Get the sequential runner
        MethodHandle runner = LOOKUP.findStatic(
                TypedSerializer.class,
                "runWriterSequence",
                MethodType.methodType(void.class, MethodHandle[].class, FastByteWriter.class, Object.class, TypedSerializer.class)
        );

        // Build individual field writers
        MethodHandle[] fieldWriters = new MethodHandle[schema.fields.length];
        for (int i = 0; i < schema.fields.length; i++) {
            fieldWriters[i] = createFieldWriterHandle(schema.fields[i]);
        }

        // Bind the array of writers
        return MethodHandles.insertArguments(runner, 0, (Object) fieldWriters);
    }

    /**
     * Builds a single composed MethodHandle that reads all fields sequentially.
     * Signature: (FastByteReader, Object, TypedSerializer) -> void
     */
    private MethodHandle buildComposedReader(SerializationSchema schema) throws Throwable {
        MethodType targetType = MethodType.methodType(void.class, FastByteReader.class, Object.class, TypedSerializer.class);

        if (schema.fields.length == 0) {
            return MethodHandles.empty(targetType);
        }

        // Get the sequential runner
        MethodHandle runner = LOOKUP.findStatic(
                TypedSerializer.class,
                "runReaderSequence",
                MethodType.methodType(void.class, MethodHandle[].class, FastByteReader.class, Object.class, TypedSerializer.class)
        );

        // Build individual field readers
        MethodHandle[] fieldReaders = new MethodHandle[schema.fields.length];
        for (int i = 0; i < schema.fields.length; i++) {
            fieldReaders[i] = createFieldReaderHandle(schema.fields[i]);
        }

        // Bind the array of readers
        return MethodHandles.insertArguments(runner, 0, (Object) fieldReaders);
    }

    /**
     * Executes all writer handles in sequence - this method can be inlined by JIT
     */
    private static void runWriterSequence(MethodHandle[] writers, FastByteWriter w, Object obj, TypedSerializer<?> ser) throws Throwable {
        for (int i = 0; i < writers.length; i++) {
            writers[i].invokeExact(w, obj, ser);
        }
    }

    /**
     * Executes all reader handles in sequence - this method can be inlined by JIT
     */
    private static void runReaderSequence(MethodHandle[] readers, FastByteReader r, Object obj, TypedSerializer<?> ser) throws Throwable {
        for (int i = 0; i < readers.length; i++) {
            readers[i].invokeExact(r, obj, ser);
        }
    }

    /**
     * Creates a MethodHandle for writing a single field.
     * Signature: (FastByteWriter, Object, TypedSerializer) -> void
     */
    private MethodHandle createFieldWriterHandle(FieldSchema f) throws Throwable {
        String methodName = switch (f.type) {
            case INT -> "writeFieldInt";
            case LONG -> "writeFieldLong";
            case DOUBLE -> "writeFieldDouble";
            case FLOAT -> "writeFieldFloat";
            case SHORT -> "writeFieldShort";
            case BOOLEAN -> "writeFieldBoolean";
            case STRING -> "writeFieldString";
            case LIST -> "writeFieldList";
            case MAP -> "writeFieldMap";
            case OBJECT -> "writeFieldObject";
        };

        MethodHandle writer = LOOKUP.findStatic(
                TypedSerializer.class,
                methodName,
                MethodType.methodType(void.class, FastByteWriter.class, Object.class, TypedSerializer.class, FieldSchema.class)
        );

        // Bind the FieldSchema so we get: (FastByteWriter, Object, TypedSerializer) -> void
        return MethodHandles.insertArguments(writer, 3, f);
    }

    /**
     * Creates a MethodHandle for reading a single field.
     * Signature: (FastByteReader, Object, TypedSerializer) -> void
     */
    private MethodHandle createFieldReaderHandle(FieldSchema f) throws Throwable {
        String methodName = switch (f.type) {
            case INT -> "readFieldInt";
            case LONG -> "readFieldLong";
            case DOUBLE -> "readFieldDouble";
            case FLOAT -> "readFieldFloat";
            case SHORT -> "readFieldShort";
            case BOOLEAN -> "readFieldBoolean";
            case STRING -> "readFieldString";
            case LIST -> "readFieldList";
            case MAP -> "readFieldMap";
            case OBJECT -> "readFieldObject";
        };

        MethodHandle reader = LOOKUP.findStatic(
                TypedSerializer.class,
                methodName,
                MethodType.methodType(void.class, FastByteReader.class, Object.class, TypedSerializer.class, FieldSchema.class)
        );

        return MethodHandles.insertArguments(reader, 3, f);
    }

    // ==================== STATIC FIELD WRITER METHODS ====================

    private static void writeFieldInt(FastByteWriter w, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        w.writeInt(f.field.getInt(obj));
    }

    private static void writeFieldLong(FastByteWriter w, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        w.writeLong(f.field.getLong(obj));
    }

    private static void writeFieldDouble(FastByteWriter w, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        w.writeDouble(f.field.getDouble(obj));
    }

    private static void writeFieldFloat(FastByteWriter w, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        w.writeFloat(f.field.getFloat(obj));
    }

    private static void writeFieldShort(FastByteWriter w, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        w.writeShort(f.field.getShort(obj));
    }

    private static void writeFieldBoolean(FastByteWriter w, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        w.writeBoolean(f.field.getBoolean(obj));
    }

    private static void writeFieldString(FastByteWriter w, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        String v = (String) f.field.get(obj);
        if (v == null) {
            w.writeByte(0);
            return;
        }
        w.writeByte(1);
        ser.writeStr(w, v);
    }

    private static void writeFieldList(FastByteWriter w, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        List<?> v = (List<?>) f.field.get(obj);
        if (v == null) {
            w.writeByte(0);
            return;
        }
        w.writeByte(1);
        ser.writeList(w, v, f.listElementType, f.elementSchema);
    }

    private static void writeFieldMap(FastByteWriter w, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        Map<?, ?> v = (Map<?, ?>) f.field.get(obj);
        if (v == null) {
            w.writeByte(0);
            return;
        }
        w.writeByte(1);
        ser.writeMap(w, v, f.mapKeyType, f.mapValueType, f.mapKeySchema, f.mapValueSchema);
    }

    private static void writeFieldObject(FastByteWriter w, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        Object v = f.field.get(obj);
        if (v == null) {
            w.writeByte(0);
            return;
        }
        w.writeByte(1);
        ser.writeNested(w, v, f.nestedSchema);
    }

    // ==================== STATIC FIELD READER METHODS ====================

    private static void readFieldInt(FastByteReader r, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        f.field.setInt(obj, r.readInt());
    }

    private static void readFieldLong(FastByteReader r, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        f.field.setLong(obj, r.readLong());
    }

    private static void readFieldDouble(FastByteReader r, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        f.field.setDouble(obj, r.readDouble());
    }

    private static void readFieldFloat(FastByteReader r, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        f.field.setFloat(obj, r.readFloat());
    }

    private static void readFieldShort(FastByteReader r, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        f.field.setShort(obj, r.readShort());
    }

    private static void readFieldBoolean(FastByteReader r, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        f.field.setBoolean(obj, r.readBoolean());
    }

    private static void readFieldString(FastByteReader r, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        byte marker = r.readByte();
        if (marker == 0) return;
        f.field.set(obj, ser.readStr(r));
    }

    private static void readFieldList(FastByteReader r, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        byte marker = r.readByte();
        if (marker == 0) return;
        f.field.set(obj, ser.readList(r, f.listElementType, f.elementSchema));
    }

    private static void readFieldMap(FastByteReader r, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        byte marker = r.readByte();
        if (marker == 0) return;
        f.field.set(obj, ser.readMap(r, f.mapKeyType, f.mapValueType, f.mapKeySchema, f.mapValueSchema));
    }

    private static void readFieldObject(FastByteReader r, Object obj, TypedSerializer<?> ser, FieldSchema f) throws Throwable {
        byte marker = r.readByte();
        if (marker == 0) return;
        f.field.set(obj, ser.readNested(r, f.nestedSchema));
    }

    // ==================== SCHEMA BUILDING ====================

    private SerializationSchema buildSchema(Class<?> clazz) {
        Map<Class<?>, SerializationSchema> cache = new HashMap<>();
        return buildSchemaRecursive(clazz, cache);
    }

    private SerializationSchema buildSchemaRecursive(Class<?> clazz, Map<Class<?>, SerializationSchema> cache) {
        if (cache.containsKey(clazz)) return cache.get(clazz);

        SerializationSchema schema = new SerializationSchema(clazz, new FieldSchema[0]);
        cache.put(clazz, schema);

        Field[] fields = clazz.getDeclaredFields();
        List<FieldSchema> fieldSchemas = new ArrayList<>();

        for (Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    java.lang.reflect.Modifier.isTransient(field.getModifiers())) continue;

            field.setAccessible(true);
            fieldSchemas.add(analyzeField(field, cache));
        }

        schema.fields = fieldSchemas.toArray(new FieldSchema[0]);
        return schema;
    }

    private FieldSchema analyzeField(Field field, Map<Class<?>, SerializationSchema> cache) {
        Class<?> fieldType = field.getType();

        if (fieldType == int.class || fieldType == Integer.class) return new FieldSchema(field, FieldType.INT, null, null, null);
        if (fieldType == long.class || fieldType == Long.class) return new FieldSchema(field, FieldType.LONG, null, null, null);
        if (fieldType == boolean.class || fieldType == Boolean.class) return new FieldSchema(field, FieldType.BOOLEAN, null, null, null);
        if (fieldType == double.class || fieldType == Double.class) return new FieldSchema(field, FieldType.DOUBLE, null, null, null);
        if (fieldType == float.class || fieldType == Float.class) return new FieldSchema(field, FieldType.FLOAT, null, null, null);
        if (fieldType == short.class || fieldType == Short.class) return new FieldSchema(field, FieldType.SHORT, null, null, null);
        if (fieldType == String.class) return new FieldSchema(field, FieldType.STRING, null, null, null);

        if (List.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            Class<?> elementType = extractGenericType(genericType, 0);
            FieldType listElementType = determineFieldType(elementType);
            SerializationSchema elementSchema = listElementType == FieldType.OBJECT ? buildSchemaRecursive(elementType, cache) : null;
            return new FieldSchema(field, FieldType.LIST, listElementType, null, elementSchema);
        }

        if (Map.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            Class<?> keyType = extractGenericType(genericType, 0);
            Class<?> valueType = extractGenericType(genericType, 1);
            FieldType mapKeyType = determineFieldType(keyType);
            FieldType mapValueType = determineFieldType(valueType);
            SerializationSchema keySchema = mapKeyType == FieldType.OBJECT ? buildSchemaRecursive(keyType, cache) : null;
            SerializationSchema valueSchema = mapValueType == FieldType.OBJECT ? buildSchemaRecursive(valueType, cache) : null;
            return new FieldSchema(field, FieldType.MAP, mapKeyType, mapValueType, null).withMapSchemas(keySchema, valueSchema);
        }

        return new FieldSchema(field, FieldType.OBJECT, null, null, buildSchemaRecursive(fieldType, cache));
    }

    private Class<?> extractGenericType(Type genericType, int index) {
        if (genericType instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            if (typeArgs.length > index && typeArgs[index] instanceof Class) {
                return (Class<?>) typeArgs[index];
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
        if (clazz == String.class) return FieldType.STRING;
        return FieldType.OBJECT;
    }

    // ==================== HELPER METHODS ====================

    private void writeStr(FastByteWriter w, String s) {
        int maxBytes = s.length() * 3;
        if (maxBytes <= 255) {
            byte[] buf = STRING_BUFFER.get();
            if (buf.length < maxBytes) {
                buf = new byte[maxBytes];
                STRING_BUFFER.set(buf);
            }
            int len = encodeUTF8(s, buf);
            w.writeByte((byte) len);
            w.writeBytes(buf, len);
        } else {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            w.writeByte((byte) 255);
            w.writeInt(b.length);
            w.writeBytes(b, b.length);
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

    private String readStr(FastByteReader r) {
        int len = r.readByte() & 0xFF;
        if (len == 255) {
            len = r.readInt();
        }
        byte[] b = new byte[len];
        r.readFully(b, 0, len);
        return new String(b, StandardCharsets.UTF_8);
    }

    private void writeNested(FastByteWriter w, Object v, SerializationSchema schema) throws Throwable {
        ArrayDeque<FastByteWriter> pool = NESTED_WRITER_POOL.get();
        FastByteWriter nw = pool.poll();
        if (nw == null) nw = new FastByteWriter();

        try {
            nw.reset(256);
            nw.writeByte((byte) schema.fields.length);
            writeObjectWithSchema(nw, v, schema);
            int len = nw.getPosition();
            w.writeByte((byte) len);
            w.writeBytes(nw.getBuffer(), len);
        } finally {
            if (pool.size() < 8) pool.push(nw);
        }
    }

    private void writeObjectWithSchema(FastByteWriter w, Object obj, SerializationSchema schema) throws Throwable {
        for (FieldSchema f : schema.fields) {
            switch (f.type) {
                case INT -> w.writeInt(f.field.getInt(obj));
                case LONG -> w.writeLong(f.field.getLong(obj));
                case DOUBLE -> w.writeDouble(f.field.getDouble(obj));
                case FLOAT -> w.writeFloat(f.field.getFloat(obj));
                case SHORT -> w.writeShort(f.field.getShort(obj));
                case BOOLEAN -> w.writeBoolean(f.field.getBoolean(obj));
                case STRING -> {
                    String v = (String) f.field.get(obj);
                    if (v == null) { w.writeByte(0); } else { w.writeByte(1); writeStr(w, v); }
                }
                case LIST -> {
                    List<?> v = (List<?>) f.field.get(obj);
                    if (v == null) { w.writeByte(0); } else { w.writeByte(1); writeList(w, v, f.listElementType, f.elementSchema); }
                }
                case MAP -> {
                    Map<?, ?> v = (Map<?, ?>) f.field.get(obj);
                    if (v == null) { w.writeByte(0); } else { w.writeByte(1); writeMap(w, v, f.mapKeyType, f.mapValueType, f.mapKeySchema, f.mapValueSchema); }
                }
                case OBJECT -> {
                    Object v = f.field.get(obj);
                    if (v == null) { w.writeByte(0); } else { w.writeByte(1); writeNested(w, v, f.nestedSchema); }
                }
            }
        }
    }

    private Object readNested(FastByteReader r, SerializationSchema schema) throws Throwable {
        int len = r.readByte() & 0xFF;
        byte[] b = new byte[len];
        r.readFully(b, 0, len);

        ArrayDeque<FastByteReader> pool = NESTED_READER_POOL.get();
        FastByteReader nr = pool.poll();
        if (nr == null) nr = new FastByteReader();

        try {
            nr.setData(b);
            return readObjectWithSchema(nr, schema);
        } finally {
            if (pool.size() < 8) pool.push(nr);
        }
    }

    private Object readObjectWithSchema(FastByteReader r, SerializationSchema schema) throws Throwable {
        Object instance = schema.constructor.newInstance();
        int fieldCount = r.readByte() & 0xFF;

        for (int i = 0; i < fieldCount && i < schema.fields.length; i++) {
            FieldSchema f = schema.fields[i];
            switch (f.type) {
                case INT -> f.field.setInt(instance, r.readInt());
                case LONG -> f.field.setLong(instance, r.readLong());
                case DOUBLE -> f.field.setDouble(instance, r.readDouble());
                case FLOAT -> f.field.setFloat(instance, r.readFloat());
                case SHORT -> f.field.setShort(instance, r.readShort());
                case BOOLEAN -> f.field.setBoolean(instance, r.readBoolean());
                case STRING -> { if (r.readByte() != 0) f.field.set(instance, readStr(r)); }
                case LIST -> { if (r.readByte() != 0) f.field.set(instance, readList(r, f.listElementType, f.elementSchema)); }
                case MAP -> { if (r.readByte() != 0) f.field.set(instance, readMap(r, f.mapKeyType, f.mapValueType, f.mapKeySchema, f.mapValueSchema)); }
                case OBJECT -> { if (r.readByte() != 0) f.field.set(instance, readNested(r, f.nestedSchema)); }
            }
        }
        return instance;
    }

    private void writeList(FastByteWriter w, List<?> list, FieldType elemType, SerializationSchema elemSchema) throws Throwable {
        int size = list.size();
        w.writeInt(size);
        if (size == 0) return;

        switch (elemType) {
            case INT -> { for (int i = 0; i < size; i++) w.writeInt((Integer) list.get(i)); }
            case LONG -> { for (int i = 0; i < size; i++) w.writeLong((Long) list.get(i)); }
            case STRING -> { for (int i = 0; i < size; i++) writeStr(w, (String) list.get(i)); }
            case BOOLEAN -> { for (int i = 0; i < size; i++) w.writeBoolean((Boolean) list.get(i)); }
            case DOUBLE -> { for (int i = 0; i < size; i++) w.writeDouble((Double) list.get(i)); }
            case FLOAT -> { for (int i = 0; i < size; i++) w.writeFloat((Float) list.get(i)); }
            case SHORT -> { for (int i = 0; i < size; i++) w.writeShort((Short) list.get(i)); }
            case OBJECT -> { for (int i = 0; i < size; i++) writeNested(w, list.get(i), elemSchema); }
            default -> {}
        }
    }

    private List<Object> readList(FastByteReader r, FieldType elemType, SerializationSchema elemSchema) throws Throwable {
        int size = r.readInt();
        List<Object> list = new ArrayList<>(size);
        if (size == 0) return list;

        switch (elemType) {
            case INT -> { for (int i = 0; i < size; i++) list.add(r.readInt()); }
            case LONG -> { for (int i = 0; i < size; i++) list.add(r.readLong()); }
            case STRING -> { for (int i = 0; i < size; i++) list.add(readStr(r)); }
            case BOOLEAN -> { for (int i = 0; i < size; i++) list.add(r.readBoolean()); }
            case DOUBLE -> { for (int i = 0; i < size; i++) list.add(r.readDouble()); }
            case FLOAT -> { for (int i = 0; i < size; i++) list.add(r.readFloat()); }
            case SHORT -> { for (int i = 0; i < size; i++) list.add(r.readShort()); }
            case OBJECT -> { for (int i = 0; i < size; i++) list.add(readNested(r, elemSchema)); }
            default -> {}
        }
        return list;
    }

    private void writeMap(FastByteWriter w, Map<?, ?> map, FieldType keyType, FieldType valType,
                          SerializationSchema keySchema, SerializationSchema valSchema) throws Throwable {
        int size = map.size();
        w.writeInt(size);
        if (size == 0) return;

        if (keyType == FieldType.STRING && valType == FieldType.INT) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                writeStr(w, (String) e.getKey());
                w.writeInt((Integer) e.getValue());
            }
            return;
        }
        if (keyType == FieldType.STRING && valType == FieldType.STRING) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                writeStr(w, (String) e.getKey());
                writeStr(w, (String) e.getValue());
            }
            return;
        }

        for (Map.Entry<?, ?> e : map.entrySet()) {
            writeElem(w, e.getKey(), keyType, keySchema);
            writeElem(w, e.getValue(), valType, valSchema);
        }
    }

    private Map<Object, Object> readMap(FastByteReader r, FieldType keyType, FieldType valType,
                                         SerializationSchema keySchema, SerializationSchema valSchema) throws Throwable {
        int size = r.readInt();
        Map<Object, Object> map = new HashMap<>(size);
        if (size == 0) return map;

        if (keyType == FieldType.STRING && valType == FieldType.INT) {
            for (int i = 0; i < size; i++) map.put(readStr(r), r.readInt());
            return map;
        }
        if (keyType == FieldType.STRING && valType == FieldType.STRING) {
            for (int i = 0; i < size; i++) map.put(readStr(r), readStr(r));
            return map;
        }

        for (int i = 0; i < size; i++) {
            map.put(readElem(r, keyType, keySchema), readElem(r, valType, valSchema));
        }
        return map;
    }

    private void writeElem(FastByteWriter w, Object v, FieldType t, SerializationSchema s) throws Throwable {
        switch (t) {
            case INT -> w.writeInt((Integer) v);
            case LONG -> w.writeLong((Long) v);
            case BOOLEAN -> w.writeBoolean((Boolean) v);
            case DOUBLE -> w.writeDouble((Double) v);
            case FLOAT -> w.writeFloat((Float) v);
            case SHORT -> w.writeShort((Short) v);
            case STRING -> writeStr(w, (String) v);
            case OBJECT -> writeNested(w, v, s);
            default -> {}
        }
    }

    private Object readElem(FastByteReader r, FieldType t, SerializationSchema s) throws Throwable {
        return switch (t) {
            case INT -> r.readInt();
            case LONG -> r.readLong();
            case BOOLEAN -> r.readBoolean();
            case DOUBLE -> r.readDouble();
            case FLOAT -> r.readFloat();
            case SHORT -> r.readShort();
            case STRING -> readStr(r);
            case OBJECT -> readNested(r, s);
            default -> null;
        };
    }

    // ==================== SCHEMA CLASSES ====================

    private static class SerializationSchema {
        final Class<?> clazz;
        final Constructor<?> constructor;
        FieldSchema[] fields;

        SerializationSchema(Class<?> clazz, FieldSchema[] fields) {
            this.clazz = clazz;
            this.fields = fields;

            try {
                this.constructor = clazz.getDeclaredConstructor();
                this.constructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("No default constructor for " + clazz.getName(), e);
            }
        }
    }

    private static class FieldSchema {
        final Field field;
        final FieldType type;
        final FieldType listElementType;
        final FieldType mapKeyType;
        final FieldType mapValueType;
        final SerializationSchema nestedSchema;
        final SerializationSchema elementSchema;
        SerializationSchema mapKeySchema;
        SerializationSchema mapValueSchema;

        FieldSchema(Field field, FieldType type, FieldType listElementType, FieldType mapValueType, SerializationSchema nestedSchema) {
            this.field = field;
            this.type = type;
            this.listElementType = listElementType;
            this.mapKeyType = listElementType;
            this.mapValueType = mapValueType;
            this.nestedSchema = nestedSchema;
            this.elementSchema = nestedSchema;
        }

        FieldSchema withMapSchemas(SerializationSchema keySchema, SerializationSchema valueSchema) {
            this.mapKeySchema = keySchema;
            this.mapValueSchema = valueSchema;
            return this;
        }
    }

    private enum FieldType { INT, LONG, BOOLEAN, DOUBLE, FLOAT, SHORT, STRING, LIST, MAP, OBJECT }
}
