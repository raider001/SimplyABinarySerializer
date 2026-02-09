package com.kalynx.simplyabinaryserializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.kalynx.simplyabinaryserializer.TypeMarkers.*;



public class ClassSchema {
    final Field[] fields;
    final int fieldCount;
    final byte[] expectedTypes;     // Pre-computed type markers (-1 = needs runtime check, -2 = List)
    final int fixedHeaderSize;      // TYPE_OBJECT_PACKED + fieldCount + nibbles
    final java.lang.invoke.MethodHandle[] getters; // Fast field getters

    ClassSchema(Class<?> clazz) {
        Field[] allFields = clazz.getDeclaredFields();
        List<Field> serializableFields = new ArrayList<>();
        for (Field field : allFields) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
                field.setAccessible(true);
                serializableFields.add(field);
            }
        }
        this.fields = serializableFields.toArray(new Field[0]);
        this.fieldCount = fields.length;

        // Pre-compute expected types based on declared field type
        this.expectedTypes = new byte[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            Class<?> fieldType = fields[i].getType();
            if (fieldType == String.class) {
                expectedTypes[i] = TYPE_STRING;
            } else if (fieldType == int.class || fieldType == Integer.class) {
                expectedTypes[i] = TYPE_INT;
            } else if (fieldType == long.class || fieldType == Long.class) {
                expectedTypes[i] = TYPE_LONG;
            } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                expectedTypes[i] = TYPE_BOOLEAN;
            } else if (fieldType == double.class || fieldType == Double.class) {
                expectedTypes[i] = TYPE_DOUBLE;
            } else if (fieldType == float.class || fieldType == Float.class) {
                expectedTypes[i] = TYPE_FLOAT;
            } else if (fieldType == short.class || fieldType == Short.class) {
                expectedTypes[i] = TYPE_SHORT;
            } else if (List.class.isAssignableFrom(fieldType)) {
                expectedTypes[i] = -2; // List - check at runtime for string list
            } else if (Map.class.isAssignableFrom(fieldType)) {
                expectedTypes[i] = TYPE_MAP; // Map type
            } else {
                expectedTypes[i] = -1; // Unknown - full runtime detection
            }
        }

        // Create MethodHandle getters for faster field access
        this.getters = new java.lang.invoke.MethodHandle[fieldCount];
        java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup();
        for (int i = 0; i < fieldCount; i++) {
            try {
                getters[i] = lookup.unreflectGetter(fields[i]);
            } catch (IllegalAccessException e) {
                getters[i] = null; // Fallback to Field.get
            }
        }

        this.fixedHeaderSize = 1 + 1 + (fieldCount + 1) / 2; // marker + count + nibbles
    }
}