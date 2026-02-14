package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify array support in OptimizedSerializer
 */
public class ArraySupportTest {

    public static class IntArrayObject {
        public int id;
        public int[] values;
        public IntArrayObject() {}
    }

    public static class StringArrayObject {
        public int id;
        public String[] names;
        public StringArrayObject() {}
    }

    @Test
    public void testIntArray() throws Throwable {
        OptimizedSerializer<IntArrayObject> serializer = new OptimizedSerializer<>(IntArrayObject.class);

        IntArrayObject obj = new IntArrayObject();
        obj.id = 42;
        obj.values = new int[]{1, 2, 3, 4, 5};

        byte[] data = serializer.serialize(obj);
        IntArrayObject result = serializer.deserialize(data);

        assertEquals(42, result.id);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, result.values);

        System.out.println("✅ Int array test passed!");
    }

    @Test
    public void testStringArray() throws Throwable {
        OptimizedSerializer<StringArrayObject> serializer = new OptimizedSerializer<>(StringArrayObject.class);

        StringArrayObject obj = new StringArrayObject();
        obj.id = 99;
        obj.names = new String[]{"Alice", "Bob", "Charlie"};

        byte[] data = serializer.serialize(obj);
        StringArrayObject result = serializer.deserialize(data);

        assertEquals(99, result.id);
        assertArrayEquals(new String[]{"Alice", "Bob", "Charlie"}, result.names);

        System.out.println("✅ String array test passed!");
    }
}

