package com.kalynx.simplyabinaryserializer.serializer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;

import static com.kalynx.simplyabinaryserializer.serializer.SerializerConstants.*;

/**
 * Generates optimized bytecode for writing primitive arrays to FastByteWriter.
 * Uses Java ClassFile API to create specialized array writer classes at runtime,
 * eliminating ALL conditionals and method call overhead.
 */
public class ArrayWriterGenerator {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @FunctionalInterface
    public interface ArrayWriter {
        void writeArray(FastByteWriter writer, Object array);
    }

    /**
     * Generates an optimized writer for a specific array type.
     * Creates bytecode that directly writes array elements without any conditionals.
     *
     * @param componentType The component type of the array (e.g., int.class for int[])
     * @return A generated writer instance
     * @throws Throwable if bytecode generation fails
     */
    @SuppressWarnings("unchecked")
    public ArrayWriter generateArrayWriter(Class<?> componentType) throws Throwable {
        String className = "com.kalynx.simplyabinaryserializer.serializer.GeneratedArrayWriter$" +
                           componentType.getSimpleName() + "$" + CLASS_COUNTER.incrementAndGet();

        byte[] classBytes = ClassFile.of().build(
                ClassDesc.of(className),
                classBuilder -> {
                    classBuilder.withFlags(ACC_PUBLIC_FINAL_SYNTHETIC);
                    classBuilder.withInterfaceSymbols(ClassDesc.of("com.kalynx.simplyabinaryserializer.serializer.ArrayWriterGenerator$ArrayWriter"));

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

                    // writeArray(FastByteWriter writer, Object array) method
                    classBuilder.withMethodBody(
                            "writeArray",
                            MethodTypeDesc.of(ConstantDescs.CD_void, CD_FastByteWriter, ConstantDescs.CD_Object),
                            ACC_PUBLIC,
                            codeBuilder -> generateWriteArrayMethod(codeBuilder, componentType)
                    );
                }
        );

        Class<?> generatedClass = LOOKUP.defineClass(classBytes);
        return (ArrayWriter) generatedClass.getDeclaredConstructor().newInstance();
    }

    private void generateWriteArrayMethod(CodeBuilder cb, Class<?> componentType) {
        Label notNullLabel = cb.newLabel();
        Label notEmptyLabel = cb.newLabel();
        Label loopStart = cb.newLabel();
        Label loopCheck = cb.newLabel();
        Label loopEnd = cb.newLabel();

        ClassDesc arrayDesc = getArrayDescriptor(componentType);

        // if (array == null) { writer.writeInt(-1); return; }
        cb.aload(2); // array (Object)
        cb.ifnonnull(notNullLabel);
        cb.aload(1); // writer
        cb.iconst_m1();
        cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);
        cb.return_();

        cb.labelBinding(notNullLabel);

        // Cast array to correct type and store
        cb.aload(2); // array
        cb.checkcast(arrayDesc);
        cb.astore(3); // typed array

        // int length = array.length;
        cb.aload(3);
        cb.arraylength();
        cb.istore(4); // length

        // writer.writeInt(length);
        cb.aload(1); // writer
        cb.iload(4); // length
        cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);

        // if (length == 0) return;
        cb.iload(4); // length
        cb.ifne(notEmptyLabel);
        cb.return_();

        cb.labelBinding(notEmptyLabel);

        // int i = 0;
        cb.iconst_0();
        cb.istore(5); // i

        // goto loopCheck;
        cb.goto_(loopCheck);

        // Loop body
        cb.labelBinding(loopStart);

        // Write array[i]
        generateElementWrite(cb, componentType);

        // i++;
        cb.iinc(5, 1);

        // Loop check: if (i < length) goto loopStart;
        cb.labelBinding(loopCheck);
        cb.iload(5); // i
        cb.iload(4); // length
        cb.if_icmplt(loopStart);

        cb.labelBinding(loopEnd);
        cb.return_();
    }

    private void generateElementWrite(CodeBuilder cb, Class<?> componentType) {
        // Load array[i] and write it
        cb.aload(1); // writer
        cb.aload(3); // array
        cb.iload(5); // i

        if (componentType == int.class) {
            cb.iaload();
            cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);
        } else if (componentType == long.class) {
            cb.laload();
            cb.invokevirtual(CD_FastByteWriter, "writeLong", MTD_void_long);
        } else if (componentType == double.class) {
            cb.daload();
            cb.invokevirtual(CD_FastByteWriter, "writeDouble", MTD_void_double);
        } else if (componentType == float.class) {
            cb.faload();
            cb.invokevirtual(CD_FastByteWriter, "writeFloat", MTD_void_float);
        } else if (componentType == short.class) {
            cb.saload();
            cb.invokevirtual(CD_FastByteWriter, "writeShort", MTD_void_short);
        } else if (componentType == byte.class) {
            cb.baload();
            cb.invokevirtual(CD_FastByteWriter, "writeByte", MTD_void_int);
        } else if (componentType == boolean.class) {
            cb.baload();
            cb.invokevirtual(CD_FastByteWriter, "writeBoolean", MTD_void_boolean);
        } else if (componentType == char.class) {
            cb.caload();
            cb.i2s(); // char to short
            cb.invokevirtual(CD_FastByteWriter, "writeShort", MTD_void_short);
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
}

