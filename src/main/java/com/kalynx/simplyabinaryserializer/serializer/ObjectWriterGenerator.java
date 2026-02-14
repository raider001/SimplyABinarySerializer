package com.kalynx.simplyabinaryserializer.serializer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;

import static com.kalynx.simplyabinaryserializer.serializer.SerializerConstants.*;

/**
 * Generates optimized bytecode for writing nested objects.
 * Creates specialized writer classes for each nested object type at runtime.
 */
public class ObjectWriterGenerator {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final byte TYPE_NULL = 0;
    private static final byte TYPE_OBJECT = 1;

    @FunctionalInterface
    public interface ObjectWriter {
        void writeObject(FastByteWriter writer, Object obj, BinarySerializer<?> nestedSerializer) throws Throwable;
    }

    /**
     * Generates an optimized writer for a nested object type.
     *
     * @param objectType The nested object class type
     * @return A generated writer instance
     * @throws Throwable if bytecode generation fails
     */
    @SuppressWarnings("unchecked")
    public ObjectWriter generateObjectWriter(Class<?> objectType) throws Throwable {
        String className = "com.kalynx.simplyabinaryserializer.serializer.GeneratedObjectWriter$" +
                           objectType.getSimpleName() + "$" + CLASS_COUNTER.incrementAndGet();

        byte[] classBytes = ClassFile.of().build(
                ClassDesc.of(className),
                classBuilder -> {
                    classBuilder.withFlags(ACC_PUBLIC_FINAL_SYNTHETIC);
                    classBuilder.withInterfaceSymbols(ClassDesc.of("com.kalynx.simplyabinaryserializer.serializer.ObjectWriterGenerator$ObjectWriter"));

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

                    // writeObject(FastByteWriter writer, Object obj, BinarySerializer nestedSerializer) method
                    classBuilder.withMethodBody(
                            "writeObject",
                            MethodTypeDesc.of(ConstantDescs.CD_void, CD_FastByteWriter, ConstantDescs.CD_Object,
                                    ClassDesc.of("com.kalynx.simplyabinaryserializer.serializer.BinarySerializer")),
                            ACC_PUBLIC,
                            codeBuilder -> generateWriteObjectMethod(codeBuilder, objectType)
                    );
                }
        );

        Class<?> generatedClass = LOOKUP.defineClass(classBytes);
        return (ObjectWriter) generatedClass.getDeclaredConstructor().newInstance();
    }

    private void generateWriteObjectMethod(CodeBuilder cb, Class<?> objectType) {
        Label nullLabel = cb.newLabel();
        Label notNullLabel = cb.newLabel();

        // if (obj == null) { writer.writeByte(TYPE_NULL); return; }
        cb.aload(2); // obj
        cb.ifnonnull(notNullLabel);

        cb.labelBinding(nullLabel);
        cb.aload(1); // writer
        cb.iconst_0(); // TYPE_NULL
        cb.invokevirtual(CD_FastByteWriter, "writeByte", MTD_void_int);
        cb.return_();

        cb.labelBinding(notNullLabel);

        // writer.writeByte(TYPE_OBJECT);
        cb.aload(1); // writer
        cb.iconst_1(); // TYPE_OBJECT
        cb.invokevirtual(CD_FastByteWriter, "writeByte", MTD_void_int);

        // nestedSerializer.serialize(obj)
        cb.aload(3); // nestedSerializer
        cb.aload(2); // obj
        cb.checkcast(ClassDesc.of(objectType.getName()));
        cb.invokevirtual(ClassDesc.of("com.kalynx.simplyabinaryserializer.serializer.BinarySerializer"),
                "serialize",
                MethodTypeDesc.of(CD_byte_array, ConstantDescs.CD_Object));

        // byte[] nestedBytes = ...
        cb.astore(4); // nestedBytes

        // writer.writeInt(nestedBytes.length);
        cb.aload(1); // writer
        cb.aload(4); // nestedBytes
        cb.arraylength();
        cb.invokevirtual(CD_FastByteWriter, "writeInt", MTD_void_int);

        // writer.writeBytes(nestedBytes, nestedBytes.length);
        cb.aload(1); // writer
        cb.aload(4); // nestedBytes
        cb.aload(4); // nestedBytes
        cb.arraylength();
        cb.invokevirtual(CD_FastByteWriter, "writeBytes",
                MethodTypeDesc.of(ConstantDescs.CD_void, CD_byte_array, ConstantDescs.CD_int));

        cb.return_();
    }
}

