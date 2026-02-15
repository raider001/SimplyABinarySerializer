package com.kalynx.simplyabinaryserializer;

import com.kalynx.simplyabinaryserializer.deserializer.BinaryDeserializer;
import com.kalynx.simplyabinaryserializer.serializer.BinarySerializer;

import java.lang.reflect.Type;

/**
 * Top-level serializer that supports multiple registered classes.
 * Includes an internal registry for managing serializers and deserializers.
 * Supports both simple classes and generic types via TypeReference.
 */
public class KalynxSerializer {

    private final SerializerRegistry registry = new SerializerRegistry();

    /**
     * Register a class for serialization/deserialization.
     */
    public <T> KalynxSerializer register(Class<T> clazz) throws Throwable {
        registry.register(clazz);
        return this;
    }

    /**
     * Register a generic type for serialization/deserialization.
     * Use this for types with generic parameters like List&lt;Integer&gt;, Map&lt;String, User&gt;, etc.
     *
     * Example:
     * <pre>
     * serializer.register(new TypeReference&lt;List&lt;Integer&gt;&gt;() {});
     * </pre>
     */
    public <T> KalynxSerializer register(TypeReference<T> typeRef) throws Throwable {
        registry.register(typeRef);
        return this;
    }

    /**
     * Check if a class is registered.
     */
    public boolean isRegistered(Class<?> clazz) {
        return registry.isRegistered(clazz);
    }

    /**
     * Check if a generic type is registered.
     */
    public boolean isRegistered(TypeReference<?> typeRef) {
        return registry.isRegistered(typeRef.getType());
    }

    /**
     * Serialize an object. The object's class must be registered first.
     */
    @SuppressWarnings("unchecked")
    public <T> byte[] serialize(T obj) throws Throwable {
        if (obj == null) {
            throw new IllegalArgumentException("Cannot serialize null object");
        }

        BinarySerializer<T> serializer = (BinarySerializer<T>) registry.getSerializer(obj.getClass());
        return serializer.serialize(obj);
    }

    /**
     * Deserialize bytes into an object of the specified type.
     */
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws Throwable {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Cannot deserialize null or empty bytes");
        }

        BinaryDeserializer<T> deserializer = registry.getDeserializer(clazz);
        return deserializer.deserialize(bytes);
    }

    /**
     * Deserialize bytes into an object of the specified generic type.
     * Use this for types with generic parameters like List&lt;Integer&gt;, Map&lt;String, User&gt;, etc.
     *
     * Example:
     * <pre>
     * List&lt;Integer&gt; list = serializer.deserialize(bytes, new TypeReference&lt;List&lt;Integer&gt;&gt;() {});
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, TypeReference<T> typeRef) throws Throwable {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Cannot deserialize null or empty bytes");
        }

        BinaryDeserializer<T> deserializer = (BinaryDeserializer<T>) registry.getDeserializer(typeRef.getType());
        return deserializer.deserialize(bytes);
    }

    /**
     * Get the internal registry for advanced use cases.
     */
    public SerializerRegistry getRegistry() {
        return registry;
    }
}
