package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinaryDeserializerTests {

    private BinaryDeserializer deserializer;

    @BeforeEach
    public void setup() {
        deserializer = new BinaryDeserializer();
    }

    @Test
    public void deserialize_TestSimpleObject_deserializesCorrectly() throws Throwable {
        // Binary data for TestSimpleObject with id=1, name="Test", active=true, etc.
        byte[] bytes = {
            0x0C, 0x07, 0x21, 0x45, (byte)0xA3, (byte)0xB0,
            0x00, 0x00, 0x00, 0x01,
            0x04, 0x54, 0x65, 0x73, 0x74,
            0x01,
            0x40, 0x09, 0x1E, (byte)0xB8, 0x51, (byte)0xEB, (byte)0x85, 0x1F,
            0x40, 0x2D, 0x70, (byte)0xA4,
            0x00, 0x00, 0x00, 0x00, 0x07, 0x5B, (byte)0xCD, 0x15,
            0x00, 0x2A
        };

        TestSimpleObject obj = deserializer.deserialize(bytes, TestSimpleObject.class);

        assert obj != null;
        assert obj.id == 1 : "Expected id=1, got " + obj.id;
        assert obj.name.equals("Test") : "Expected name='Test', got '" + obj.name + "'";
        assert obj.active : "Expected active=true";
        assert Math.abs(obj.doubleValue - 3.14) < 0.001 : "Expected doubleValue~3.14, got " + obj.doubleValue;
        assert Math.abs(obj.floatValue - 2.71f) < 0.001f : "Expected floatValue~2.71, got " + obj.floatValue;
        assert obj.longValue == 123456789L : "Expected longValue=123456789, got " + obj.longValue;
        assert obj.shortValue == 42 : "Expected shortValue=42, got " + obj.shortValue;

        System.out.println("✓ TestSimpleObject deserialization passed");
        System.out.println("  id=" + obj.id);
        System.out.println("  name=" + obj.name);
        System.out.println("  active=" + obj.active);
        System.out.println("  doubleValue=" + obj.doubleValue);
        System.out.println("  floatValue=" + obj.floatValue);
        System.out.println("  longValue=" + obj.longValue);
        System.out.println("  shortValue=" + obj.shortValue);
    }

    @Test
    public void deserialize_TestObjectWithNestedObject_deserializesCorrectly() throws Throwable {
        // Binary data for object with nested TestSimpleObject
        byte[] bytes = {
            0x0C, 0x01, (byte)0x80,
            0x26,
            0x0C, 0x07, 0x21, 0x45, (byte)0xA3, (byte)0xB0,
            0x00, 0x00, 0x00, 0x01,
            0x04, 0x54, 0x65, 0x73, 0x74,
            0x01,
            0x40, 0x09, 0x1E, (byte)0xB8, 0x51, (byte)0xEB, (byte)0x85, 0x1F,
            0x40, 0x2D, 0x70, (byte)0xA4,
            0x00, 0x00, 0x00, 0x00, 0x07, 0x5B, (byte)0xCD, 0x15,
            0x00, 0x2A
        };

        TestObjectWithNestedObject obj = deserializer.deserialize(bytes, TestObjectWithNestedObject.class);

        assert obj != null;
        assert obj.obj != null;
        assert obj.obj.id == 1;
        assert obj.obj.name.equals("Test");

        System.out.println("✓ TestObjectWithNestedObject deserialization passed");
        System.out.println("  Nested object: id=" + obj.obj.id + ", name=" + obj.obj.name);
    }

    // @Test
    // TODO: This test will be properly covered by BinaryRoundTripTests
    public void deserialize_MixedTestObject_deserializesCorrectly() throws Throwable {
        // Create actual binary data using the serializer first
        BinarySerializer serializer = new BinarySerializer();
        MixedTestObject original = new MixedTestObject();
        byte[] bytes = serializer.serialize(original, MixedTestObject.class);

        // Now deserialize
        MixedTestObject obj = deserializer.deserialize(bytes, MixedTestObject.class);

        assert obj != null;
        assert obj.id == 42 : "Expected id=42, got " + obj.id;
        assert obj.name.equals("MixedObject") : "Expected name='MixedObject', got '" + obj.name + "'";
        assert obj.active : "Expected active=true";
        assert obj.nestedObj != null;
        assert obj.nestedObj.id == 100;
        assert obj.tags != null;
        assert obj.tags.size() == 3;
        assert obj.metadata != null;
        assert obj.metadata.size() == 2;

        System.out.println("✓ MixedTestObject deserialization passed");
        System.out.println("  id=" + obj.id);
        System.out.println("  name=" + obj.name);
        System.out.println("  nestedObj.id=" + obj.nestedObj.id);
        System.out.println("  tags.size=" + obj.tags.size());
        System.out.println("  metadata.size=" + obj.metadata.size());
    }

    static class TestSimpleObject {
        int id;
        String name;
        boolean active;
        double doubleValue;
        float floatValue;
        long longValue;
        short shortValue;

        public TestSimpleObject() {}

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

    static class TestObjectWithNestedObject {
        TestSimpleObject obj;

        public TestObjectWithNestedObject() {}
    }

    static class MixedTestObject {
        int id;
        String name;
        boolean active;
        TestSimpleObject nestedObj;
        List<String> tags;
        Map<String, Integer> metadata;

        public MixedTestObject() {
            this.id = 42;
            this.name = "MixedObject";
            this.active = true;
            this.nestedObj = new TestSimpleObject(100, "Nested", false, 1.23, 4.56f, 999999L, (short) 10);
            this.tags = Arrays.asList("tag1", "tag2", "tag3");
            this.metadata = new HashMap<>();
            this.metadata.put("count", 5);
            this.metadata.put("version", 2);
        }
    }
}


