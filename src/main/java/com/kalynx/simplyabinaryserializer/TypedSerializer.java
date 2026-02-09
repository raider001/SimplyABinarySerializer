package com.kalynx.simplyabinaryserializer;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.kalynx.simplyabinaryserializer.TypeMarkers.*;

/**
 * Type-specific serializer that builds a complete schema upfront for a given class.
 * Eliminates all runtime type detection by pre-computing the entire object graph structure.
 *
 * Usage:
 * <pre>
 * TypedSerializer<MyClass> serializer = new TypedSerializer<>(MyClass.class);
 * byte[] data = serializer.serialize(myObject);
 * MyClass restored = serializer.deserialize(data);
 * </pre>
 *
 * @param <T> The type this serializer handles
 */
public class TypedSerializer<T> implements Serializer, Deserializer {

    private final Class<T> targetClass;
    private final SerializationSchema schema;

    // Thread-local writer for zero-allocation serialization
    private static final ThreadLocal<FastByteWriter> WRITER_POOL =
            ThreadLocal.withInitial(FastByteWriter::new);

    // Thread-local reader for deserialization
    private static final ThreadLocal<FastByteReader> READER_POOL =
            ThreadLocal.withInitial(FastByteReader::new);

    /**
     * Creates a typed serializer for the given class.
     * Performs deep schema analysis on construction.
     */
    public TypedSerializer(Class<T> targetClass) {
        this.targetClass = targetClass;
        this.schema = buildSchema(targetClass);
    }

    @Override
    public <U> byte[] serialize(U obj, Class<U> type) throws Exception {
        if (!targetClass.isInstance(obj)) {
            throw new IllegalArgumentException("Object must be of type " + targetClass.getName());
        }
        return serialize(targetClass.cast(obj));
    }

    /**
     * Serialize an object using the pre-computed schema.
     * Optimized: use conservative allocation, no size calculation pass.
     */
    public byte[] serialize(T obj) throws Exception {
        if (obj == null) {
            return new byte[] { TYPE_NULL };
        }

        FastByteWriter writer = WRITER_POOL.get();

        // Conservative allocation: estimate size without full traversal
        int estimatedSize = estimateSize(schema) * 2; // 2x safety margin
        writer.reset(estimatedSize);

        writer.writeByte(TYPE_OBJECT_PACKED);
        writeObjectOptimized(writer, obj, schema);

        return writer.toByteArray();
    }

    /**
     * Fast size estimation based on schema without object traversal.
     * Uses indexed loop for JIT optimization.
     */
    private int estimateSize(SerializationSchema schema) {
        int estimate = 10; // header
        int fieldCount = schema.fields.length;
        for (int i = 0; i < fieldCount; i++) {
            switch (schema.fields[i].type) {
                case INT: case FLOAT: estimate += 5; break;
                case LONG: case DOUBLE: estimate += 9; break;
                case BOOLEAN: case SHORT: estimate += 3; break;
                case STRING: estimate += 30; break; // avg string
                case LIST: estimate += 50; break; // avg list
                case MAP: estimate += 100; break; // avg map
                case OBJECT: estimate += 50; break; // nested object
            }
        }
        return estimate;
    }

    @Override
    public <U> U deserialize(byte[] data, Class<U> type) throws Exception {
        if (!targetClass.equals(type)) {
            throw new IllegalArgumentException("Type must be " + targetClass.getName());
        }
        return type.cast(deserialize(data));
    }

    /**
     * Deserialize data using the pre-computed schema.
     */
    public T deserialize(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            return null;
        }

        FastByteReader reader = READER_POOL.get();
        reader.setData(data);

        byte typeMarker = reader.readByte();
        if (typeMarker == TYPE_NULL) {
            return null;
        }

        if (typeMarker != TYPE_OBJECT_PACKED) {
            throw new IllegalArgumentException("Invalid type marker: " + typeMarker);
        }

        return readObject(reader, schema);
    }

    /**
     * Deep schema builder that analyzes the entire object graph.
     */
    private SerializationSchema buildSchema(Class<?> clazz) {
        Map<Class<?>, SerializationSchema> schemaCache = new HashMap<>();
        return buildSchemaRecursive(clazz, schemaCache);
    }

    private SerializationSchema buildSchemaRecursive(Class<?> clazz, Map<Class<?>, SerializationSchema> cache) {
        // Check cache to handle circular references
        if (cache.containsKey(clazz)) {
            return cache.get(clazz);
        }

        Field[] fields = clazz.getDeclaredFields();
        List<FieldSchema> fieldSchemas = new ArrayList<>();

        // Create placeholder to handle circular references
        SerializationSchema schema = new SerializationSchema(clazz, new FieldSchema[0]);
        cache.put(clazz, schema);

        for (Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);
            FieldSchema fieldSchema = analyzeField(field, cache);
            fieldSchemas.add(fieldSchema);
        }

        schema.fields = fieldSchemas.toArray(new FieldSchema[0]);
        return schema;
    }

    private FieldSchema analyzeField(Field field, Map<Class<?>, SerializationSchema> cache) {
        Class<?> fieldType = field.getType();

        // Primitive types
        if (fieldType == int.class || fieldType == Integer.class) {
            return new FieldSchema(field, FieldType.INT, null, null, null);
        }
        if (fieldType == long.class || fieldType == Long.class) {
            return new FieldSchema(field, FieldType.LONG, null, null, null);
        }
        if (fieldType == boolean.class || fieldType == Boolean.class) {
            return new FieldSchema(field, FieldType.BOOLEAN, null, null, null);
        }
        if (fieldType == double.class || fieldType == Double.class) {
            return new FieldSchema(field, FieldType.DOUBLE, null, null, null);
        }
        if (fieldType == float.class || fieldType == Float.class) {
            return new FieldSchema(field, FieldType.FLOAT, null, null, null);
        }
        if (fieldType == short.class || fieldType == Short.class) {
            return new FieldSchema(field, FieldType.SHORT, null, null, null);
        }
        if (fieldType == String.class) {
            return new FieldSchema(field, FieldType.STRING, null, null, null);
        }

        // Collections
        if (List.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            Class<?> elementType = extractGenericType(genericType, 0);
            FieldType listElementType = determineFieldType(elementType);
            SerializationSchema elementSchema = null;
            if (listElementType == FieldType.OBJECT) {
                elementSchema = buildSchemaRecursive(elementType, cache);
            }
            return new FieldSchema(field, FieldType.LIST, listElementType, null, elementSchema);
        }

        if (Map.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            Class<?> keyType = extractGenericType(genericType, 0);
            Class<?> valueType = extractGenericType(genericType, 1);
            FieldType mapKeyType = determineFieldType(keyType);
            FieldType mapValueType = determineFieldType(valueType);
            SerializationSchema keySchema = mapKeyType == FieldType.OBJECT ? buildSchemaRecursive(keyType, cache) : null;
            SerializationSchema valueSchema = mapValueType == FieldType.OBJECT ? buildSchemaRecursive(valueType, cache) : null;
            return new FieldSchema(field, FieldType.MAP, mapKeyType, mapValueType, null)
                    .withMapSchemas(keySchema, valueSchema);
        }

        // Nested objects
        SerializationSchema nestedSchema = buildSchemaRecursive(fieldType, cache);
        return new FieldSchema(field, FieldType.OBJECT, null, null, nestedSchema);
    }

    private Class<?> extractGenericType(Type genericType, int index) {
        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > index) {
                Type typeArg = typeArgs[index];
                if (typeArg instanceof Class) {
                    return (Class<?>) typeArg;
                }
            }
        }
        return Object.class;
    }

    private FieldType determineFieldType(Class<?> clazz) {
        if (clazz == int.class || clazz == Integer.class) return FieldType.INT;
        if (clazz == long.class || clazz == Long.class) return FieldType.LONG;
        if (clazz == boolean.class || clazz == Boolean.class) return FieldType.BOOLEAN;
        if (clazz == double.class || clazz == Double.class) return FieldType.DOUBLE;
        if (clazz == float.class || clazz == Float.class) return FieldType.FLOAT;
        if (clazz == short.class || clazz == Short.class) return FieldType.SHORT;
        if (clazz == String.class) return FieldType.STRING;
        return FieldType.OBJECT;
    }

    // Optimized serialization methods

    /**
     * Optimized object writing - no nibbles, types known from schema.
     * Uses indexed loop for maximum JIT optimization.
     */
    private void writeObjectOptimized(FastByteWriter writer, Object obj, SerializationSchema schema) throws Exception {
        // Write field count
        int fieldCount = schema.fields.length;
        writer.writeVarint(fieldCount);

        // Write field data directly - schema knows all types, use indexed loop for JIT
        for (int i = 0; i < fieldCount; i++) {
            FieldSchema fieldSchema = schema.fields[i];
            Object value = fieldSchema.field.get(obj);
            if (value != null) {
                writer.writeByte(1); // not null marker
                writeFieldValueOptimized(writer, value, fieldSchema);
            } else {
                writer.writeByte(0); // null marker
            }
        }
    }

    /**
     * Optimized field writing - inlined for primitives, reuses writer for nested.
     */
    private void writeFieldValueOptimized(FastByteWriter writer, Object value, FieldSchema fieldSchema) throws Exception {
        switch (fieldSchema.type) {
            case INT: writer.writeInt((Integer) value); break;
            case LONG: writer.writeLong((Long) value); break;
            case BOOLEAN: writer.writeBoolean((Boolean) value); break;
            case DOUBLE: writer.writeDouble((Double) value); break;
            case FLOAT: writer.writeFloat((Float) value); break;
            case SHORT: writer.writeShort((Short) value); break;
            case STRING:
                String str = (String) value;
                byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
                writer.writeVarint(bytes.length);
                writer.writeBytes(bytes);
                break;
            case LIST:
                writeListOptimized(writer, (List<?>) value, fieldSchema);
                break;
            case MAP:
                writeMapOptimized(writer, (Map<?, ?>) value, fieldSchema);
                break;
            case OBJECT:
                // Serialize nested object to temporary buffer
                FastByteWriter nestedWriter = new FastByteWriter();
                nestedWriter.reset(estimateSize(fieldSchema.nestedSchema) * 2);
                writeObjectOptimized(nestedWriter, value, fieldSchema.nestedSchema);
                byte[] nestedBytes = nestedWriter.toByteArray();
                writer.writeVarint(nestedBytes.length);
                writer.writeBytes(nestedBytes);
                break;
        }
    }

    private void writeListOptimized(FastByteWriter writer, List<?> list, FieldSchema fieldSchema) throws Exception {
        writer.writeInt(list.size());
        writer.writeByte(1); // uniform
        writer.writeByte(getTypeMarkerFromFieldType(fieldSchema.listElementType));

        for (Object item : list) {
            writeElementOptimized(writer, item, fieldSchema.listElementType, fieldSchema.elementSchema);
        }
    }

    private void writeMapOptimized(FastByteWriter writer, Map<?, ?> map, FieldSchema fieldSchema) throws Exception {
        writer.writeInt(map.size());
        writer.writeByte(3); // both uniform
        writer.writeByte(getTypeMarkerFromFieldType(fieldSchema.mapKeyType));
        writer.writeByte(getTypeMarkerFromFieldType(fieldSchema.mapValueType));

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            writeElementOptimized(writer, entry.getKey(), fieldSchema.mapKeyType, fieldSchema.mapKeySchema);
            writeElementOptimized(writer, entry.getValue(), fieldSchema.mapValueType, fieldSchema.mapValueSchema);
        }
    }

    private void writeElementOptimized(FastByteWriter writer, Object value, FieldType type, SerializationSchema schema) throws Exception {
        if (value == null) return;

        switch (type) {
            case INT: writer.writeInt((Integer) value); break;
            case LONG: writer.writeLong((Long) value); break;
            case BOOLEAN: writer.writeBoolean((Boolean) value); break;
            case DOUBLE: writer.writeDouble((Double) value); break;
            case FLOAT: writer.writeFloat((Float) value); break;
            case SHORT: writer.writeShort((Short) value); break;
            case STRING:
                byte[] bytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                writer.writeShort((short) bytes.length);
                writer.writeBytes(bytes);
                break;
            case OBJECT:
                FastByteWriter nestedWriter = new FastByteWriter();
                nestedWriter.reset(estimateSize(schema) * 2);
                writeObjectOptimized(nestedWriter, value, schema);
                byte[] nestedBytes = nestedWriter.toByteArray();
                writer.writeVarint(nestedBytes.length);
                writer.writeBytes(nestedBytes);
                break;
        }
    }

    // Keep old methods for backward compatibility but they won't be called

    private void writeObject(FastByteWriter writer, Object obj, SerializationSchema schema) throws Exception {
        writeObjectOptimized(writer, obj, schema);
    }

    private byte getTypeMarkerFromFieldType(FieldType type) {
        switch (type) {
            case INT: return TYPE_INT;
            case LONG: return TYPE_LONG;
            case BOOLEAN: return TYPE_BOOLEAN;
            case DOUBLE: return TYPE_DOUBLE;
            case FLOAT: return TYPE_FLOAT;
            case SHORT: return TYPE_SHORT;
            case STRING: return TYPE_STRING;
            case OBJECT: return TYPE_OBJECT_PACKED;
            default: return TYPE_NULL;
        }
    }

    // Deserialization methods using schema

    @SuppressWarnings("unchecked")
    private T readObject(FastByteReader reader, SerializationSchema schema) throws Exception {
        return (T) readObjectGeneric(reader, schema);
    }

    @SuppressWarnings("unchecked")
    private Object readObjectGeneric(FastByteReader reader, SerializationSchema schema) throws Exception {
        Object instance = schema.clazz.getDeclaredConstructor().newInstance();

        // Read field count
        int fieldCount = readVarint(reader);

        // Read field data - schema knows all types
        for (int i = 0; i < fieldCount; i++) {
            FieldSchema fieldSchema = schema.fields[i];
            byte nullMarker = reader.readByte();

            if (nullMarker == 0) {
                continue; // null value
            }

            Object value = readFieldValue(reader, fieldSchema);
            fieldSchema.field.set(instance, value);
        }

        return instance;
    }

    private Object readFieldValue(FastByteReader reader, FieldSchema fieldSchema) throws Exception {
        switch (fieldSchema.type) {
            case INT: return reader.readInt();
            case LONG: return reader.readLong();
            case BOOLEAN: return reader.readBoolean();
            case DOUBLE: return reader.readDouble();
            case FLOAT: return reader.readFloat();
            case SHORT: return reader.readShort();
            case STRING:
                int strLen = readVarint(reader);
                byte[] strBytes = new byte[strLen];
                reader.readFully(strBytes, 0, strLen);
                return new String(strBytes, StandardCharsets.UTF_8);
            case LIST:
                return readList(reader, fieldSchema);
            case MAP:
                return readMap(reader, fieldSchema);
            case OBJECT:
                int objLen = readVarint(reader);
                byte[] objBytes = new byte[objLen];
                reader.readFully(objBytes, 0, objLen);
                FastByteReader nestedReader = new FastByteReader();
                nestedReader.setData(objBytes);
                return readObjectGeneric(nestedReader, fieldSchema.nestedSchema);
            default:
                return null;
        }
    }

    private List<Object> readList(FastByteReader reader, FieldSchema fieldSchema) throws Exception {
        int size = reader.readInt();
        byte uniformFlag = reader.readByte();
        byte elementType = reader.readByte();

        List<Object> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Object item = readElement(reader, fieldSchema.listElementType, fieldSchema.elementSchema);
            list.add(item);
        }
        return list;
    }

    private Map<Object, Object> readMap(FastByteReader reader, FieldSchema fieldSchema) throws Exception {
        int size = reader.readInt();
        byte uniformFlags = reader.readByte();
        byte keyType = reader.readByte();
        byte valueType = reader.readByte();

        Map<Object, Object> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            Object key = readElement(reader, fieldSchema.mapKeyType, fieldSchema.mapKeySchema);
            Object value = readElement(reader, fieldSchema.mapValueType, fieldSchema.mapValueSchema);
            map.put(key, value);
        }
        return map;
    }

    private Object readElement(FastByteReader reader, FieldType type, SerializationSchema schema) throws Exception {
        switch (type) {
            case INT: return reader.readInt();
            case LONG: return reader.readLong();
            case BOOLEAN: return reader.readBoolean();
            case DOUBLE: return reader.readDouble();
            case FLOAT: return reader.readFloat();
            case SHORT: return reader.readShort();
            case STRING:
                int len = reader.readShort() & 0xFFFF;
                byte[] bytes = new byte[len];
                reader.readFully(bytes, 0, len);
                return new String(bytes, StandardCharsets.UTF_8);
            case OBJECT:
                int objLen = readVarint(reader);
                byte[] objBytes = new byte[objLen];
                reader.readFully(objBytes, 0, objLen);
                FastByteReader nestedReader = new FastByteReader();
                nestedReader.setData(objBytes);
                return readObjectGeneric(nestedReader, schema);
            default:
                return null;
        }
    }

    private int readVarint(FastByteReader reader) {
        byte b = reader.readByte();
        if (b >= 0) {
            return b;
        }
        int result = b & 0x7F;
        b = reader.readByte();
        result |= (b & 0x7F) << 7;
        return result;
    }

    // Schema classes

    private static class SerializationSchema {
        final Class<?> clazz;
        FieldSchema[] fields;

        SerializationSchema(Class<?> clazz, FieldSchema[] fields) {
            this.clazz = clazz;
            this.fields = fields;
        }
    }

    private static class FieldSchema {
        final Field field;
        final FieldType type;
        final FieldType listElementType;
        final FieldType mapKeyType;
        final FieldType mapValueType;
        final SerializationSchema nestedSchema;
        final SerializationSchema elementSchema;
        SerializationSchema mapKeySchema;
        SerializationSchema mapValueSchema;

        FieldSchema(Field field, FieldType type, FieldType listElementType, FieldType mapValueType,
                    SerializationSchema nestedSchema) {
            this.field = field;
            this.type = type;
            this.listElementType = listElementType;
            this.mapKeyType = listElementType; // For maps, reuse this field
            this.mapValueType = mapValueType;
            this.nestedSchema = nestedSchema;
            this.elementSchema = nestedSchema; // For lists, reuse this field
        }

        FieldSchema withMapSchemas(SerializationSchema keySchema, SerializationSchema valueSchema) {
            this.mapKeySchema = keySchema;
            this.mapValueSchema = valueSchema;
            return this;
        }
    }

    private enum FieldType {
        INT, LONG, BOOLEAN, DOUBLE, FLOAT, SHORT, STRING, LIST, MAP, OBJECT
    }
}


















