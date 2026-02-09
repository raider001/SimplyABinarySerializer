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

    private static final Map<Class<?>, ClassSchema> SCHEMA_CACHE = new java.util.concurrent.ConcurrentHashMap<>();


    @Override
    public <T> byte[] serialize(T obj, Class<T> type) throws Exception {
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

    private void writeMap(DataOutputStream dos, Map<?, ?> map) throws IOException {
        dos.writeInt(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            writeString(dos, entry.getKey().toString());

            if (entry.getValue() != null) {
                writeString(dos, entry.getValue().toString());
            } else {
                writeString(dos, "");
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
                    byte[] mapBytes = serializeMapFast((Map<?, ?>) value);
                    listBytesCache[i] = mapBytes;
                    totalSize += mapBytes.length;
                    break;
                default:
                    byte[] nestedBytes = serialize(value);
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

        // Pre-calculate total size and cache string bytes
        Object[] items = new Object[size];
        byte[] markers = new byte[size];
        byte[][] stringBytes = new byte[size][];
        byte[][] objectBytes = new byte[size][];

        int totalSize = 4; // list size

        for (int i = 0; i < size; i++) {
            Object item = list.get(i);
            items[i] = item;
            byte marker = getTypeMarker(item);
            markers[i] = marker;

            totalSize += 1; // type marker

            switch (marker) {
                case TYPE_NULL:
                    break;
                case TYPE_STRING:
                    byte[] strBytes = ((String) item).getBytes(StandardCharsets.UTF_8);
                    stringBytes[i] = strBytes;
                    totalSize += 2 + strBytes.length;
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
                case TYPE_OBJECT:
                case TYPE_OBJECT_PACKED:
                    // Nested object - serialize recursively
                    byte[] objBytes = serialize(item);
                    objectBytes[i] = objBytes;
                    totalSize += 4 + objBytes.length; // length + data
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type in fast list serialization: " + marker);
            }
        }

        // Write to pre-allocated array
        byte[] result = new byte[totalSize];
        FastByteWriter writer = FAST_WRITER.get();
        writer.setBuffer(result);

        writer.writeInt(size);

        for (int i = 0; i < size; i++) {
            byte marker = markers[i];
            writer.writeByte(marker);

            switch (marker) {
                case TYPE_NULL:
                    break;
                case TYPE_STRING:
                    writer.writeString(stringBytes[i]);
                    break;
                case TYPE_INT:
                    writer.writeInt((Integer) items[i]);
                    break;
                case TYPE_LONG:
                    writer.writeLong((Long) items[i]);
                    break;
                case TYPE_BOOLEAN:
                    writer.writeBoolean((Boolean) items[i]);
                    break;
                case TYPE_DOUBLE:
                    writer.writeDouble((Double) items[i]);
                    break;
                case TYPE_FLOAT:
                    writer.writeFloat((Float) items[i]);
                    break;
                case TYPE_SHORT:
                    writer.writeShort((Short) items[i]);
                    break;
                case TYPE_OBJECT:
                case TYPE_OBJECT_PACKED:
                    byte[] objBytes = objectBytes[i];
                    writer.writeInt(objBytes.length);
                    writer.writeBytes(objBytes, objBytes.length);
                    break;
            }
        }

        return result;
    }

    private byte[] serializeMapFast(Map<?, ?> map) throws Exception {
        int size = map.size();

        // Pre-calculate total size and cache string bytes
        Object[] keys = new Object[size];
        Object[] values = new Object[size];
        byte[] keyMarkers = new byte[size];
        byte[] valueMarkers = new byte[size];
        byte[][] keyStringBytes = new byte[size][];
        byte[][] valueStringBytes = new byte[size][];

        int totalSize = 4; // map size

        int index = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            keys[index] = key;
            values[index] = value;

            byte keyMarker = getTypeMarker(key);
            byte valueMarker = getTypeMarker(value);
            keyMarkers[index] = keyMarker;
            valueMarkers[index] = valueMarker;

            totalSize += 2; // key and value type markers

            // Calculate key size
            switch (keyMarker) {
                case TYPE_NULL:
                    break;
                case TYPE_STRING:
                    byte[] keyStrBytes = ((String) key).getBytes(StandardCharsets.UTF_8);
                    keyStringBytes[index] = keyStrBytes;
                    totalSize += 2 + keyStrBytes.length;
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
                default:
                    throw new IllegalArgumentException("Unsupported key type in map serialization: " + keyMarker);
            }

            // Calculate value size
            switch (valueMarker) {
                case TYPE_NULL:
                    break;
                case TYPE_STRING:
                    byte[] valStrBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                    valueStringBytes[index] = valStrBytes;
                    totalSize += 2 + valStrBytes.length;
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
                case TYPE_LIST:
                case TYPE_LIST_STRING:
                    byte[] listBytes = serialize(value);
                    valueStringBytes[index] = listBytes;
                    totalSize += 4 + listBytes.length;
                    break;
                case TYPE_OBJECT:
                case TYPE_OBJECT_PACKED:
                    // Nested object - serialize recursively
                    byte[] objBytes = serialize(value);
                    valueStringBytes[index] = objBytes;
                    totalSize += 4 + objBytes.length;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported value type in map serialization: " + valueMarker);
            }

            index++;
        }

        // Write to pre-allocated array
        byte[] result = new byte[totalSize];
        FastByteWriter writer = FAST_WRITER.get();
        writer.setBuffer(result);

        writer.writeInt(size);

        for (int i = 0; i < size; i++) {
            // Write key
            byte keyMarker = keyMarkers[i];
            writer.writeByte(keyMarker);

            switch (keyMarker) {
                case TYPE_NULL:
                    break;
                case TYPE_STRING:
                    writer.writeString(keyStringBytes[i]);
                    break;
                case TYPE_INT:
                    writer.writeInt((Integer) keys[i]);
                    break;
                case TYPE_LONG:
                    writer.writeLong((Long) keys[i]);
                    break;
                case TYPE_BOOLEAN:
                    writer.writeBoolean((Boolean) keys[i]);
                    break;
                case TYPE_DOUBLE:
                    writer.writeDouble((Double) keys[i]);
                    break;
            }

            // Write value
            byte valueMarker = valueMarkers[i];
            writer.writeByte(valueMarker);

            switch (valueMarker) {
                case TYPE_NULL:
                    break;
                case TYPE_STRING:
                    writer.writeString(valueStringBytes[i]);
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
                case TYPE_LIST:
                case TYPE_LIST_STRING, TYPE_OBJECT, TYPE_OBJECT_PACKED:
                    byte[] listBytes = valueStringBytes[i];
                    writer.writeInt(listBytes.length);
                    writer.writeBytes(listBytes, listBytes.length);
                    break;
            }
        }

        return result;
    }
}
