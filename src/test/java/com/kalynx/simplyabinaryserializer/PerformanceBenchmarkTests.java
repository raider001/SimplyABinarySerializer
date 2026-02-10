package com.kalynx.simplyabinaryserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerformanceBenchmarkTests {

    private BinarySerializer binarySerializer;
    private BinaryDeserializer binaryDeserializer;
    private ObjectMapper jacksonMapper;

    private static final int WARMUP_ITERATIONS = 10000;
    private static final int BENCHMARK_ITERATIONS = 100000;

    @BeforeEach
    public void setup() {
        binarySerializer = new BinarySerializer();
        binaryDeserializer = new BinaryDeserializer();
        jacksonMapper = new ObjectMapper();
    }

    @Test
    public void benchmark_SimpleObject_Serialization() throws Throwable {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            SimpleObject obj = new SimpleObject(i, "Test" + i, i % 2 == 0, 3.14 + i, 2.71f + i, 123456789L + i, (short) (42 + i % 100));
            binarySerializer.serialize(obj, SimpleObject.class);
            jacksonMapper.writeValueAsBytes(obj);
        }

        // Binary Serializer benchmark - create unique object each time
        long binaryStart = System.nanoTime();
        byte[] binaryResult = null;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            SimpleObject obj = new SimpleObject(i, "Test" + i, i % 2 == 0, 3.14 + i, 2.71f + i, 123456789L + i, (short) (42 + i % 100));
            binaryResult = binarySerializer.serialize(obj, SimpleObject.class);
        }
        long binaryTime = System.nanoTime() - binaryStart;

        // Jackson benchmark - create unique object each time
        long jacksonStart = System.nanoTime();
        byte[] jacksonResult = null;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            SimpleObject obj = new SimpleObject(i, "Test" + i, i % 2 == 0, 3.14 + i, 2.71f + i, 123456789L + i, (short) (42 + i % 100));
            jacksonResult = jacksonMapper.writeValueAsBytes(obj);
        }
        long jacksonTime = System.nanoTime() - jacksonStart;

        printResults("SimpleObject Serialization", binaryTime, jacksonTime, binaryResult.length, jacksonResult.length);
    }

    @Test
    public void benchmark_SimpleObject_Deserialization() throws Throwable {
        // Pre-generate unique serialized objects to avoid caching
        byte[][] binaryBytesArray = new byte[BENCHMARK_ITERATIONS][];
        byte[][] jacksonBytesArray = new byte[BENCHMARK_ITERATIONS][];

        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            SimpleObject obj = new SimpleObject(i, "Test" + i, i % 2 == 0, 3.14 + i, 2.71f + i, 123456789L + i, (short) (42 + i % 100));
            binaryBytesArray[i] = binarySerializer.serialize(obj, SimpleObject.class);
            jacksonBytesArray[i] = jacksonMapper.writeValueAsBytes(obj);
        }

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int idx = i % BENCHMARK_ITERATIONS;
            binaryDeserializer.deserialize(binaryBytesArray[idx], SimpleObject.class);
            jacksonMapper.readValue(jacksonBytesArray[idx], SimpleObject.class);
        }

        // Binary Deserializer benchmark
        long binaryStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            binaryDeserializer.deserialize(binaryBytesArray[i], SimpleObject.class);
        }
        long binaryTime = System.nanoTime() - binaryStart;

        // Jackson benchmark
        long jacksonStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            jacksonMapper.readValue(jacksonBytesArray[i], SimpleObject.class);
        }
        long jacksonTime = System.nanoTime() - jacksonStart;

        printResults("SimpleObject Deserialization", binaryTime, jacksonTime, 0, 0);
    }

    @Test
    public void benchmark_ComplexObject_Serialization() throws Throwable {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS / 10; i++) {
            ComplexObject obj = new ComplexObject(i);
            binarySerializer.serialize(obj, ComplexObject.class);
            jacksonMapper.writeValueAsBytes(obj);
        }

        // Binary Serializer benchmark - create unique object each time
        long binaryStart = System.nanoTime();
        byte[] binaryResult = null;
        for (int i = 0; i < BENCHMARK_ITERATIONS / 10; i++) {
            ComplexObject obj = new ComplexObject(i);
            binaryResult = binarySerializer.serialize(obj, ComplexObject.class);
        }
        long binaryTime = System.nanoTime() - binaryStart;

        // Jackson benchmark - create unique object each time
        long jacksonStart = System.nanoTime();
        byte[] jacksonResult = null;
        for (int i = 0; i < BENCHMARK_ITERATIONS / 10; i++) {
            ComplexObject obj = new ComplexObject(i);
            jacksonResult = jacksonMapper.writeValueAsBytes(obj);
        }
        long jacksonTime = System.nanoTime() - jacksonStart;

        printResults("ComplexObject Serialization (10K iterations)", binaryTime, jacksonTime, binaryResult.length, jacksonResult.length);
    }

    // @Test
    // TODO: Fix ComplexObject deserialization with nested objects and maps
    public void benchmark_ComplexObject_Deserialization() throws Throwable {
        ComplexObject obj = new ComplexObject();
        byte[] binaryBytes = binarySerializer.serialize(obj, ComplexObject.class);
        byte[] jacksonBytes = jacksonMapper.writeValueAsBytes(obj);

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS / 10; i++) {
            binaryDeserializer.deserialize(binaryBytes, ComplexObject.class);
            jacksonMapper.readValue(jacksonBytes, ComplexObject.class);
        }

        // Binary Deserializer benchmark
        long binaryStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS / 10; i++) {
            binaryDeserializer.deserialize(binaryBytes, ComplexObject.class);
        }
        long binaryTime = System.nanoTime() - binaryStart;

        // Jackson benchmark
        long jacksonStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS / 10; i++) {
            jacksonMapper.readValue(jacksonBytes, ComplexObject.class);
        }
        long jacksonTime = System.nanoTime() - jacksonStart;

        printResults("ComplexObject Deserialization (10K iterations)", binaryTime, jacksonTime, 0, 0);
    }

    @Test
    public void benchmark_ListObject_Serialization() throws Throwable {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS / 10; i++) {
            ListObject obj = new ListObject(i);
            binarySerializer.serialize(obj, ListObject.class);
            jacksonMapper.writeValueAsBytes(obj);
        }

        // Binary Serializer benchmark - create unique object each time
        long binaryStart = System.nanoTime();
        byte[] binaryResult = null;
        for (int i = 0; i < BENCHMARK_ITERATIONS / 10; i++) {
            ListObject obj = new ListObject(i);
            binaryResult = binarySerializer.serialize(obj, ListObject.class);
        }
        long binaryTime = System.nanoTime() - binaryStart;

        // Jackson benchmark - create unique object each time
        long jacksonStart = System.nanoTime();
        byte[] jacksonResult = null;
        for (int i = 0; i < BENCHMARK_ITERATIONS / 10; i++) {
            ListObject obj = new ListObject(i);
            jacksonResult = jacksonMapper.writeValueAsBytes(obj);
        }
        long jacksonTime = System.nanoTime() - jacksonStart;

        printResults("ListObject Serialization (10K iterations)", binaryTime, jacksonTime, binaryResult.length, jacksonResult.length);
    }

    private void printResults(String testName, long binaryTimeNs, long jacksonTimeNs, int binarySize, int jacksonSize) {
        double binaryMs = binaryTimeNs / 1_000_000.0;
        double jacksonMs = jacksonTimeNs / 1_000_000.0;
        double speedup = (double) jacksonTimeNs / binaryTimeNs;

        System.out.println("\n" + "=".repeat(80));
        System.out.println(testName);
        System.out.println("=".repeat(80));
        System.out.println(String.format("%-20s %12.2f ms", "BinarySerializer:", binaryMs));
        System.out.println(String.format("%-20s %12.2f ms", "Jackson (JSON):", jacksonMs));
        System.out.println(String.format("%-20s %12.2fx faster", "Speedup:", speedup));

        if (binarySize > 0) {
            double sizeReduction = (1.0 - (double) binarySize / jacksonSize) * 100;
            System.out.println(String.format("%-20s %12d bytes", "Binary size:", binarySize));
            System.out.println(String.format("%-20s %12d bytes", "JSON size:", jacksonSize));
            System.out.println(String.format("%-20s %12.1f%%", "Size reduction:", sizeReduction));
        }

        System.out.println("=".repeat(80));
    }

    // Test classes
    public static class SimpleObject {
        public int id;
        public String name;
        public boolean active;
        public double doubleValue;
        public float floatValue;
        public long longValue;
        public short shortValue;

        public SimpleObject() {}

        public SimpleObject(int id, String name, boolean active, double doubleValue, float floatValue, long longValue, short shortValue) {
            this.id = id;
            this.name = name;
            this.active = active;
            this.doubleValue = doubleValue;
            this.floatValue = floatValue;
            this.longValue = longValue;
            this.shortValue = shortValue;
        }
    }

    public static class ComplexObject {
        public int id;
        public String name;
        public boolean active;
        public SimpleObject nested;
        public Map<String, Integer> data = new HashMap<>();

        public ComplexObject() {
            this(0);
        }

        public ComplexObject(int seed) {
            this.id = 42 + seed;
            this.name = "ComplexObject" + seed;
            this.active = seed % 2 == 0;
            this.nested = new SimpleObject(100 + seed, "Nested" + seed, seed % 3 == 0, 1.23 + seed, 4.56f + seed, 999999L + seed, (short) (10 + seed % 50));
            data.put("count", 5 + seed);
            data.put("version", 2 + seed);
            data.put("priority", 1 + seed);
        }
    }

    public static class ListObject {
        public List<Integer> integers;
        public List<String> strings;
        public List<Boolean> booleans;

        public ListObject() {
            this(0);
        }

        public ListObject(int seed) {
            integers = Arrays.asList(seed+1, seed+2, seed+3, seed+4, seed+5, seed+6, seed+7, seed+8, seed+9, seed+10);
            strings = Arrays.asList("alpha"+seed, "beta"+seed, "gamma"+seed, "delta"+seed, "epsilon"+seed);
            booleans = Arrays.asList(seed % 2 == 0, seed % 3 == 0, seed % 5 == 0);
        }
    }
}







