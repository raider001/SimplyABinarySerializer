package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify nested list optimization works correctly
 */
public class NestedListOptimizationTest {

    public static class NestedObject {
        public int id;
        public String name;
        public double value;
        public NestedObject() {}
    }

    public static class ComplexListObject {
        public String id;
        public List<NestedObject> items;
        public ComplexListObject() {}
    }

    @Test
    public void testNestedListSerializationDeserialization() throws Throwable {
        System.out.println("\n=== Testing Nested List Optimization ===");

        OptimizedSerializer<ComplexListObject> serializer = new OptimizedSerializer<>(ComplexListObject.class);

        // Create test object
        ComplexListObject obj = new ComplexListObject();
        obj.id = "test-123";
        obj.items = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            NestedObject nested = new NestedObject();
            nested.id = i;
            nested.name = "Item" + i;
            nested.value = i * 1.5;
            obj.items.add(nested);
        }

        // Serialize
        long serStart = System.nanoTime();
        byte[] data = serializer.serialize(obj);
        long serTime = System.nanoTime() - serStart;

        // Deserialize
        long desStart = System.nanoTime();
        ComplexListObject result = serializer.deserialize(data);
        long desTime = System.nanoTime() - desStart;

        // Verify correctness
        assertEquals("test-123", result.id);
        assertEquals(5, result.items.size());

        for (int i = 0; i < 5; i++) {
            NestedObject item = result.items.get(i);
            assertEquals(i, item.id);
            assertEquals("Item" + i, item.name);
            assertEquals(i * 1.5, item.value, 0.001);
        }

        System.out.println("✅ Correctness test passed!");
        System.out.println("Serialization:   " + serTime + " ns");
        System.out.println("Deserialization: " + desTime + " ns");
        System.out.println("Binary size:     " + data.length + " bytes");
        System.out.println("Des/Ser ratio:   " + String.format("%.2f", (double)desTime / serTime) + "x");

        // The optimization should make deserialization closer to serialization speed
        assertTrue(desTime < serTime * 4,
            "Deserialization should not be more than 4x slower than serialization. " +
            "Actual ratio: " + String.format("%.2f", (double)desTime / serTime) + "x");
    }

    @Test
    public void testLargeNestedList() throws Throwable {
        System.out.println("\n=== Testing Large Nested List (Performance) ===");

        OptimizedSerializer<ComplexListObject> serializer = new OptimizedSerializer<>(ComplexListObject.class);

        // Create larger test object
        ComplexListObject obj = new ComplexListObject();
        obj.id = "large-test";
        obj.items = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            NestedObject nested = new NestedObject();
            nested.id = i;
            nested.name = "LargeItem" + i + "_WithLongerName";
            nested.value = i * 2.5;
            obj.items.add(nested);
        }

        // Warmup
        for (int i = 0; i < 1000; i++) {
            byte[] data = serializer.serialize(obj);
            serializer.deserialize(data);
        }

        // Benchmark
        int iterations = 10000;
        long totalSerTime = 0;
        long totalDesTime = 0;
        byte[] data = null;

        for (int i = 0; i < iterations; i++) {
            long serStart = System.nanoTime();
            data = serializer.serialize(obj);
            totalSerTime += System.nanoTime() - serStart;

            long desStart = System.nanoTime();
            serializer.deserialize(data);
            totalDesTime += System.nanoTime() - desStart;
        }

        double avgSer = totalSerTime / (double) iterations;
        double avgDes = totalDesTime / (double) iterations;

        System.out.println("Iterations:      " + iterations);
        System.out.println("Avg Serialize:   " + String.format("%.2f", avgSer) + " ns/op");
        System.out.println("Avg Deserialize: " + String.format("%.2f", avgDes) + " ns/op");
        System.out.println("Binary size:     " + data.length + " bytes");
        System.out.println("Des/Ser ratio:   " + String.format("%.2f", avgDes / avgSer) + "x");

        System.out.println("\n✅ Performance test completed!");
        System.out.println("Target: Deserialization < 3x serialization time");
        System.out.println("Result: " + (avgDes < avgSer * 3 ? "PASSED ✅" : "NEEDS MORE WORK ⚠️"));
    }
}

