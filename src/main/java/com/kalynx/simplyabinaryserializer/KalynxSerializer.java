package com.kalynx.simplyabinaryserializer;

import com.kalynx.simplyabinaryserializer.deserializer.BinaryDeserializer;
import com.kalynx.simplyabinaryserializer.serializer.BinarySerializer;

/**
 * Top-level serializer that delegates to separate serialization and deserialization controllers.
 * This design ensures complete separation between read and write operations.
 *
 * @param <T> The type this serializer handles
 */
public class KalynxSerializer<T> implements OldSerializer, Deserializer {

    private final BinarySerializer<T> serializer;
    private final BinaryDeserializer<T> deserializer;

    public KalynxSerializer(Class<T> targetClass) {
        this.serializer = new BinarySerializer<>(targetClass);
        this.deserializer = new BinaryDeserializer<>(targetClass);
    }

    @Override
    public <R> byte[] serialize(R obj, Class<R> type) throws Throwable {
        return serializer.serialize(obj, type);
    }

    @Override
    public <R> R deserialize(byte[] bytes, Class<R> type) throws Throwable {
        return deserializer.deserialize(bytes, type);
    }
}
