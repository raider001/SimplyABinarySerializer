package com.kalynx.simplyabinaryserializer.deserializer;

import com.kalynx.simplyabinaryserializer.Deserializer;

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
public class BinaryDeserializer<T> implements Deserializer {

    private final Class<T> targetClass;

    public BinaryDeserializer(Class<T> targetClass) {
        this.targetClass = targetClass;
        // TODO: Initialize deserialization-specific structures
        // - Field analysis for reading
        // - Bytecode generation for reader
        // - Pre-allocated read handlers
    }

    /**
     * Deserialize bytes to an object.
     *
     * @param bytes The serialized data
     * @param type The type to deserialize as
     * @return Deserialized object
     * @throws Throwable if deserialization fails
     */
    public <R> R deserialize(byte[] bytes, Class<R> type) throws Throwable {
        // TODO: Implement deserialization logic
        // - Get pooled FastByteReader
        // - Create instance via constructor handle
        // - Invoke generated reader
        // - Return object
        throw new UnsupportedOperationException("Deserialization logic to be ported");
    }
}
