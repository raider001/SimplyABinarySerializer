package com.kalynx.simplyabinaryserializer.deserializer;

import com.kalynx.simplyabinaryserializer.Deserializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Binary deserialization controller - handles read operations for primitives, lists, and arrays.
 *
 * Design philosophy:
 * - One-way operation: bytes → Object
 * - No references to serializer classes
 * - Optimized for read performance
 * - Supports primitives, List fields, and primitive arrays
 *
 * @param <T> The type this deserializer handles
 */
public class BinaryDeserializer<T> implements Deserializer<T> {

    private final Class<T> targetClass;
    private final ReaderGenerator.PrimitiveReader<T> primitiveReader;
    private final FastByteReader byteReader;
    private final Field[] listFields;
    private final ListReaderGenerator.ListReader[] listReaders;
    private final Field[] arrayFields;
    private final ArrayReaderGenerator.ArrayReader[] arrayReaders;

    public BinaryDeserializer(Class<T> targetClass) throws Throwable {
        this.targetClass = targetClass;
        this.byteReader = new FastByteReader();

        // Analyze fields
        FieldAnalysisResult analysis = analyzeFields(targetClass);

        // Generate optimized reader for primitive fields
        if (analysis.primitiveFields.length > 0) {
            ReaderGenerator generator = new ReaderGenerator();
            this.primitiveReader = generator.generatePrimitiveReader(targetClass, analysis.primitiveFields);
        } else {
            this.primitiveReader = null;
        }

        // Store list fields and generate bytecode list readers
        this.listFields = analysis.listFields.toArray(new Field[0]);
        this.listReaders = new ListReaderGenerator.ListReader[listFields.length];

        // Generate optimized list readers using bytecode (zero overhead!)
        ListReaderGenerator listGenerator = new ListReaderGenerator();
        for (int i = 0; i < listFields.length; i++) {
            Class<?> elementType = analysis.listElementTypes.get(i);
            this.listReaders[i] = listGenerator.generateListReader(elementType);
        }

        // Store array fields and generate bytecode array readers
        this.arrayFields = analysis.arrayFields.toArray(new Field[0]);
        this.arrayReaders = new ArrayReaderGenerator.ArrayReader[arrayFields.length];

        // Generate optimized array readers using bytecode (zero overhead!)
        ArrayReaderGenerator arrayGenerator = new ArrayReaderGenerator();
        for (int i = 0; i < arrayFields.length; i++) {
            Class<?> componentType = analysis.arrayComponentTypes.get(i);
            this.arrayReaders[i] = arrayGenerator.generateArrayReader(componentType);
        }
    }

    private static class FieldAnalysisResult {
        Field[] primitiveFields;
        List<Field> listFields;
        List<Class<?>> listElementTypes;
        List<Field> arrayFields;
        List<Class<?>> arrayComponentTypes;
    }

    /**
     * Analyzes the target class and categorizes fields.
     */
    private FieldAnalysisResult analyzeFields(Class<T> clazz) {
        FieldAnalysisResult result = new FieldAnalysisResult();
        List<Field> primitiveFields = new ArrayList<>();
        List<Field> listFields = new ArrayList<>();
        List<Class<?>> listElementTypes = new ArrayList<>();
        List<Field> arrayFields = new ArrayList<>();
        List<Class<?>> arrayComponentTypes = new ArrayList<>();

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
            // Check if array
            else if (field.getType().isArray()) {
                Class<?> componentType = field.getType().getComponentType();
                if (componentType.isPrimitive()) {
                    arrayFields.add(field);
                    arrayComponentTypes.add(componentType);
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
        }

        result.primitiveFields = primitiveFields.toArray(new Field[0]);
        result.listFields = listFields;
        result.listElementTypes = listElementTypes;
        result.arrayFields = arrayFields;
        result.arrayComponentTypes = arrayComponentTypes;
        return result;
    }

    @Override
    public T deserialize(byte[] bytes) throws Throwable {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        // Set data in reader
        byteReader.setData(bytes);

        // Read primitive fields using generated bytecode (zero overhead)
        T obj;
        if (primitiveReader != null) {
            obj = primitiveReader.read(byteReader);
        } else {
            // No primitive fields, create instance manually
            obj = targetClass.getDeclaredConstructor().newInstance();
        }

        // Read list fields using generated bytecode (zero overhead, no conditionals!)
        for (int i = 0; i < listFields.length; i++) {
            Field field = listFields[i];
            List<?> listValue = listReaders[i].readList(byteReader);
            field.set(obj, listValue);
        }

        // Read array fields using generated bytecode (zero overhead, no conditionals!)
        for (int i = 0; i < arrayFields.length; i++) {
            Field field = arrayFields[i];
            Object arrayValue = arrayReaders[i].readArray(byteReader);
            field.set(obj, arrayValue);
        }

        return obj;
    }
}

