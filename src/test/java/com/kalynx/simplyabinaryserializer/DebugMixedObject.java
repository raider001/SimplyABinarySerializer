package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugMixedObject {

    @Test
    public void debugSerializedBytes() throws Throwable {
        BinarySerializer serializer = new BinarySerializer();

        MixedTestObject obj = new MixedTestObject();
        obj.id = 42;
        obj.name = "MixedObject";
        obj.active = true;
        obj.nestedObj = new TestSimpleObject(100, "Nested", false, 1.23, 4.56f, 999999L, (short) 10);
        obj.tags = Arrays.asList("tag1", "tag2", "tag3");
        obj.metadata = new HashMap<>();
        obj.metadata.put("count", 5);
        obj.metadata.put("version", 2);

        byte[] bytes = serializer.serialize(obj, MixedTestObject.class);

        System.out.println("Serialized " + bytes.length + " bytes");
        System.out.print("Hex: ");
        for (int i = 0; i < Math.min(60, bytes.length); i++) {
            System.out.print(String.format("%02X ", bytes[i] & 0xFF));
        }
        System.out.println();

        // Show field order
        System.out.println("\nField order via reflection:");
        java.lang.reflect.Field[] fields = MixedTestObject.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            System.out.println(i + ": " + fields[i].getName() + " (" + fields[i].getType().getSimpleName() + ")");
        }
    }

    static class MixedTestObject {
        int id;
        String name;
        boolean active;
        TestSimpleObject nestedObj;
        List<String> tags;
        Map<String, Integer> metadata;
    }

    static class TestSimpleObject {
        int id;
        String name;
        boolean active;
        double doubleValue;
        float floatValue;
        long longValue;
        short shortValue;

        public TestSimpleObject(int id, String name, boolean active, double doubleValue, float floatValue, long longValue, short shortValue) {
            this.id = id;
            this.name = name;
            this.active = active;
            this.doubleValue = doubleValue;
            this.floatValue = floatValue;
            this.longValue = longValue;
            this.shortValue = shortValue;
        }
    }
}
