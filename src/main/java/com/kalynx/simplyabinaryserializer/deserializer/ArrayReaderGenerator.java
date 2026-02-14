package com.kalynx.simplyabinaryserializer.deserializer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;

import static com.kalynx.simplyabinaryserializer.serializer.SerializerConstants.*;

/**
 * Generates optimized bytecode for reading primitive arrays from FastByteReader.
 * Uses Java ClassFile API to create specialized array reader classes at runtime,
 * eliminating ALL conditionals and method call overhead.
 */
public class ArrayReaderGenerator {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @FunctionalInterface
    public interface ArrayReader {
        Object readArray(FastByteReader reader);
    }

    /**
     * Generates an optimized reader for a specific array type.
     * Creates bytecode that directly reads array elements without any conditionals.
     *
     * @param componentType The component type of the array (e.g., int.class for int[])
     * @return A generated reader instance
     * @throws Throwable if bytecode generation fails
     */
    @SuppressWarnings("unchecked")
    public ArrayReader generateArrayReader(Class<?> componentType) throws Throwable {
        String className = "com.kalynx.simplyabinaryserializer.deserializer.GeneratedArrayReader$" +
                           componentType.getSimpleName() + "$" + CLASS_COUNTER.incrementAndGet();

        byte[] classBytes = ClassFile.of().build(
                ClassDesc.of(className),
                classBuilder -> {
                    classBuilder.withFlags(ACC_PUBLIC_FINAL_SYNTHETIC);
                    classBuilder.withInterfaceSymbols(ClassDesc.of("com.kalynx.simplyabinaryserializer.deserializer.ArrayReaderGenerator$ArrayReader"));

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

                    // readArray(FastByteReader reader) method
                    classBuilder.withMethodBody(
                            "readArray",
                            MethodTypeDesc.of(ConstantDescs.CD_Object, CD_FastByteReader),
                            ACC_PUBLIC,
                            codeBuilder -> generateReadArrayMethod(codeBuilder, componentType)
                    );
                }
        );

        Class<?> generatedClass = LOOKUP.defineClass(classBytes);
        return (ArrayReader) generatedClass.getDeclaredConstructor().newInstance();
    }

    private void generateReadArrayMethod(CodeBuilder cb, Class<?> componentType) {
        Label nullLabel = cb.newLabel();
        Label notNullLabel = cb.newLabel();
        Label emptyLabel = cb.newLabel();
        Label notEmptyLabel = cb.newLabel();
        Label loopStart = cb.newLabel();
        Label loopCheck = cb.newLabel();
        Label loopEnd = cb.newLabel();

        ClassDesc arrayDesc = getArrayDescriptor(componentType);
        ClassDesc componentDesc = getComponentDescriptor(componentType);

        // int length = reader.readInt();
        cb.aload(1); // reader
        cb.invokevirtual(CD_FastByteReader, "readInt", MTD_int);
        cb.istore(2); // length

        // if (length == -1) return null;
        cb.iload(2); // length
        cb.iconst_m1();
        cb.if_icmpeq(nullLabel);
        cb.goto_(notNullLabel);

        cb.labelBinding(nullLabel);
        cb.aconst_null();
        cb.areturn();

        cb.labelBinding(notNullLabel);

        // if (length == 0) return new ComponentType[0];
        cb.iload(2); // length
        cb.ifne(notEmptyLabel);

        cb.labelBinding(emptyLabel);
        cb.iconst_0();
        cb.newarray(getNewarrayType(componentType));
        cb.areturn();

        cb.labelBinding(notEmptyLabel);

        // ComponentType[] array = new ComponentType[length];
        cb.iload(2); // length
        cb.newarray(getNewarrayType(componentType));
        cb.astore(3); // array

        // int i = 0;
        cb.iconst_0();
        cb.istore(4); // i

        // goto loopCheck;
        cb.goto_(loopCheck);

        // Loop body
        cb.labelBinding(loopStart);

        // array[i] = reader.readXXX();
        generateElementRead(cb, componentType);

        // i++;
        cb.iinc(4, 1);

        // Loop check: if (i < length) goto loopStart;
        cb.labelBinding(loopCheck);
        cb.iload(4); // i
        cb.iload(2); // length
        cb.if_icmplt(loopStart);

        cb.labelBinding(loopEnd);

        // return array;
        cb.aload(3); // array
        cb.areturn();
    }

    private void generateElementRead(CodeBuilder cb, Class<?> componentType) {
        // array[i] = reader.readXXX();
        cb.aload(3); // array
        cb.iload(4); // i
        cb.aload(1); // reader

        if (componentType == int.class) {
            cb.invokevirtual(CD_FastByteReader, "readInt", MTD_int);
            cb.iastore();
        } else if (componentType == long.class) {
            cb.invokevirtual(CD_FastByteReader, "readLong", MTD_long);
            cb.lastore();
        } else if (componentType == double.class) {
            cb.invokevirtual(CD_FastByteReader, "readDouble", MTD_double);
            cb.dastore();
        } else if (componentType == float.class) {
            cb.invokevirtual(CD_FastByteReader, "readFloat", MTD_float);
            cb.fastore();
        } else if (componentType == short.class) {
            cb.invokevirtual(CD_FastByteReader, "readShort", MTD_short);
            cb.sastore();
        } else if (componentType == byte.class) {
            cb.invokevirtual(CD_FastByteReader, "readByte", MTD_byte);
            cb.bastore();
        } else if (componentType == boolean.class) {
            cb.invokevirtual(CD_FastByteReader, "readBoolean", MTD_boolean);
            cb.bastore();
        } else if (componentType == char.class) {
            cb.invokevirtual(CD_FastByteReader, "readShort", MTD_short);
            cb.i2c(); // short to char
            cb.castore();
        }
    }

    private ClassDesc getArrayDescriptor(Class<?> componentType) {
        if (componentType == int.class) {
            return ConstantDescs.CD_int.arrayType();
        } else if (componentType == long.class) {
            return ConstantDescs.CD_long.arrayType();
        } else if (componentType == double.class) {
            return ConstantDescs.CD_double.arrayType();
        } else if (componentType == float.class) {
            return ConstantDescs.CD_float.arrayType();
        } else if (componentType == short.class) {
            return ConstantDescs.CD_short.arrayType();
        } else if (componentType == byte.class) {
            return ConstantDescs.CD_byte.arrayType();
        } else if (componentType == boolean.class) {
            return ConstantDescs.CD_boolean.arrayType();
        } else if (componentType == char.class) {
            return ConstantDescs.CD_char.arrayType();
        }
        throw new UnsupportedOperationException("Array type not yet supported: " + componentType.getName());
    }

    private ClassDesc getComponentDescriptor(Class<?> componentType) {
        if (componentType == int.class) {
            return ConstantDescs.CD_int;
        } else if (componentType == long.class) {
            return ConstantDescs.CD_long;
        } else if (componentType == double.class) {
            return ConstantDescs.CD_double;
        } else if (componentType == float.class) {
            return ConstantDescs.CD_float;
        } else if (componentType == short.class) {
            return ConstantDescs.CD_short;
        } else if (componentType == byte.class) {
            return ConstantDescs.CD_byte;
        } else if (componentType == boolean.class) {
            return ConstantDescs.CD_boolean;
        } else if (componentType == char.class) {
            return ConstantDescs.CD_char;
        }
        throw new UnsupportedOperationException("Array type not yet supported: " + componentType.getName());
    }

    private TypeKind getNewarrayType(Class<?> componentType) {
        if (componentType == int.class) {
            return TypeKind.INT;
        } else if (componentType == long.class) {
            return TypeKind.LONG;
        } else if (componentType == double.class) {
            return TypeKind.DOUBLE;
        } else if (componentType == float.class) {
            return TypeKind.FLOAT;
        } else if (componentType == short.class) {
            return TypeKind.SHORT;
        } else if (componentType == byte.class) {
            return TypeKind.BYTE;
        } else if (componentType == boolean.class) {
            return TypeKind.BOOLEAN;
        } else if (componentType == char.class) {
            return TypeKind.CHAR;
        }
        throw new UnsupportedOperationException("Array type not yet supported: " + componentType.getName());
    }
}


