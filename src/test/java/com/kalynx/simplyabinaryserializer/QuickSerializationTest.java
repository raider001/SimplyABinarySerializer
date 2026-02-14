package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Quick test to verify basic serialization still works
 */
public class QuickSerializationTest {

    public static class SimpleObj {
        public int id;
        public String name;
        public SimpleObj() {}
    }

    public static class ListObj {
        public List<SimpleObj> items;
        public ListObj() {}
    }

    @Test
    public void testSimpleObjectWorks() throws Throwable {
        OptimizedSerializer<SimpleObj> ser = new OptimizedSerializer<>(SimpleObj.class);

        SimpleObj obj = new SimpleObj();
        obj.id = 42;
        obj.name = "test";

        byte[] data = ser.serialize(obj);
        SimpleObj result = ser.deserialize(data);

        assertEquals(42, result.id);
        assertEquals("test", result.name);
        System.out.println("✅ Simple object test passed!");
    }

    @Test
    public void testListObjectWorks() throws Throwable {
        OptimizedSerializer<ListObj> ser = new OptimizedSerializer<>(ListObj.class);

        ListObj obj = new ListObj();
        obj.items = new ArrayList<>();

        SimpleObj item1 = new SimpleObj();
        item1.id = 1;
        item1.name = "one";
        obj.items.add(item1);

        SimpleObj item2 = new SimpleObj();
        item2.id = 2;
        item2.name = "two";
        obj.items.add(item2);

        byte[] data = ser.serialize(obj);
        ListObj result = ser.deserialize(data);

        assertEquals(2, result.items.size());
        assertEquals(1, result.items.get(0).id);
        assertEquals("one", result.items.get(0).name);
        assertEquals(2, result.items.get(1).id);
        assertEquals("two", result.items.get(1).name);

        System.out.println("✅ List object test passed!");
    }
}

