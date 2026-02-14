package com.kalynx.simplyabinaryserializer.serializer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import static com.kalynx.simplyabinaryserializer.serializer.SerializerConstants.*;

/**
 * Generates optimized bytecode for writing Maps with primitive type keys and values.
 * Creates specialized writer classes for each Map<K,V> combination at runtime.
 */
public class MapWriterGenerator {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final ClassDesc CD_Map = ClassDesc.of("java.util.Map");
    private static final ClassDesc CD_MapEntry = ClassDesc.of("java.util.Map$Entry");
    private static final ClassDesc CD_Iterator = ClassDesc.of("java.util.Iterator");
    private static final ClassDesc CD_Set = ClassDesc.of("java.util.Set");

    @FunctionalInterface
    public interface MapWriter {
        void writeMap(FastByteWriter writer, Map<?, ?> map);
    }

    /**
     * Generates an optimized writer for a specific Map<K,V> type.
     *
     * @param keyType Key type (e.g., String.class, Integer.class, int.class)
     * @param valueType Value type
     * @return A generated writer instance
     * @throws Throwable if bytecode generation fails
     */
    @SuppressWarnings("unchecked")
    public MapWriter generateMapWriter(Class<?> keyType, Class<?> valueType) throws Throwable {
        String className = "com.kalynx.simplyabinaryserializer.serializer.GeneratedMapWriter$" +
                           getTypeName(keyType) + "_" + getTypeName(valueType) + "$" + CLASS_COUNTER.incrementAndGet();

        byte[] classBytes = ClassFile.of().build(
                ClassDesc.of(className),
                classBuilder -> {
                    classBuilder.withFlags(ACC_PUBLIC_FINAL_SYNTHETIC);
                    classBuilder.withInterfaceSymbols(ClassDesc.of("com.kalynx.simplyabinaryserializer.serializer.MapWriterGenerator$MapWriter"));

                    // Default constructor
                    classBuilder.withMethodBody(
                            ConstantDescs.INIT_NAME,
                            MethodTypeDesc.of(ConstantDescs.CD_void),
                            ACC_PUBLIC,
                            codeBuilder -> {
                                codeBuilder.aload(0);
                                codeBuilder.invokespecial(ConstantDescs.CD_Object, ConstantDescs.INIT_NAME,
                                        MethodTypeDesc.of(ConstantDescs.CD_void));
                                codeBuilder.return_();
                            }
                    );

                    // writeMap(FastByteWriter writer, Map<?, ?> map) method
                    classBuilder.withMethodBody(
                            "writeMap",
                            MethodTypeDesc.of(ConstantDescs.CD_void, CD_FastByteWriter, CD_Map),
                            ACC_PUBLIC,
                            codeBuilder -> generateWriteMapMethod(codeBuilder, keyType, valueType)
                    );
                }
        );

        Class<?> generatedClass = LOOKUP.defineClass(classBytes);
        return (MapWriter) generatedClass.getDeclaredConstructor().newInstance();
    }

    private void generateWriteMapMethod(CodeBuilder cb, Class<?> keyType, Class<?> valueType) {
        Label nullLabel = cb.newLabel();
        Label notNullLabel = cb.newLabel();
        Label emptyLabel = cb.newLabel();
        Label notEmptyLabel = cb.newLabel();
        Label loopStart = cb.newLabel();
        Label loopCheck = cb.newLabel();

        // if (map == null) { writer.writeInt(-1); return; }
        cb.aload(2); // map
        cb.ifnonnull(notNullLabel);

        cb.labelBinding(nullLabel);
        cb.aload(1); // writer
        cb.iconst_m1();
        cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);
        cb.return_();

        cb.labelBinding(notNullLabel);

        // int size = map.size();
        cb.aload(2); // map
        cb.invokeinterface(CD_Map, "size", MethodTypeDesc.of(ConstantDescs.CD_int));
        cb.istore(3); // size

        // writer.writeInt(size);
        cb.aload(1); // writer
        cb.iload(3); // size
        cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);

        // if (size == 0) return;
        cb.iload(3); // size
        cb.ifne(notEmptyLabel);

        cb.labelBinding(emptyLabel);
        cb.return_();

        cb.labelBinding(notEmptyLabel);

        // Iterator<Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        cb.aload(2); // map
        cb.invokeinterface(CD_Map, "entrySet", MethodTypeDesc.of(CD_Set));
        cb.invokeinterface(CD_Set, "iterator", MethodTypeDesc.of(CD_Iterator));
        cb.astore(4); // iterator

        // while (iterator.hasNext())
        cb.goto_(loopCheck);

        cb.labelBinding(loopStart);

        // Map.Entry<?, ?> entry = iterator.next();
        cb.aload(4); // iterator
        cb.invokeinterface(CD_Iterator, "next", MethodTypeDesc.of(ConstantDescs.CD_Object));
        cb.checkcast(CD_MapEntry);
        cb.astore(5); // entry

        // Write key
        generateKeyWrite(cb, keyType);

        // Write value
        generateValueWrite(cb, valueType);

        // Loop check
        cb.labelBinding(loopCheck);
        cb.aload(4); // iterator
        cb.invokeinterface(CD_Iterator, "hasNext", MethodTypeDesc.of(ConstantDescs.CD_boolean));
        cb.ifne(loopStart);

        cb.return_();
    }

    private void generateKeyWrite(CodeBuilder cb, Class<?> keyType) {
        cb.aload(1); // writer
        cb.aload(5); // entry
        cb.invokeinterface(CD_MapEntry, "getKey", MethodTypeDesc.of(ConstantDescs.CD_Object));

        if (keyType == int.class || keyType == Integer.class) {
            cb.checkcast(ConstantDescs.CD_Integer);
            cb.invokevirtual(ConstantDescs.CD_Integer, "intValue", MethodTypeDesc.of(ConstantDescs.CD_int));
            cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);
        } else if (keyType == long.class || keyType == Long.class) {
            cb.checkcast(ConstantDescs.CD_Long);
            cb.invokevirtual(ConstantDescs.CD_Long, "longValue", MethodTypeDesc.of(ConstantDescs.CD_long));
            cb.invokevirtual(CD_FastByteWriter, "writeLong", MTD_void_long);
        } else if (keyType == String.class) {
            // Convert String to bytes: str.getBytes(UTF_8)
            cb.checkcast(ConstantDescs.CD_String);
            cb.getstatic(CD_StandardCharsets, "UTF_8", CD_Charset);
            cb.invokevirtual(ConstantDescs.CD_String, "getBytes",
                    MethodTypeDesc.of(CD_byte_array, CD_Charset));
            cb.invokevirtual(CD_FastByteWriter, "writeString", MTD_void_byteArray);
        } else if (keyType == double.class || keyType == Double.class) {
            cb.checkcast(ConstantDescs.CD_Double);
            cb.invokevirtual(ConstantDescs.CD_Double, "doubleValue", MethodTypeDesc.of(ConstantDescs.CD_double));
            cb.invokevirtual(CD_FastByteWriter, "writeDouble", MTD_void_double);
        } else if (keyType == float.class || keyType == Float.class) {
            cb.checkcast(ConstantDescs.CD_Float);
            cb.invokevirtual(ConstantDescs.CD_Float, "floatValue", MethodTypeDesc.of(ConstantDescs.CD_float));
            cb.invokevirtual(CD_FastByteWriter, "writeFloat", MTD_void_float);
        } else if (keyType == short.class || keyType == Short.class) {
            cb.checkcast(ConstantDescs.CD_Short);
            cb.invokevirtual(ConstantDescs.CD_Short, "shortValue", MethodTypeDesc.of(ConstantDescs.CD_short));
            cb.invokevirtual(CD_FastByteWriter, "writeShort", MTD_void_short);
        } else if (keyType == byte.class || keyType == Byte.class) {
            cb.checkcast(ConstantDescs.CD_Byte);
            cb.invokevirtual(ConstantDescs.CD_Byte, "byteValue", MethodTypeDesc.of(ConstantDescs.CD_byte));
            cb.invokevirtual(CD_FastByteWriter, "writeByte", MTD_void_int);
        } else if (keyType == boolean.class || keyType == Boolean.class) {
            cb.checkcast(ConstantDescs.CD_Boolean);
            cb.invokevirtual(ConstantDescs.CD_Boolean, "booleanValue", MethodTypeDesc.of(ConstantDescs.CD_boolean));
            cb.invokevirtual(CD_FastByteWriter, "writeBoolean", MTD_void_boolean);
        } else if (keyType == char.class || keyType == Character.class) {
            cb.checkcast(ClassDesc.of("java.lang.Character"));
            cb.invokevirtual(ClassDesc.of("java.lang.Character"), "charValue", MethodTypeDesc.of(ConstantDescs.CD_char));
            cb.i2s(); // char to short
            cb.invokevirtual(CD_FastByteWriter, "writeShort", MTD_void_short);
        }
    }

    private void generateValueWrite(CodeBuilder cb, Class<?> valueType) {
        cb.aload(1); // writer
        cb.aload(5); // entry
        cb.invokeinterface(CD_MapEntry, "getValue", MethodTypeDesc.of(ConstantDescs.CD_Object));

        if (valueType == int.class || valueType == Integer.class) {
            cb.checkcast(ConstantDescs.CD_Integer);
            cb.invokevirtual(ConstantDescs.CD_Integer, "intValue", MethodTypeDesc.of(ConstantDescs.CD_int));
            cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);
        } else if (valueType == long.class || valueType == Long.class) {
            cb.checkcast(ConstantDescs.CD_Long);
            cb.invokevirtual(ConstantDescs.CD_Long, "longValue", MethodTypeDesc.of(ConstantDescs.CD_long));
            cb.invokevirtual(CD_FastByteWriter, "writeLong", MTD_void_long);
        } else if (valueType == String.class) {
            // Convert String to bytes: str.getBytes(UTF_8)
            cb.checkcast(ConstantDescs.CD_String);
            cb.getstatic(CD_StandardCharsets, "UTF_8", CD_Charset);
            cb.invokevirtual(ConstantDescs.CD_String, "getBytes",
                    MethodTypeDesc.of(CD_byte_array, CD_Charset));
            cb.invokevirtual(CD_FastByteWriter, "writeString", MTD_void_byteArray);
        } else if (valueType == double.class || valueType == Double.class) {
            cb.checkcast(ConstantDescs.CD_Double);
            cb.invokevirtual(ConstantDescs.CD_Double, "doubleValue", MethodTypeDesc.of(ConstantDescs.CD_double));
            cb.invokevirtual(CD_FastByteWriter, "writeDouble", MTD_void_double);
        } else if (valueType == float.class || valueType == Float.class) {
            cb.checkcast(ConstantDescs.CD_Float);
            cb.invokevirtual(ConstantDescs.CD_Float, "floatValue", MethodTypeDesc.of(ConstantDescs.CD_float));
            cb.invokevirtual(CD_FastByteWriter, "writeFloat", MTD_void_float);
        } else if (valueType == short.class || valueType == Short.class) {
            cb.checkcast(ConstantDescs.CD_Short);
            cb.invokevirtual(ConstantDescs.CD_Short, "shortValue", MethodTypeDesc.of(ConstantDescs.CD_short));
            cb.invokevirtual(CD_FastByteWriter, "writeShort", MTD_void_short);
        } else if (valueType == byte.class || valueType == Byte.class) {
            cb.checkcast(ConstantDescs.CD_Byte);
            cb.invokevirtual(ConstantDescs.CD_Byte, "byteValue", MethodTypeDesc.of(ConstantDescs.CD_byte));
            cb.invokevirtual(CD_FastByteWriter, "writeByte", MTD_void_int);
        } else if (valueType == boolean.class || valueType == Boolean.class) {
            cb.checkcast(ConstantDescs.CD_Boolean);
            cb.invokevirtual(ConstantDescs.CD_Boolean, "booleanValue", MethodTypeDesc.of(ConstantDescs.CD_boolean));
            cb.invokevirtual(CD_FastByteWriter, "writeBoolean", MTD_void_boolean);
        } else if (valueType == char.class || valueType == Character.class) {
            cb.checkcast(ClassDesc.of("java.lang.Character"));
            cb.invokevirtual(ClassDesc.of("java.lang.Character"), "charValue", MethodTypeDesc.of(ConstantDescs.CD_char));
            cb.i2s(); // char to short
            cb.invokevirtual(CD_FastByteWriter, "writeShort", MTD_void_short);
        }
    }

    private String getTypeName(Class<?> type) {
        if (type == int.class || type == Integer.class) return "Int";
        if (type == long.class || type == Long.class) return "Long";
        if (type == double.class || type == Double.class) return "Double";
        if (type == float.class || type == Float.class) return "Float";
        if (type == short.class || type == Short.class) return "Short";
        if (type == byte.class || type == Byte.class) return "Byte";
        if (type == boolean.class || type == Boolean.class) return "Boolean";
        if (type == char.class || type == Character.class) return "Char";
        if (type == String.class) return "String";
        return "Object";
    }
}

