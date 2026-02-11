package com.kalynx.simplyabinaryserializer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.kalynx.simplyabinaryserializer.TypeMarkers.*;


/**
 * Ultra-fast type-specific serializer with pre-computed schema.
 * Eliminates all runtime type detection by pre-computing the entire object graph structure.
 *
 * @param <T> The type this serializer handles
 */
public class TypedSerializer<T> implements Serializer, Deserializer {

    private final Class<T> targetClass;
    private final SerializationSchema schema;
    private final int estimatedSize;

    private static final ThreadLocal<FastByteWriter> WRITER_POOL =
            ThreadLocal.withInitial(FastByteWriter::new);

    private static final ThreadLocal<FastByteReader> READER_POOL =
            ThreadLocal.withInitial(FastByteReader::new);

    private static final ThreadLocal<ArrayDeque<FastByteWriter>> NESTED_WRITER_POOL =
            ThreadLocal.withInitial(() -> {
                ArrayDeque<FastByteWriter> pool = new ArrayDeque<>(8);
                for (int i = 0; i < 4; i++) {
                    FastByteWriter w = new FastByteWriter();
                    w.reset(512); // Pre-allocate with larger buffer
                    pool.push(w);
                }
                return pool;
            });

    private static final ThreadLocal<ArrayDeque<FastByteReader>> NESTED_READER_POOL =
            ThreadLocal.withInitial(() -> {
                ArrayDeque<FastByteReader> pool = new ArrayDeque<>(8);
                for (int i = 0; i < 4; i++) pool.push(new FastByteReader());
                return pool;
            });

    // ThreadLocal buffer for String encoding to avoid repeated allocations
    private static final ThreadLocal<byte[]> STRING_BUFFER =
            ThreadLocal.withInitial(() -> new byte[256]);

    public TypedSerializer(Class<T> targetClass) {
        this.targetClass = targetClass;
        this.schema = buildSchema(targetClass);
        this.estimatedSize = computeEstimatedSize(schema);
    }

    private int computeEstimatedSize(SerializationSchema schema) {
        int estimate = 10;
        for (int i = 0; i < schema.fields.length; i++) {
            switch (schema.fields[i].type) {
                case INT: case FLOAT: estimate += 6; break;
                case LONG: case DOUBLE: estimate += 10; break;
                case BOOLEAN: case SHORT: estimate += 4; break;
                case STRING: estimate += 32; break;
                case LIST: estimate += 64; break;
                case MAP: estimate += 128; break;
                case OBJECT: estimate += 64; break;
            }
        }
        return estimate;
    }

    @Override
    public <U> byte[] serialize(U obj, Class<U> type) throws Throwable {
        return serialize(targetClass.cast(obj));
    }

    public byte[] serialize(T obj) throws Throwable {
        if (obj == null) return new byte[] { TYPE_NULL };

        FastByteWriter writer = WRITER_POOL.get();
        writer.reset(estimatedSize * 2);
        writer.writeByte(TYPE_OBJECT_PACKED);
        writeObject(writer, obj, schema);
        return writer.toByteArray();
    }

    @Override
    public <U> U deserialize(byte[] data, Class<U> type) throws Throwable {
        return type.cast(deserialize(data));
    }

    public T deserialize(byte[] data) throws Throwable {
        if (data == null || data.length == 0) return null;

        FastByteReader reader = READER_POOL.get();
        reader.setData(data);

        byte typeMarker = reader.readByte();
        if (typeMarker == TYPE_NULL) return null;

        return readObject(reader, schema);
    }

    // ==================== SCHEMA BUILDING ====================

    private SerializationSchema buildSchema(Class<?> clazz) {
        Map<Class<?>, SerializationSchema> cache = new HashMap<>();
        return buildSchemaRecursive(clazz, cache);
    }

    private SerializationSchema buildSchemaRecursive(Class<?> clazz, Map<Class<?>, SerializationSchema> cache) {
        if (cache.containsKey(clazz)) return cache.get(clazz);

        SerializationSchema schema = new SerializationSchema(clazz, new FieldSchema[0]);
        cache.put(clazz, schema);

        Field[] fields = clazz.getDeclaredFields();
        List<FieldSchema> fieldSchemas = new ArrayList<>();

        for (Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                java.lang.reflect.Modifier.isTransient(field.getModifiers())) continue;

            field.setAccessible(true);
            fieldSchemas.add(analyzeField(field, cache));
        }

        schema.fields = fieldSchemas.toArray(new FieldSchema[0]);
        return schema;
    }

    private FieldSchema analyzeField(Field field, Map<Class<?>, SerializationSchema> cache) {
        Class<?> fieldType = field.getType();

        // Create MethodHandle getter and setter for fast access
        MethodHandle getter = null;
        MethodHandle setter = null;
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            getter = lookup.unreflectGetter(field);
            setter = lookup.unreflectSetter(field);
        } catch (IllegalAccessException e) {
            // Fallback handled in writeObject/readObject
        }

        if (fieldType == int.class || fieldType == Integer.class) return new FieldSchema(field, getter, setter, FieldType.INT, null, null, null);
        if (fieldType == long.class || fieldType == Long.class) return new FieldSchema(field, getter, setter, FieldType.LONG, null, null, null);
        if (fieldType == boolean.class || fieldType == Boolean.class) return new FieldSchema(field, getter, setter, FieldType.BOOLEAN, null, null, null);
        if (fieldType == double.class || fieldType == Double.class) return new FieldSchema(field, getter, setter, FieldType.DOUBLE, null, null, null);
        if (fieldType == float.class || fieldType == Float.class) return new FieldSchema(field, getter, setter, FieldType.FLOAT, null, null, null);
        if (fieldType == short.class || fieldType == Short.class) return new FieldSchema(field, getter, setter, FieldType.SHORT, null, null, null);
        if (fieldType == String.class) return new FieldSchema(field, getter, setter, FieldType.STRING, null, null, null);

        if (List.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            Class<?> elementType = extractGenericType(genericType, 0);
            FieldType listElementType = determineFieldType(elementType);
            SerializationSchema elementSchema = listElementType == FieldType.OBJECT ? buildSchemaRecursive(elementType, cache) : null;
            return new FieldSchema(field, getter, setter, FieldType.LIST, listElementType, null, elementSchema);
        }

        if (Map.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            Class<?> keyType = extractGenericType(genericType, 0);
            Class<?> valueType = extractGenericType(genericType, 1);
            FieldType mapKeyType = determineFieldType(keyType);
            FieldType mapValueType = determineFieldType(valueType);
            SerializationSchema keySchema = mapKeyType == FieldType.OBJECT ? buildSchemaRecursive(keyType, cache) : null;
            SerializationSchema valueSchema = mapValueType == FieldType.OBJECT ? buildSchemaRecursive(valueType, cache) : null;
            return new FieldSchema(field, getter, setter, FieldType.MAP, mapKeyType, mapValueType, null).withMapSchemas(keySchema, valueSchema);
        }

        return new FieldSchema(field, getter, setter, FieldType.OBJECT, null, null, buildSchemaRecursive(fieldType, cache));
    }

    private Class<?> extractGenericType(Type genericType, int index) {
        if (genericType instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            if (typeArgs.length > index && typeArgs[index] instanceof Class) {
                return (Class<?>) typeArgs[index];
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

    // ==================== ULTRA-FAST SERIALIZATION ====================

    private void writeObject(FastByteWriter w, Object obj, SerializationSchema schema) throws Throwable {
        final FieldSchema[] fields = schema.fields;
        final int len = fields.length;
        w.writeByte((byte) len);

        for (int i = 0; i < len; i++) {
            FieldSchema f = fields[i];

            // Primitives: No null marker needed (can't be null)
            switch (f.type) {
                case INT -> w.writeInt(f.getter != null ? (int) f.getter.invoke(obj) : f.field.getInt(obj));
                case LONG -> w.writeLong(f.getter != null ? (long) f.getter.invoke(obj) : f.field.getLong(obj));
                case BOOLEAN -> w.writeBoolean(f.getter != null ? (boolean) f.getter.invoke(obj) : f.field.getBoolean(obj));
                case DOUBLE -> w.writeDouble(f.getter != null ? (double) f.getter.invoke(obj) : f.field.getDouble(obj));
                case FLOAT -> w.writeFloat(f.getter != null ? (float) f.getter.invoke(obj) : f.field.getFloat(obj));
                case SHORT -> w.writeShort(f.getter != null ? (short) f.getter.invoke(obj) : f.field.getShort(obj));
                default -> {
                    // Object types - can be null, need marker
                    Object v = f.getter != null ? f.getter.invoke(obj) : f.field.get(obj);
                    if (v == null) { w.writeByte(0); continue; }
                    w.writeByte(1);
                    writeValue(w, v, f);
                }
            }
        }
    }

    private void writeValue(FastByteWriter w, Object v, FieldSchema f) throws Throwable {
        switch (f.type) {
            case INT: w.writeInt((Integer) v); return;
            case LONG: w.writeLong((Long) v); return;
            case BOOLEAN: w.writeBoolean((Boolean) v); return;
            case DOUBLE: w.writeDouble((Double) v); return;
            case FLOAT: w.writeFloat((Float) v); return;
            case SHORT: w.writeShort((Short) v); return;
            case STRING: writeStr(w, (String) v); return;
            case LIST: writeList(w, (List<?>) v, f); return;
            case MAP: writeMap(w, (Map<?, ?>) v, f); return;
            case OBJECT: writeNested(w, v, f.nestedSchema); return;
        }
    }

    private void writeStr(FastByteWriter w, String s) {
        // Fast path for small strings using ThreadLocal buffer
        int maxBytes = s.length() * 3; // UTF-8 worst case
        if (maxBytes <= 255) {
            byte[] buf = STRING_BUFFER.get();
            if (buf.length < maxBytes) {
                buf = new byte[maxBytes];
                STRING_BUFFER.set(buf);
            }

            // Encode directly into buffer
            int len = encodeUTF8(s, buf);
            w.writeByte((byte) len);
            w.writeBytes(buf, len);
        } else {
            // Large strings - use int for length
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            w.writeByte((byte) 255); // Marker for large string
            w.writeInt(b.length);    // Full length as int
            w.writeBytes(b, b.length);
        }
    }

    // Fast UTF-8 encoding for ASCII-heavy strings
    private int encodeUTF8(String s, byte[] buf) {
        int len = s.length();
        int pos = 0;

        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c < 0x80) {
                // ASCII fast path
                buf[pos++] = (byte) c;
            } else if (c < 0x800) {
                buf[pos++] = (byte) (0xC0 | (c >> 6));
                buf[pos++] = (byte) (0x80 | (c & 0x3F));
            } else {
                buf[pos++] = (byte) (0xE0 | (c >> 12));
                buf[pos++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                buf[pos++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        return pos;
    }

    private void writeNested(FastByteWriter w, Object v, SerializationSchema schema) throws Throwable {
        ArrayDeque<FastByteWriter> pool = NESTED_WRITER_POOL.get();
        FastByteWriter nw = pool.poll();
        if (nw == null) nw = new FastByteWriter();

        try {
            nw.reset(256); // Larger initial buffer for nested objects
            writeObject(nw, v, schema);
            int len = nw.getPosition();
            w.writeByte((byte) len);
            w.writeBytes(nw.getBuffer(), len);
        } finally {
            if (pool.size() < 8) pool.push(nw);
        }
    }

    private void writeList(FastByteWriter w, List<?> list, FieldSchema f) throws Throwable {
        int size = list.size();
        w.writeInt(size);
        if (size == 0) return;

        FieldType t = f.listElementType;
        SerializationSchema s = f.elementSchema;

        switch (t) {
            case INT: for (int i = 0; i < size; i++) w.writeInt((Integer) list.get(i)); return;
            case LONG: for (int i = 0; i < size; i++) w.writeLong((Long) list.get(i)); return;
            case STRING: for (int i = 0; i < size; i++) writeStr(w, (String) list.get(i)); return;
            case BOOLEAN: for (int i = 0; i < size; i++) w.writeBoolean((Boolean) list.get(i)); return;
            case OBJECT: for (int i = 0; i < size; i++) writeNested(w, list.get(i), s); return;
            default: for (int i = 0; i < size; i++) writeElem(w, list.get(i), t, s);
        }
    }

    private void writeMap(FastByteWriter w, Map<?, ?> map, FieldSchema f) throws Throwable {
        int size = map.size();
        w.writeInt(size);
        if (size == 0) return;

        FieldType kt = f.mapKeyType, vt = f.mapValueType;
        SerializationSchema ks = f.mapKeySchema, vs = f.mapValueSchema;

        if (kt == FieldType.STRING && vt == FieldType.INT) {
            for (Map.Entry<?, ?> e : map.entrySet()) { writeStr(w, (String) e.getKey()); w.writeInt((Integer) e.getValue()); }
            return;
        }
        if (kt == FieldType.STRING && vt == FieldType.STRING) {
            for (Map.Entry<?, ?> e : map.entrySet()) { writeStr(w, (String) e.getKey()); writeStr(w, (String) e.getValue()); }
            return;
        }
        for (Map.Entry<?, ?> e : map.entrySet()) {
            writeElem(w, e.getKey(), kt, ks);
            writeElem(w, e.getValue(), vt, vs);
        }
    }

    private void writeElem(FastByteWriter w, Object v, FieldType t, SerializationSchema s) throws Throwable {
        switch (t) {
            case INT: w.writeInt((Integer) v); break;
            case LONG: w.writeLong((Long) v); break;
            case BOOLEAN: w.writeBoolean((Boolean) v); break;
            case DOUBLE: w.writeDouble((Double) v); break;
            case FLOAT: w.writeFloat((Float) v); break;
            case SHORT: w.writeShort((Short) v); break;
            case STRING: writeStr(w, (String) v); break;
            case OBJECT: writeNested(w, v, s); break;
        }
    }

    // ==================== ULTRA-FAST DESERIALIZATION ====================

    @SuppressWarnings("unchecked")
    private T readObject(FastByteReader r, SerializationSchema schema) throws Throwable {
        Object instance = schema.constructor.newInstance();  // Use cached constructor
        final FieldSchema[] fields = schema.fields;
        int len = r.readByte() & 0xFF;

        for (int i = 0; i < len; i++) {
            FieldSchema f = fields[i];

            // Primitives: No null marker (can't be null)
            switch (f.type) {
                case INT -> {
                    int val = r.readInt();
                    if (f.setter != null) f.setter.invoke(instance, val);
                    else f.field.setInt(instance, val);
                }
                case LONG -> {
                    long val = r.readLong();
                    if (f.setter != null) f.setter.invoke(instance, val);
                    else f.field.setLong(instance, val);
                }
                case BOOLEAN -> {
                    boolean val = r.readBoolean();
                    if (f.setter != null) f.setter.invoke(instance, val);
                    else f.field.setBoolean(instance, val);
                }
                case DOUBLE -> {
                    double val = r.readDouble();
                    if (f.setter != null) f.setter.invoke(instance, val);
                    else f.field.setDouble(instance, val);
                }
                case FLOAT -> {
                    float val = r.readFloat();
                    if (f.setter != null) f.setter.invoke(instance, val);
                    else f.field.setFloat(instance, val);
                }
                case SHORT -> {
                    short val = r.readShort();
                    if (f.setter != null) f.setter.invoke(instance, val);
                    else f.field.setShort(instance, val);
                }
                default -> {
                    // Object types - have null marker
                    byte marker = r.readByte();
                    if (marker == 0) continue;
                    Object value = readValue(r, f);
                    if (f.setter != null) f.setter.invoke(instance, value);
                    else f.field.set(instance, value);
                }
            }
        }
        return (T) instance;
    }

    private Object readValue(FastByteReader r, FieldSchema f) throws Throwable {
        switch (f.type) {
            case INT: return r.readInt();
            case LONG: return r.readLong();
            case BOOLEAN: return r.readBoolean();
            case DOUBLE: return r.readDouble();
            case FLOAT: return r.readFloat();
            case SHORT: return r.readShort();
            case STRING: return readStr(r);
            case LIST: return readList(r, f);
            case MAP: return readMap(r, f);
            case OBJECT: return readNested(r, f.nestedSchema);
            default: return null;
        }
    }

    private String readStr(FastByteReader r) {
        int len = r.readByte() & 0xFF;

        // Check for large string marker
        if (len == 255) {
            len = r.readInt(); // Read full length as int
        }

        byte[] b = new byte[len];
        r.readFully(b, 0, len);
        return new String(b, StandardCharsets.UTF_8);
    }

    private Object readNested(FastByteReader r, SerializationSchema schema) throws Throwable {
        int len = r.readByte() & 0xFF;
        byte[] b = new byte[len];
        r.readFully(b, 0, len);

        ArrayDeque<FastByteReader> pool = NESTED_READER_POOL.get();
        FastByteReader nr = pool.poll();
        if (nr == null) nr = new FastByteReader();

        try {
            nr.setData(b);
            return readObjectGeneric(nr, schema);
        } finally {
            if (pool.size() < 8) pool.push(nr);
        }
    }

    @SuppressWarnings("unchecked")
    private Object readObjectGeneric(FastByteReader r, SerializationSchema schema) throws Throwable {
        Object instance = schema.constructor.newInstance();  // Use cached constructor
        final FieldSchema[] fields = schema.fields;
        int len = r.readByte() & 0xFF;

        for (int i = 0; i < len; i++) {
            FieldSchema f = fields[i];

            // Primitives: No null marker (can't be null)
            switch (f.type) {
                case INT -> {
                    int val = r.readInt();
                    if (f.setter != null) f.setter.invoke(instance, val);
                    else f.field.setInt(instance, val);
                }
                case LONG -> {
                    long val = r.readLong();
                    if (f.setter != null) f.setter.invoke(instance, val);
                    else f.field.setLong(instance, val);
                }
                case BOOLEAN -> {
                    boolean val = r.readBoolean();
                    if (f.setter != null) f.setter.invoke(instance, val);
                    else f.field.setBoolean(instance, val);
                }
                case DOUBLE -> {
                    double val = r.readDouble();
                    if (f.setter != null) f.setter.invoke(instance, val);
                    else f.field.setDouble(instance, val);
                }
                case FLOAT -> {
                    float val = r.readFloat();
                    if (f.setter != null) f.setter.invoke(instance, val);
                    else f.field.setFloat(instance, val);
                }
                case SHORT -> {
                    short val = r.readShort();
                    if (f.setter != null) f.setter.invoke(instance, val);
                    else f.field.setShort(instance, val);
                }
                default -> {
                    // Object types - have null marker
                    byte marker = r.readByte();
                    if (marker == 0) continue;
                    Object value = readValue(r, f);
                    if (f.setter != null) f.setter.invoke(instance, value);
                    else f.field.set(instance, value);
                }
            }
        }
        return instance;
    }

    private List<Object> readList(FastByteReader r, FieldSchema f) throws Throwable {
        int size = r.readInt();
        List<Object> list = new ArrayList<>(size);
        if (size == 0) return list;

        FieldType t = f.listElementType;
        SerializationSchema s = f.elementSchema;

        switch (t) {
            case INT: for (int i = 0; i < size; i++) list.add(r.readInt()); return list;
            case LONG: for (int i = 0; i < size; i++) list.add(r.readLong()); return list;
            case STRING: for (int i = 0; i < size; i++) list.add(readStr(r)); return list;
            case BOOLEAN: for (int i = 0; i < size; i++) list.add(r.readBoolean()); return list;
            case OBJECT: for (int i = 0; i < size; i++) list.add(readNested(r, s)); return list;
            default: for (int i = 0; i < size; i++) list.add(readElem(r, t, s)); return list;
        }
    }

    private Map<Object, Object> readMap(FastByteReader r, FieldSchema f) throws Throwable {
        int size = r.readInt();
        Map<Object, Object> map = new HashMap<>(size);
        if (size == 0) return map;

        FieldType kt = f.mapKeyType, vt = f.mapValueType;
        SerializationSchema ks = f.mapKeySchema, vs = f.mapValueSchema;

        if (kt == FieldType.STRING && vt == FieldType.INT) {
            for (int i = 0; i < size; i++) map.put(readStr(r), r.readInt());
            return map;
        }
        if (kt == FieldType.STRING && vt == FieldType.STRING) {
            for (int i = 0; i < size; i++) map.put(readStr(r), readStr(r));
            return map;
        }
        for (int i = 0; i < size; i++) map.put(readElem(r, kt, ks), readElem(r, vt, vs));
        return map;
    }

    private Object readElem(FastByteReader r, FieldType t, SerializationSchema s) throws Throwable {
        switch (t) {
            case INT: return r.readInt();
            case LONG: return r.readLong();
            case BOOLEAN: return r.readBoolean();
            case DOUBLE: return r.readDouble();
            case FLOAT: return r.readFloat();
            case SHORT: return r.readShort();
            case STRING: return readStr(r);
            case OBJECT: return readNested(r, s);
            default: return null;
        }
    }

    // ==================== SCHEMA CLASSES ====================

    private static class SerializationSchema {
        final Class<?> clazz;
        final Constructor<?> constructor;  // Cached constructor for fast instantiation
        FieldSchema[] fields;

        SerializationSchema(Class<?> clazz, FieldSchema[] fields) {
            this.clazz = clazz;
            this.fields = fields;
            try {
                this.constructor = clazz.getDeclaredConstructor();
                this.constructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("No default constructor for " + clazz.getName(), e);
            }
        }
    }

    private static class FieldSchema {
        final Field field;
        final MethodHandle getter;  // Fast getter - replaces field.get()
        final MethodHandle setter;  // Fast setter - replaces field.set()
        final FieldType type;
        final FieldType listElementType;
        final FieldType mapKeyType;
        final FieldType mapValueType;
        final SerializationSchema nestedSchema;
        final SerializationSchema elementSchema;
        SerializationSchema mapKeySchema;
        SerializationSchema mapValueSchema;

        FieldSchema(Field field, MethodHandle getter, MethodHandle setter, FieldType type, FieldType listElementType, FieldType mapValueType, SerializationSchema nestedSchema) {
            this.field = field;
            this.getter = getter;
            this.setter = setter;
            this.type = type;
            this.listElementType = listElementType;
            this.mapKeyType = listElementType;
            this.mapValueType = mapValueType;
            this.nestedSchema = nestedSchema;
            this.elementSchema = nestedSchema;
        }

        FieldSchema withMapSchemas(SerializationSchema keySchema, SerializationSchema valueSchema) {
            this.mapKeySchema = keySchema;
            this.mapValueSchema = valueSchema;
            return this;
        }
    }

    private enum FieldType { INT, LONG, BOOLEAN, DOUBLE, FLOAT, SHORT, STRING, LIST, MAP, OBJECT }
}



