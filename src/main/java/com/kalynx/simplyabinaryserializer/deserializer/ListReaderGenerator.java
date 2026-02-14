package com.kalynx.simplyabinaryserializer.deserializer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static com.kalynx.simplyabinaryserializer.serializer.SerializerConstants.*;

/**
 * Generates optimized bytecode for reading List elements from FastByteReader.
 * Uses Java ClassFile API to create specialized list reader classes at runtime,
 * eliminating ALL conditionals and method call overhead.
 */
public class ListReaderGenerator {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @FunctionalInterface
    public interface ListReader {
        List<?> readList(FastByteReader reader);
    }

    /**
     * Generates an optimized reader for a specific list element type.
     * Creates bytecode that directly reads elements without any conditionals.
     *
     * @param elementType The type of elements in the list
     * @return A generated reader instance
     * @throws Throwable if bytecode generation fails
     */
    @SuppressWarnings("unchecked")
    public <E> ListReader generateListReader(Class<E> elementType) throws Throwable {
        String className = "com.kalynx.simplyabinaryserializer.deserializer.GeneratedListReader$" +
                           elementType.getSimpleName() + "$" + CLASS_COUNTER.incrementAndGet();

        byte[] classBytes = ClassFile.of().build(
                ClassDesc.of(className),
                classBuilder -> {
                    classBuilder.withFlags(ACC_PUBLIC_FINAL_SYNTHETIC);
                    classBuilder.withInterfaceSymbols(ClassDesc.of("com.kalynx.simplyabinaryserializer.deserializer.ListReaderGenerator$ListReader"));

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

                    // readList(FastByteReader reader) method
                    classBuilder.withMethodBody(
                            "readList",
                            MethodTypeDesc.of(CD_List, CD_FastByteReader),
                            ACC_PUBLIC,
                            codeBuilder -> generateReadListMethod(codeBuilder, elementType)
                    );
                }
        );

        Class<?> generatedClass = LOOKUP.defineClass(classBytes);
        return (ListReader) generatedClass.getDeclaredConstructor().newInstance();
    }

    private <E> void generateReadListMethod(CodeBuilder cb, Class<E> elementType) {
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

        // if (size == 0) return new ArrayList<>(0);
        cb.iload(2); // size
        cb.ifne(notEmptyLabel);

        cb.labelBinding(emptyLabel);
        cb.new_(CD_ArrayList);
        cb.dup();
        cb.iconst_0();
        cb.invokespecial(CD_ArrayList, ConstantDescs.INIT_NAME,
                MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
        cb.areturn();

        cb.labelBinding(notEmptyLabel);

        // List list = new ArrayList(size);
        cb.new_(CD_ArrayList);
        cb.dup();
        cb.iload(2); // size
        cb.invokespecial(CD_ArrayList, ConstantDescs.INIT_NAME,
                MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
        cb.astore(3); // list

        // int i = 0;
        cb.iconst_0();
        cb.istore(4); // i

        // goto loopCheck;
        cb.goto_(loopCheck);

        // Loop body
        cb.labelBinding(loopStart);

        // Read element and add to list
        generateElementRead(cb, elementType);

        // list.add(element); // element is on stack
        cb.aload(3); // list
        cb.swap();
        cb.invokeinterface(CD_List, "add",
                MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_Object));
        cb.pop(); // discard boolean return

        // i++;
        cb.iinc(4, 1);

        // Loop check: if (i < size) goto loopStart;
        cb.labelBinding(loopCheck);
        cb.iload(4); // i
        cb.iload(2); // size
        cb.if_icmplt(loopStart);

        cb.labelBinding(loopEnd);

        // return list;
        cb.aload(3); // list
        cb.areturn();
    }

    private <E> void generateElementRead(CodeBuilder cb, Class<E> elementType) {
        if (elementType == String.class) {
            Label notNullLabel = cb.newLabel();
            Label endLabel = cb.newLabel();

            // int len = reader.readInt();
            cb.aload(1); // reader
            cb.invokevirtual(CD_FastByteReader, "readInt", MTD_int);
            cb.dup();
            cb.istore(5); // len

            // if (len == -1) return null;
            cb.iconst_m1();
            cb.if_icmpne(notNullLabel);
            cb.aconst_null();
            cb.goto_(endLabel);

            cb.labelBinding(notNullLabel);

            // OPTIMIZED: Use String(byte[], int offset, int length, Charset) constructor
            // This avoids allocating an intermediate byte[] array

            // new String(reader.getBuffer(), reader.getPosition(), len, UTF_8)
            cb.new_(ConstantDescs.CD_String);
            cb.dup();

            // Get buffer: reader.getBuffer()
            cb.aload(1); // reader
            cb.invokevirtual(CD_FastByteReader, "getBuffer", MTD_byte_array);

            // Get current position: reader.getPosition()
            cb.aload(1); // reader
            cb.invokevirtual(CD_FastByteReader, "getPosition", MTD_int);

            // Length
            cb.iload(5); // len

            // UTF_8 charset
            cb.getstatic(CD_StandardCharsets, "UTF_8", CD_Charset);

            // Invoke String(byte[], int, int, Charset) constructor
            cb.invokespecial(ConstantDescs.CD_String, ConstantDescs.INIT_NAME,
                    MethodTypeDesc.of(ConstantDescs.CD_void, CD_byte_array, ConstantDescs.CD_int, ConstantDescs.CD_int, CD_Charset));

            // Advance reader position manually
            cb.aload(1); // reader
            cb.aload(1); // reader
            cb.invokevirtual(CD_FastByteReader, "getPosition", MTD_int);
            cb.iload(5); // len
            cb.iadd();
            cb.invokevirtual(CD_FastByteReader, "setPosition", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));

            cb.labelBinding(endLabel);
        } else {
            Label notNullLabel = cb.newLabel();
            Label endLabel = cb.newLabel();

            // boolean notNull = reader.readBoolean();
            cb.aload(1); // reader
            cb.invokevirtual(CD_FastByteReader, "readBoolean", MTD_boolean);
            cb.dup();
            cb.istore(5); // notNull

            cb.ifne(notNullLabel);

            // Null case
            cb.aconst_null();
            cb.goto_(endLabel);

            cb.labelBinding(notNullLabel);

            // Not null - read and box the value
            if (elementType == Integer.class || elementType == int.class) {
                cb.aload(1); // reader
                cb.invokevirtual(CD_FastByteReader, "readInt", MTD_int);
                cb.invokestatic(CD_Integer, "valueOf",
                        MethodTypeDesc.of(CD_Integer, ConstantDescs.CD_int));
            } else if (elementType == Long.class || elementType == long.class) {
                cb.aload(1); // reader
                cb.invokevirtual(CD_FastByteReader, "readLong", MTD_long);
                cb.invokestatic(CD_Long, "valueOf",
                        MethodTypeDesc.of(CD_Long, ConstantDescs.CD_long));
            } else if (elementType == Double.class || elementType == double.class) {
                cb.aload(1); // reader
                cb.invokevirtual(CD_FastByteReader, "readDouble", MTD_double);
                cb.invokestatic(CD_Double, "valueOf",
                        MethodTypeDesc.of(CD_Double, ConstantDescs.CD_double));
            } else if (elementType == Float.class || elementType == float.class) {
                cb.aload(1); // reader
                cb.invokevirtual(CD_FastByteReader, "readFloat", MTD_float);
                cb.invokestatic(CD_Float, "valueOf",
                        MethodTypeDesc.of(CD_Float, ConstantDescs.CD_float));
            } else if (elementType == Short.class || elementType == short.class) {
                cb.aload(1); // reader
                cb.invokevirtual(CD_FastByteReader, "readShort", MTD_short);
                cb.invokestatic(CD_Short, "valueOf",
                        MethodTypeDesc.of(CD_Short, ConstantDescs.CD_short));
            } else if (elementType == Byte.class || elementType == byte.class) {
                cb.aload(1); // reader
                cb.invokevirtual(CD_FastByteReader, "readByte", MTD_byte);
                cb.invokestatic(CD_Byte, "valueOf", MethodTypeDesc.of(CD_Byte, ConstantDescs.CD_byte));
            } else if (elementType == Boolean.class || elementType == boolean.class) {
                cb.aload(1); // reader
                cb.invokevirtual(CD_FastByteReader, "readBoolean", MTD_boolean);
                cb.invokestatic(CD_Boolean, "valueOf",
                        MethodTypeDesc.of(CD_Boolean, ConstantDescs.CD_boolean));
            } else if (elementType == Character.class || elementType == char.class) {
                cb.aload(1); // reader
                cb.invokevirtual(CD_FastByteReader, "readShort", MTD_short);
                cb.i2c(); // short to char
                cb.invokestatic(CD_Character, "valueOf",
                        MethodTypeDesc.of(CD_Character, ConstantDescs.CD_char));
            }

            cb.labelBinding(endLabel);
        }
    }
}

