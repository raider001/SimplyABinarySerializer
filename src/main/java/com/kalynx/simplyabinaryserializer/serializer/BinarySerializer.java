package com.kalynx.simplyabinaryserializer.serializer;

import com.kalynx.simplyabinaryserializer.Serializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binary serialization controller - handles write operations for primitives, lists, arrays, and maps.
 *
 * Design philosophy:
 * - One-way operation: Object → bytes
 * - No references to deserializer classes
 * - Optimized for write performance
 * - Supports primitives, List fields, primitive arrays, and Maps
 *
 * @param <T> The type this serializer handles
 */
public class BinarySerializer<T> implements Serializer<T> {

    private final Class<T> targetClass;
    private final WriterGenerator.PrimitiveWriter<T> primitiveWriter;
    private final FastByteWriter byteWriter;
    private final Field[] stringFields; // Direct String fields
    private final Field[] listFields;
    private final ListWriterGenerator.ListWriter[] listWriters;
    private final Field[] arrayFields;
    private final ArrayWriterGenerator.ArrayWriter[] arrayWriters;
    private final Field[] mapFields;
    private final MapWriterGenerator.MapWriter[] mapWriters;
    private final Field[] objectFields;
    private final ObjectWriterGenerator.ObjectWriter[] objectWriters;
    private final BinarySerializer<?>[] nestedSerializers;
    private final int estimatedSize;

    public BinarySerializer(Class<T> targetClass) throws Throwable {
        this.targetClass = targetClass;
        this.byteWriter = new FastByteWriter();

        // Analyze fields
        FieldAnalysisResult analysis = analyzeFields(targetClass);

        // Store string fields (handled like primitives - direct serialization)
        this.stringFields = analysis.stringFields.toArray(new Field[0]);

        // Store list fields and generate bytecode list writers
        this.listFields = analysis.listFields.toArray(new Field[0]);
        this.listWriters = new ListWriterGenerator.ListWriter[listFields.length];

        // Generate optimized list writers using bytecode (zero overhead!)
        ListWriterGenerator listGenerator = new ListWriterGenerator();
        for (int i = 0; i < listFields.length; i++) {
            Class<?> elementType = analysis.listElementTypes.get(i);
            this.listWriters[i] = listGenerator.generateListWriter(elementType);
        }

        // Store array fields and generate bytecode array writers
        this.arrayFields = analysis.arrayFields.toArray(new Field[0]);
        this.arrayWriters = new ArrayWriterGenerator.ArrayWriter[arrayFields.length];

        // Generate optimized array writers using bytecode (zero overhead!)
        ArrayWriterGenerator arrayGenerator = new ArrayWriterGenerator();
        for (int i = 0; i < arrayFields.length; i++) {
            Class<?> componentType = analysis.arrayComponentTypes.get(i);
            this.arrayWriters[i] = arrayGenerator.generateArrayWriter(componentType);
        }

        // Store map fields and generate bytecode map writers
        this.mapFields = analysis.mapFields.toArray(new Field[0]);
        this.mapWriters = new MapWriterGenerator.MapWriter[mapFields.length];

        // Generate optimized map writers using bytecode (zero overhead!)
        MapWriterGenerator mapGenerator = new MapWriterGenerator();
        for (int i = 0; i < mapFields.length; i++) {
            Class<?> keyType = analysis.mapKeyTypes.get(i);
            Class<?> valueType = analysis.mapValueTypes.get(i);
            this.mapWriters[i] = mapGenerator.generateMapWriter(keyType, valueType);
        }

        // Store object fields and generate bytecode object writers
        this.objectFields = analysis.objectFields.toArray(new Field[0]);
        this.objectWriters = new ObjectWriterGenerator.ObjectWriter[objectFields.length];
        this.nestedSerializers = new BinarySerializer<?>[objectFields.length];

        // Generate optimized object writers using bytecode (zero overhead!)
        ObjectWriterGenerator objectGenerator = new ObjectWriterGenerator();
        for (int i = 0; i < objectFields.length; i++) {
            Class<?> objectType = analysis.objectTypes.get(i);
            this.objectWriters[i] = objectGenerator.generateObjectWriter(objectType);
            this.nestedSerializers[i] = new BinarySerializer<>(objectType);
        }

        // Generate optimized writer for primitive fields
        WriterGenerator generator = new WriterGenerator();
        if (analysis.primitiveFields.length > 0) {
            this.primitiveWriter = generator.generatePrimitiveWriter(targetClass, analysis.primitiveFields);
            int primitiveSize = generator.estimatePrimitiveSize(analysis.primitiveFields);
            int listSize = estimateListFieldsSize(analysis.listElementTypes);
            int arraySize = estimateArrayFieldsSize(analysis.arrayComponentTypes);
            int mapSize = estimateMapFieldsSize(analysis.mapKeyTypes, analysis.mapValueTypes);
            int objectSize = estimateObjectFieldsSize(analysis.objectTypes);
            this.estimatedSize = primitiveSize + listSize + arraySize + mapSize + objectSize;
        } else {
            this.primitiveWriter = null;
            int listSize = estimateListFieldsSize(analysis.listElementTypes);
            int arraySize = estimateArrayFieldsSize(analysis.arrayComponentTypes);
            int mapSize = estimateMapFieldsSize(analysis.mapKeyTypes, analysis.mapValueTypes);
            int objectSize = estimateObjectFieldsSize(analysis.objectTypes);
            this.estimatedSize = listSize + arraySize + mapSize + objectSize;
        }
    }

    private static class FieldAnalysisResult {
        Field[] primitiveFields;
        List<Field> stringFields; // Direct String fields
        List<Field> listFields;
        List<Class<?>> listElementTypes;
        List<Field> arrayFields;
        List<Class<?>> arrayComponentTypes;
        List<Field> mapFields;
        List<Class<?>> mapKeyTypes;
        List<Class<?>> mapValueTypes;
        List<Field> objectFields;
        List<Class<?>> objectTypes;
    }

    /**
     * Analyzes the target class and categorizes fields.
     */
    private FieldAnalysisResult analyzeFields(Class<T> clazz) {
        FieldAnalysisResult result = new FieldAnalysisResult();
        List<Field> primitiveFields = new ArrayList<>();
        List<Field> stringFields = new ArrayList<>(); // String fields
        List<Field> listFields = new ArrayList<>();
        List<Class<?>> listElementTypes = new ArrayList<>();
        List<Field> arrayFields = new ArrayList<>();
        List<Class<?>> arrayComponentTypes = new ArrayList<>();
        List<Field> mapFields = new ArrayList<>();
        List<Class<?>> mapKeyTypes = new ArrayList<>();
        List<Class<?>> mapValueTypes = new ArrayList<>();
        List<Field> objectFields = new ArrayList<>();
        List<Class<?>> objectTypes = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            // Skip static and transient fields
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            // IMPORTANT: Never call setAccessible on String-typed fields to avoid
            // triggering module system checks on String's internal structure.
            // String fields don't need setAccessible since we handle them through
            // simple field.get/set, not deep reflection.
            if (field.getType() != String.class) {
                field.setAccessible(true);
            }

            // Check if primitive
            if (field.getType().isPrimitive()) {
                primitiveFields.add(field);
            }
            // Check if String (handle separately - it's like a primitive for serialization)
            else if (field.getType() == String.class) {
                stringFields.add(field);
            }
            // Check if array
            else if (field.getType().isArray()) {
                Class<?> componentType = field.getType().getComponentType();
                if (componentType.isPrimitive()) {
                    arrayFields.add(field);
                    arrayComponentTypes.add(componentType);
                }
            }
            // Check if Map
            else if (Map.class.isAssignableFrom(field.getType())) {
                mapFields.add(field);

                // Extract generic types for key and value
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) genericType;
                    Type[] typeArgs = paramType.getActualTypeArguments();
                    if (typeArgs.length >= 2) {
                        Class<?> keyType = (typeArgs[0] instanceof Class) ? (Class<?>) typeArgs[0] : Object.class;
                        Class<?> valueType = (typeArgs[1] instanceof Class) ? (Class<?>) typeArgs[1] : Object.class;
                        mapKeyTypes.add(keyType);
                        mapValueTypes.add(valueType);
                    } else {
                        mapKeyTypes.add(Object.class);
                        mapValueTypes.add(Object.class);
                    }
                } else {
                    mapKeyTypes.add(Object.class);
                    mapValueTypes.add(Object.class);
                }
            }
            // Check if List
            else if (List.class.isAssignableFrom(field.getType())) {
                listFields.add(field);

                // Extract generic type
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) genericType;
                    Type[] typeArgs = paramType.getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                        listElementTypes.add((Class<?>) typeArgs[0]);
                    } else {
                        listElementTypes.add(Object.class);
                    }
                } else {
                    listElementTypes.add(Object.class);
                }
            }
            // Otherwise it's a nested object field
            else {
                objectFields.add(field);
                objectTypes.add(field.getType());
            }
        }

        result.primitiveFields = primitiveFields.toArray(new Field[0]);
        result.stringFields = stringFields; // Store string fields
        result.listFields = listFields;
        result.listElementTypes = listElementTypes;
        result.arrayFields = arrayFields;
        result.arrayComponentTypes = arrayComponentTypes;
        result.mapFields = mapFields;
        result.mapKeyTypes = mapKeyTypes;
        result.mapValueTypes = mapValueTypes;
        result.objectFields = objectFields;
        result.objectTypes = objectTypes;
        return result;
    }

    private int estimateListFieldsSize(List<Class<?>> listElementTypes) {
        // Conservative estimate for list fields
        int total = 0;
        for (Class<?> elementType : listElementTypes) {
            total += estimateListSize(elementType, 10); // Assume average of 10 elements
        }
        return total;
    }

    private static int estimateListSize(Class<?> elementType, int estimatedCount) {
        if (estimatedCount == 0) {
            return 4; // size field only
        }

        int size = 4; // size field

        // Estimate based on element type
        if (elementType == Integer.class || elementType == int.class) {
            size += estimatedCount * 5; // 1 byte null marker + 4 bytes int
        } else if (elementType == Long.class || elementType == long.class) {
            size += estimatedCount * 9; // 1 byte null marker + 8 bytes long
        } else if (elementType == Double.class || elementType == double.class) {
            size += estimatedCount * 9; // 1 byte null marker + 8 bytes double
        } else if (elementType == Float.class || elementType == float.class) {
            size += estimatedCount * 5; // 1 byte null marker + 4 bytes float
        } else if (elementType == Short.class || elementType == short.class) {
            size += estimatedCount * 3; // 1 byte null marker + 2 bytes short
        } else if (elementType == Byte.class || elementType == byte.class) {
            size += estimatedCount * 2; // 1 byte null marker + 1 byte
        } else if (elementType == Boolean.class || elementType == boolean.class) {
            size += estimatedCount * 2; // 1 byte null marker + 1 byte boolean
        } else if (elementType == Character.class || elementType == char.class) {
            size += estimatedCount * 3; // 1 byte null marker + 2 bytes char
        } else if (elementType == String.class) {
            // Average string length estimate: 20 bytes
            size += estimatedCount * 24; // 4 bytes length + ~20 bytes content
        } else {
            // Conservative estimate for objects
            size += estimatedCount * 100;
        }

        return size;
    }

    private int estimateArrayFieldsSize(List<Class<?>> arrayComponentTypes) {
        // Conservative estimate for array fields
        int total = 0;
        for (Class<?> componentType : arrayComponentTypes) {
            total += estimateArraySize(componentType, 10); // Assume average of 10 elements
        }
        return total;
    }

    private static int estimateArraySize(Class<?> componentType, int estimatedLength) {
        if (estimatedLength == 0) {
            return 4; // length field only
        }

        int size = 4; // length field

        // Estimate based on component type
        if (componentType == int.class) {
            size += estimatedLength * 4;
        } else if (componentType == long.class) {
            size += estimatedLength * 8;
        } else if (componentType == double.class) {
            size += estimatedLength * 8;
        } else if (componentType == float.class) {
            size += estimatedLength * 4;
        } else if (componentType == short.class) {
            size += estimatedLength * 2;
        } else if (componentType == byte.class) {
            size += estimatedLength * 1;
        } else if (componentType == boolean.class) {
            size += estimatedLength * 1;
        } else if (componentType == char.class) {
            size += estimatedLength * 2;
        }

        return size;
    }

    private int estimateMapFieldsSize(List<Class<?>> mapKeyTypes, List<Class<?>> mapValueTypes) {
        // Conservative estimate for map fields
        int total = 0;
        for (int i = 0; i < mapKeyTypes.size(); i++) {
            Class<?> keyType = mapKeyTypes.get(i);
            Class<?> valueType = mapValueTypes.get(i);
            total += estimateMapSize(keyType, valueType, 10); // Assume average of 10 entries
        }
        return total;
    }

    private int estimateObjectFieldsSize(List<Class<?>> objectTypes) {
        // Conservative estimate for nested objects
        int total = 0;
        for (Class<?> objectType : objectTypes) {
            total += 1 + 4 + 50; // 1 byte type marker, 4 bytes length, ~50 bytes object data
        }
        return total;
    }

    private static int estimateMapSize(Class<?> keyType, Class<?> valueType, int estimatedEntries) {
        if (estimatedEntries == 0) {
            return 4; // size field only
        }

        int size = 4; // size field
        int keySize = estimateTypeSize(keyType);
        int valueSize = estimateTypeSize(valueType);
        size += estimatedEntries * (keySize + valueSize);
        return size;
    }

    private static int estimateTypeSize(Class<?> type) {
        if (type == int.class || type == Integer.class) return 4;
        if (type == long.class || type == Long.class) return 8;
        if (type == double.class || type == Double.class) return 8;
        if (type == float.class || type == Float.class) return 4;
        if (type == short.class || type == Short.class) return 2;
        if (type == byte.class || type == Byte.class) return 1;
        if (type == boolean.class || type == Boolean.class) return 1;
        if (type == char.class || type == Character.class) return 2;
        if (type == String.class) return 24; // 4 bytes length + ~20 bytes content
        return 50; // Conservative estimate for objects
    }

    /**
     * Serialize an object to bytes using generated bytecode for primitives, lists, arrays, and maps.
     * ZERO conditionals - everything is direct bytecode calls.
     *
     * @param obj The object to serialize
     * @return Serialized byte array
     * @throws Throwable if serialization fails
     */
    @Override
    public byte[] serialize(T obj) throws Throwable {
        if (obj == null) {
            return new byte[0];
        }

        // Reset writer with estimated size
        byteWriter.reset(estimatedSize);

        // Write primitive fields using generated bytecode (zero overhead)
        if (primitiveWriter != null) {
            primitiveWriter.write(byteWriter, obj);
        }

        // Write String fields directly (simple and efficient)
        for (Field stringField : stringFields) {
            String stringValue = (String) stringField.get(obj);
            if (stringValue == null) {
                byteWriter.writeInt(-1); // null marker
            } else {
                byte[] strBytes = stringValue.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byteWriter.writeInt(strBytes.length);
                byteWriter.writeBytes(strBytes, strBytes.length);
            }
        }

        // Write list fields using generated bytecode (zero overhead, no conditionals!)
        for (int i = 0; i < listFields.length; i++) {
            Field field = listFields[i];
            @SuppressWarnings("unchecked")
            List<?> listValue = (List<?>) field.get(obj);

            listWriters[i].writeList(byteWriter, listValue);
        }

        // Write array fields using generated bytecode (zero overhead, no conditionals!)
        for (int i = 0; i < arrayFields.length; i++) {
            Field field = arrayFields[i];
            Object arrayValue = field.get(obj);

            arrayWriters[i].writeArray(byteWriter, arrayValue);
        }

        // Write map fields using generated bytecode (zero overhead, no conditionals!)
        for (int i = 0; i < mapFields.length; i++) {
            Field field = mapFields[i];
            @SuppressWarnings("unchecked")
            Map<?, ?> mapValue = (Map<?, ?>) field.get(obj);

            mapWriters[i].writeMap(byteWriter, mapValue);
        }

        // Write nested object fields using generated bytecode (zero overhead, no conditionals!)
        for (int i = 0; i < objectFields.length; i++) {
            Field field = objectFields[i];
            Object objectValue = field.get(obj);

            objectWriters[i].writeObject(byteWriter, objectValue, nestedSerializers[i]);
        }

        return byteWriter.toByteArray();
    }
}

