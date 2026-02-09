package com.kalynx.simplyabinaryserializer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kalynx.simplyabinaryserializer.TypeMarkers.*;

public class BinarySerializer implements Serializer {

    private static final ThreadLocal<ByteArrayOutputStream> REUSABLE_BAOS =
            ThreadLocal.withInitial(() -> new ByteArrayOutputStream(512));

    private static final byte[] TYPE_TO_NIBBLE = new byte[20];

    static {
        TYPE_TO_NIBBLE[TYPE_NULL] = NIBBLE_NULL;
        TYPE_TO_NIBBLE[TYPE_STRING] = NIBBLE_STRING;
        TYPE_TO_NIBBLE[TYPE_INT] = NIBBLE_INT;
        TYPE_TO_NIBBLE[TYPE_LONG] = NIBBLE_LONG;
        TYPE_TO_NIBBLE[TYPE_BOOLEAN] = NIBBLE_BOOLEAN;
        TYPE_TO_NIBBLE[TYPE_DOUBLE] = NIBBLE_DOUBLE;
        TYPE_TO_NIBBLE[TYPE_FLOAT] = NIBBLE_FLOAT;
        TYPE_TO_NIBBLE[TYPE_SHORT] = NIBBLE_SHORT;
        TYPE_TO_NIBBLE[TYPE_OBJECT] = NIBBLE_NESTED_OBJECT;
        TYPE_TO_NIBBLE[TYPE_LIST] = NIBBLE_LIST_GENERIC;
        TYPE_TO_NIBBLE[TYPE_OBJECT_PACKED] = NIBBLE_NESTED_OBJECT;
        TYPE_TO_NIBBLE[TYPE_LIST_STRING] = NIBBLE_LIST_STRING;
        TYPE_TO_NIBBLE[TYPE_MAP] = NIBBLE_MAP;
    }


    private static final ThreadLocal<FastByteWriter> FAST_WRITER =
            ThreadLocal.withInitial(FastByteWriter::new);

    // Pool of FastByteWriter instances for nested serialization to avoid ThreadLocal conflicts
    private static final ThreadLocal<java.util.ArrayDeque<FastByteWriter>> WRITER_POOL =
            ThreadLocal.withInitial(() -> {
                java.util.ArrayDeque<FastByteWriter> pool = new java.util.ArrayDeque<>(4);
                for (int i = 0; i < 4; i++) {
                    pool.push(new FastByteWriter());
                }
                return pool;
            });

    private static final Map<Class<?>, ClassSchema> SCHEMA_CACHE = new java.util.concurrent.ConcurrentHashMap<>();


    @Override
    public <T> byte[] serialize(T obj, Class<T> type) throws Exception {
        // For simple types - use direct approach
        if (obj instanceof String || obj instanceof Integer || obj instanceof Long ||
                obj instanceof Boolean || obj instanceof Double || obj instanceof Float || obj instanceof Short) {
            return serializeDirect(obj);
        }

        // For complex objects, use fast two-pass serialization
        byte typeMarker = getTypeMarker(obj);

        if (typeMarker == TYPE_OBJECT_PACKED) {
            return serializeObjectFast(obj);
        }

        // Optimize Map serialization - use FastByteWriter instead of DataOutputStream
        if (obj instanceof Map) {
            return serializeMapFast((Map<?, ?>) obj);
        }

        // For other complex types (List, Set, Array), use streaming approach
        ByteArrayOutputStream baos = REUSABLE_BAOS.get();
        baos.reset();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeByte(typeMarker);

        if (obj instanceof List) {
            writeList(dos, (List<?>) obj);
        } else if (obj instanceof Set) {
            writeSet(dos, (Set<?>) obj);
        } else if (obj.getClass().isArray()) {
            writeArray(dos, obj);
        }

        dos.flush();
        return baos.toByteArray();
    }

    /**
     * Optimized map serialization using FastByteWriter for better performance.
     * This version includes TYPE_MAP marker for standalone maps.
     */
    private byte[] serializeMapFast(Map<?, ?> map) throws Exception {
        // Pre-calculate size for single allocation
        int estimatedSize = 1 + 4 + (map.size() * 20); // type + size + estimated per-entry
        FastByteWriter writer = FAST_WRITER.get();
        writer.reset(estimatedSize);

        writer.writeByte(TYPE_MAP);
        writer.writeInt(map.size());

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            writeValueFast(writer, entry.getKey());
            writeValueFast(writer, entry.getValue());
        }

        return writer.toByteArray();
    }

    /**
     * Serialize map content without TYPE_MAP marker for use in nested contexts.
     * Optimized: detects uniform types in single pass without caching overhead.
     */
    private byte[] serializeMapContentFast(Map<?, ?> map) throws Exception {
        int size = map.size();

        // Fast path for empty maps
        if (size == 0) {
            return new byte[] {0, 0, 0, 0, 0}; // size + uniform flags
        }

        // Get writer from pool
        java.util.ArrayDeque<FastByteWriter> pool = WRITER_POOL.get();
        FastByteWriter writer = pool.poll();
        if (writer == null) {
            writer = new FastByteWriter();
        }

        try {
            // Single-pass: detect uniformity while collecting data
            Object[] keys = new Object[size];
            Object[] values = new Object[size];
            byte[][] keyStringBytes = new byte[size][];
            byte[][] valueStringBytes = new byte[size][];

            byte firstKeyType = -1;
            byte firstValueType = -1;
            boolean uniformKeys = true;
            boolean uniformValues = true;

            int totalSize = 4 + 1; // size + uniform flags
            int idx = 0;

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                keys[idx] = entry.getKey();
                values[idx] = entry.getValue();

                // Check key type
                byte keyType = getTypeMarkerFast(keys[idx]);
                if (idx == 0) {
                    firstKeyType = keyType;
                    totalSize += 1;
                } else if (uniformKeys && keyType != firstKeyType) {
                    uniformKeys = false;
                    totalSize += idx; // Add markers we skipped
                }
                if (!uniformKeys) totalSize += 1;
                totalSize += getSizeForType(keys[idx], keyType, keyStringBytes, idx);

                // Check value type
                byte valueType = getTypeMarkerFast(values[idx]);
                if (idx == 0) {
                    firstValueType = valueType;
                    totalSize += 1;
                } else if (uniformValues && valueType != firstValueType) {
                    uniformValues = false;
                    totalSize += idx;
                }
                if (!uniformValues) totalSize += 1;
                totalSize += getSizeForType(values[idx], valueType, valueStringBytes, idx);

                idx++;
            }

            // Write optimized format
            writer.reset(totalSize);
            writer.writeInt(size);

            byte uniformFlags = (byte)((uniformKeys ? 1 : 0) | (uniformValues ? 2 : 0));
            writer.writeByte(uniformFlags);

            if (uniformKeys) writer.writeByte(firstKeyType);
            if (uniformValues) writer.writeByte(firstValueType);

            for (int i = 0; i < size; i++) {
                if (!uniformKeys) writer.writeByte(getTypeMarkerFast(keys[i]));
                writeValueData(writer, keys[i], uniformKeys ? firstKeyType : getTypeMarkerFast(keys[i]), keyStringBytes[i]);

                if (!uniformValues) writer.writeByte(getTypeMarkerFast(values[i]));
                writeValueData(writer, values[i], uniformValues ? firstValueType : getTypeMarkerFast(values[i]), valueStringBytes[i]);
            }

            return writer.toByteArray();
        } finally {
            if (pool.size() < 4) {
                pool.push(writer);
            }
        }
    }

    /**
     * Write just the data without type marker (marker already written or assumed from shape).
     */
    private void writeValueData(FastByteWriter writer, Object value, byte marker, byte[] cachedBytes) throws Exception {
        switch (marker) {
            case TYPE_NULL: break;
            case TYPE_STRING:
                writer.writeShort((short) cachedBytes.length);
                writer.writeBytes(cachedBytes);
                break;
            case TYPE_INT: writer.writeInt((Integer) value); break;
            case TYPE_LONG: writer.writeLong((Long) value); break;
            case TYPE_BOOLEAN: writer.writeBoolean((Boolean) value); break;
            case TYPE_DOUBLE: writer.writeDouble((Double) value); break;
            case TYPE_FLOAT: writer.writeFloat((Float) value); break;
            case TYPE_SHORT: writer.writeShort((Short) value); break;
            case TYPE_OBJECT:
            case TYPE_OBJECT_PACKED:
                if (cachedBytes != null) {
                    writer.writeInt(cachedBytes.length);
                    writer.writeBytes(cachedBytes);
                } else {
                    @SuppressWarnings("unchecked")
                    byte[] nested = serialize(value, (Class<Object>) value.getClass());
                    writer.writeInt(nested.length);
                    writer.writeBytes(nested);
                }
                break;
        }
    }

    private byte getTypeMarkerFast(Object obj) {
        if (obj == null) return TYPE_NULL;
        if (obj instanceof String) return TYPE_STRING;
        if (obj instanceof Integer) return TYPE_INT;
        if (obj instanceof Long) return TYPE_LONG;
        if (obj instanceof Boolean) return TYPE_BOOLEAN;
        if (obj instanceof Double) return TYPE_DOUBLE;
        if (obj instanceof Float) return TYPE_FLOAT;
        if (obj instanceof Short) return TYPE_SHORT;
        return TYPE_OBJECT_PACKED; // Use packed format for complex objects
    }

    private int getSizeForType(Object value, byte marker, byte[][] cache, int idx) throws Exception {
        switch (marker) {
            case TYPE_NULL: return 0;
            case TYPE_STRING:
                byte[] strBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                cache[idx] = strBytes;
                return 2 + strBytes.length;
            case TYPE_INT: return 4;
            case TYPE_LONG: return 8;
            case TYPE_BOOLEAN: return 1;
            case TYPE_DOUBLE: return 8;
            case TYPE_FLOAT: return 4;
            case TYPE_SHORT: return 2;
            case TYPE_OBJECT:
            case TYPE_OBJECT_PACKED:
                // Complex object - need to serialize to get size
                @SuppressWarnings("unchecked")
                byte[] objBytes = serialize(value, (Class<Object>) value.getClass());
                cache[idx] = objBytes; // Cache the serialized bytes
                return 4 + objBytes.length; // length prefix + data
            default: return 0;
        }
    }

    private void writeValueToWriter(FastByteWriter writer, Object value, byte marker, byte[] cachedBytes) throws Exception {
        writer.writeByte(marker);
        switch (marker) {
            case TYPE_NULL: break;
            case TYPE_STRING:
                writer.writeShort((short) cachedBytes.length);
                writer.writeBytes(cachedBytes);
                break;
            case TYPE_INT: writer.writeInt((Integer) value); break;
            case TYPE_LONG: writer.writeLong((Long) value); break;
            case TYPE_BOOLEAN: writer.writeBoolean((Boolean) value); break;
            case TYPE_DOUBLE: writer.writeDouble((Double) value); break;
            case TYPE_FLOAT: writer.writeFloat((Float) value); break;
            case TYPE_SHORT: writer.writeShort((Short) value); break;
            case TYPE_OBJECT:
            case TYPE_OBJECT_PACKED:
                // Use cached serialized bytes from pass 1
                if (cachedBytes != null) {
                    writer.writeInt(cachedBytes.length);
                    writer.writeBytes(cachedBytes);
                } else {
                    // Fallback if not cached
                    @SuppressWarnings("unchecked")
                    byte[] nested = serialize(value, (Class<Object>) value.getClass());
                    writer.writeInt(nested.length);
                    writer.writeBytes(nested);
                }
                break;
        }
    }

    /**
     * Fast value writing with type markers - used for maps and lists.
     */
    private void writeValueFast(FastByteWriter writer, Object value) throws Exception {
        if (value == null) {
            writer.writeByte(TYPE_NULL);
        } else if (value instanceof String str) {
            writer.writeByte(TYPE_STRING);
            byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
            writer.writeShort((short) strBytes.length);
            writer.writeBytes(strBytes);
        } else if (value instanceof Integer i) {
            writer.writeByte(TYPE_INT);
            writer.writeInt(i);
        } else if (value instanceof Long l) {
            writer.writeByte(TYPE_LONG);
            writer.writeLong(l);
        } else if (value instanceof Boolean b) {
            writer.writeByte(TYPE_BOOLEAN);
            writer.writeBoolean(b);
        } else if (value instanceof Double d) {
            writer.writeByte(TYPE_DOUBLE);
            writer.writeDouble(d);
        } else if (value instanceof Float f) {
            writer.writeByte(TYPE_FLOAT);
            writer.writeFloat(f);
        } else if (value instanceof Short s) {
            writer.writeByte(TYPE_SHORT);
            writer.writeShort(s);
        } else {
            // Nested object - serialize recursively
            writer.writeByte(TYPE_OBJECT_PACKED);
            @SuppressWarnings("unchecked")
            byte[] nested = serialize(value, (Class<Object>) value.getClass());
            writer.writeInt(nested.length);
            writer.writeBytes(nested);
        }
    }

    private void writeMap(DataOutputStream dos, Map<?, ?> map) throws IOException {
        dos.writeInt(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            // Write key with type marker
            writeValueWithType(dos, key);

            // Write value with type marker
            writeValueWithType(dos, value);
        }
    }

    private void writeValueWithType(DataOutputStream dos, Object value) throws IOException {
        if (value == null) {
            dos.writeByte(TYPE_NULL);
        } else if (value instanceof String) {
            dos.writeByte(TYPE_STRING);
            writeString(dos, (String) value);
        } else if (value instanceof Integer) {
            dos.writeByte(TYPE_INT);
            dos.writeInt((Integer) value);
        } else if (value instanceof Long) {
            dos.writeByte(TYPE_LONG);
            dos.writeLong((Long) value);
        } else if (value instanceof Boolean) {
            dos.writeByte(TYPE_BOOLEAN);
            dos.writeBoolean((Boolean) value);
        } else if (value instanceof Double) {
            dos.writeByte(TYPE_DOUBLE);
            dos.writeDouble((Double) value);
        } else if (value instanceof Float) {
            dos.writeByte(TYPE_FLOAT);
            dos.writeFloat((Float) value);
        } else if (value instanceof Short) {
            dos.writeByte(TYPE_SHORT);
            dos.writeShort((Short) value);
        } else {
            // For complex types, serialize recursively
            dos.writeByte(TYPE_OBJECT_PACKED);
            try {
                @SuppressWarnings("unchecked")
                byte[] nested = serialize(value, (Class<Object>) value.getClass());
                dos.writeInt(nested.length);
                dos.write(nested);
            } catch (Exception e) {
                throw new IOException("Failed to serialize map value", e);
            }
        }
    }

    private void writeArray(DataOutputStream dos, Object array) throws Exception {
        int length = Array.getLength(array);
        dos.writeInt(length);

        for (int i = 0; i < length; i++) {
            Object item = Array.get(array, i);
            byte typeMarker = getTypeMarker(item);
            dos.writeByte(typeMarker);

            switch (typeMarker) {
                case TYPE_NULL:
                    break;
                case TYPE_STRING:
                    writeString(dos, (String) item);
                    break;
                case TYPE_INT:
                    dos.writeInt((Integer) item);
                    break;
                case TYPE_LONG:
                    dos.writeLong((Long) item);
                    break;
                case TYPE_BOOLEAN:
                    dos.writeBoolean((Boolean) item);
                    break;
                case TYPE_DOUBLE:
                    dos.writeDouble((Double) item);
                    break;
                case TYPE_FLOAT:
                    dos.writeFloat((Float) item);
                    break;
                case TYPE_SHORT:
                    dos.writeShort((Short) item);
                    break;
                default:
                    byte[] itemBytes = serialize(item);
                    dos.writeInt(itemBytes.length);
                    dos.write(itemBytes);
                    break;
            }
        }
    }

    private byte[] serialize(Object obj) throws Exception {
        if (obj == null) {
            return new byte[0];
        }

        // For simple types and objects, use direct array writing with precomputed size
        // This eliminates the toByteArray() copy overhead
        return serializeOptimized(obj);
    }

    private byte[] serializeDirect(Object obj) throws Exception {
        if (obj instanceof String str) {
            byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
            byte[] result = new byte[1 + 2 + strBytes.length]; // type + length + data
            result[0] = TYPE_STRING;
            result[1] = (byte) ((strBytes.length >> 8) & 0xFF);
            result[2] = (byte) (strBytes.length & 0xFF);
            System.arraycopy(strBytes, 0, result, 3, strBytes.length);
            return result;
        } else if (obj instanceof Integer) {
            byte[] result = new byte[5]; // type + int
            result[0] = TYPE_INT;
            int val = (Integer) obj;
            result[1] = (byte) (val >>> 24);
            result[2] = (byte) (val >>> 16);
            result[3] = (byte) (val >>> 8);
            result[4] = (byte) val;
            return result;
        } else if (obj instanceof Long) {
            byte[] result = new byte[9]; // type + long
            result[0] = TYPE_LONG;
            long val = (Long) obj;
            result[1] = (byte) (val >>> 56);
            result[2] = (byte) (val >>> 48);
            result[3] = (byte) (val >>> 40);
            result[4] = (byte) (val >>> 32);
            result[5] = (byte) (val >>> 24);
            result[6] = (byte) (val >>> 16);
            result[7] = (byte) (val >>> 8);
            result[8] = (byte) val;
            return result;
        } else if (obj instanceof Boolean) {
            return new byte[] { TYPE_BOOLEAN, (byte) ((Boolean) obj ? 1 : 0) };
        } else if (obj instanceof Double) {
            byte[] result = new byte[9]; // type + double
            result[0] = TYPE_DOUBLE;
            long val = Double.doubleToLongBits((Double) obj);
            result[1] = (byte) (val >>> 56);
            result[2] = (byte) (val >>> 48);
            result[3] = (byte) (val >>> 40);
            result[4] = (byte) (val >>> 32);
            result[5] = (byte) (val >>> 24);
            result[6] = (byte) (val >>> 16);
            result[7] = (byte) (val >>> 8);
            result[8] = (byte) val;
            return result;
        } else if (obj instanceof Float) {
            byte[] result = new byte[5]; // type + float
            result[0] = TYPE_FLOAT;
            int val = Float.floatToIntBits((Float) obj);
            result[1] = (byte) (val >>> 24);
            result[2] = (byte) (val >>> 16);
            result[3] = (byte) (val >>> 8);
            result[4] = (byte) val;
            return result;
        } else if (obj instanceof Short) {
            byte[] result = new byte[3]; // type + short
            result[0] = TYPE_SHORT;
            short val = (Short) obj;
            result[1] = (byte) (val >>> 8);
            result[2] = (byte) val;
            return result;
        }

        throw new IllegalStateException("Unsupported type: " + obj.getClass());
    }

    private byte getTypeMarker(Object obj) {
        if (obj == null) return TYPE_NULL;
        if (obj instanceof String) return TYPE_STRING;
        if (obj instanceof Integer) return TYPE_INT;
        if (obj instanceof Long) return TYPE_LONG;
        if (obj instanceof Boolean) return TYPE_BOOLEAN;
        if (obj instanceof Double) return TYPE_DOUBLE;
        if (obj instanceof Float) return TYPE_FLOAT;
        if (obj instanceof Short) return TYPE_SHORT;
        if (obj instanceof List<?> list) {
            // Check if it's a homogeneous list of strings
            if (!list.isEmpty() && isHomogeneousStringList(list)) {
                return TYPE_LIST_STRING;
            }
            return TYPE_LIST;
        }
        if (obj instanceof Set) return TYPE_SET;
        if (obj.getClass().isArray()) return TYPE_ARRAY;
        if (obj instanceof Map) return TYPE_MAP;
        return TYPE_OBJECT_PACKED;  // Always use packed format for custom objects
    }

    private boolean isHomogeneousStringList(List<?> list) {
        int size = list.size();
        // Only check first item for speed - if first is String, assume all are
        // This is a performance tradeoff: faster check but may misidentify
        if (size > 0) {
            Object first = list.getFirst();
            return first == null || first instanceof String;
        }
        return true;
    }

    /**
     * Fast object serialization using ClassSchema for pre-computed field types.
     * Uses TYPE_OBJECT_PACKED format with nibble-encoded field types and varint sizes.
     */
    private byte[] serializeObjectFast(Object obj) throws Exception {
        Class<?> clazz = obj.getClass();

        // Get or create schema (caches fields and expected types)
        ClassSchema schema = SCHEMA_CACHE.computeIfAbsent(clazz, ClassSchema::new);
        Field[] fields = schema.fields;
        int fieldCount = schema.fieldCount;
        byte[] expectedTypes = schema.expectedTypes;
        java.lang.invoke.MethodHandle[] getters = schema.getters;

        // Always allocate new arrays to avoid corruption from recursive serialization calls
        // (when serializing nested objects, the ThreadLocal arrays would get overwritten)
        Object[] values = new Object[fieldCount];
        byte[] typeMarkers = new byte[fieldCount];
        byte[][] stringBytesCache = new byte[fieldCount][];
        byte[][] listBytesCache = new byte[fieldCount][];

        // Pass 1: Read field values and calculate total size
        // Use pre-computed header size
        int totalSize = schema.fixedHeaderSize;

        for (int i = 0; i < fieldCount; i++) {
            // Use MethodHandle getter (faster than Field.get)
            Object value;
            try {
                java.lang.invoke.MethodHandle getter = getters[i];
                value = (getter != null) ? getter.invoke(obj) : fields[i].get(obj);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to get field value", t);
            }
            values[i] = value;

            // Use pre-computed type when possible
            byte expectedType = expectedTypes[i];
            byte marker;

            if (value == null) {
                marker = TYPE_NULL;
            } else if (expectedType > 0) {
                // Known type from schema - skip getTypeMarker()
                marker = expectedType;
            } else if (expectedType == -2) {
                // List - check at runtime for string list
                List<?> list = (List<?>) value;
                marker = (!list.isEmpty() && isHomogeneousStringList(list)) ? TYPE_LIST_STRING : TYPE_LIST;
            } else {
                // Unknown type - full detection
                marker = getTypeMarker(value);
            }
            typeMarkers[i] = marker;

            switch (marker) {
                case TYPE_NULL:
                    break;
                case TYPE_STRING:
                    byte[] strBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                    stringBytesCache[i] = strBytes;
                    int strLen = strBytes.length;
                    // Inline varintSize: most strings < 128 bytes
                    totalSize += (strLen < 128 ? 1 : strLen < 16384 ? 2 : strLen < 2097152 ? 3 : 4) + strLen;
                    break;
                case TYPE_INT:
                    totalSize += 4;
                    break;
                case TYPE_LONG:
                    totalSize += 8;
                    break;
                case TYPE_BOOLEAN:
                    totalSize += 1;
                    break;
                case TYPE_DOUBLE:
                    totalSize += 8;
                    break;
                case TYPE_FLOAT:
                    totalSize += 4;
                    break;
                case TYPE_SHORT:
                    totalSize += 2;
                    break;
                case TYPE_LIST_STRING:
                    List<?> strList = (List<?>) value;
                    int listSize = strList.size();
                    // Inline varintSize for list size
                    totalSize += (listSize < 128 ? 1 : listSize < 16384 ? 2 : 3);
                    byte[][] listStrBytes = new byte[listSize][];
                    for (int j = 0; j < listSize; j++) {
                        Object item = strList.get(j);
                        if (item == null) {
                            listStrBytes[j] = null;
                            totalSize += 1;
                        } else {
                            byte[] itemBytes = ((String) item).getBytes(StandardCharsets.UTF_8);
                            listStrBytes[j] = itemBytes;
                            int itemLen = itemBytes.length;
                            // Inline varintSize
                            totalSize += (itemLen < 128 ? 1 : itemLen < 16384 ? 2 : itemLen < 2097152 ? 3 : 4) + itemLen;
                        }
                    }
                    values[i] = listStrBytes;
                    break;
                case TYPE_LIST:
                    byte[] genericListBytes = serializeListFast((List<?>) value);
                    listBytesCache[i] = genericListBytes;
                    totalSize += genericListBytes.length;
                    break;
                case TYPE_MAP:
                    byte[] mapBytes = serializeMapContentFast((Map<?, ?>) value);
                    listBytesCache[i] = mapBytes;
                    totalSize += mapBytes.length;
                    break;
                default:
                    // Optimize: directly serialize as object if it's a custom type
                    byte nestedMarker = getTypeMarker(value);
                    byte[] nestedBytes;
                    if (nestedMarker == TYPE_OBJECT_PACKED) {
                        // Fast path: directly call serializeObjectFast
                        nestedBytes = serializeObjectFast(value);
                    } else {
                        nestedBytes = serialize(value);
                    }
                    listBytesCache[i] = nestedBytes;
                    int nestedLen = nestedBytes.length;
                    totalSize += (nestedLen < 128 ? 1 : nestedLen < 16384 ? 2 : nestedLen < 2097152 ? 3 : 4) + nestedLen;
                    break;
            }
        }

        // Pass 2: Write directly to pre-allocated array
        byte[] result = new byte[totalSize];
        FastByteWriter writer = FAST_WRITER.get();
        writer.setBuffer(result);

        writer.writeByte(TYPE_OBJECT_PACKED);
        writer.writeByte(fieldCount);

        // Write packed type descriptors (4 bits per field) - inline lookup for speed
        for (int i = 0; i < fieldCount; i += 2) {
            byte m1 = typeMarkers[i];
            byte m2 = (i + 1 < fieldCount) ? typeMarkers[i + 1] : 0;
            // Inline lookup: TYPE_TO_NIBBLE array access is faster than method call
            byte nibble1 = (m1 < TYPE_TO_NIBBLE.length) ? TYPE_TO_NIBBLE[m1] : NIBBLE_NESTED_OBJECT;
            byte nibble2 = (m2 < TYPE_TO_NIBBLE.length) ? TYPE_TO_NIBBLE[m2] : NIBBLE_NESTED_OBJECT;
            writer.writeByte((nibble1 << 4) | nibble2);
        }

        // Write field data
        for (int i = 0; i < fieldCount; i++) {
            byte marker = typeMarkers[i];

            switch (marker) {
                case TYPE_NULL:
                    break;
                case TYPE_STRING:
                    byte[] strBytes = stringBytesCache[i];
                    writer.writeVarint(strBytes.length);
                    writer.writeBytes(strBytes, strBytes.length);
                    break;
                case TYPE_INT:
                    writer.writeInt((Integer) values[i]);
                    break;
                case TYPE_LONG:
                    writer.writeLong((Long) values[i]);
                    break;
                case TYPE_BOOLEAN:
                    writer.writeBoolean((Boolean) values[i]);
                    break;
                case TYPE_DOUBLE:
                    writer.writeDouble((Double) values[i]);
                    break;
                case TYPE_FLOAT:
                    writer.writeFloat((Float) values[i]);
                    break;
                case TYPE_SHORT:
                    writer.writeShort((Short) values[i]);
                    break;
                case TYPE_LIST_STRING:
                    // Write inline - values[i] contains the pre-encoded byte arrays (exact size)
                    byte[][] listStrBytesWrite = (byte[][]) values[i];
                    writer.writeVarint(listStrBytesWrite.length);
                    for (byte[] itemBytes : listStrBytesWrite) {
                        if (itemBytes == null) {
                            writer.writeVarint(0);
                        } else {
                            writer.writeVarint(itemBytes.length);
                            writer.writeBytes(itemBytes, itemBytes.length);
                        }
                    }
                    break;
                case TYPE_LIST:
                    writer.writeBytes(listBytesCache[i], listBytesCache[i].length);
                    break;
                case TYPE_MAP:
                    writer.writeBytes(listBytesCache[i], listBytesCache[i].length);
                    break;
                default:
                    byte[] nestedBytes = listBytesCache[i];
                    writer.writeVarint(nestedBytes.length);
                    writer.writeBytes(nestedBytes, nestedBytes.length);
                    break;
            }
        }

        return result;
    }

    private void writeSet(DataOutputStream dos, Set<?> set) throws Exception {
        dos.writeInt(set.size());
        for (Object item : set) {
            byte typeMarker = getTypeMarker(item);
            dos.writeByte(typeMarker);

            switch (typeMarker) {
                case TYPE_NULL:
                    break;
                case TYPE_STRING:
                    writeString(dos, (String) item);
                    break;
                case TYPE_INT:
                    dos.writeInt((Integer) item);
                    break;
                case TYPE_LONG:
                    dos.writeLong((Long) item);
                    break;
                case TYPE_BOOLEAN:
                    dos.writeBoolean((Boolean) item);
                    break;
                case TYPE_DOUBLE:
                    dos.writeDouble((Double) item);
                    break;
                case TYPE_FLOAT:
                    dos.writeFloat((Float) item);
                    break;
                case TYPE_SHORT:
                    dos.writeShort((Short) item);
                    break;
                default:
                    byte[] itemBytes = serialize(item);
                    dos.writeInt(itemBytes.length);
                    dos.write(itemBytes);
                    break;
            }
        }
    }

    private void writeString(DataOutputStream dos, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        dos.writeShort(bytes.length);
        dos.write(bytes);
    }

    private void writeList(DataOutputStream dos, List<?> list) throws Exception {
        dos.writeInt(list.size());
        for (Object item : list) {
            byte typeMarker = getTypeMarker(item);
            dos.writeByte(typeMarker);

            // Use switch on type marker for better JIT optimization
            switch (typeMarker) {
                case TYPE_NULL:
                    // Null already handled by type marker
                    break;
                case TYPE_STRING:
                    writeString(dos, (String) item);
                    break;
                case TYPE_INT:
                    dos.writeInt((Integer) item);
                    break;
                case TYPE_LONG:
                    dos.writeLong((Long) item);
                    break;
                case TYPE_BOOLEAN:
                    dos.writeBoolean((Boolean) item);
                    break;
                case TYPE_DOUBLE:
                    dos.writeDouble((Double) item);
                    break;
                case TYPE_FLOAT:
                    dos.writeFloat((Float) item);
                    break;
                case TYPE_SHORT:
                    dos.writeShort((Short) item);
                    break;
                default:
                    // Complex items (TYPE_OBJECT, TYPE_LIST, TYPE_SET, etc.) - recursively serialize
                    byte[] itemBytes = serialize(item);
                    dos.writeInt(itemBytes.length);
                    dos.write(itemBytes);
                    break;
            }
        }
    }

    private byte[] serializeOptimized(Object obj) throws Exception {
        // For simple types - use direct approach
        if (obj instanceof String || obj instanceof Integer || obj instanceof Long ||
                obj instanceof Boolean || obj instanceof Double || obj instanceof Float || obj instanceof Short) {
            return serializeDirect(obj);
        }

        // For complex objects, use fast two-pass serialization
        // This completely bypasses DataOutputStream for the hot path
        byte typeMarker = getTypeMarker(obj);

        if (typeMarker == TYPE_OBJECT_PACKED) {
            return serializeObjectFast(obj);
        }

        // For other complex types (List, Set, Map, Array), use streaming approach
        ByteArrayOutputStream baos = REUSABLE_BAOS.get();
        baos.reset();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeByte(typeMarker);

        if (obj instanceof List) {
            writeList(dos, (List<?>) obj);
        } else if (obj instanceof Set) {
            writeSet(dos, (Set<?>) obj);
        } else if (obj.getClass().isArray()) {
            writeArray(dos, obj);
        } else if (obj instanceof Map) {
            writeMap(dos, (Map<?, ?>) obj);
        }

        dos.flush();
        return baos.toByteArray();
    }

    private byte[] serializeListFast(List<?> list) throws Exception {
        int size = list.size();

        // Fast path for empty lists
        if (size == 0) {
            return new byte[] {0, 0, 0, 0, 0}; // size + uniform flag
        }

        // Single-pass: detect uniformity while collecting data
        Object[] items = new Object[size];
        byte[][] stringBytes = new byte[size][];
        byte[][] objectBytes = new byte[size][];

        byte firstType = -1;
        boolean uniform = true;
        int totalSize = 4 + 1; // size + uniform flag

        for (int i = 0; i < size; i++) {
            items[i] = list.get(i);
            byte itemType = getTypeMarker(items[i]);

            if (i == 0) {
                firstType = itemType;
                totalSize += 1;
            } else if (uniform && itemType != firstType) {
                uniform = false;
                totalSize += i; // Add markers we skipped
            }

            if (!uniform) totalSize += 1;

            switch (itemType) {
                case TYPE_NULL: break;
                case TYPE_STRING:
                    byte[] strBytes = ((String) items[i]).getBytes(StandardCharsets.UTF_8);
                    stringBytes[i] = strBytes;
                    totalSize += 2 + strBytes.length;
                    break;
                case TYPE_INT: totalSize += 4; break;
                case TYPE_LONG: totalSize += 8; break;
                case TYPE_BOOLEAN: totalSize += 1; break;
                case TYPE_DOUBLE: totalSize += 8; break;
                case TYPE_FLOAT: totalSize += 4; break;
                case TYPE_SHORT: totalSize += 2; break;
                case TYPE_OBJECT:
                case TYPE_OBJECT_PACKED:
                    byte[] objBytes = serialize(items[i]);
                    objectBytes[i] = objBytes;
                    totalSize += 4 + objBytes.length;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type in fast list serialization: " + itemType);
            }
        }

        // Write optimized format
        byte[] result = new byte[totalSize];
        FastByteWriter writer = FAST_WRITER.get();
        writer.setBuffer(result);

        writer.writeInt(size);
        writer.writeByte(uniform ? (byte)1 : (byte)0);
        if (uniform) writer.writeByte(firstType);

        for (int i = 0; i < size; i++) {
            byte itemType = uniform ? firstType : getTypeMarker(items[i]);

            if (!uniform) writer.writeByte(itemType);

            switch (itemType) {
                case TYPE_NULL: break;
                case TYPE_STRING: writer.writeString(stringBytes[i]); break;
                case TYPE_INT: writer.writeInt((Integer) items[i]); break;
                case TYPE_LONG: writer.writeLong((Long) items[i]); break;
                case TYPE_BOOLEAN: writer.writeBoolean((Boolean) items[i]); break;
                case TYPE_DOUBLE: writer.writeDouble((Double) items[i]); break;
                case TYPE_FLOAT: writer.writeFloat((Float) items[i]); break;
                case TYPE_SHORT: writer.writeShort((Short) items[i]); break;
                case TYPE_OBJECT:
                case TYPE_OBJECT_PACKED:
                    writer.writeInt(objectBytes[i].length);
                    writer.writeBytes(objectBytes[i], objectBytes[i].length);
                    break;
            }
        }

        return result;
    }
}
