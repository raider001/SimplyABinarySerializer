package com.kalynx.simplyabinaryserializer.serializer;

import com.kalynx.simplyabinaryserializer.deserializer.FastByteReader;

/**
 * Simple test runner to verify BinarySerializer works with all primitive types.
 */
public class SimplePrimitiveTest {
    
    static class AllPrimitivesObject {
        byte byteVal = 42;
        short shortVal = 1000;
        int intVal = 123456;
        long longVal = 9876543210L;
        float floatVal = 3.14f;
        double doubleVal = 2.718281828;
        boolean boolVal = true;
        char charVal = 'Z';
    }
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("BinarySerializer Primitive Type Test");
        System.out.println("========================================\n");
        
        try {
            // Create serializer
            System.out.println("Creating BinarySerializer...");
            BinarySerializer<AllPrimitivesObject> serializer = new BinarySerializer<>(AllPrimitivesObject.class);
            System.out.println("✓ Serializer created\n");
            
            // Create test object
            AllPrimitivesObject obj = new AllPrimitivesObject();
            System.out.println("Test object values:");
            System.out.println("  byte:    " + obj.byteVal);
            System.out.println("  short:   " + obj.shortVal);
            System.out.println("  int:     " + obj.intVal);
            System.out.println("  long:    " + obj.longVal);
            System.out.println("  float:   " + obj.floatVal);
            System.out.println("  double:  " + obj.doubleVal);
            System.out.println("  boolean: " + obj.boolVal);
            System.out.println("  char:    " + obj.charVal);
            System.out.println();
            
            // Serialize
            System.out.println("Serializing...");
            byte[] bytes = serializer.serialize(obj);
            System.out.println("✓ Serialized to " + bytes.length + " bytes");
            System.out.println("  Expected: 30 bytes (1+2+4+8+4+8+1+2)");
            System.out.println();
            
            // Deserialize and verify
            System.out.println("Verifying serialized values...");
            FastByteReader reader = new FastByteReader();
            reader.setData(bytes);
            
            byte b = reader.readByte();
            short s = reader.readShort();
            int i = reader.readInt();
            long l = reader.readLong();
            float f = reader.readFloat();
            double d = reader.readDouble();
            boolean bool = reader.readBoolean();
            char c = (char) reader.readShort();
            
            System.out.println("Deserialized values:");
            System.out.println("  byte:    " + b + " (expected: 42)");
            System.out.println("  short:   " + s + " (expected: 1000)");
            System.out.println("  int:     " + i + " (expected: 123456)");
            System.out.println("  long:    " + l + " (expected: 9876543210)");
            System.out.println("  float:   " + f + " (expected: 3.14)");
            System.out.println("  double:  " + d + " (expected: 2.718281828)");
            System.out.println("  boolean: " + bool + " (expected: true)");
            System.out.println("  char:    " + c + " (expected: Z)");
            System.out.println();
            
            // Verify correctness
            boolean allCorrect = 
                (b == 42) &&
                (s == 1000) &&
                (i == 123456) &&
                (l == 9876543210L) &&
                (Math.abs(f - 3.14f) < 0.001f) &&
                (Math.abs(d - 2.718281828) < 0.000001) &&
                (bool == true) &&
                (c == 'Z') &&
                (bytes.length == 30);
            
            if (allCorrect) {
                System.out.println("========================================");
                System.out.println("✓✓✓ ALL TESTS PASSED ✓✓✓");
                System.out.println("========================================");
                System.exit(0);
            } else {
                System.out.println("========================================");
                System.out.println("✗✗✗ SOME VALUES INCORRECT ✗✗✗");
                System.out.println("========================================");
                System.exit(1);
            }
            
        } catch (Throwable e) {
            System.out.println("\n========================================");
            System.out.println("✗✗✗ TEST FAILED WITH EXCEPTION ✗✗✗");
            System.out.println("========================================");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

