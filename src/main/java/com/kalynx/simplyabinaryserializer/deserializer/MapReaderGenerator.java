package com.kalynx.simplyabinaryserializer.deserializer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import static com.kalynx.simplyabinaryserializer.serializer.SerializerConstants.*;

/**
 * Generates optimized bytecode for reading Maps with primitive type keys and values.
 * Creates specialized reader classes for each Map<K,V> combination at runtime.
 */
public class MapReaderGenerator {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final ClassDesc CD_Map = ClassDesc.of("java.util.Map");
    private static final ClassDesc CD_HashMap = ClassDesc.of("java.util.HashMap");

    @FunctionalInterface
    public interface MapReader {
        Map<?, ?> readMap(FastByteReader reader);
    }

    /**
     * Generates an optimized reader for a specific Map<K,V> type.
     *
     * @param keyType Key type (e.g., String.class, Integer.class, int.class)
     * @param valueType Value type
     * @return A generated reader instance
     * @throws Throwable if bytecode generation fails
     */
    @SuppressWarnings("unchecked")
    public MapReader generateMapReader(Class<?> keyType, Class<?> valueType) throws Throwable {
        String className = "com.kalynx.simplyabinaryserializer.deserializer.GeneratedMapReader$" +
                           getTypeName(keyType) + "_" + getTypeName(valueType) + "$" + CLASS_COUNTER.incrementAndGet();

        byte[] classBytes = ClassFile.of().build(
                ClassDesc.of(className),
                classBuilder -> {
                    classBuilder.withFlags(ACC_PUBLIC_FINAL_SYNTHETIC);
                    classBuilder.withInterfaceSymbols(ClassDesc.of("com.kalynx.simplyabinaryserializer.deserializer.MapReaderGenerator$MapReader"));

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

                    // readMap(FastByteReader reader) method
                    classBuilder.withMethodBody(
                            "readMap",
                            MethodTypeDesc.of(CD_Map, CD_FastByteReader),
                            ACC_PUBLIC,
                            codeBuilder -> generateReadMapMethod(codeBuilder, keyType, valueType)
                    );
                }
        );

        Class<?> generatedClass = LOOKUP.defineClass(classBytes);
        return (MapReader) generatedClass.getDeclaredConstructor().newInstance();
    }

    private void generateReadMapMethod(CodeBuilder cb, Class<?> keyType, Class<?> valueType) {
        Label nullLabel = cb.newLabel();
        Label notNullLabel = cb.newLabel();
        Label emptyLabel = cb.newLabel();
        Label notEmptyLabel = cb.newLabel();
        Label loopStart = cb.newLabel();
        Label loopCheck = cb.newLabel();
        Label loopEnd = cb.newLabel();

        // int size = reader.readInt();
        cb.aload(1); // reader
        cb.invokevirtual(CD_FastByteReader, "readInt", MTD_int);
        cb.istore(2); // size

        // if (size == -1) return null;
        cb.iload(2); // size
        cb.iconst_m1();
        cb.if_icmpeq(nullLabel);
        cb.goto_(notNullLabel);

        cb.labelBinding(nullLabel);
        cb.aconst_null();
        cb.areturn();

        cb.labelBinding(notNullLabel);

        // if (size == 0) return new HashMap<>();
        cb.iload(2); // size
        cb.ifne(notEmptyLabel);

        cb.labelBinding(emptyLabel);
        cb.new_(CD_HashMap);
        cb.dup();
        cb.invokespecial(CD_HashMap, ConstantDescs.INIT_NAME, MethodTypeDesc.of(ConstantDescs.CD_void));
        cb.areturn();

        cb.labelBinding(notEmptyLabel);

        // HashMap map = new HashMap<>((int)(size / 0.75f) + 1);
        cb.new_(CD_HashMap);
        cb.dup();
        cb.iload(2); // size
        cb.i2f();
        cb.ldc(0.75f);
        cb.fdiv();
        cb.f2i();
        cb.iconst_1();
        cb.iadd();
        cb.invokespecial(CD_HashMap, ConstantDescs.INIT_NAME, MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
        cb.astore(3); // map

        // int i = 0;
        cb.iconst_0();
        cb.istore(4); // i

        // goto loopCheck;
        cb.goto_(loopCheck);

        // Loop body
        cb.labelBinding(loopStart);

        // Read key
        generateKeyRead(cb, keyType);
        cb.astore(5); // key (boxed)

        // Read value
        generateValueRead(cb, valueType);
        cb.astore(6); // value (boxed)

        // map.put(key, value);
        cb.aload(3); // map
        cb.aload(5); // key
        cb.aload(6); // value
        cb.invokeinterface(CD_Map, "put",
                MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object, ConstantDescs.CD_Object));
        cb.pop(); // discard return value

        // i++;
        cb.iinc(4, 1);

        // Loop check: if (i < size) goto loopStart;
        cb.labelBinding(loopCheck);
        cb.iload(4); // i
        cb.iload(2); // size
        cb.if_icmplt(loopStart);

        cb.labelBinding(loopEnd);

        // return map;
        cb.aload(3); // map
        cb.areturn();
    }

    private void generateKeyRead(CodeBuilder cb, Class<?> keyType) {
        cb.aload(1); // reader

        if (keyType == int.class || keyType == Integer.class) {
            cb.invokevirtual(CD_FastByteReader, "readInt", MTD_int);
            cb.invokestatic(ConstantDescs.CD_Integer, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Integer, ConstantDescs.CD_int));
        } else if (keyType == long.class || keyType == Long.class) {
            cb.invokevirtual(CD_FastByteReader, "readLong", MTD_long);
            cb.invokestatic(ConstantDescs.CD_Long, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Long, ConstantDescs.CD_long));
        } else if (keyType == String.class) {
            // Read short for length, then readBytes, convert to String
            // short len = reader.readShort();
            cb.invokevirtual(CD_FastByteReader, "readShort", MTD_short);
            // byte[] bytes = reader.readBytes(len);
            cb.aload(1); // reader
            cb.swap();
            cb.invokevirtual(CD_FastByteReader, "readBytes", MTD_byte_array_int);
            // new String(bytes, UTF_8)
            cb.new_(ConstantDescs.CD_String);
            cb.dup_x1();
            cb.swap();
            cb.getstatic(CD_StandardCharsets, "UTF_8", CD_Charset);
            cb.invokespecial(ConstantDescs.CD_String, ConstantDescs.INIT_NAME,
                    MethodTypeDesc.of(ConstantDescs.CD_void, CD_byte_array, CD_Charset));
        } else if (keyType == double.class || keyType == Double.class) {
            cb.invokevirtual(CD_FastByteReader, "readDouble", MTD_double);
            cb.invokestatic(ConstantDescs.CD_Double, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Double, ConstantDescs.CD_double));
        } else if (keyType == float.class || keyType == Float.class) {
            cb.invokevirtual(CD_FastByteReader, "readFloat", MTD_float);
            cb.invokestatic(ConstantDescs.CD_Float, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Float, ConstantDescs.CD_float));
        } else if (keyType == short.class || keyType == Short.class) {
            cb.invokevirtual(CD_FastByteReader, "readShort", MTD_short);
            cb.invokestatic(ConstantDescs.CD_Short, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Short, ConstantDescs.CD_short));
        } else if (keyType == byte.class || keyType == Byte.class) {
            cb.invokevirtual(CD_FastByteReader, "readByte", MTD_byte);
            cb.invokestatic(ConstantDescs.CD_Byte, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Byte, ConstantDescs.CD_byte));
        } else if (keyType == boolean.class || keyType == Boolean.class) {
            cb.invokevirtual(CD_FastByteReader, "readBoolean", MTD_boolean);
            cb.invokestatic(ConstantDescs.CD_Boolean, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Boolean, ConstantDescs.CD_boolean));
        } else if (keyType == char.class || keyType == Character.class) {
            cb.invokevirtual(CD_FastByteReader, "readShort", MTD_short);
            cb.i2c(); // short to char
            cb.invokestatic(ClassDesc.of("java.lang.Character"), "valueOf",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.Character"), ConstantDescs.CD_char));
        }
    }

    private void generateValueRead(CodeBuilder cb, Class<?> valueType) {
        cb.aload(1); // reader

        if (valueType == int.class || valueType == Integer.class) {
            cb.invokevirtual(CD_FastByteReader, "readInt", MTD_int);
            cb.invokestatic(ConstantDescs.CD_Integer, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Integer, ConstantDescs.CD_int));
        } else if (valueType == long.class || valueType == Long.class) {
            cb.invokevirtual(CD_FastByteReader, "readLong", MTD_long);
            cb.invokestatic(ConstantDescs.CD_Long, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Long, ConstantDescs.CD_long));
        } else if (valueType == String.class) {
            // Read short for length, then readBytes, convert to String
            // short len = reader.readShort();
            cb.invokevirtual(CD_FastByteReader, "readShort", MTD_short);
            // byte[] bytes = reader.readBytes(len);
            cb.aload(1); // reader
            cb.swap();
            cb.invokevirtual(CD_FastByteReader, "readBytes", MTD_byte_array_int);
            // new String(bytes, UTF_8)
            cb.new_(ConstantDescs.CD_String);
            cb.dup_x1();
            cb.swap();
            cb.getstatic(CD_StandardCharsets, "UTF_8", CD_Charset);
            cb.invokespecial(ConstantDescs.CD_String, ConstantDescs.INIT_NAME,
                    MethodTypeDesc.of(ConstantDescs.CD_void, CD_byte_array, CD_Charset));
        } else if (valueType == double.class || valueType == Double.class) {
            cb.invokevirtual(CD_FastByteReader, "readDouble", MTD_double);
            cb.invokestatic(ConstantDescs.CD_Double, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Double, ConstantDescs.CD_double));
        } else if (valueType == float.class || valueType == Float.class) {
            cb.invokevirtual(CD_FastByteReader, "readFloat", MTD_float);
            cb.invokestatic(ConstantDescs.CD_Float, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Float, ConstantDescs.CD_float));
        } else if (valueType == short.class || valueType == Short.class) {
            cb.invokevirtual(CD_FastByteReader, "readShort", MTD_short);
            cb.invokestatic(ConstantDescs.CD_Short, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Short, ConstantDescs.CD_short));
        } else if (valueType == byte.class || valueType == Byte.class) {
            cb.invokevirtual(CD_FastByteReader, "readByte", MTD_byte);
            cb.invokestatic(ConstantDescs.CD_Byte, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Byte, ConstantDescs.CD_byte));
        } else if (valueType == boolean.class || valueType == Boolean.class) {
            cb.invokevirtual(CD_FastByteReader, "readBoolean", MTD_boolean);
            cb.invokestatic(ConstantDescs.CD_Boolean, "valueOf", MethodTypeDesc.of(ConstantDescs.CD_Boolean, ConstantDescs.CD_boolean));
        } else if (valueType == char.class || valueType == Character.class) {
            cb.invokevirtual(CD_FastByteReader, "readShort", MTD_short);
            cb.i2c(); // short to char
            cb.invokestatic(ClassDesc.of("java.lang.Character"), "valueOf",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.Character"), ConstantDescs.CD_char));
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


