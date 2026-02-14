package com.kalynx.simplyabinaryserializer.deserializer;

import com.kalynx.simplyabinaryserializer.Deserializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Binary deserialization controller - handles ONLY read operations.
 * This class has no knowledge of serialization logic.
 *
 * Design philosophy:
 * - One-way operation: bytes → Object
 * - No references to serializer classes
 * - Optimized for read performance
 *
 * @param <T> The type this deserializer handles
 */
public class BinaryDeserializer<T> implements Deserializer<T> {

    private final ReaderGenerator.PrimitiveReader<T> reader;
    private final FastByteReader byteReader;

    public BinaryDeserializer(Class<T> targetClass) throws Throwable {
        this.byteReader = new FastByteReader();

        // Analyze fields and filter for primitives only
        Field[] primitiveFields = analyzePrimitiveFields(targetClass);

        // Generate optimized reader using ReaderGenerator
        ReaderGenerator generator = new ReaderGenerator();
        this.reader = generator.generatePrimitiveReader(targetClass, primitiveFields);
    }

    /**
     * Analyzes the target class and extracts all primitive fields.
     */
    private Field[] analyzePrimitiveFields(Class<T> clazz) {
        List<Field> primitiveFields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            // Skip static and transient fields
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            // Only include primitive fields
            if (field.getType().isPrimitive()) {
                field.setAccessible(true);
                primitiveFields.add(field);
            }
        }

        return primitiveFields.toArray(new Field[0]);
    }

    @Override
    public T deserialize(byte[] bytes) throws Throwable {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        // Set data in reader
        byteReader.setData(bytes);

        // Use generated reader for optimal performance
        return reader.read(byteReader);
    }
}
