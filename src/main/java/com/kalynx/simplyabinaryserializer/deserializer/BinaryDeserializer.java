package com.kalynx.simplyabinaryserializer.deserializer;

import com.kalynx.simplyabinaryserializer.Deserializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binary deserialization controller - handles read operations for primitives, lists, arrays, and maps.
 *
 * Design philosophy:
 * - One-way operation: bytes → Object
 * - No references to serializer classes
 * - Optimized for read performance
 * - Supports primitives, List fields, primitive arrays, and Maps
 *
 * @param <T> The type this deserializer handles
 */
public class BinaryDeserializer<T> implements Deserializer<T> {

    private final Class<T> targetClass;
    private final ReaderGenerator.PrimitiveReader<T> primitiveReader;
    private final FastByteReader byteReader;
    private final Field[] stringFields; // Direct String fields
    private final Field[] listFields;
    private final ListReaderGenerator.ListReader[] listReaders;
    private final Field[] arrayFields;
    private final ArrayReaderGenerator.ArrayReader[] arrayReaders;
    private final Field[] mapFields;
    private final MapReaderGenerator.MapReader[] mapReaders;
    private final Field[] objectFields;
    private final ObjectReaderGenerator.ObjectReader[] objectReaders;
    private final BinaryDeserializer<?>[] nestedDeserializers;

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

        // Store string fields (handled like primitives - direct deserialization)
        this.stringFields = analysis.stringFields.toArray(new Field[0]);

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

        // Store map fields and generate bytecode map readers
        this.mapFields = analysis.mapFields.toArray(new Field[0]);
        this.mapReaders = new MapReaderGenerator.MapReader[mapFields.length];

        // Generate optimized map readers using bytecode (zero overhead!)
        MapReaderGenerator mapGenerator = new MapReaderGenerator();
        for (int i = 0; i < mapFields.length; i++) {
            Class<?> keyType = analysis.mapKeyTypes.get(i);
            Class<?> valueType = analysis.mapValueTypes.get(i);
            this.mapReaders[i] = mapGenerator.generateMapReader(keyType, valueType);
        }

        // Store object fields and generate bytecode object readers
        this.objectFields = analysis.objectFields.toArray(new Field[0]);
        this.objectReaders = new ObjectReaderGenerator.ObjectReader[objectFields.length];
        this.nestedDeserializers = new BinaryDeserializer<?>[objectFields.length];

        // Generate optimized object readers using bytecode (zero overhead!)
        ObjectReaderGenerator objectGenerator = new ObjectReaderGenerator();
        for (int i = 0; i < objectFields.length; i++) {
            Class<?> objectType = analysis.objectTypes.get(i);
            this.objectReaders[i] = objectGenerator.generateObjectReader(objectType);
            this.nestedDeserializers[i] = new BinaryDeserializer<>(objectType);
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

        // Read String fields directly (simple and efficient)
        for (Field stringField : stringFields) {
            int length = byteReader.readInt();
            if (length == -1) {
                stringField.set(obj, null); // null string
            } else {
                byte[] strBytes = byteReader.readBytes(length);
                String stringValue = new String(strBytes, java.nio.charset.StandardCharsets.UTF_8);
                stringField.set(obj, stringValue);
            }
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

        // Read map fields using generated bytecode (zero overhead, no conditionals!)
        for (int i = 0; i < mapFields.length; i++) {
            Field field = mapFields[i];
            Map<?, ?> mapValue = mapReaders[i].readMap(byteReader);
            field.set(obj, mapValue);
        }

        // Read nested object fields using generated bytecode (zero overhead, no conditionals!)
        for (int i = 0; i < objectFields.length; i++) {
            Field field = objectFields[i];
            Object objectValue = objectReaders[i].readObject(byteReader, nestedDeserializers[i]);
            field.set(obj, objectValue);
        }

        return obj;
    }
}

