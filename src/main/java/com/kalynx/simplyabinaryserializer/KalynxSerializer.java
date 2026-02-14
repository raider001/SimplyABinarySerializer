package com.kalynx.simplyabinaryserializer;

import com.kalynx.simplyabinaryserializer.deserializer.BinaryDeserializer;
import com.kalynx.simplyabinaryserializer.serializer.BinarySerializer;

/**
 * Top-level serializer that delegates to separate serialization and deserialization controllers.
 * This design ensures complete separation between read and write operations.
 *
 * @param <T> The type this serializer handles
 */
public class KalynxSerializer<T> implements Serializer<T>, Deserializer<T> {

    private final BinarySerializer<T> serializer;
    private final BinaryDeserializer<T> deserializer;

    public KalynxSerializer(Class<T> targetClass) throws Throwable {
        this.serializer = new BinarySerializer<>(targetClass);
        this.deserializer = new BinaryDeserializer<>(targetClass);
    }

    @Override
    public  byte[] serialize(T obj) throws Throwable {
        return serializer.serialize(obj);
    }

    @Override
    public T deserialize(byte[] bytes) throws Throwable {
        return deserializer.deserialize(bytes);
    }
}
