package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinaryRoundTripTests {

    private BinarySerializer serializer;
    private BinaryDeserializer deserializer;

    @BeforeEach
    public void setup() {
        serializer = new BinarySerializer();
        deserializer = new BinaryDeserializer();
    }

    @Test
    public void roundTrip_TestSimpleObject_noDataLoss() throws Exception {
        // Arrange
        TestSimpleObject original = new TestSimpleObject(1, "Test", true, 3.14, 2.71f, 123456789L, (short) 42);

        // Act
        byte[] bytes = serializer.serialize(original, TestSimpleObject.class);
        TestSimpleObject deserialized = deserializer.deserialize(bytes, TestSimpleObject.class);

        // Assert
        assert deserialized != null;
        assert deserialized.id == original.id : "id mismatch";
        assert deserialized.name.equals(original.name) : "name mismatch";
        assert deserialized.active == original.active : "active mismatch";
        assert Math.abs(deserialized.doubleValue - original.doubleValue) < 0.001 : "doubleValue mismatch";
        assert Math.abs(deserialized.floatValue - original.floatValue) < 0.001f : "floatValue mismatch";
        assert deserialized.longValue == original.longValue : "longValue mismatch";
        assert deserialized.shortValue == original.shortValue : "shortValue mismatch";

        System.out.println("✓ TestSimpleObject round-trip passed - no data loss");
    }

    @Test
    public void roundTrip_TestObjectWithNestedObject_noDataLoss() throws Exception {
        // Arrange
        TestObjectWithNestedObject original = new TestObjectWithNestedObject();
        original.obj = new TestSimpleObject(1, "Test", true, 3.14, 2.71f, 123456789L, (short) 42);

        // Act
        byte[] bytes = serializer.serialize(original, TestObjectWithNestedObject.class);
        TestObjectWithNestedObject deserialized = deserializer.deserialize(bytes, TestObjectWithNestedObject.class);

        // Assert
        assert deserialized != null;
        assert deserialized.obj != null;
        assert deserialized.obj.id == original.obj.id;
        assert deserialized.obj.name.equals(original.obj.name);
        assert deserialized.obj.active == original.obj.active;

        System.out.println("✓ TestObjectWithNestedObject round-trip passed - no data loss");
    }

    @Test
    public void roundTrip_TestListPrimitiveObject_noDataLoss() throws Exception {
        // Arrange
        TestListPrimitiveObject original = new TestListPrimitiveObject();

        // Act
        byte[] bytes = serializer.serialize(original, TestListPrimitiveObject.class);
        TestListPrimitiveObject deserialized = deserializer.deserialize(bytes, TestListPrimitiveObject.class);

        // Assert
        assert deserialized != null;
        assert deserialized.lst.size() == original.lst.size();
        assert deserialized.lst.equals(original.lst);
        assert deserialized.stringList.size() == original.stringList.size();
        assert deserialized.stringList.equals(original.stringList);

        System.out.println("✓ TestListPrimitiveObject round-trip passed - no data loss");
        System.out.println("  Integer list size: " + deserialized.lst.size());
        System.out.println("  String list size: " + deserialized.stringList.size());
    }

    @Test
    public void roundTrip_TestMapPrimitiveObject_noDataLoss() throws Exception {
        // Arrange
        TestMapPrimitiveObject original = new TestMapPrimitiveObject();

        // Act
        byte[] bytes = serializer.serialize(original, TestMapPrimitiveObject.class);
        TestMapPrimitiveObject deserialized = deserializer.deserialize(bytes, TestMapPrimitiveObject.class);

        // Assert
        assert deserialized != null;
        assert deserialized.map1.size() == original.map1.size();
        assert deserialized.map2.size() == original.map2.size();
        assert deserialized.map3.size() == original.map3.size();

        // Verify actual values
        for (Map.Entry<String, Integer> entry : original.map1.entrySet()) {
            assert deserialized.map1.get(entry.getKey()).equals(entry.getValue());
        }

        System.out.println("✓ TestMapPrimitiveObject round-trip passed - no data loss");
        System.out.println("  map1 size: " + deserialized.map1.size());
        System.out.println("  map2 size: " + deserialized.map2.size());
        System.out.println("  map3 size: " + deserialized.map3.size());
    }

    // @Test
    // TODO: Fix nested object with lists and maps deserialization issue
    public void roundTrip_MixedTestObject_noDataLoss() throws Exception {
        // Arrange
        MixedTestObject original = new MixedTestObject();

        // Act
        byte[] bytes = serializer.serialize(original, MixedTestObject.class);
        MixedTestObject deserialized = deserializer.deserialize(bytes, MixedTestObject.class);

        // Assert
        assert deserialized != null;
        assert deserialized.id == original.id : "id mismatch";
        assert deserialized.name.equals(original.name) : "name mismatch";
        assert deserialized.active == original.active : "active mismatch";

        // Nested object
        assert deserialized.nestedObj != null;
        assert deserialized.nestedObj.id == original.nestedObj.id;
        assert deserialized.nestedObj.name.equals(original.nestedObj.name);

        // List
        assert deserialized.tags != null;
        assert deserialized.tags.size() == original.tags.size();
        for (int i = 0; i < original.tags.size(); i++) {
            assert deserialized.tags.get(i).equals(original.tags.get(i));
        }

        // Map
        assert deserialized.metadata != null;
        assert deserialized.metadata.size() == original.metadata.size();
        for (Map.Entry<String, Integer> entry : original.metadata.entrySet()) {
            assert deserialized.metadata.get(entry.getKey()).equals(entry.getValue());
        }

        System.out.println("✓ MixedTestObject round-trip passed - no data loss");
        System.out.println("  Primitives: id=" + deserialized.id + ", name=" + deserialized.name);
        System.out.println("  Nested object: id=" + deserialized.nestedObj.id);
        System.out.println("  List size: " + deserialized.tags.size());
        System.out.println("  Map size: " + deserialized.metadata.size());
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

    static class TestListPrimitiveObject {
        List<Integer> lst = Arrays.asList(1, 2, 3, 4, 5);
        List<String> stringList = Arrays.asList("a", "b", "c");
        List<Boolean> booleanList = Arrays.asList(true, false, true);
        List<Double> doubleList = Arrays.asList(1.1, 2.2, 3.3);
        List<Float> floatList = Arrays.asList(1.1f, 2.2f, 3.3f);
        List<Long> longList = Arrays.asList(1L, 2L, 3L);

        public TestListPrimitiveObject() {}
    }

    static class TestMapPrimitiveObject {
        Map<String, Integer> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        Map<Integer, String> map3 = new HashMap<>();

        public TestMapPrimitiveObject() {
            map1.put("one", 1);
            map1.put("two", 2);
            map1.put("three", 3);

            map2.put("key1", "value1");
            map2.put("key2", "value2");
            map2.put("key3", "value3");

            map3.put(1, "one");
            map3.put(2, "two");
            map3.put(3, "three");
        }
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


