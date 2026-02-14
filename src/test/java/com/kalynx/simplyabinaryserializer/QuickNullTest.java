package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class QuickNullTest {
    
    public static class TestObj {
        public int id;
        public String name;
        public TestObj() {}
    }
    
    @Test
    public void testNullSerialization() throws Throwable {
        OptimizedSerializer<TestObj> ser = new OptimizedSerializer<>(TestObj.class);
        
        byte[] bytes = ser.serialize(null);
        System.out.println("Serialized null to " + bytes.length + " bytes");
        System.out.println("First byte value: " + bytes[0]);
        System.out.println("TYPE_NULL value: " + TypeMarkers.TYPE_NULL);
        
        assertEquals(1, bytes.length, "Should be 1 byte");
        assertEquals(TypeMarkers.TYPE_NULL, bytes[0], "Should be TYPE_NULL marker");
        
        TestObj result = ser.deserialize(bytes);
        assertNull(result, "Should deserialize to null");
        
        System.out.println("✅ Null test passed!");
    }
}

