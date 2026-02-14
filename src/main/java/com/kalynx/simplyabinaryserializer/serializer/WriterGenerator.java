package com.kalynx.simplyabinaryserializer.serializer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import static com.kalynx.simplyabinaryserializer.serializer.SerializerConstants.*;

/**
 * Generates optimized bytecode for writing primitive types to FastByteWriter.
 * Uses Java ClassFile API to create specialized writer classes at runtime,
 * eliminating reflection overhead for primitive serialization.
 */
public class WriterGenerator {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @FunctionalInterface
    public interface PrimitiveWriter<T> {
        void write(FastByteWriter writer, T obj) throws Throwable;
    }

    /**
     * Generates an optimized writer for a class with primitive fields only.
     *
     * @param targetClass The class to generate a writer for
     * @param fields Array of fields to serialize (must be primitives only)
     * @param <T> The type being serialized
     * @return A generated writer instance
     * @throws Throwable if bytecode generation fails
     */
    @SuppressWarnings("unchecked")
    public <T> PrimitiveWriter<T> generatePrimitiveWriter(Class<T> targetClass, Field[] fields) throws Throwable {
        validatePrimitiveFields(fields);

        String className = "com.kalynx.simplyabinaryserializer.serializer.GeneratedPrimitiveWriter$" + CLASS_COUNTER.incrementAndGet();
        ClassDesc targetClassDesc = ClassDesc.of(targetClass.getName());

        byte[] classBytes = ClassFile.of().build(
                ClassDesc.of(className),
                classBuilder -> {
                    classBuilder.withFlags(ACC_PUBLIC_FINAL_SYNTHETIC);
                    classBuilder.withInterfaceSymbols(ClassDesc.of("com.kalynx.simplyabinaryserializer.serializer.WriterGenerator$PrimitiveWriter"));

                    // Default constructor
                    classBuilder.withMethodBody(
                            ConstantDescs.INIT_NAME,
                            MethodTypeDesc.of(ConstantDescs.CD_void),
                            ClassFile.ACC_PUBLIC,
                            codeBuilder -> {
                                codeBuilder.aload(0);
                                codeBuilder.invokespecial(CD_Object, ConstantDescs.INIT_NAME,
                                        MethodTypeDesc.of(ConstantDescs.CD_void));
                                codeBuilder.return_();
                            }
                    );

                    // write(FastByteWriter writer, Object obj) method
                    classBuilder.withMethodBody(
                            "write",
                            MethodTypeDesc.of(ConstantDescs.CD_void, CD_FastByteWriter, CD_Object),
                            ClassFile.ACC_PUBLIC,
                            codeBuilder -> {
                                // Cast obj to target type and store in local 3
                                codeBuilder.aload(2); // obj
                                codeBuilder.checkcast(targetClassDesc);
                                codeBuilder.astore(3); // typed obj

                                // Generate field writes for each primitive field
                                for (Field field : fields) {
                                    generatePrimitiveFieldWrite(codeBuilder, field, targetClassDesc);
                                }

                                codeBuilder.return_();
                            }
                    );
                }
        );

        // Define and instantiate the generated class
        MethodHandles.Lookup lookup = LOOKUP.defineHiddenClass(classBytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
        Class<?> writerClass = lookup.lookupClass();
        return (PrimitiveWriter<T>) writerClass.getDeclaredConstructor().newInstance();
    }

    /**
     * Validates that all fields are primitive types.
     */
    private void validatePrimitiveFields(Field[] fields) {
        for (Field field : fields) {
            if (!field.getType().isPrimitive()) {
                throw new IllegalArgumentException("WriterGenerator currently only supports primitive types. Found: " + field.getType().getName());
            }
        }
    }

    /**
     * Generates bytecode to write a single primitive field.
     */
    private void generatePrimitiveFieldWrite(CodeBuilder cb, Field field, ClassDesc targetClassDesc) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();

        // Load writer (local 1)
        cb.aload(1);
        // Load typed object (local 3)
        cb.aload(3);

        // Get the field value and write it
        if (fieldType == int.class) {
            cb.getfield(targetClassDesc, fieldName, ConstantDescs.CD_int);
            cb.invokevirtual(CD_FastByteWriter, "writeInt", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
        } else if (fieldType == long.class) {
            cb.getfield(targetClassDesc, fieldName, ConstantDescs.CD_long);
            cb.invokevirtual(CD_FastByteWriter, "writeLong", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_long));
        } else if (fieldType == double.class) {
            cb.getfield(targetClassDesc, fieldName, ConstantDescs.CD_double);
            cb.invokevirtual(CD_FastByteWriter, "writeDouble", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_double));
        } else if (fieldType == float.class) {
            cb.getfield(targetClassDesc, fieldName, ConstantDescs.CD_float);
            cb.invokevirtual(CD_FastByteWriter, "writeFloat", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_float));
        } else if (fieldType == short.class) {
            cb.getfield(targetClassDesc, fieldName, ConstantDescs.CD_short);
            cb.invokevirtual(CD_FastByteWriter, "writeShort", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_short));
        } else if (fieldType == byte.class) {
            cb.getfield(targetClassDesc, fieldName, ConstantDescs.CD_byte);
            cb.invokevirtual(CD_FastByteWriter, "writeByte", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
        } else if (fieldType == boolean.class) {
            cb.getfield(targetClassDesc, fieldName, ConstantDescs.CD_boolean);
            cb.invokevirtual(CD_FastByteWriter, "writeBoolean", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_boolean));
        } else if (fieldType == char.class) {
            cb.getfield(targetClassDesc, fieldName, ConstantDescs.CD_char);
            // Write char as short (2 bytes)
            cb.invokevirtual(CD_FastByteWriter, "writeShort", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int));
        } else {
            throw new IllegalArgumentException("Unsupported primitive type: " + fieldType.getName());
        }
    }

    /**
     * Estimates the serialized size of an object with primitive fields.
     */
    public int estimatePrimitiveSize(Field[] fields) {
        int size = 0;
        for (Field field : fields) {
            Class<?> type = field.getType();
            if (type == int.class) size += 4;
            else if (type == long.class) size += 8;
            else if (type == double.class) size += 8;
            else if (type == float.class) size += 4;
            else if (type == short.class) size += 2;
            else if (type == byte.class) size += 1;
            else if (type == boolean.class) size += 1;
            else if (type == char.class) size += 2;
        }
        return size;
    }
}


