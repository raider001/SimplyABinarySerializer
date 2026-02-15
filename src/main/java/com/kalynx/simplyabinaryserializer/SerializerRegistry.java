package com.kalynx.simplyabinaryserializer;

import com.kalynx.simplyabinaryserializer.deserializer.BinaryDeserializer;
import com.kalynx.simplyabinaryserializer.serializer.BinarySerializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing multiple serializers and deserializers.
 * Thread-safe for concurrent registration and lookup.
 */
public class SerializerRegistry {

    private final Map<Class<?>, BinarySerializer<?>> serializers = new ConcurrentHashMap<>();
    private final Map<Class<?>, BinaryDeserializer<?>> deserializers = new ConcurrentHashMap<>();

    /**
     * Register a class for serialization/deserialization.
     */
    public <T> void register(Class<T> clazz) throws Throwable {
        if (serializers.containsKey(clazz)) {
            return;
        }
        serializers.put(clazz, new BinarySerializer<>(clazz));
        deserializers.put(clazz, new BinaryDeserializer<>(clazz));
    }

    /**
     * Check if a class is registered.
     */
    public boolean isRegistered(Class<?> clazz) {
        return serializers.containsKey(clazz);
    }

    /**
     * Get the serializer for a specific class.
     */
    @SuppressWarnings("unchecked")
    public <T> BinarySerializer<T> getSerializer(Class<T> clazz) {
        BinarySerializer<T> serializer = (BinarySerializer<T>) serializers.get(clazz);
        if (serializer == null) {
            throw new IllegalStateException("Class not registered: " + clazz.getName());
        }
        return serializer;
    }

    /**
     * Get the deserializer for a specific class.
     */
    @SuppressWarnings("unchecked")
    public <T> BinaryDeserializer<T> getDeserializer(Class<T> clazz) {
        BinaryDeserializer<T> deserializer = (BinaryDeserializer<T>) deserializers.get(clazz);
        if (deserializer == null) {
            throw new IllegalStateException("Class not registered: " + clazz.getName());
        }
        return deserializer;
    }
}

