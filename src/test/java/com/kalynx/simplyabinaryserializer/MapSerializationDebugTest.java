package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class MapSerializationDebugTest {

    @Test
    public void debugMapObjectSerialization() throws Throwable {
        BinarySerializer serializer = new BinarySerializer();
        BinaryDeserializer deserializer = new BinaryDeserializer();

        // Create a simple map object
        MapObject obj = new MapObject();
        obj.map1 = new HashMap<>();
        obj.map1.put("key1", 1);
        obj.map1.put("key2", 2);

        System.out.println("Serializing MapObject with map1 containing 2 entries");

        byte[] bytes = serializer.serialize(obj, MapObject.class);
        System.out.println("Serialized to " + bytes.length + " bytes");
        System.out.println("First 20 bytes: ");
        for (int i = 0; i < Math.min(20, bytes.length); i++) {
            System.out.printf("%02X ", bytes[i] & 0xFF);
        }
        System.out.println();

        System.out.println("Type marker: " + (bytes[0] & 0xFF));

        // Try to deserialize
        MapObject result = deserializer.deserialize(bytes, MapObject.class);
        System.out.println("Deserialized successfully");
        System.out.println("map1 size: " + (result.map1 != null ? result.map1.size() : "null"));
    }

    static class MapObject {
        public Map<String, Integer> map1;
        public Map<String, String> map2;
        public Map<Integer, String> map3;
    }
}

