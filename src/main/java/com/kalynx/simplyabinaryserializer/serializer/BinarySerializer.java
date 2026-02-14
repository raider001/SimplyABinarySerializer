package com.kalynx.simplyabinaryserializer.serializer;

import com.kalynx.simplyabinaryserializer.Serializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Binary serialization controller - handles write operations for primitives and lists.
 *
 * Design philosophy:
 * - One-way operation: Object → bytes
 * - No references to deserializer classes
 * - Optimized for write performance
 * - Supports primitives and List fields
 *
 * @param <T> The type this serializer handles
 */
public class BinarySerializer<T> implements Serializer<T> {

    private final Class<T> targetClass;
    private final WriterGenerator.PrimitiveWriter<T> primitiveWriter;
    private final FastByteWriter byteWriter;
    private final Field[] listFields;
    private final ListWriterGenerator.ListWriter[] listWriters;
    private final int estimatedSize;

    public BinarySerializer(Class<T> targetClass) throws Throwable {
        this.targetClass = targetClass;
        this.byteWriter = new FastByteWriter();

        // Analyze fields
        FieldAnalysisResult analysis = analyzeFields(targetClass);

        // Store list fields and generate bytecode list writers
        this.listFields = analysis.listFields.toArray(new Field[0]);
        this.listWriters = new ListWriterGenerator.ListWriter[listFields.length];

        // Generate optimized list writers using bytecode (zero overhead!)
        ListWriterGenerator listGenerator = new ListWriterGenerator();
        for (int i = 0; i < listFields.length; i++) {
            Class<?> elementType = analysis.listElementTypes.get(i);
            this.listWriters[i] = listGenerator.generateListWriter(elementType);
        }

        // Generate optimized writer for primitive fields
        WriterGenerator generator = new WriterGenerator();
        if (analysis.primitiveFields.length > 0) {
            this.primitiveWriter = generator.generatePrimitiveWriter(targetClass, analysis.primitiveFields);
            int primitiveSize = generator.estimatePrimitiveSize(analysis.primitiveFields);
            this.estimatedSize = primitiveSize + estimateListFieldsSize(analysis.listElementTypes);
        } else {
            this.primitiveWriter = null;
            this.estimatedSize = estimateListFieldsSize(analysis.listElementTypes);
        }
    }

    private static class FieldAnalysisResult {
        Field[] primitiveFields;
        List<Field> listFields;
        List<Class<?>> listElementTypes;
    }

    /**
     * Analyzes the target class and categorizes fields.
     */
    private FieldAnalysisResult analyzeFields(Class<T> clazz) {
        FieldAnalysisResult result = new FieldAnalysisResult();
        List<Field> primitiveFields = new ArrayList<>();
        List<Field> listFields = new ArrayList<>();
        List<Class<?>> listElementTypes = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            // Skip static and transient fields
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);

            // Check if primitive
            if (field.getType().isPrimitive()) {
                primitiveFields.add(field);
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
        }

        result.primitiveFields = primitiveFields.toArray(new Field[0]);
        result.listFields = listFields;
        result.listElementTypes = listElementTypes;
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

    /**
     * Serialize an object to bytes using generated bytecode for both primitives and lists.
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

        // Write list fields using generated bytecode (zero overhead, no conditionals!)
        for (int i = 0; i < listFields.length; i++) {
            Field field = listFields[i];
            @SuppressWarnings("unchecked")
            List<?> listValue = (List<?>) field.get(obj);

            listWriters[i].writeList(byteWriter, listValue);
        }

        return byteWriter.toByteArray();
    }
}