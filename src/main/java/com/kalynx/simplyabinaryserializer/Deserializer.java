package com.kalynx.simplyabinaryserializer;

public interface Deserializer {
    <T> T deserialize(byte[] bytes, Class<T> type) throws Throwable;
}
