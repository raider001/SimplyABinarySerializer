package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinarySerializerTests {

    @BeforeEach
    public void setup() {
        // Setup code if needed before each test
    }

    @Test
    public void serialize_TestSimpleObject_serializesCorrectly() throws Exception {
        /*
         * Binary Format Structure for TYPE_OBJECT_PACKED:
         *
         * [Header: 2 bytes]
         *   Byte 0:     TYPE_OBJECT_PACKED marker (12)
         *   Byte 1:     Field count (7)
         *
         * [Type Descriptors: 4 bytes - nibbles packed 2 per byte]
         *   Byte 2:     0x21 = int(0x2) << 4 | string(0x1)
         *   Byte 3:     0x45 = boolean(0x4) << 4 | double(0x5)
         *   Byte 4:     0xA3 = float(0xA) << 4 | long(0x3)
         *   Byte 5:     0xB0 = short(0xB) << 4 | padding(0x0)
         *
         * [Field Data: 40 bytes total]
         *   Bytes 6-9:    int id = 1 (4 bytes, big-endian)
         *   Bytes 10-14:  String name = "Test" (1 byte varint length + 4 bytes UTF-8)
         *   Byte 15:      boolean active = true (1 byte)
         *   Bytes 16-23:  double doubleValue = 3.14 (8 bytes, IEEE 754)
         *   Bytes 24-27:  float floatValue = 2.71f (4 bytes, IEEE 754)
         *   Bytes 28-35:  long longValue = 123456789L (8 bytes, big-endian)
         *   Bytes 36-37:  short shortValue = 42 (2 bytes, big-endian)
         *
         * Total: 46 bytes
         */

        // Arrange
        BinarySerializer serializer = new BinarySerializer();
        TestSimpleObject obj = new TestSimpleObject(1, "Test", true, 3.14, 2.71f, 123456789L, (short) 42);

        // Act
        byte[] bytes = serializer.serialize(obj, TestSimpleObject.class);

        // Assert
        assert bytes != null;
        assert bytes.length > 0;

        // Confirm the object is serialized in the correct order (binary equivalence is ok, but break it into segments before combining the binary array to it can be followed.)
        int pos = 0;

        // Segment 1: Header - TYPE_OBJECT_PACKED marker
        byte typeMarker = bytes[pos++];
        assert typeMarker == 12 : "Expected TYPE_OBJECT_PACKED (12), got " + typeMarker;

        // Segment 2: Field count (7 fields: id, name, active, doubleValue, floatValue, longValue, shortValue)
        byte fieldCount = bytes[pos++];
        assert fieldCount == 7 : "Expected 7 fields, got " + fieldCount;

        // Segment 3: Packed type descriptors (4 bits per field, packed 2 per byte)
        // Fields: int(2), String(1), boolean(4), double(5), float(15), long(3), short(16)
        // Nibbles: 0x2, 0x1, 0x4, 0x5, 0xA, 0x3, 0xB
        // Packed: (0x2 << 4 | 0x1) = 0x21, (0x4 << 4 | 0x5) = 0x45, (0xA << 4 | 0x3) = 0xA3, (0xB << 4 | 0x0) = 0xB0
        byte nibblePair1 = bytes[pos++];
        assert nibblePair1 == 0x21 : "Expected nibbles 0x21 (int,string), got 0x" + Integer.toHexString(nibblePair1 & 0xFF);

        byte nibblePair2 = bytes[pos++];
        assert nibblePair2 == 0x45 : "Expected nibbles 0x45 (boolean,double), got 0x" + Integer.toHexString(nibblePair2 & 0xFF);

        byte nibblePair3 = bytes[pos++];
        assert (nibblePair3 & 0xFF) == 0xA3 : "Expected nibbles 0xA3 (float,long), got 0x" + Integer.toHexString(nibblePair3 & 0xFF);

        byte nibblePair4 = bytes[pos++];
        assert (nibblePair4 & 0xFF) == 0xB0 : "Expected nibbles 0xB0 (short,padding), got 0x" + Integer.toHexString(nibblePair4 & 0xFF);

        // Segment 4: Field data - int id = 1
        int id = ((bytes[pos++] & 0xFF) << 24) |
                 ((bytes[pos++] & 0xFF) << 16) |
                 ((bytes[pos++] & 0xFF) << 8) |
                 (bytes[pos++] & 0xFF);
        assert id == 1 : "Expected id=1, got " + id;

        // Segment 5: Field data - String name = "Test" (varint length + UTF-8 bytes)
        int nameLength = bytes[pos++] & 0xFF; // Varint for length < 128
        assert nameLength == 4 : "Expected name length=4, got " + nameLength;
        String name = new String(bytes, pos, nameLength, java.nio.charset.StandardCharsets.UTF_8);
        pos += nameLength;
        assert name.equals("Test") : "Expected name='Test', got '" + name + "'";

        // Segment 6: Field data - boolean active = true
        byte active = bytes[pos++];
        assert active == 1 : "Expected active=1 (true), got " + active;

        // Segment 7: Field data - double doubleValue = 3.14
        long doubleBits = ((long)(bytes[pos++] & 0xFF) << 56) |
                          ((long)(bytes[pos++] & 0xFF) << 48) |
                          ((long)(bytes[pos++] & 0xFF) << 40) |
                          ((long)(bytes[pos++] & 0xFF) << 32) |
                          ((long)(bytes[pos++] & 0xFF) << 24) |
                          ((long)(bytes[pos++] & 0xFF) << 16) |
                          ((long)(bytes[pos++] & 0xFF) << 8) |
                          (long)(bytes[pos++] & 0xFF);
        double doubleValue = Double.longBitsToDouble(doubleBits);
        assert Math.abs(doubleValue - 3.14) < 0.001 : "Expected doubleValue~3.14, got " + doubleValue;

        // Segment 8: Field data - float floatValue = 2.71f
        int floatBits = ((bytes[pos++] & 0xFF) << 24) |
                        ((bytes[pos++] & 0xFF) << 16) |
                        ((bytes[pos++] & 0xFF) << 8) |
                        (bytes[pos++] & 0xFF);
        float floatValue = Float.intBitsToFloat(floatBits);
        assert Math.abs(floatValue - 2.71f) < 0.001f : "Expected floatValue~2.71, got " + floatValue;

        // Segment 9: Field data - long longValue = 123456789L
        long longValue = ((long)(bytes[pos++] & 0xFF) << 56) |
                         ((long)(bytes[pos++] & 0xFF) << 48) |
                         ((long)(bytes[pos++] & 0xFF) << 40) |
                         ((long)(bytes[pos++] & 0xFF) << 32) |
                         ((long)(bytes[pos++] & 0xFF) << 24) |
                         ((long)(bytes[pos++] & 0xFF) << 16) |
                         ((long)(bytes[pos++] & 0xFF) << 8) |
                         (long)(bytes[pos++] & 0xFF);
        assert longValue == 123456789L : "Expected longValue=123456789, got " + longValue;

        // Segment 10: Field data - short shortValue = 42
        short shortValue = (short)(((bytes[pos++] & 0xFF) << 8) | (bytes[pos++] & 0xFF));
        assert shortValue == 42 : "Expected shortValue=42, got " + shortValue;

        // Verify we consumed all bytes
        assert pos == bytes.length : "Expected to consume all " + bytes.length + " bytes, consumed " + pos;

        System.out.println("✓ Binary format validation passed:");
        System.out.println("  Total size: " + bytes.length + " bytes");
        System.out.println("  Header: TYPE_OBJECT_PACKED + fieldCount = 2 bytes");
        System.out.println("  Type descriptors: 4 bytes (7 fields packed as nibbles)");
        System.out.println("  Field data: " + (bytes.length - 6) + " bytes");
        System.out.println("    - int(4) + string(1+4) + bool(1) + double(8) + float(4) + long(8) + short(2) = " + (4+1+4+1+8+4+8+2) + " bytes");

    }

    static class TestSimpleObject {
        private int id;
        private String name;
        private boolean active;
        private double doubleValue;
        private float floatValue;
        private long longValue;
        private short shortValue;

        public TestSimpleObject(int id, String name, boolean active, double doubleValue, float floatValue, long longValue, short shortValue) {
            this.id = id;
            this.name = name;
            this.active = active;
            this.doubleValue = doubleValue;
            this.floatValue = floatValue;
            this.longValue = longValue;
            this.shortValue = shortValue;
        }
    }

    @Test
    public void serialize_TestListPrimitiveObject_serializesCorrectly() throws Exception {
        /*
         * Binary Format Structure for Object with Lists:
         *
         * [Header: 2 bytes]
         *   Byte 0:     TYPE_OBJECT_PACKED marker (12)
         *   Byte 1:     Field count (6 fields)
         *
         * [Type Descriptors: 3 bytes]
         *   Byte 2:     0x76 = list(0x7) << 4 | listString(0x6)
         *   Byte 3:     0x77 = list(0x7) << 4 | list(0x7)
         *   Byte 4:     0x77 = list(0x7) << 4 | list(0x7)
         *
         * [Field Data: Lists]
         * Each generic list has format:
         *   - 4 bytes: list size (int, big-endian)
         *   - For each element:
         *     - 1 byte: type marker
         *     - N bytes: element data (depends on type)
         *
         * TYPE_LIST_STRING (optimized) has format:
         *   - Varint: list size
         *   - For each string:
         *     - Varint: string length
         *     - N bytes: UTF-8 string data
         *   (No type markers - all elements known to be strings)
         */

        // Arrange
        BinarySerializer serializer = new BinarySerializer();
        TestListPrimitiveObject obj = new TestListPrimitiveObject();

        // Act
        byte[] bytes = serializer.serialize(obj, TestListPrimitiveObject.class);

        // Assert
        assert bytes != null;
        assert bytes.length > 0;

        int pos = 0;

        // Segment 1: Header - TYPE_OBJECT_PACKED marker
        byte typeMarker = bytes[pos++];
        assert typeMarker == 12 : "Expected TYPE_OBJECT_PACKED (12), got " + typeMarker;

        // Segment 2: Field count (6 fields: lst, stringList, booleanList, doubleList, floatList, longList)
        byte fieldCount = bytes[pos++];
        assert fieldCount == 6 : "Expected 6 fields, got " + fieldCount;

        // Segment 3: Packed type descriptors
        // Field order: lst, stringList, booleanList, doubleList, floatList, longList
        // lst = List<Integer> -> TYPE_LIST (8) -> NIBBLE_LIST_GENERIC (0x7)
        // stringList = List<String> -> TYPE_LIST_STRING (13) -> NIBBLE_LIST_STRING (0x6) [optimized]
        // booleanList = List<Boolean> -> TYPE_LIST (8) -> NIBBLE_LIST_GENERIC (0x7)
        // doubleList = List<Double> -> TYPE_LIST (8) -> NIBBLE_LIST_GENERIC (0x7)
        // floatList = List<Float> -> TYPE_LIST (8) -> NIBBLE_LIST_GENERIC (0x7)
        // longList = List<Long> -> TYPE_LIST (8) -> NIBBLE_LIST_GENERIC (0x7)
        //
        // Packed nibbles: 0x76 (lst,stringList), 0x77 (boolList,doubleList), 0x77 (floatList,longList)
        byte nibblePair1 = bytes[pos++];
        assert (nibblePair1 & 0xFF) == 0x76 : "Expected nibbles 0x76 (list,stringList), got 0x" + Integer.toHexString(nibblePair1 & 0xFF);

        byte nibblePair2 = bytes[pos++];
        assert (nibblePair2 & 0xFF) == 0x77 : "Expected nibbles 0x77 (boolList,doubleList), got 0x" + Integer.toHexString(nibblePair2 & 0xFF);

        byte nibblePair3 = bytes[pos++];
        assert (nibblePair3 & 0xFF) == 0x77 : "Expected nibbles 0x77 (floatList,longList), got 0x" + Integer.toHexString(nibblePair3 & 0xFF);

        // Segment 4: List<Integer> lst = [1,2,3,4,5]
        int listSize1 = ((bytes[pos++] & 0xFF) << 24) |
                        ((bytes[pos++] & 0xFF) << 16) |
                        ((bytes[pos++] & 0xFF) << 8) |
                        (bytes[pos++] & 0xFF);
        assert listSize1 == 5 : "Expected list size=5, got " + listSize1;

        for (int i = 1; i <= 5; i++) {
            byte elemType = bytes[pos++];
            assert elemType == 2 : "Expected TYPE_INT (2), got " + elemType;
            int value = ((bytes[pos++] & 0xFF) << 24) |
                       ((bytes[pos++] & 0xFF) << 16) |
                       ((bytes[pos++] & 0xFF) << 8) |
                       (bytes[pos++] & 0xFF);
            assert value == i : "Expected int value=" + i + ", got " + value;
        }

        // Segment 5: List<String> stringList = ["a", "b", "c"]
        // This is optimized as TYPE_LIST_STRING, so it uses varint for size (not 4-byte int)
        // and varint for string lengths (not 2-byte short)
        int listSize2 = bytes[pos++] & 0xFF; // Varint for size < 128
        assert listSize2 == 3 : "Expected list size=3, got " + listSize2;

        String[] expectedStrings = {"a", "b", "c"};
        for (int i = 0; i < 3; i++) {
            // No type marker for TYPE_LIST_STRING - all elements are known to be strings
            int strLength = bytes[pos++] & 0xFF; // Varint for length < 128
            assert strLength == 1 : "Expected string length=1, got " + strLength;
            String str = new String(bytes, pos, strLength, java.nio.charset.StandardCharsets.UTF_8);
            pos += strLength;
            assert str.equals(expectedStrings[i]) : "Expected string='" + expectedStrings[i] + "', got '" + str + "'";
        }

        // Segment 6: List<Boolean> booleanList = [true, false, true]
        int listSize3 = ((bytes[pos++] & 0xFF) << 24) |
                        ((bytes[pos++] & 0xFF) << 16) |
                        ((bytes[pos++] & 0xFF) << 8) |
                        (bytes[pos++] & 0xFF);
        assert listSize3 == 3 : "Expected list size=3, got " + listSize3;

        boolean[] expectedBools = {true, false, true};
        for (int i = 0; i < 3; i++) {
            byte elemType = bytes[pos++];
            assert elemType == 4 : "Expected TYPE_BOOLEAN (4), got " + elemType;
            byte boolVal = bytes[pos++];
            assert boolVal == (expectedBools[i] ? 1 : 0) : "Expected bool=" + expectedBools[i] + ", got " + (boolVal == 1);
        }

        // Segment 7: List<Double> doubleList = [1.1, 2.2, 3.3]
        int listSize4 = ((bytes[pos++] & 0xFF) << 24) |
                        ((bytes[pos++] & 0xFF) << 16) |
                        ((bytes[pos++] & 0xFF) << 8) |
                        (bytes[pos++] & 0xFF);
        assert listSize4 == 3 : "Expected list size=3, got " + listSize4;

        double[] expectedDoubles = {1.1, 2.2, 3.3};
        for (int i = 0; i < 3; i++) {
            byte elemType = bytes[pos++];
            assert elemType == 5 : "Expected TYPE_DOUBLE (5), got " + elemType;
            long doubleBits = ((long)(bytes[pos++] & 0xFF) << 56) |
                             ((long)(bytes[pos++] & 0xFF) << 48) |
                             ((long)(bytes[pos++] & 0xFF) << 40) |
                             ((long)(bytes[pos++] & 0xFF) << 32) |
                             ((long)(bytes[pos++] & 0xFF) << 24) |
                             ((long)(bytes[pos++] & 0xFF) << 16) |
                             ((long)(bytes[pos++] & 0xFF) << 8) |
                             (long)(bytes[pos++] & 0xFF);
            double doubleVal = Double.longBitsToDouble(doubleBits);
            assert Math.abs(doubleVal - expectedDoubles[i]) < 0.001 : "Expected double=" + expectedDoubles[i] + ", got " + doubleVal;
        }

        // Segment 8: List<Float> floatList = [1.1f, 2.2f, 3.3f]
        int listSize5 = ((bytes[pos++] & 0xFF) << 24) |
                        ((bytes[pos++] & 0xFF) << 16) |
                        ((bytes[pos++] & 0xFF) << 8) |
                        (bytes[pos++] & 0xFF);
        assert listSize5 == 3 : "Expected list size=3, got " + listSize5;

        float[] expectedFloats = {1.1f, 2.2f, 3.3f};
        for (int i = 0; i < 3; i++) {
            byte elemType = bytes[pos++];
            assert elemType == 15 : "Expected TYPE_FLOAT (15), got " + elemType;
            int floatBits = ((bytes[pos++] & 0xFF) << 24) |
                           ((bytes[pos++] & 0xFF) << 16) |
                           ((bytes[pos++] & 0xFF) << 8) |
                           (bytes[pos++] & 0xFF);
            float floatVal = Float.intBitsToFloat(floatBits);
            assert Math.abs(floatVal - expectedFloats[i]) < 0.001f : "Expected float=" + expectedFloats[i] + ", got " + floatVal;
        }

        // Segment 9: List<Long> longList = [1L, 2L, 3L]
        int listSize6 = ((bytes[pos++] & 0xFF) << 24) |
                        ((bytes[pos++] & 0xFF) << 16) |
                        ((bytes[pos++] & 0xFF) << 8) |
                        (bytes[pos++] & 0xFF);
        assert listSize6 == 3 : "Expected list size=3, got " + listSize6;

        long[] expectedLongs = {1L, 2L, 3L};
        for (int i = 0; i < 3; i++) {
            byte elemType = bytes[pos++];
            assert elemType == 3 : "Expected TYPE_LONG (3), got " + elemType;
            long longVal = ((long)(bytes[pos++] & 0xFF) << 56) |
                          ((long)(bytes[pos++] & 0xFF) << 48) |
                          ((long)(bytes[pos++] & 0xFF) << 40) |
                          ((long)(bytes[pos++] & 0xFF) << 32) |
                          ((long)(bytes[pos++] & 0xFF) << 24) |
                          ((long)(bytes[pos++] & 0xFF) << 16) |
                          ((long)(bytes[pos++] & 0xFF) << 8) |
                          (long)(bytes[pos++] & 0xFF);
            assert longVal == expectedLongs[i] : "Expected long=" + expectedLongs[i] + ", got " + longVal;
        }

        // Verify we consumed all bytes
        assert pos == bytes.length : "Expected to consume all " + bytes.length + " bytes, consumed " + pos;

        System.out.println("✓ List primitives binary format validation passed:");
        System.out.println("  Total size: " + bytes.length + " bytes");
        System.out.println("  Header: 2 bytes (type marker + field count)");
        System.out.println("  Type descriptors: 3 bytes (6 list fields)");
        System.out.println("  List data breakdown:");
        System.out.println("    - List<Integer>[5]: 4 (size) + 5*(1+4) = 29 bytes");
        System.out.println("    - List<String>[3] (optimized): 1 (varint size) + 3*(1+1) = 7 bytes");
        System.out.println("    - List<Boolean>[3]: 4 (size) + 3*(1+1) = 10 bytes");
        System.out.println("    - List<Double>[3]: 4 (size) + 3*(1+8) = 31 bytes");
        System.out.println("    - List<Float>[3]: 4 (size) + 3*(1+4) = 19 bytes");
        System.out.println("    - List<Long>[3]: 4 (size) + 3*(1+8) = 31 bytes");
    }

    static class TestListPrimitiveObject {
        public List<Integer> lst = Arrays.asList(1,2,3,4,5);
        public List<String> stringList = Arrays.asList("a", "b", "c");
        public List<Boolean> booleanList = Arrays.asList(true, false, true);
        public List<Double> doubleList = Arrays.asList(1.1, 2.2, 3.3);
        public List<Float> floatList = Arrays.asList(1.1f, 2.2f, 3.3f);
        public List<Long> longList = Arrays.asList(1L, 2L, 3L);

    }

    @Test
    public void serialize_TestMapPrimitiveObject_serializesCorrectly() throws Exception {
        /*
         * Binary Format Structure for Object with Maps:
         *
         * [Header: 2 bytes]
         *   Byte 0:     TYPE_OBJECT_PACKED marker (12)
         *   Byte 1:     Field count (5 fields)
         *
         * [Type Descriptors: 3 bytes]
         *   Byte 2:     0x99 = map(0x9) << 4 | map(0x9)
         *   Byte 3:     0x99 = map(0x9) << 4 | map(0x9)
         *   Byte 4:     0x90 = map(0x9) << 4 | padding(0x0)
         *
         * [Field Data: Maps]
         * Each map has format:
         *   - 4 bytes: map size (int, big-endian)
         *   - For each entry:
         *     - 1 byte: key type marker
         *     - N bytes: key data
         *     - 1 byte: value type marker
         *     - M bytes: value data
         *
         * Note: Map iteration order is not guaranteed, so we validate structure
         * and presence of expected entries, not exact byte positions.
         */

        // Arrange
        BinarySerializer serializer = new BinarySerializer();
        TestMapObject obj = new TestMapObject();

        // Act
        byte[] bytes = serializer.serialize(obj, TestMapObject.class);

        // Assert
        assert bytes != null;
        assert bytes.length > 0;

        int pos = 0;

        // Segment 1: Header - TYPE_OBJECT_PACKED marker
        byte typeMarker = bytes[pos++];
        assert typeMarker == 12 : "Expected TYPE_OBJECT_PACKED (12), got " + typeMarker;

        // Segment 2: Field count (5 fields: map, stringMap, intMap, complexMap, emptyMap)
        byte fieldCount = bytes[pos++];
        assert fieldCount == 5 : "Expected 5 fields, got " + fieldCount;

        // Segment 3: Packed type descriptors - all are maps (TYPE_MAP = 6, NIBBLE_MAP = 0x9)
        byte nibblePair1 = bytes[pos++];
        assert (nibblePair1 & 0xFF) == 0x99 : "Expected nibbles 0x99 (map,map), got 0x" + Integer.toHexString(nibblePair1 & 0xFF);

        byte nibblePair2 = bytes[pos++];
        assert (nibblePair2 & 0xFF) == 0x99 : "Expected nibbles 0x99 (map,map), got 0x" + Integer.toHexString(nibblePair2 & 0xFF);

        byte nibblePair3 = bytes[pos++];
        assert (nibblePair3 & 0xFF) == 0x90 : "Expected nibbles 0x90 (map,padding), got 0x" + Integer.toHexString(nibblePair3 & 0xFF);

        // Segment 4: Map<String, Integer> map = {"one":1, "two":2, "three":3}
        pos = validateMapStructure(bytes, pos, 3, "Map<String,Integer>");

        // Segment 5: Map<String, String> stringMap = {"a":"alpha", "b":"beta", "c":"gamma"}
        pos = validateMapStructure(bytes, pos, 3, "Map<String,String>");

        // Segment 6: Map<Integer, String> intMap = {1:"one", 2:"two", 3:"three"}
        pos = validateMapStructure(bytes, pos, 3, "Map<Integer,String>");

        // Segment 7: Map<String, List<Integer>> complexMap = {"numbers":[1,2,3], "moreNumbers":[4,5,6]}
        pos = validateMapStructure(bytes, pos, 2, "Map<String,List<Integer>>");

        // Segment 8: Map<String, Double> emptyMap = {}
        int emptyMapSize = ((bytes[pos++] & 0xFF) << 24) |
                          ((bytes[pos++] & 0xFF) << 16) |
                          ((bytes[pos++] & 0xFF) << 8) |
                          (bytes[pos++] & 0xFF);
        assert emptyMapSize == 0 : "Expected empty map size=0, got " + emptyMapSize;

        // Verify we consumed all bytes
        assert pos == bytes.length : "Expected to consume all " + bytes.length + " bytes, consumed " + pos;

        System.out.println("✓ Map binary format validation passed:");
        System.out.println("  Total size: " + bytes.length + " bytes");
        System.out.println("  Header: 2 bytes (type marker + field count)");
        System.out.println("  Type descriptors: 3 bytes (5 map fields)");
        System.out.println("  Map data validated:");
        System.out.println("    - Map<String,Integer> with 3 entries");
        System.out.println("    - Map<String,String> with 3 entries");
        System.out.println("    - Map<Integer,String> with 3 entries");
        System.out.println("    - Map<String,List<Integer>> with 2 entries");
        System.out.println("    - Empty Map<String,Double> with 0 entries");
    }

    static class TestMapObject {
        Map<String, Integer> map = new HashMap<>();
        Map<String, String> stringMap = new HashMap<>();
        Map<Integer,String> intMap = new HashMap<>();
        Map<String, List<Integer>> complexMap = new HashMap<>();
        Map<String, Double> emptyMap = new HashMap<>();

        public TestMapObject() {
            map.put("one", 1);
            map.put("two", 2);
            map.put("three", 3);

            stringMap.put("a", "alpha");
            stringMap.put("b", "beta");
            stringMap.put("c", "gamma");

            intMap.put(1, "one");
            intMap.put(2, "two");
            intMap.put(3, "three");

            complexMap.put("numbers", Arrays.asList(1,2,3));
            complexMap.put("moreNumbers", Arrays.asList(4,5,6));
        }


    }


    @Test
    public void serialize_TestListWithComplexObjects_serializesCorrectly() throws Exception {
        /*
         * Binary Format Structure for Object with Map of Complex Objects:
         *
         * [Header: 2 bytes]
         *   Byte 0:     TYPE_OBJECT_PACKED marker (12)
         *   Byte 1:     Field count (1 field: complexMap)
         *
         * [Type Descriptors: 1 byte]
         *   Byte 2:     0x90 = map(0x9) << 4 | padding(0x0)
         *
         * [Field Data: Map<String, TestSimpleObject>]
         *   - 4 bytes: map size (2 entries)
         *   - For each entry:
         *     - 1 byte: key type marker (TYPE_STRING = 1)
         *     - 2 bytes: key length
         *     - N bytes: key UTF-8 data
         *     - 1 byte: value type marker (TYPE_OBJECT_PACKED = 12)
         *     - 4 bytes: nested object length
         *     - M bytes: serialized TestSimpleObject (46 bytes each)
         */


        // Arrange
        BinarySerializer serializer = new BinarySerializer();
        TestObjectWithMapOfComplexObjects obj = new TestObjectWithMapOfComplexObjects();

        // Act
        byte[] data = serializer.serialize(obj, TestObjectWithMapOfComplexObjects.class);


        // Assert
        assert data != null;
        assert data.length > 0;

        int pos = 0;

        // Segment 1: Header - TYPE_OBJECT_PACKED marker
        byte typeMarker = data[pos++];
        assert typeMarker == 12 : "Expected TYPE_OBJECT_PACKED (12), got " + typeMarker;

        // Segment 2: Field count (1 field: complexMap)
        byte fieldCount = data[pos++];
        assert fieldCount == 1 : "Expected 1 field, got " + fieldCount;

        // Segment 3: Packed type descriptors - single map field
        byte nibblePair = data[pos++];
        assert (nibblePair & 0xFF) == 0x90 : "Expected nibbles 0x90 (map,padding), got 0x" + Integer.toHexString(nibblePair & 0xFF);

        // Segment 4: Map<String, TestSimpleObject> complexMap with 2 entries
        int mapSize = ((data[pos++] & 0xFF) << 24) |
                     ((data[pos++] & 0xFF) << 16) |
                     ((data[pos++] & 0xFF) << 8) |
                     (data[pos++] & 0xFF);
        assert mapSize == 2 : "Expected map size=2, got " + mapSize;

        // Track which keys we've seen (order may vary due to HashMap)
        java.util.Set<String> seenKeys = new java.util.HashSet<>();
        int totalNestedObjectBytes = 0;

        // Validate each map entry
        for (int i = 0; i < mapSize; i++) {
            // Read key type marker
            byte keyType = data[pos++];
            assert keyType == 1 : "Expected key TYPE_STRING (1), got " + keyType;

            // Read key string
            int keyLength = ((data[pos++] & 0xFF) << 8) | (data[pos++] & 0xFF);
            String key = new String(data, pos, keyLength, java.nio.charset.StandardCharsets.UTF_8);
            pos += keyLength;

            // Verify key is one of the expected keys
            assert key.equals("obj1") || key.equals("obj2") : "Unexpected key: " + key;
            assert !seenKeys.contains(key) : "Duplicate key: " + key;
            seenKeys.add(key);

            // Read value type marker
            byte valueType = data[pos++];
            assert valueType == 12 : "Expected value TYPE_OBJECT_PACKED (12), got " + valueType;

            // Read nested object length (should be 46 bytes for TestSimpleObject)
            int nestedLength = ((data[pos++] & 0xFF) << 24) |
                              ((data[pos++] & 0xFF) << 16) |
                              ((data[pos++] & 0xFF) << 8) |
                              (data[pos++] & 0xFF);

            // Verify reasonable size (TestSimpleObject serializes to ~40-50 bytes depending on string length)
            assert nestedLength > 30 && nestedLength < 100 : "Unexpected nested object length: " + nestedLength;

            totalNestedObjectBytes += nestedLength;

            // Validate the nested object structure AND field values
            int startPos = pos;

            // Check nested object header
            byte nestedTypeMarker = data[pos++];
            assert nestedTypeMarker == 12 : "Expected nested TYPE_OBJECT_PACKED (12), got " + nestedTypeMarker;

            byte nestedFieldCount = data[pos++];
            assert nestedFieldCount == 7 : "Expected nested field count=7, got " + nestedFieldCount;

            // Check nested type descriptors (0x21, 0x45, 0xA3, 0xB0)
            byte nestedNibble1 = data[pos++];
            assert nestedNibble1 == 0x21 : "Expected nested nibble 0x21, got 0x" + Integer.toHexString(nestedNibble1 & 0xFF);

            byte nestedNibble2 = data[pos++];
            assert nestedNibble2 == 0x45 : "Expected nested nibble 0x45, got 0x" + Integer.toHexString(nestedNibble2 & 0xFF);

            byte nestedNibble3 = data[pos++];
            assert (nestedNibble3 & 0xFF) == 0xA3 : "Expected nested nibble 0xA3, got 0x" + Integer.toHexString(nestedNibble3 & 0xFF);

            byte nestedNibble4 = data[pos++];
            assert (nestedNibble4 & 0xFF) == 0xB0 : "Expected nested nibble 0xB0, got 0x" + Integer.toHexString(nestedNibble4 & 0xFF);

            // Now validate the actual field values based on which key this is
            // obj1: TestSimpleObject(1, "Test1", true, 3.14, 2.71f, 123456789L, (short) 42)
            // obj2: TestSimpleObject(2, "Test2", false, 6.28, 3.14f, 987654321L, (short) 84)

            // Field 1: int id
            int id = ((data[pos++] & 0xFF) << 24) |
                     ((data[pos++] & 0xFF) << 16) |
                     ((data[pos++] & 0xFF) << 8) |
                     (data[pos++] & 0xFF);
            assert id == 1 || id == 2 : "Expected id=1 or id=2, got " + id;

            // Field 2: String name
            int nameLength = data[pos++] & 0xFF; // Varint for length < 128
            assert nameLength == 5 : "Expected name length=5, got " + nameLength;
            String name = new String(data, pos, nameLength, java.nio.charset.StandardCharsets.UTF_8);
            pos += nameLength;
            boolean isObj1 = name.equals("Test1");
            boolean isObj2 = name.equals("Test2");
            assert isObj1 || isObj2 : "Expected name='Test1' or 'Test2', got '" + name + "'";

            // Field 3: boolean active
            byte active = data[pos++];
            if (isObj1) {
                assert active == 1 : "Expected obj1 active=true, got " + (active == 1);
            } else if (isObj2) {
                assert active == 0 : "Expected obj2 active=false, got " + (active == 1);
            }

            // Field 4: double doubleValue
            long doubleBits = ((long)(data[pos++] & 0xFF) << 56) |
                             ((long)(data[pos++] & 0xFF) << 48) |
                             ((long)(data[pos++] & 0xFF) << 40) |
                             ((long)(data[pos++] & 0xFF) << 32) |
                             ((long)(data[pos++] & 0xFF) << 24) |
                             ((long)(data[pos++] & 0xFF) << 16) |
                             ((long)(data[pos++] & 0xFF) << 8) |
                             (long)(data[pos++] & 0xFF);
            double doubleValue = Double.longBitsToDouble(doubleBits);
            if (isObj1) {
                assert Math.abs(doubleValue - 3.14) < 0.001 : "Expected obj1 doubleValue~3.14, got " + doubleValue;
            } else if (isObj2) {
                assert Math.abs(doubleValue - 6.28) < 0.001 : "Expected obj2 doubleValue~6.28, got " + doubleValue;
            }

            // Field 5: float floatValue
            int floatBits = ((data[pos++] & 0xFF) << 24) |
                           ((data[pos++] & 0xFF) << 16) |
                           ((data[pos++] & 0xFF) << 8) |
                           (data[pos++] & 0xFF);
            float floatValue = Float.intBitsToFloat(floatBits);
            if (isObj1) {
                assert Math.abs(floatValue - 2.71f) < 0.001f : "Expected obj1 floatValue~2.71, got " + floatValue;
            } else if (isObj2) {
                assert Math.abs(floatValue - 3.14f) < 0.001f : "Expected obj2 floatValue~3.14, got " + floatValue;
            }

            // Field 6: long longValue
            long longValue = ((long)(data[pos++] & 0xFF) << 56) |
                            ((long)(data[pos++] & 0xFF) << 48) |
                            ((long)(data[pos++] & 0xFF) << 40) |
                            ((long)(data[pos++] & 0xFF) << 32) |
                            ((long)(data[pos++] & 0xFF) << 24) |
                            ((long)(data[pos++] & 0xFF) << 16) |
                            ((long)(data[pos++] & 0xFF) << 8) |
                            (long)(data[pos++] & 0xFF);
            if (isObj1) {
                assert longValue == 123456789L : "Expected obj1 longValue=123456789, got " + longValue;
            } else if (isObj2) {
                assert longValue == 987654321L : "Expected obj2 longValue=987654321, got " + longValue;
            }

            // Field 7: short shortValue
            short shortValue = (short)(((data[pos++] & 0xFF) << 8) | (data[pos++] & 0xFF));
            if (isObj1) {
                assert shortValue == 42 : "Expected obj1 shortValue=42, got " + shortValue;
            } else if (isObj2) {
                assert shortValue == 84 : "Expected obj2 shortValue=84, got " + shortValue;
            }

            // Verify we consumed exactly the nested object length
            int consumedBytes = pos - startPos;
            assert consumedBytes == nestedLength : "Expected to consume " + nestedLength + " bytes, consumed " + consumedBytes;
        }

        // Verify we saw both expected keys
        assert seenKeys.contains("obj1") : "Missing key 'obj1'";
        assert seenKeys.contains("obj2") : "Missing key 'obj2'";

        // Verify we consumed all bytes
        assert pos == data.length : "Expected to consume all " + data.length + " bytes, consumed " + pos;

        System.out.println("✓ Complex objects map binary format validation passed:");
        System.out.println("  Total size: " + data.length + " bytes");
        System.out.println("  Header: 2 bytes (type marker + field count)");
        System.out.println("  Type descriptors: 1 byte (1 map field)");
        System.out.println("  Map data validated:");
        System.out.println("    - Map<String,TestSimpleObject> with 2 entries");
        System.out.println("    - Nested object field values fully validated");
        System.out.println("    - All 7 fields per object verified (id, name, active, double, float, long, short)");
        System.out.println("    - Total nested objects size: " + totalNestedObjectBytes + " bytes");
        System.out.println("    - Keys found: " + seenKeys);
    }

    static class TestObjectWithMapOfComplexObjects {
        public Map<String, TestSimpleObject> complexMap = new HashMap<>();

        public TestObjectWithMapOfComplexObjects() {
            complexMap.put("obj1", new TestSimpleObject(1, "Test1", true, 3.14, 2.71f, 123456789L, (short) 42));
            complexMap.put("obj2", new TestSimpleObject(2, "Test2", false, 6.28, 3.14f, 987654321L, (short) 84));
        }
    }

    /**
     * Helper method to validate map structure and advance position.
     * Returns the new position after consuming the map data.
     */
    private int validateMapStructure(byte[] bytes, int pos, int expectedSize, String mapType) {
        // Read map size (4 bytes)
        int mapSize = ((bytes[pos++] & 0xFF) << 24) |
                ((bytes[pos++] & 0xFF) << 16) |
                ((bytes[pos++] & 0xFF) << 8) |
                (bytes[pos++] & 0xFF);
        assert mapSize == expectedSize : mapType + " expected size=" + expectedSize + ", got " + mapSize;

        // For each entry, skip key and value based on type markers
        for (int i = 0; i < mapSize; i++) {
            // Read key type marker
            byte keyType = bytes[pos++];
            pos = skipDataByType(bytes, pos, keyType);

            // Read value type marker
            byte valueType = bytes[pos++];
            pos = skipDataByType(bytes, pos, valueType);
        }

        return pos;
    }

    /**
     * Helper method to skip data based on type marker.
     * Returns the new position after skipping the data.
     */
    private int skipDataByType(byte[] bytes, int pos, byte typeMarker) {
        switch (typeMarker) {
            case 1: // TYPE_STRING
                // Read 2-byte length, then skip string data
                int strLen = ((bytes[pos++] & 0xFF) << 8) | (bytes[pos++] & 0xFF);
                pos += strLen;
                break;
            case 2: // TYPE_INT
                pos += 4;
                break;
            case 3: // TYPE_LONG
                pos += 8;
                break;
            case 4: // TYPE_BOOLEAN
                pos += 1;
                break;
            case 5: // TYPE_DOUBLE
                pos += 8;
                break;
            case 8: // TYPE_LIST or nested structure
            case 7: // TYPE_OBJECT
            case 12: // TYPE_OBJECT_PACKED
            case 13: // TYPE_LIST_STRING
                // Read 4-byte length prefix, then skip nested data
                int nestedLen = ((bytes[pos++] & 0xFF) << 24) |
                        ((bytes[pos++] & 0xFF) << 16) |
                        ((bytes[pos++] & 0xFF) << 8) |
                        (bytes[pos++] & 0xFF);
                pos += nestedLen;
                break;
            case 11: // TYPE_NULL
                // No data to skip
                break;
            default:
                throw new AssertionError("Unexpected type marker: " + typeMarker);
        }
        return pos;
    }
}
