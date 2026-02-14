package com.kalynx.simplyabinaryserializer;

public interface Deserializer<T> {
     T deserialize(byte[] bytes) throws Throwable;
}
