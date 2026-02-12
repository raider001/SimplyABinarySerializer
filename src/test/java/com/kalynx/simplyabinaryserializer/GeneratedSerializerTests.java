package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the GeneratedSerializer using runtime bytecode generation.
 */
public class GeneratedSerializerTests {

    // ==================== TEST CLASSES ====================

    public static class SimpleObject {
        public int id;
        public String name;
        public boolean active;
        public long timestamp;
        public double score;

        public SimpleObject() {}

        public SimpleObject(int id, String name, boolean active, long timestamp, double score) {
            this.id = id;
            this.name = name;
            this.active = active;
            this.timestamp = timestamp;
            this.score = score;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimpleObject that = (SimpleObject) o;
            return id == that.id && active == that.active && timestamp == that.timestamp &&
                    Double.compare(that.score, score) == 0 && Objects.equals(name, that.name);
        }
    }

    public static class NestedObject {
        public String label;
        public int value;

        public NestedObject() {}

        public NestedObject(String label, int value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NestedObject that = (NestedObject) o;
            return value == that.value && Objects.equals(label, that.label);
        }
    }

    public static class ComplexObject {
        public int id;
        public String description;
        public NestedObject nested;
        public Map<String, Integer> metadata;
        public boolean enabled;

        public ComplexObject() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComplexObject that = (ComplexObject) o;
            return id == that.id && enabled == that.enabled &&
                    Objects.equals(description, that.description) &&
                    Objects.equals(nested, that.nested) &&
                    Objects.equals(metadata, that.metadata);
        }
    }

    public static class ListObject {
        public List<String> names;
        public List<Integer> numbers;

        public ListObject() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListObject that = (ListObject) o;
            return Objects.equals(names, that.names) && Objects.equals(numbers, that.numbers);
        }
    }

    // ==================== TESTS ====================

    private GeneratedSerializer<SimpleObject> simpleSerializer;
    private GeneratedSerializer<ComplexObject> complexSerializer;
    private GeneratedSerializer<ListObject> listSerializer;

    @BeforeEach
    void setUp() {
        simpleSerializer = new GeneratedSerializer<>(SimpleObject.class);
        complexSerializer = new GeneratedSerializer<>(ComplexObject.class);
        listSerializer = new GeneratedSerializer<>(ListObject.class);
    }

    @Test
    void serialize_SimpleObject_roundTripsCorrectly() throws Throwable {
        SimpleObject original = new SimpleObject(42, "TestObject", true, 1234567890L, 99.99);

        byte[] bytes = simpleSerializer.serialize(original);
        SimpleObject deserialized = simpleSerializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ Simple object test passed");
        System.out.println("  Serialized size: " + bytes.length + " bytes");
    }

    @Test
    void serialize_SimpleObjectWithNullString_roundTripsCorrectly() throws Throwable {
        SimpleObject original = new SimpleObject(42, null, true, 1234567890L, 99.99);

        byte[] bytes = simpleSerializer.serialize(original);
        SimpleObject deserialized = simpleSerializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ Simple object with null string test passed");
    }

    @Test
    void serialize_ComplexObject_roundTripsCorrectly() throws Throwable {
        ComplexObject original = new ComplexObject();
        original.id = 100;
        original.description = "Complex test";
        original.nested = new NestedObject("nested-label", 999);
        original.metadata = new HashMap<>();
        original.metadata.put("key1", 1);
        original.metadata.put("key2", 2);
        original.enabled = true;

        byte[] bytes = complexSerializer.serialize(original);
        ComplexObject deserialized = complexSerializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ Complex object test passed");
        System.out.println("  Serialized size: " + bytes.length + " bytes");
    }

    @Test
    void serialize_ListObject_roundTripsCorrectly() throws Throwable {
        ListObject original = new ListObject();
        original.names = Arrays.asList("Alice", "Bob", "Charlie");
        original.numbers = Arrays.asList(1, 2, 3, 4, 5);

        byte[] bytes = listSerializer.serialize(original);
        ListObject deserialized = listSerializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ List object test passed");
        System.out.println("  Serialized size: " + bytes.length + " bytes");
    }

    @Test
    void serialize_NullObject_returnsNullMarker() throws Throwable {
        byte[] bytes = simpleSerializer.serialize(null);
        assertEquals(1, bytes.length);
        assertEquals(TypeMarkers.TYPE_NULL, bytes[0]);

        SimpleObject deserialized = simpleSerializer.deserialize(bytes);
        assertNull(deserialized);
        System.out.println("✓ Null object test passed");
    }

    @Test
    void performance_GeneratedVsTyped_comparison() throws Throwable {
        final int iterations = 50_000;

        SimpleObject testObj = new SimpleObject(42, "Performance Test", true, System.currentTimeMillis(), 123.456);

        // Warm up
        for (int i = 0; i < 5000; i++) {
            byte[] b = simpleSerializer.serialize(testObj);
            simpleSerializer.deserialize(b);
        }

        // GeneratedSerializer benchmark
        long genStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            byte[] b = simpleSerializer.serialize(testObj);
            simpleSerializer.deserialize(b);
        }
        long genTime = System.nanoTime() - genStart;

        // TypedSerializer benchmark
        TypedSerializer<SimpleObject> typedSerializer = new TypedSerializer<>(SimpleObject.class);

        // Warm up
        for (int i = 0; i < 5000; i++) {
            byte[] b = typedSerializer.serialize(testObj);
            typedSerializer.deserialize(b);
        }

        long typedStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            byte[] b = typedSerializer.serialize(testObj);
            typedSerializer.deserialize(b);
        }
        long typedTime = System.nanoTime() - typedStart;

        double genMs = genTime / 1_000_000.0;
        double typedMs = typedTime / 1_000_000.0;
        double speedup = typedMs / genMs;

        System.out.println("====================================");
        System.out.println("PERFORMANCE COMPARISON (" + iterations + " iterations)");
        System.out.println("====================================");
        System.out.println(String.format("GeneratedSerializer: %.2f ms (%.2f ns/op)", genMs, genTime / (double) iterations));
        System.out.println(String.format("TypedSerializer:     %.2f ms (%.2f ns/op)", typedMs, typedTime / (double) iterations));
        System.out.println(String.format("Speedup:             %.2fx %s", speedup, speedup > 1 ? "FASTER" : "slower"));
        System.out.println("====================================");
    }

    @Test
    void performance_GeneratedVsFury_comparison() throws Throwable {
        final int iterations = 100_000;

        org.apache.fury.Fury fury = org.apache.fury.Fury.builder()
            .withLanguage(org.apache.fury.config.Language.JAVA)
            .requireClassRegistration(false)
            .withRefTracking(false)
            .build();
        fury.register(SimpleObject.class);

        SimpleObject testObj = new SimpleObject(42, "Performance Test", true, System.currentTimeMillis(), 123.456);

        // Warm up both
        for (int i = 0; i < 10000; i++) {
            byte[] b1 = simpleSerializer.serialize(testObj);
            simpleSerializer.deserialize(b1);
            byte[] b2 = fury.serialize(testObj);
            fury.deserialize(b2);
        }

        // GeneratedSerializer benchmark
        long genSerTime = 0, genDesTime = 0;
        byte[][] genData = new byte[iterations][];

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            genData[i] = simpleSerializer.serialize(testObj);
        }
        genSerTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            simpleSerializer.deserialize(genData[i]);
        }
        genDesTime = System.nanoTime() - start;

        // Fury benchmark
        long furySerTime = 0, furyDesTime = 0;
        byte[][] furyData = new byte[iterations][];

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            furyData[i] = fury.serialize(testObj);
        }
        furySerTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            fury.deserialize(furyData[i]);
        }
        furyDesTime = System.nanoTime() - start;

        double genSerNs = genSerTime / (double) iterations;
        double genDesNs = genDesTime / (double) iterations;
        double genRtNs = genSerNs + genDesNs;

        double furySerNs = furySerTime / (double) iterations;
        double furyDesNs = furyDesTime / (double) iterations;
        double furyRtNs = furySerNs + furyDesNs;

        System.out.println("\n====================================");
        System.out.println("GENERATED SERIALIZER VS FURY (" + iterations + " iterations)");
        System.out.println("====================================");
        System.out.println(String.format("GeneratedSerializer: Ser=%.1fns  Des=%.1fns  RT=%.1fns", genSerNs, genDesNs, genRtNs));
        System.out.println(String.format("Fury:                Ser=%.1fns  Des=%.1fns  RT=%.1fns", furySerNs, furyDesNs, furyRtNs));
        System.out.println(String.format("Generated is %.2fx %s than Fury",
            furyRtNs / genRtNs > 1 ? furyRtNs / genRtNs : genRtNs / furyRtNs,
            furyRtNs > genRtNs ? "FASTER" : "slower"));
        System.out.println(String.format("Serialized sizes: Generated=%d bytes, Fury=%d bytes", genData[0].length, furyData[0].length));
        System.out.println("====================================");
    }
}


