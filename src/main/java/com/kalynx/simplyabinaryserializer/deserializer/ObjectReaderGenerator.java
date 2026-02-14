package com.kalynx.simplyabinaryserializer.deserializer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;

import static com.kalynx.simplyabinaryserializer.serializer.SerializerConstants.*;

/**
 * Generates optimized bytecode for reading nested objects.
 * Creates specialized reader classes for each nested object type at runtime.
 */
public class ObjectReaderGenerator {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final byte TYPE_NULL = 0;
    private static final byte TYPE_OBJECT = 1;

    @FunctionalInterface
    public interface ObjectReader {
        Object readObject(FastByteReader reader, BinaryDeserializer<?> nestedDeserializer) throws Throwable;
    }

    /**
     * Generates an optimized reader for a nested object type.
     *
     * @param objectType The nested object class type
     * @return A generated reader instance
     * @throws Throwable if bytecode generation fails
     */
    @SuppressWarnings("unchecked")
    public ObjectReader generateObjectReader(Class<?> objectType) throws Throwable {
        String className = "com.kalynx.simplyabinaryserializer.deserializer.GeneratedObjectReader$" +
                           objectType.getSimpleName() + "$" + CLASS_COUNTER.incrementAndGet();

        byte[] classBytes = ClassFile.of().build(
                ClassDesc.of(className),
                classBuilder -> {
                    classBuilder.withFlags(ACC_PUBLIC_FINAL_SYNTHETIC);
                    classBuilder.withInterfaceSymbols(ClassDesc.of("com.kalynx.simplyabinaryserializer.deserializer.ObjectReaderGenerator$ObjectReader"));

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

                    // readObject(FastByteReader reader, BinaryDeserializer nestedDeserializer) method
                    classBuilder.withMethodBody(
                            "readObject",
                            MethodTypeDesc.of(ConstantDescs.CD_Object, CD_FastByteReader,
                                    ClassDesc.of("com.kalynx.simplyabinaryserializer.deserializer.BinaryDeserializer")),
                            ACC_PUBLIC,
                            codeBuilder -> generateReadObjectMethod(codeBuilder, objectType)
                    );
                }
        );

        Class<?> generatedClass = LOOKUP.defineClass(classBytes);
        return (ObjectReader) generatedClass.getDeclaredConstructor().newInstance();
    }

    private void generateReadObjectMethod(CodeBuilder cb, Class<?> objectType) {
        Label nullLabel = cb.newLabel();
        Label notNullLabel = cb.newLabel();
        Label endLabel = cb.newLabel();

        // byte type = reader.readByte();
        cb.aload(1); // reader
        cb.invokevirtual(CD_FastByteReader, "readByte", MTD_byte);
        cb.istore(3); // type

        // if (type == TYPE_NULL) return null;
        cb.iload(3); // type
        cb.iconst_0(); // TYPE_NULL
        cb.if_icmpne(notNullLabel);

        cb.labelBinding(nullLabel);
        cb.aconst_null();
        cb.areturn();

        cb.labelBinding(notNullLabel);

        // int length = reader.readInt();
        cb.aload(1); // reader
        cb.invokevirtual(CD_FastByteReader, "readInt", MTD_int);
        cb.istore(4); // length

        // byte[] nestedBytes = reader.readBytes(length);
        cb.aload(1); // reader
        cb.iload(4); // length
        cb.invokevirtual(CD_FastByteReader, "readBytes", MTD_byte_array_int);
        cb.astore(5); // nestedBytes

        // Object result = nestedDeserializer.deserialize(nestedBytes);
        cb.aload(2); // nestedDeserializer
        cb.aload(5); // nestedBytes
        cb.invokevirtual(ClassDesc.of("com.kalynx.simplyabinaryserializer.deserializer.BinaryDeserializer"),
                "deserialize",
                MethodTypeDesc.of(ConstantDescs.CD_Object, CD_byte_array));

        // Cast and return
        cb.checkcast(ClassDesc.of(objectType.getName()));
        cb.areturn();
    }
}

