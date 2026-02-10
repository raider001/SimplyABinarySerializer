package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugDeserialize {

    @Test
    public void debugDeserialization() throws Throwable {
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
        System.out.print("Header and type descriptors: ");
        for (int i = 0; i < Math.min(10, bytes.length); i++) {
            System.out.print(String.format("%02X ", bytes[i] & 0xFF));
        }
        System.out.println();

        // Manually parse the structure
        int pos = 0;
        byte typeMarker = bytes[pos++];
        System.out.println("Type marker: 0x" + Integer.toHexString(typeMarker & 0xFF));

        byte fieldCount = bytes[pos++];
        System.out.println("Field count: " + fieldCount);

        // Read type nibbles
        System.out.println("Type nibbles:");
        for (int i = 0; i < fieldCount; i += 2) {
            byte packed = bytes[pos++];
            byte nibble1 = (byte) ((packed >>> 4) & 0x0F);
            byte nibble2 = (byte) (packed & 0x0F);
            System.out.println("  Fields " + i + "," + (i+1) + ": 0x" + Integer.toHexString(packed & 0xFF) +
                             " = nibble " + Integer.toHexString(nibble1 & 0xFF) + " + nibble " + Integer.toHexString(nibble2 & 0xFF));
        }

        // Show actual fields
        System.out.println("\nActual fields:");
        Field[] fields = getSerializableFields(MixedTestObject.class);
        for (int i = 0; i < fields.length; i++) {
            System.out.println("  " + i + ": " + fields[i].getName() + " (" + fields[i].getType().getSimpleName() + ")");
        }
    }

    private Field[] getSerializableFields(Class<?> clazz) {
        Field[] allFields = clazz.getDeclaredFields();
        List<Field> serializableFields = new ArrayList<>();
        for (Field field : allFields) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
                field.setAccessible(true);
                serializableFields.add(field);
            }
        }
        return serializableFields.toArray(new Field[0]);
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

