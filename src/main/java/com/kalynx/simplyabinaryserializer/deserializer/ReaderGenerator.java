package com.kalynx.simplyabinaryserializer.deserializer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static com.kalynx.simplyabinaryserializer.serializer.SerializerConstants.*;

/**
 * Generates optimized bytecode for reading primitive types from FastByteReader.
 * Uses Java ClassFile API to create specialized reader classes at runtime,
 * eliminating reflection overhead for primitive deserialization.
 */
public class ReaderGenerator {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final ClassDesc CD_FastByteReader = ClassDesc.of("com.kalynx.simplyabinaryserializer.deserializer.FastByteReader");

    @FunctionalInterface
    public interface PrimitiveReader<T> {
        T read(FastByteReader reader) throws Throwable;
    }

    /**
     * Generates an optimized reader for a class with primitive fields only.
     *
     * @param targetClass The class to generate a reader for
     * @param fields Array of fields to deserialize (must be primitives only)
     * @param <T> The type being deserialized
     * @return A generated reader instance
     * @throws Throwable if bytecode generation fails
     */
    @SuppressWarnings("unchecked")
    public <T> PrimitiveReader<T> generatePrimitiveReader(Class<T> targetClass, Field[] fields) throws Throwable {
        validatePrimitiveFields(fields);

        String className = "com.kalynx.simplyabinaryserializer.deserializer.GeneratedPrimitiveReader$" + CLASS_COUNTER.incrementAndGet();
        ClassDesc targetClassDesc = ClassDesc.of(targetClass.getName());

        // Find no-arg constructor
        Constructor<T> constructor = targetClass.getDeclaredConstructor();
        constructor.setAccessible(true);

        byte[] classBytes = ClassFile.of().build(
                ClassDesc.of(className),
                classBuilder -> {
                    classBuilder.withFlags(ACC_PUBLIC_FINAL_SYNTHETIC);
                    classBuilder.withInterfaceSymbols(ClassDesc.of("com.kalynx.simplyabinaryserializer.deserializer.ReaderGenerator$PrimitiveReader"));

                    // Store constructor reference as a field
                    classBuilder.withField("constructor", ClassDesc.of("java.lang.reflect.Constructor"), ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL);

                    // Constructor that takes the target class constructor
                    classBuilder.withMethodBody(
                            ConstantDescs.INIT_NAME,
                            MethodTypeDesc.of(ConstantDescs.CD_void, ClassDesc.of("java.lang.reflect.Constructor")),
                            ClassFile.ACC_PUBLIC,
                            codeBuilder -> {
                                codeBuilder.aload(0);
                                codeBuilder.invokespecial(CD_Object, ConstantDescs.INIT_NAME,
                                        MethodTypeDesc.of(ConstantDescs.CD_void));
                                codeBuilder.aload(0);
                                codeBuilder.aload(1);
                                codeBuilder.putfield(ClassDesc.of(className), "constructor", ClassDesc.of("java.lang.reflect.Constructor"));
                                codeBuilder.return_();
                            }
                    );

                    // read(FastByteReader reader) method
                    classBuilder.withMethodBody(
                            "read",
                            MethodTypeDesc.of(CD_Object, CD_FastByteReader),
                            ClassFile.ACC_PUBLIC,
                            codeBuilder -> {
                                // Create new instance using constructor
                                codeBuilder.aload(0);
                                codeBuilder.getfield(ClassDesc.of(className), "constructor", ClassDesc.of("java.lang.reflect.Constructor"));
                                codeBuilder.iconst_0();
                                codeBuilder.anewarray(CD_Object);
                                codeBuilder.invokevirtual(
                                        ClassDesc.of("java.lang.reflect.Constructor"),
                                        "newInstance",
                                        MethodTypeDesc.of(CD_Object, CD_Object.arrayType())
                                );
                                codeBuilder.checkcast(targetClassDesc);
                                codeBuilder.astore(2); // Store object in local 2

                                // Generate field reads for each primitive field
                                for (Field field : fields) {
                                    generatePrimitiveFieldRead(codeBuilder, field, targetClassDesc);
                                }

                                // Return the object
                                codeBuilder.aload(2);
                                codeBuilder.areturn();
                            }
                    );
                }
        );

        // Define and instantiate the generated class
        MethodHandles.Lookup lookup = LOOKUP.defineHiddenClass(classBytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
        Class<?> readerClass = lookup.lookupClass();
        return (PrimitiveReader<T>) readerClass.getDeclaredConstructor(Constructor.class).newInstance(constructor);
    }

    /**
     * Validates that all fields are primitive types.
     */
    private void validatePrimitiveFields(Field[] fields) {
        for (Field field : fields) {
            if (!field.getType().isPrimitive()) {
                throw new IllegalArgumentException("ReaderGenerator currently only supports primitive types. Found: " + field.getType().getName());
            }
        }
    }

    /**
     * Generates bytecode to read a single primitive field.
     */
    private void generatePrimitiveFieldRead(CodeBuilder cb, Field field, ClassDesc targetClassDesc) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();

        // Load object (local 2)
        cb.aload(2);

        // Load reader (local 1) and read the value
        cb.aload(1);

        if (fieldType == int.class) {
            cb.invokevirtual(CD_FastByteReader, "readInt", MethodTypeDesc.of(ConstantDescs.CD_int));
            cb.putfield(targetClassDesc, fieldName, ConstantDescs.CD_int);
        } else if (fieldType == long.class) {
            cb.invokevirtual(CD_FastByteReader, "readLong", MethodTypeDesc.of(ConstantDescs.CD_long));
            cb.putfield(targetClassDesc, fieldName, ConstantDescs.CD_long);
        } else if (fieldType == double.class) {
            cb.invokevirtual(CD_FastByteReader, "readDouble", MethodTypeDesc.of(ConstantDescs.CD_double));
            cb.putfield(targetClassDesc, fieldName, ConstantDescs.CD_double);
        } else if (fieldType == float.class) {
            cb.invokevirtual(CD_FastByteReader, "readFloat", MethodTypeDesc.of(ConstantDescs.CD_float));
            cb.putfield(targetClassDesc, fieldName, ConstantDescs.CD_float);
        } else if (fieldType == short.class) {
            cb.invokevirtual(CD_FastByteReader, "readShort", MethodTypeDesc.of(ConstantDescs.CD_short));
            cb.putfield(targetClassDesc, fieldName, ConstantDescs.CD_short);
        } else if (fieldType == byte.class) {
            cb.invokevirtual(CD_FastByteReader, "readByte", MethodTypeDesc.of(ConstantDescs.CD_byte));
            cb.putfield(targetClassDesc, fieldName, ConstantDescs.CD_byte);
        } else if (fieldType == boolean.class) {
            cb.invokevirtual(CD_FastByteReader, "readBoolean", MethodTypeDesc.of(ConstantDescs.CD_boolean));
            cb.putfield(targetClassDesc, fieldName, ConstantDescs.CD_boolean);
        } else if (fieldType == char.class) {
            // Read char as short
            cb.invokevirtual(CD_FastByteReader, "readShort", MethodTypeDesc.of(ConstantDescs.CD_short));
            cb.i2c(); // Convert short to char
            cb.putfield(targetClassDesc, fieldName, ConstantDescs.CD_char);
        } else {
            throw new IllegalArgumentException("Unsupported primitive type: " + fieldType.getName());
        }
    }
}


