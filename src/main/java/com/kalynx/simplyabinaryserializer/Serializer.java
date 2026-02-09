package com.kalynx.simplyabinaryserializer;

/**
 * Interface for serialization strategies.
 * Implementations handle converting objects to/from byte arrays.
 */
public interface Serializer {

    /**
     * Serialize an object to bytes with explicit type parameter.
     * May be faster for known types as it eliminates runtime type lookup.
     */
    <T> byte[] serialize(T obj, Class<T> type) throws Exception;

}

