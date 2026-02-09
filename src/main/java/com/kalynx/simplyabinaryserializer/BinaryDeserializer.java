package com.kalynx.simplyabinaryserializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.kalynx.simplyabinaryserializer.TypeMarkers.*;

public class BinaryDeserializer implements Deserializer {

    private static final ThreadLocal<FastByteReader> FAST_READER =
            ThreadLocal.withInitial(FastByteReader::new);

    private static final ThreadLocal<byte[]> REUSABLE_STRING_BUFFER =
            ThreadLocal.withInitial(() -> new byte[1024]);

    private static final Map<Class<?>, Field[]> FIELD_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static final Map<Class<?>, java.lang.invoke.MethodHandle> CONSTRUCTOR_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final Map<Class<?>, java.lang.invoke.MethodHandle[]> METHOD_HANDLE_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final int MAX_REUSABLE_FIELDS = 64;
    private static final ThreadLocal<byte[]> REUSABLE_TYPE_NIBBLES =
            ThreadLocal.withInitial(() -> new byte[MAX_REUSABLE_FIELDS]);

    private static final byte[] NIBBLE_TO_TYPE = new byte[16];

    static {
        NIBBLE_TO_TYPE[NIBBLE_NULL] = TYPE_NULL;
        NIBBLE_TO_TYPE[NIBBLE_STRING] = TYPE_STRING;
        NIBBLE_TO_TYPE[NIBBLE_INT] = TYPE_INT;
        NIBBLE_TO_TYPE[NIBBLE_LONG] = TYPE_LONG;
        NIBBLE_TO_TYPE[NIBBLE_BOOLEAN] = TYPE_BOOLEAN;
        NIBBLE_TO_TYPE[NIBBLE_DOUBLE] = TYPE_DOUBLE;
        NIBBLE_TO_TYPE[NIBBLE_FLOAT] = TYPE_FLOAT;
        NIBBLE_TO_TYPE[NIBBLE_SHORT] = TYPE_SHORT;
        NIBBLE_TO_TYPE[NIBBLE_LIST_STRING] = TYPE_LIST_STRING;
        NIBBLE_TO_TYPE[NIBBLE_LIST_GENERIC] = TYPE_LIST;
        NIBBLE_TO_TYPE[NIBBLE_NESTED_OBJECT] = TYPE_OBJECT_PACKED;
        NIBBLE_TO_TYPE[NIBBLE_MAP] = TYPE_MAP;
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) throws Exception {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        byte typeMarker = bytes[0];

        if (typeMarker == TYPE_OBJECT_PACKED) {
            // Always create a new FastByteReader instance to avoid ThreadLocal conflicts during recursion
            FastByteReader reader = new FastByteReader();
            reader.setData(bytes);
            reader.readByte(); // Skip type marker
            try {
                return type.cast(readObjectPacked(reader, type));
            } catch (Throwable t) {
                if (t instanceof Exception) {
                    throw (Exception) t;
                }
                throw new Exception("Deserialization failed", t);
            }
        } else if (typeMarker == TYPE_MAP) {
            // Handle standalone map deserialization
            FastByteReader reader = new FastByteReader();
            reader.setData(bytes);
            reader.readByte(); // Skip type marker
            try {
                @SuppressWarnings("unchecked")
                T result = (T) readMapFast(reader);
                return result;
            } catch (Throwable t) {
                if (t instanceof Exception) {
                    throw (Exception) t;
                }
                throw new Exception("Map deserialization failed", t);
            }
        }

        throw new IllegalArgumentException("Unsupported type marker for deserialization: " + typeMarker);
    }

    private Object readObjectPacked(FastByteReader reader, Class<?> type) throws Throwable {
        java.lang.invoke.MethodHandle constructor = CONSTRUCTOR_CACHE.computeIfAbsent(type, c -> {
            try {
                java.lang.reflect.Constructor<?> ctor = c.getDeclaredConstructor();
                ctor.setAccessible(true);
                return java.lang.invoke.MethodHandles.lookup().unreflectConstructor(ctor);
            } catch (Exception e) {
                return null;
            }
        });

        Object obj;
        if (constructor != null) {
            obj = constructor.invoke();
        } else {
            obj = type.getDeclaredConstructor().newInstance();
        }

        int fieldCount = reader.readByte() & 0xFF;

        Field[] fields = FIELD_CACHE.computeIfAbsent(type, c -> {
            Field[] allFields = c.getDeclaredFields();
            List<Field> serializableFields = new ArrayList<>();
            for (Field field : allFields) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
                    field.setAccessible(true);
                    serializableFields.add(field);
                }
            }
            return serializableFields.toArray(new Field[0]);
        });

        java.lang.invoke.MethodHandle[] setters = METHOD_HANDLE_CACHE.computeIfAbsent(type, c -> {
            java.lang.invoke.MethodHandle[] handles = new java.lang.invoke.MethodHandle[fields.length];
            java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup();
            for (int i = 0; i < fields.length; i++) {
                try {
                    handles[i] = lookup.unreflectSetter(fields[i]);
                } catch (IllegalAccessException e) {
                    handles[i] = null;
                }
            }
            return handles;
        });

        if (fields.length != fieldCount) {
            throw new IllegalStateException("Field count mismatch: expected " + fields.length + ", got " + fieldCount);
        }

        byte[] typeNibbles;
        if (fieldCount <= MAX_REUSABLE_FIELDS) {
            typeNibbles = REUSABLE_TYPE_NIBBLES.get();
        } else {
            typeNibbles = new byte[fieldCount];
        }

        for (int i = 0; i < fieldCount; i += 2) {
            byte packed = reader.readByte();
            typeNibbles[i] = (byte) ((packed >>> 4) & 0x0F);
            if (i + 1 < fieldCount) {
                typeNibbles[i + 1] = (byte) (packed & 0x0F);
            }
        }

        byte[] strBuffer = REUSABLE_STRING_BUFFER.get();
        for (int i = 0; i < fieldCount; i++) {
            byte nibble = typeNibbles[i];
            byte typeMarker = (nibble < NIBBLE_TO_TYPE.length) ? NIBBLE_TO_TYPE[nibble] : TYPE_OBJECT_PACKED;

            Object value;
            switch (typeMarker) {
                case TYPE_NULL:
                    value = null;
                    break;
                case TYPE_STRING:
                    int strLen = readVarint(reader);
                    if (strLen == 0) {
                        value = "";
                    } else if (strLen <= strBuffer.length) {
                        reader.readFully(strBuffer, 0, strLen);
                        value = new String(strBuffer, 0, strLen, StandardCharsets.UTF_8);
                    } else {
                        byte[] strBytes = new byte[strLen];
                        reader.readFully(strBytes, 0, strLen);
                        value = new String(strBytes, 0, strLen, StandardCharsets.UTF_8);
                    }
                    break;
                case TYPE_INT:
                    value = reader.readInt();
                    break;
                case TYPE_LONG:
                    value = reader.readLong();
                    break;
                case TYPE_BOOLEAN:
                    value = reader.readBoolean();
                    break;
                case TYPE_DOUBLE:
                    value = reader.readDouble();
                    break;
                case TYPE_FLOAT:
                    value = reader.readFloat();
                    break;
                case TYPE_SHORT:
                    value = reader.readShort();
                    break;
                case TYPE_LIST_STRING:
                    value = readStringListFast(reader);
                    break;
                case TYPE_LIST:
                    value = readListFast(reader);
                    break;
                case TYPE_MAP:
                    value = readMapFast(reader);
                    break;
                default:
                    int nestedLen = readVarint(reader);
                    byte[] nestedBytes = new byte[nestedLen];
                    reader.readFully(nestedBytes, 0, nestedLen);
                    // The nested bytes include TYPE_OBJECT_PACKED marker, so deserialize them properly
                    value = deserialize(nestedBytes, fields[i].getType());
                    break;
            }

            java.lang.invoke.MethodHandle setter = setters[i];
            // Use Field.set for complex types (List, Map, nested objects) to avoid ClassCastException
            // MethodHandle is stricter about types than Field.set
            if (setter != null && isPrimitiveOrString(value)) {
                setter.invoke(obj, value);
            } else {
                fields[i].set(obj, value);
            }
        }

        return obj;
    }

    private boolean isPrimitiveOrString(Object value) {
        if (value == null) return true;
        return value instanceof String ||
               value instanceof Integer ||
               value instanceof Long ||
               value instanceof Boolean ||
               value instanceof Double ||
               value instanceof Float ||
               value instanceof Short;
    }

    private List<Object> readStringListFast(FastByteReader reader) {
        int size = readVarint(reader);
        List<Object> list = new ArrayList<>(size);
        byte[] buffer = REUSABLE_STRING_BUFFER.get();

        for (int i = 0; i < size; i++) {
            int strLen = readVarint(reader);
            if (strLen == 0) {
                list.add(null);
            } else if (strLen <= buffer.length) {
                reader.readFully(buffer, 0, strLen);
                list.add(new String(buffer, 0, strLen, StandardCharsets.UTF_8));
            } else {
                byte[] strBytes = new byte[strLen];
                reader.readFully(strBytes, 0, strLen);
                list.add(new String(strBytes, 0, strLen, StandardCharsets.UTF_8));
            }
        }

        return list;
    }

    private List<Object> readListFast(FastByteReader reader) throws Exception {
        int listSize = reader.readInt();
        List<Object> list = new ArrayList<>(listSize);

        // Read uniform flag
        byte uniformFlag = reader.readByte();
        boolean uniform = uniformFlag == 1;

        // Read type marker (once if uniform)
        byte itemType = uniform ? reader.readByte() : 0;

        for (int i = 0; i < listSize; i++) {
            byte actualType = uniform ? itemType : reader.readByte();
            Object item;

            switch (actualType) {
                case TYPE_NULL:
                    item = null;
                    break;
                case TYPE_STRING:
                    int strLen = (reader.readByte() & 0xFF) << 8 | (reader.readByte() & 0xFF);
                    byte[] strBytes = new byte[strLen];
                    reader.readFully(strBytes, 0, strLen);
                    item = new String(strBytes, 0, strLen, StandardCharsets.UTF_8);
                    break;
                case TYPE_INT:
                    item = reader.readInt();
                    break;
                case TYPE_LONG:
                    item = reader.readLong();
                    break;
                case TYPE_BOOLEAN:
                    item = reader.readBoolean();
                    break;
                case TYPE_DOUBLE:
                    item = reader.readDouble();
                    break;
                case TYPE_FLOAT:
                    item = reader.readFloat();
                    break;
                default:
                    int nestedLen = reader.readInt();
                    byte[] nestedBytes = new byte[nestedLen];
                    reader.readFully(nestedBytes, 0, nestedLen);
                    item = deserialize(nestedBytes, Object.class);
                    break;
            }

            list.add(item);
        }

        return list;
    }

    private Map<Object, Object> readMapFast(FastByteReader reader) throws Exception {
        int mapSize = reader.readInt();
        Map<Object, Object> map = new HashMap<>(mapSize);

        // Read uniform flags
        byte uniformFlags = reader.readByte();
        boolean uniformKeys = (uniformFlags & 1) != 0;
        boolean uniformValues = (uniformFlags & 2) != 0;

        // Read type markers (once if uniform)
        byte keyType = uniformKeys ? reader.readByte() : 0;
        byte valueType = uniformValues ? reader.readByte() : 0;

        for (int i = 0; i < mapSize; i++) {
            // Read key
            byte actualKeyType = uniformKeys ? keyType : reader.readByte();
            Object key = readValue(reader, actualKeyType);

            // Read value
            byte actualValueType = uniformValues ? valueType : reader.readByte();
            Object value = readValue(reader, actualValueType);

            map.put(key, value);
        }

        return map;
    }

    private Object readValue(FastByteReader reader, byte typeMarker) throws Exception {
        switch (typeMarker) {
            case TYPE_NULL:
                return null;
            case TYPE_STRING:
                int strLen = (reader.readByte() & 0xFF) << 8 | (reader.readByte() & 0xFF);
                byte[] strBytes = new byte[strLen];
                reader.readFully(strBytes, 0, strLen);
                return new String(strBytes, 0, strLen, StandardCharsets.UTF_8);
            case TYPE_INT:
                return reader.readInt();
            case TYPE_LONG:
                return reader.readLong();
            case TYPE_BOOLEAN:
                return reader.readBoolean();
            case TYPE_DOUBLE:
                return reader.readDouble();
            case TYPE_FLOAT:
                return reader.readFloat();
            case TYPE_SHORT:
                return reader.readShort();
            case TYPE_LIST:
            case TYPE_LIST_STRING:
                int listLen = reader.readInt();
                byte[] listBytes = new byte[listLen];
                reader.readFully(listBytes, 0, listLen);
                return deserialize(listBytes, List.class);
            default:
                int objLen = reader.readInt();
                byte[] objBytes = new byte[objLen];
                reader.readFully(objBytes, 0, objLen);
                return deserialize(objBytes, Object.class);
        }
    }

    private int readVarint(FastByteReader reader) {
        byte b = reader.readByte();
        if (b >= 0) {
            return b;
        }
        int result = b & 0x7F;
        int shift = 7;
        do {
            b = reader.readByte();
            result |= (b & 0x7F) << shift;
            shift += 7;
        } while (b < 0);
        return result;
    }
}








