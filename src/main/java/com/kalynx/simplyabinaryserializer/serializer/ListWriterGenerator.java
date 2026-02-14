package com.kalynx.simplyabinaryserializer.serializer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static com.kalynx.simplyabinaryserializer.serializer.SerializerConstants.*;

/**
 * Generates optimized bytecode for writing List elements to FastByteWriter.
 * Uses Java ClassFile API to create specialized list writer classes at runtime,
 * eliminating ALL conditionals and method call overhead.
 */
public class ListWriterGenerator {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @FunctionalInterface
    public interface ListWriter {
        void writeList(FastByteWriter writer, List<?> list);
    }

    /**
     * Generates an optimized writer for a specific list element type.
     * Creates bytecode that directly writes elements without any conditionals.
     *
     * @param elementType The type of elements in the list
     * @return A generated writer instance
     * @throws Throwable if bytecode generation fails
     */
    @SuppressWarnings("unchecked")
    public <E> ListWriter generateListWriter(Class<E> elementType) throws Throwable {
        String className = "com.kalynx.simplyabinaryserializer.serializer.GeneratedListWriter$" +
                           elementType.getSimpleName() + "$" + CLASS_COUNTER.incrementAndGet();

        byte[] classBytes = ClassFile.of().build(
                ClassDesc.of(className),
                classBuilder -> {
                    classBuilder.withFlags(ACC_PUBLIC_FINAL_SYNTHETIC);
                    classBuilder.withInterfaceSymbols(ClassDesc.of("com.kalynx.simplyabinaryserializer.serializer.ListWriterGenerator$ListWriter"));

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

                    // writeList(FastByteWriter writer, List<?> list) method
                    classBuilder.withMethodBody(
                            "writeList",
                            MethodTypeDesc.of(ConstantDescs.CD_void, CD_FastByteWriter, CD_List),
                            ACC_PUBLIC,
                            codeBuilder -> generateWriteListMethod(codeBuilder, elementType)
                    );
                }
        );

        Class<?> generatedClass = LOOKUP.defineClass(classBytes);
        return (ListWriter) generatedClass.getDeclaredConstructor().newInstance();
    }

    private <E> void generateWriteListMethod(CodeBuilder cb, Class<E> elementType) {
        // Label for null check and empty list
        Label notNullLabel = cb.newLabel();
        Label notEmptyLabel = cb.newLabel();
        Label loopStart = cb.newLabel();
        Label loopCheck = cb.newLabel();
        Label loopEnd = cb.newLabel();

        // if (list == null) { writer.writeInt(-1); return; }
        cb.aload(2); // list
        cb.ifnonnull(notNullLabel);
        cb.aload(1); // writer
        cb.iconst_m1();
        cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);
        cb.return_();

        cb.labelBinding(notNullLabel);

        // int size = list.size();
        cb.aload(2); // list
        cb.invokeinterface(CD_List, "size", MethodTypeDesc.of(ConstantDescs.CD_int));
        cb.istore(3); // size

        // writer.writeInt(size);
        cb.aload(1); // writer
        cb.iload(3); // size
        cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);

        // if (size == 0) return;
        cb.iload(3); // size
        cb.ifne(notEmptyLabel);
        cb.return_();

        cb.labelBinding(notEmptyLabel);

        // int i = 0;
        cb.iconst_0();
        cb.istore(4); // i

        // goto loopCheck;
        cb.goto_(loopCheck);

        // Loop body
        cb.labelBinding(loopStart);

        // Object elem = list.get(i);
        cb.aload(2); // list
        cb.iload(4); // i
        cb.invokeinterface(CD_List, "get", MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_int));

        // Generate element-specific write code
        generateElementWrite(cb, elementType);

        // i++;
        cb.iinc(4, 1);

        // Loop check: if (i < size) goto loopStart;
        cb.labelBinding(loopCheck);
        cb.iload(4); // i
        cb.iload(3); // size
        cb.if_icmplt(loopStart);

        cb.labelBinding(loopEnd);
        cb.return_();
    }

    private <E> void generateElementWrite(CodeBuilder cb, Class<E> elementType) {
        Label notNullLabel = cb.newLabel();
        Label endLabel = cb.newLabel();

        // elem is on stack
        cb.dup(); // duplicate for null check

        // if (elem == null)
        cb.ifnonnull(notNullLabel);

        // Null case
        cb.pop(); // remove the null
        cb.aload(1); // writer

        if (elementType == String.class) {
            // writer.writeInt(-1);
            cb.iconst_m1();
            cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);
        } else {
            // writer.writeBoolean(false);
            cb.iconst_0();
            cb.invokevirtual(CD_FastByteWriter, "writeBoolean", MTD_void_boolean);
        }
        cb.goto_(endLabel);

        cb.labelBinding(notNullLabel);

        // Not null case - write marker and value
        if (elementType == String.class) {
            // String str = (String) elem;
            cb.checkcast(ConstantDescs.CD_String);
            cb.astore(5); // str

            // byte[] bytes = str.getBytes(UTF_8);
            cb.aload(5);
            cb.getstatic(CD_StandardCharsets, "UTF_8", CD_Charset);
            cb.invokevirtual(ConstantDescs.CD_String, "getBytes",
                    MethodTypeDesc.of(CD_byte_array, CD_Charset));
            cb.astore(6); // bytes

            // writer.writeInt(bytes.length);
            cb.aload(1); // writer
            cb.aload(6); // bytes
            cb.arraylength();
            cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);

            // writer.writeBytes(bytes, bytes.length);
            cb.aload(1); // writer
            cb.aload(6); // bytes
            cb.aload(6); // bytes
            cb.arraylength();
            cb.invokevirtual(CD_FastByteWriter, "writeBytes",
                    MethodTypeDesc.of(ConstantDescs.CD_void, CD_byte_array, ConstantDescs.CD_int));
        } else {
            // writer.writeBoolean(true);
            cb.aload(1); // writer
            cb.iconst_1();
            cb.invokevirtual(CD_FastByteWriter, "writeBoolean", MTD_void_boolean);

            // Cast and unbox to primitive, then write
            if (elementType == Integer.class || elementType == int.class) {
                cb.checkcast(CD_Integer);
                cb.invokevirtual(CD_Integer, "intValue", MethodTypeDesc.of(ConstantDescs.CD_int));
                cb.aload(1); // writer
                cb.swap();
                cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);
            } else if (elementType == Long.class || elementType == long.class) {
                cb.checkcast(CD_Long);
                cb.invokevirtual(CD_Long, "longValue", MethodTypeDesc.of(ConstantDescs.CD_long));
                cb.aload(1); // writer
                cb.dup_x2();
                cb.pop();
                cb.invokevirtual(CD_FastByteWriter, "writeLong", MTD_void_long);
            } else if (elementType == Double.class || elementType == double.class) {
                cb.checkcast(CD_Double);
                cb.invokevirtual(CD_Double, "doubleValue", MethodTypeDesc.of(ConstantDescs.CD_double));
                cb.aload(1); // writer
                cb.dup_x2();
                cb.pop();
                cb.invokevirtual(CD_FastByteWriter, "writeDouble", MTD_void_double);
            } else if (elementType == Float.class || elementType == float.class) {
                cb.checkcast(CD_Float);
                cb.invokevirtual(CD_Float, "floatValue", MethodTypeDesc.of(ConstantDescs.CD_float));
                cb.aload(1); // writer
                cb.swap();
                cb.invokevirtual(CD_FastByteWriter, "writeFloat", MTD_void_float);
            } else if (elementType == Short.class || elementType == short.class) {
                cb.checkcast(CD_Short);
                cb.invokevirtual(CD_Short, "shortValue", MethodTypeDesc.of(ConstantDescs.CD_short));
                cb.aload(1); // writer
                cb.swap();
                cb.invokevirtual(CD_FastByteWriter, "writeShort", MTD_void_short);
            } else if (elementType == Byte.class || elementType == byte.class) {
                cb.checkcast(CD_Byte);
                cb.invokevirtual(CD_Byte, "byteValue", MethodTypeDesc.of(ConstantDescs.CD_byte));
                cb.aload(1); // writer
                cb.swap();
                cb.invokevirtual(CD_FastByteWriter, "writeByte", MTD_void_byte);
            } else if (elementType == Boolean.class || elementType == boolean.class) {
                cb.checkcast(CD_Boolean);
                cb.invokevirtual(CD_Boolean, "booleanValue", MethodTypeDesc.of(ConstantDescs.CD_boolean));
                cb.aload(1); // writer
                cb.swap();
                cb.invokevirtual(CD_FastByteWriter, "writeBoolean", MTD_void_boolean);
            } else if (elementType == Character.class || elementType == char.class) {
                cb.checkcast(CD_Character);
                cb.invokevirtual(CD_Character, "charValue", MethodTypeDesc.of(ConstantDescs.CD_char));
                cb.i2s(); // char to short
                cb.aload(1); // writer
                cb.swap();
                cb.invokevirtual(CD_FastByteWriter, "writeShort", MTD_void_short);
            }
        }

        cb.labelBinding(endLabel);
    }
}

