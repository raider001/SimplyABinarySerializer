package com.kalynx.simplyabinaryserializer;

import com.kalynx.simplyabinaryserializer.deserializer.BinaryDeserializer;
import com.kalynx.simplyabinaryserializer.serializer.BinarySerializer;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing multiple serializers and deserializers.
 * Thread-safe for concurrent registration and lookup.
 * Supports both Class and Type (for generics) based registration.
 */
public class SerializerRegistry {

    private final Map<Class<?>, BinarySerializer<?>> serializersByClass = new ConcurrentHashMap<>();
    private final Map<Class<?>, BinaryDeserializer<?>> deserializersByClass = new ConcurrentHashMap<>();

    // For generic types, we use Type as the key
    private final Map<Type, BinarySerializer<?>> serializersByType = new ConcurrentHashMap<>();
    private final Map<Type, BinaryDeserializer<?>> deserializersByType = new ConcurrentHashMap<>();

    /**
     * Register a class for serialization/deserialization.
     */
    public <T> void register(Class<T> clazz) throws Throwable {
        if (serializersByClass.containsKey(clazz)) {
            return;
        }
        serializersByClass.put(clazz, new BinarySerializer<>(clazz));
        deserializersByClass.put(clazz, new BinaryDeserializer<>(clazz));
    }

    /**
     * Register a generic type for serialization/deserialization.
     */
    public <T> void register(TypeReference<T> typeRef) throws Throwable {
        Type type = typeRef.getType();
        Class<T> rawType = typeRef.getRawType();

        if (serializersByType.containsKey(type)) {
            return;
        }

        // Store by both Type (for exact generic match) and Class (for runtime lookup)
        BinarySerializer<T> serializer = new BinarySerializer<>(rawType);
        BinaryDeserializer<T> deserializer = new BinaryDeserializer<>(rawType);

        serializersByType.put(type, serializer);
        deserializersByType.put(type, deserializer);

        // Also store by raw class if not already registered
        if (!serializersByClass.containsKey(rawType)) {
            serializersByClass.put(rawType, serializer);
            deserializersByClass.put(rawType, deserializer);
        }
    }

    /**
     * Check if a class is registered.
     */
    public boolean isRegistered(Class<?> clazz) {
        return serializersByClass.containsKey(clazz);
    }

    /**
     * Check if a type is registered.
     */
    public boolean isRegistered(Type type) {
        return serializersByType.containsKey(type);
    }

    /**
     * Get the serializer for a specific class.
     */
    @SuppressWarnings("unchecked")
    public <T> BinarySerializer<T> getSerializer(Class<T> clazz) {
        BinarySerializer<T> serializer = (BinarySerializer<T>) serializersByClass.get(clazz);
        if (serializer == null) {
            throw new IllegalStateException("Class not registered: " + clazz.getName());
        }
        return serializer;
    }

    /**
     * Get the serializer for a specific type (including generic types).
     */
    public BinarySerializer<?> getSerializer(Type type) {
        BinarySerializer<?> serializer = serializersByType.get(type);
        if (serializer == null) {
            throw new IllegalStateException("Type not registered: " + type);
        }
        return serializer;
    }

    /**
     * Get the deserializer for a specific class.
     */
    @SuppressWarnings("unchecked")
    public <T> BinaryDeserializer<T> getDeserializer(Class<T> clazz) {
        BinaryDeserializer<T> deserializer = (BinaryDeserializer<T>) deserializersByClass.get(clazz);
        if (deserializer == null) {
            throw new IllegalStateException("Class not registered: " + clazz.getName());
        }
        return deserializer;
    }

    /**
     * Get the deserializer for a specific type (including generic types).
     */
    public BinaryDeserializer<?> getDeserializer(Type type) {
        BinaryDeserializer<?> deserializer = deserializersByType.get(type);
        if (deserializer == null) {
            throw new IllegalStateException("Type not registered: " + type);
        }
        return deserializer;
    }
}


