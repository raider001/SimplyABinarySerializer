package com.kalynx.simplyabinaryserializer;

import com.kalynx.simplyabinaryserializer.testutil.TestDataClasses.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for KalynxSerializer demonstrating both serialization and deserialization.
 * Each test performs a complete round-trip: serialize → deserialize → verify.
 */
public class KalynxSerializerTest {


    @Test
    void roundTrip_byteType_preservesValue() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(ByteObject.class);

        ByteObject original = new ByteObject((byte) 42);
        byte[] bytes = kalynxSerializer.serialize(original);
        ByteObject deserialized = kalynxSerializer.deserialize(bytes, ByteObject.class);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value);
        assertEquals(1, bytes.length, "byte should serialize to 1 byte");
    }

    @Test
    void roundTrip_byteType_extremeValues() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(ByteObject.class);

        // Test MIN_VALUE
        ByteObject minOriginal = new ByteObject(Byte.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        ByteObject minDeserialized = kalynxSerializer.deserialize(minBytes, ByteObject.class);
        assertEquals(Byte.MIN_VALUE, minDeserialized.value, "MIN_VALUE should round-trip correctly");

        // Test MAX_VALUE
        ByteObject maxOriginal = new ByteObject(Byte.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        ByteObject maxDeserialized = kalynxSerializer.deserialize(maxBytes, ByteObject.class);
        assertEquals(Byte.MAX_VALUE, maxDeserialized.value, "MAX_VALUE should round-trip correctly");
    }

    @Test
    void roundTrip_shortType_preservesValue() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(ShortObject.class);

        ShortObject original = new ShortObject((short) 1000);
        byte[] bytes = kalynxSerializer.serialize(original);
        ShortObject deserialized = kalynxSerializer.deserialize(bytes, ShortObject.class);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value);
        assertEquals(2, bytes.length, "short should serialize to 2 bytes");
    }

    @Test
    void roundTrip_shortType_extremeValues() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(ShortObject.class);

        // Test MIN_VALUE
        ShortObject minOriginal = new ShortObject(Short.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        ShortObject minDeserialized = kalynxSerializer.deserialize(minBytes, ShortObject.class);
        assertEquals(Short.MIN_VALUE, minDeserialized.value);

        // Test MAX_VALUE
        ShortObject maxOriginal = new ShortObject(Short.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        ShortObject maxDeserialized = kalynxSerializer.deserialize(maxBytes, ShortObject.class);
        assertEquals(Short.MAX_VALUE, maxDeserialized.value);
    }

    @Test
    void roundTrip_intType_preservesValue() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(IntObject.class);

        IntObject original = new IntObject(123456);
        byte[] bytes = kalynxSerializer.serialize(original);
        IntObject deserialized = kalynxSerializer.deserialize(bytes, IntObject.class);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value);
        assertEquals(4, bytes.length, "int should serialize to 4 bytes");
    }

    @Test
    void roundTrip_intType_extremeValues() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(IntObject.class);

        // Test MIN_VALUE
        IntObject minOriginal = new IntObject(Integer.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        IntObject minDeserialized = kalynxSerializer.deserialize(minBytes, IntObject.class);
        assertEquals(Integer.MIN_VALUE, minDeserialized.value);

        // Test MAX_VALUE
        IntObject maxOriginal = new IntObject(Integer.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        IntObject maxDeserialized = kalynxSerializer.deserialize(maxBytes, IntObject.class);
        assertEquals(Integer.MAX_VALUE, maxDeserialized.value);
    }

    @Test
    void roundTrip_longType_preservesValue() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(LongObject.class);

        LongObject original = new LongObject(9876543210L);
        byte[] bytes = kalynxSerializer.serialize(original);
        LongObject deserialized = kalynxSerializer.deserialize(bytes, LongObject.class);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value);
        assertEquals(8, bytes.length, "long should serialize to 8 bytes");
    }

    @Test
    void roundTrip_longType_extremeValues() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(LongObject.class);

        // Test MIN_VALUE
        LongObject minOriginal = new LongObject(Long.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        LongObject minDeserialized = kalynxSerializer.deserialize(minBytes, LongObject.class);
        assertEquals(Long.MIN_VALUE, minDeserialized.value);

        // Test MAX_VALUE
        LongObject maxOriginal = new LongObject(Long.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        LongObject maxDeserialized = kalynxSerializer.deserialize(maxBytes, LongObject.class);
        assertEquals(Long.MAX_VALUE, maxDeserialized.value);
    }

    @Test
    void roundTrip_floatType_preservesValue() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(FloatObject.class);

        FloatObject original = new FloatObject(3.14159f);
        byte[] bytes = kalynxSerializer.serialize(original);
        FloatObject deserialized = kalynxSerializer.deserialize(bytes, FloatObject.class);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value, 0.00001f);
        assertEquals(4, bytes.length, "float should serialize to 4 bytes");
    }

    @Test
    void roundTrip_floatType_extremeValues() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(FloatObject.class);

        // Test MIN_VALUE
        FloatObject minOriginal = new FloatObject(Float.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        FloatObject minDeserialized = kalynxSerializer.deserialize(minBytes, FloatObject.class);
        assertEquals(Float.MIN_VALUE, minDeserialized.value, 0.0f);

        // Test MAX_VALUE
        FloatObject maxOriginal = new FloatObject(Float.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        FloatObject maxDeserialized = kalynxSerializer.deserialize(maxBytes, FloatObject.class);
        assertEquals(Float.MAX_VALUE, maxDeserialized.value, 0.0f);
    }

    @Test
    void roundTrip_doubleType_preservesValue() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(DoubleObject.class);

        DoubleObject original = new DoubleObject(2.718281828459045);
        byte[] bytes = kalynxSerializer.serialize(original);
        DoubleObject deserialized = kalynxSerializer.deserialize(bytes, DoubleObject.class);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value, 0.000000000001);
        assertEquals(8, bytes.length, "double should serialize to 8 bytes");
    }

    @Test
    void roundTrip_doubleType_extremeValues() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(DoubleObject.class);

        // Test MIN_VALUE
        DoubleObject minOriginal = new DoubleObject(Double.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        DoubleObject minDeserialized = kalynxSerializer.deserialize(minBytes, DoubleObject.class);
        assertEquals(Double.MIN_VALUE, minDeserialized.value, 0.0);

        // Test MAX_VALUE
        DoubleObject maxOriginal = new DoubleObject(Double.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        DoubleObject maxDeserialized = kalynxSerializer.deserialize(maxBytes, DoubleObject.class);
        assertEquals(Double.MAX_VALUE, maxDeserialized.value, 0.0);
    }

    @Test
    void roundTrip_booleanType_preservesValue() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(BooleanObject.class);

        // Test true
        BooleanObject trueOriginal = new BooleanObject(true);
        byte[] trueBytes = kalynxSerializer.serialize(trueOriginal);
        BooleanObject trueDeserialized = kalynxSerializer.deserialize(trueBytes, BooleanObject.class);
        assertTrue(trueDeserialized.value);
        assertEquals(1, trueBytes.length, "boolean should serialize to 1 byte");

        // Test false
        BooleanObject falseOriginal = new BooleanObject(false);
        byte[] falseBytes = kalynxSerializer.serialize(falseOriginal);
        BooleanObject falseDeserialized = kalynxSerializer.deserialize(falseBytes, BooleanObject.class);
        assertFalse(falseDeserialized.value);
        assertEquals(1, falseBytes.length, "boolean should serialize to 1 byte");
    }

    @Test
    void roundTrip_charType_preservesValue() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(CharObject.class);

        CharObject original = new CharObject('X');
        byte[] bytes = kalynxSerializer.serialize(original);
        CharObject deserialized = kalynxSerializer.deserialize(bytes, CharObject.class);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value);
        assertEquals(2, bytes.length, "char should serialize to 2 bytes");
    }

    @Test
    void roundTrip_charType_extremeValues() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(CharObject.class);

        // Test MIN_VALUE
        CharObject minOriginal = new CharObject(Character.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        CharObject minDeserialized = kalynxSerializer.deserialize(minBytes, CharObject.class);
        assertEquals(Character.MIN_VALUE, minDeserialized.value);

        // Test MAX_VALUE
        CharObject maxOriginal = new CharObject(Character.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        CharObject maxDeserialized = kalynxSerializer.deserialize(maxBytes, CharObject.class);
        assertEquals(Character.MAX_VALUE, maxDeserialized.value);
    }


    @Test
    void roundTrip_allPrimitiveTypes_preservesAllValues() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(AllPrimitivesObject.class);

        AllPrimitivesObject original = new AllPrimitivesObject(
            (byte) 42,
            (short) 1000,
            123456,
            9876543210L,
            3.14f,
            2.718281828,
            true,
            'Z'
        );

        byte[] bytes = kalynxSerializer.serialize(original);
        AllPrimitivesObject deserialized = kalynxSerializer.deserialize(bytes, AllPrimitivesObject.class);

        assertNotNull(deserialized);
        // byte(1) + short(2) + int(4) + long(8) + float(4) + double(8) + bool(1) + char(2) = 30 bytes
        assertEquals(30, bytes.length, "All primitives should serialize to 30 bytes");

        assertEquals(original.byteVal, deserialized.byteVal, "byte value mismatch");
        assertEquals(original.shortVal, deserialized.shortVal, "short value mismatch");
        assertEquals(original.intVal, deserialized.intVal, "int value mismatch");
        assertEquals(original.longVal, deserialized.longVal, "long value mismatch");
        assertEquals(original.floatVal, deserialized.floatVal, 0.001f, "float value mismatch");
        assertEquals(original.doubleVal, deserialized.doubleVal, 0.000001, "double value mismatch");
        assertEquals(original.boolVal, deserialized.boolVal, "boolean value mismatch");
        assertEquals(original.charVal, deserialized.charVal, "char value mismatch");
    }

    @Test
    void roundTrip_zeroValues_preservesAllValues() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(AllPrimitivesObject.class);

        AllPrimitivesObject original = new AllPrimitivesObject(
            (byte) 0,
            (short) 0,
            0,
            0L,
            0.0f,
            0.0,
            false,
            '\0'
        );

        byte[] bytes = kalynxSerializer.serialize(original);
        AllPrimitivesObject deserialized = kalynxSerializer.deserialize(bytes, AllPrimitivesObject.class);

        assertNotNull(deserialized);
        assertEquals(0, deserialized.byteVal, "zero byte should round-trip");
        assertEquals(0, deserialized.shortVal, "zero short should round-trip");
        assertEquals(0, deserialized.intVal, "zero int should round-trip");
        assertEquals(0L, deserialized.longVal, "zero long should round-trip");
        assertEquals(0.0f, deserialized.floatVal, 0.0f, "zero float should round-trip");
        assertEquals(0.0, deserialized.doubleVal, 0.0, "zero double should round-trip");
        assertFalse(deserialized.boolVal, "false should round-trip");
        assertEquals('\0', deserialized.charVal, "null char should round-trip");
    }

    @Test
    void roundTrip_negativeValues_preservesAllValues() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(AllPrimitivesObject.class);

        AllPrimitivesObject original = new AllPrimitivesObject(
            (byte) -42,
            (short) -1000,
            -123456,
            -9876543210L,
            -3.14f,
            -2.718281828,
            false,
            'A'
        );

        byte[] bytes = kalynxSerializer.serialize(original);
        AllPrimitivesObject deserialized = kalynxSerializer.deserialize(bytes, AllPrimitivesObject.class);

        assertNotNull(deserialized);
        assertEquals(-42, deserialized.byteVal, "negative byte should round-trip");
        assertEquals(-1000, deserialized.shortVal, "negative short should round-trip");
        assertEquals(-123456, deserialized.intVal, "negative int should round-trip");
        assertEquals(-9876543210L, deserialized.longVal, "negative long should round-trip");
        assertEquals(-3.14f, deserialized.floatVal, 0.001f, "negative float should round-trip");
        assertEquals(-2.718281828, deserialized.doubleVal, 0.000001, "negative double should round-trip");
    }

    @Test
    void roundTrip_extremeValues_preservesAllValues() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(AllPrimitivesObject.class);

        AllPrimitivesObject original = new AllPrimitivesObject(
            Byte.MAX_VALUE,
            Short.MIN_VALUE,
            Integer.MAX_VALUE,
            Long.MIN_VALUE,
            Float.MAX_VALUE,
            Double.MIN_VALUE,
            true,
            Character.MAX_VALUE
        );

        byte[] bytes = kalynxSerializer.serialize(original);
        AllPrimitivesObject deserialized = kalynxSerializer.deserialize(bytes, AllPrimitivesObject.class);

        assertNotNull(deserialized);
        assertEquals(Byte.MAX_VALUE, deserialized.byteVal);
        assertEquals(Short.MIN_VALUE, deserialized.shortVal);
        assertEquals(Integer.MAX_VALUE, deserialized.intVal);
        assertEquals(Long.MIN_VALUE, deserialized.longVal);
        assertEquals(Float.MAX_VALUE, deserialized.floatVal, 0.0f);
        assertEquals(Double.MIN_VALUE, deserialized.doubleVal, 0.0);
        assertTrue(deserialized.boolVal);
        assertEquals(Character.MAX_VALUE, deserialized.charVal);
    }

    @Test
    void roundTrip_nullInput_throwsException() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(IntObject.class);

        // Test that null serialize throws exception
        IllegalArgumentException serializeException = assertThrows(
            IllegalArgumentException.class,
            () -> kalynxSerializer.serialize(null),
            "Serializing null should throw IllegalArgumentException"
        );
        assertEquals("Cannot serialize null object", serializeException.getMessage());

        // Test that null/empty deserialize throws exception
        IllegalArgumentException deserializeException = assertThrows(
            IllegalArgumentException.class,
            () -> kalynxSerializer.deserialize(null, IntObject.class),
            "Deserializing null should throw IllegalArgumentException"
        );
        assertEquals("Cannot deserialize null or empty bytes", deserializeException.getMessage());

        // Test that empty byte array deserialize throws exception
        IllegalArgumentException emptyException = assertThrows(
            IllegalArgumentException.class,
            () -> kalynxSerializer.deserialize(new byte[0], IntObject.class),
            "Deserializing empty array should throw IllegalArgumentException"
        );
        assertEquals("Cannot deserialize null or empty bytes", emptyException.getMessage());
    }

    @Test
    void roundTrip_multipleSerializations_produceConsistentResults() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(IntObject.class);

        IntObject original = new IntObject(999);

        byte[] bytes1 = kalynxSerializer.serialize(original);
        byte[] bytes2 = kalynxSerializer.serialize(original);
        byte[] bytes3 = kalynxSerializer.serialize(original);

        // All serializations should produce identical bytes
        assertArrayEquals(bytes1, bytes2, "First and second serialization should match");
        assertArrayEquals(bytes2, bytes3, "Second and third serialization should match");

        // All deserializations should produce identical values
        IntObject result1 = kalynxSerializer.deserialize(bytes1, IntObject.class);
        IntObject result2 = kalynxSerializer.deserialize(bytes2, IntObject.class);
        IntObject result3 = kalynxSerializer.deserialize(bytes3, IntObject.class);

        assertEquals(999, result1.value);
        assertEquals(999, result2.value);
        assertEquals(999, result3.value);
    }

    @Test
    void roundTrip_multipleInstances_independent() throws Throwable {
        KalynxSerializer kalynxSerializer = new KalynxSerializer();
        kalynxSerializer.register(IntObject.class);

        IntObject obj1 = new IntObject(100);
        IntObject obj2 = new IntObject(200);
        IntObject obj3 = new IntObject(300);

        byte[] bytes1 = kalynxSerializer.serialize(obj1);
        byte[] bytes2 = kalynxSerializer.serialize(obj2);
        byte[] bytes3 = kalynxSerializer.serialize(obj3);

        // Deserialize in different order
        IntObject result3 = kalynxSerializer.deserialize(bytes3, IntObject.class);
        IntObject result1 = kalynxSerializer.deserialize(bytes1, IntObject.class);
        IntObject result2 = kalynxSerializer.deserialize(bytes2, IntObject.class);

        assertEquals(100, result1.value);
        assertEquals(200, result2.value);
        assertEquals(300, result3.value);
    }

    // ========== List Serialization Tests ==========

    @Test
    void roundTrip_integerList_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(IntegerListObject.class);

        List<Integer> values = Arrays.asList(1, 2, 3, 4, 5);
        IntegerListObject original = new IntegerListObject(values);

        byte[] bytes = serializer.serialize(original);
        IntegerListObject deserialized = serializer.deserialize(bytes, IntegerListObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertEquals(5, deserialized.values.size());
        assertEquals(values, deserialized.values);
    }

    @Test
    void roundTrip_integerList_withNulls_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(IntegerListObject.class);

        List<Integer> values = Arrays.asList(1, null, 3, null, 5);
        IntegerListObject original = new IntegerListObject(values);

        byte[] bytes = serializer.serialize(original);
        IntegerListObject deserialized = serializer.deserialize(bytes, IntegerListObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertEquals(5, deserialized.values.size());
        assertEquals(Integer.valueOf(1), deserialized.values.get(0));
        assertNull(deserialized.values.get(1));
        assertEquals(Integer.valueOf(3), deserialized.values.get(2));
        assertNull(deserialized.values.get(3));
        assertEquals(Integer.valueOf(5), deserialized.values.get(4));
    }

    @Test
    void roundTrip_emptyIntegerList_preservesEmpty() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(IntegerListObject.class);

        IntegerListObject original = new IntegerListObject(new ArrayList<>());

        byte[] bytes = serializer.serialize(original);
        IntegerListObject deserialized = serializer.deserialize(bytes, IntegerListObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertEquals(0, deserialized.values.size());
    }

    @Test
    void roundTrip_nullIntegerList_preservesNull() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(IntegerListObject.class);

        IntegerListObject original = new IntegerListObject(null);

        byte[] bytes = serializer.serialize(original);
        IntegerListObject deserialized = serializer.deserialize(bytes, IntegerListObject.class);

        assertNotNull(deserialized);
        assertNull(deserialized.values);
    }

    @Test
    void roundTrip_stringList_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(StringListObject.class);

        List<String> strings = Arrays.asList("hello", "world", "test", "serialization");
        StringListObject original = new StringListObject(strings);

        byte[] bytes = serializer.serialize(original);
        StringListObject deserialized = serializer.deserialize(bytes, StringListObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.strings);
        assertEquals(4, deserialized.strings.size());
        assertEquals(strings, deserialized.strings);
    }

    @Test
    void roundTrip_stringList_withNullsAndEmptyStrings_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(StringListObject.class);

        List<String> strings = Arrays.asList("hello", null, "", "world");
        StringListObject original = new StringListObject(strings);

        byte[] bytes = serializer.serialize(original);
        StringListObject deserialized = serializer.deserialize(bytes, StringListObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.strings);
        assertEquals(4, deserialized.strings.size());
        assertEquals("hello", deserialized.strings.get(0));
        assertNull(deserialized.strings.get(1));
        assertEquals("", deserialized.strings.get(2));
        assertEquals("world", deserialized.strings.get(3));
    }

    @Test
    void roundTrip_longList_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(LongListObject.class);

        List<Long> values = Arrays.asList(1L, 1000L, 1000000L, Long.MAX_VALUE, Long.MIN_VALUE);
        LongListObject original = new LongListObject(values);

        byte[] bytes = serializer.serialize(original);
        LongListObject deserialized = serializer.deserialize(bytes, LongListObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertEquals(5, deserialized.values.size());
        assertEquals(values, deserialized.values);
    }

    @Test
    void roundTrip_doubleList_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(DoubleListObject.class);

        List<Double> values = Arrays.asList(1.0, 2.5, 3.14159, Double.MAX_VALUE, Double.MIN_VALUE);
        DoubleListObject original = new DoubleListObject(values);

        byte[] bytes = serializer.serialize(original);
        DoubleListObject deserialized = serializer.deserialize(bytes, DoubleListObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertEquals(5, deserialized.values.size());
        for (int i = 0; i < values.size(); i++) {
            assertEquals(values.get(i), deserialized.values.get(i), 0.0);
        }
    }

    @Test
    void roundTrip_mixedPrimitiveAndList_preservesAllValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(MixedPrimitiveAndListObject.class);

        List<Integer> intList = Arrays.asList(10, 20, 30);
        MixedPrimitiveAndListObject original = new MixedPrimitiveAndListObject(42, intList);

        byte[] bytes = serializer.serialize(original);
        MixedPrimitiveAndListObject deserialized = serializer.deserialize(bytes, MixedPrimitiveAndListObject.class);

        assertNotNull(deserialized);
        assertEquals(42, deserialized.intValue);
        assertNotNull(deserialized.intList);
        assertEquals(3, deserialized.intList.size());
        assertEquals(intList, deserialized.intList);
    }

    @Test
    void roundTrip_allPrimitivesWithLists_preservesAllValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(AllPrimitivesWithListsObject.class);

        List<Integer> intList = Arrays.asList(1, 2, 3);
        List<String> stringList = Arrays.asList("a", "b", "c");
        AllPrimitivesWithListsObject original = new AllPrimitivesWithListsObject(
            (byte) 99,
            intList,
            123456789L,
            stringList,
            true
        );

        byte[] bytes = serializer.serialize(original);
        AllPrimitivesWithListsObject deserialized = serializer.deserialize(bytes, AllPrimitivesWithListsObject.class);

        assertNotNull(deserialized);
        assertEquals(99, deserialized.byteVal);
        assertEquals(intList, deserialized.intList);
        assertEquals(123456789L, deserialized.longVal);
        assertEquals(stringList, deserialized.stringList);
        assertTrue(deserialized.boolVal);
    }

    @Test
    void roundTrip_largeIntegerList_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(IntegerListObject.class);

        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            values.add(i);
        }
        IntegerListObject original = new IntegerListObject(values);

        byte[] bytes = serializer.serialize(original);
        IntegerListObject deserialized = serializer.deserialize(bytes, IntegerListObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertEquals(1000, deserialized.values.size());
        assertEquals(values, deserialized.values);
    }

    // ========== Array Tests ==========

    @Test
    void roundTrip_intArray_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(IntArrayObject.class);

        IntArrayObject original = new IntArrayObject(new int[]{1, 2, 3, 4, 5});
        byte[] bytes = serializer.serialize(original);
        IntArrayObject deserialized = serializer.deserialize(bytes, IntArrayObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertArrayEquals(original.values, deserialized.values);
    }

    @Test
    void roundTrip_longArray_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(LongArrayObject.class);

        LongArrayObject original = new LongArrayObject(new long[]{1L, 2L, 3L, Long.MAX_VALUE});
        byte[] bytes = serializer.serialize(original);
        LongArrayObject deserialized = serializer.deserialize(bytes, LongArrayObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertArrayEquals(original.values, deserialized.values);
    }

    @Test
    void roundTrip_doubleArray_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(DoubleArrayObject.class);

        DoubleArrayObject original = new DoubleArrayObject(new double[]{1.1, 2.2, 3.3, Math.PI});
        byte[] bytes = serializer.serialize(original);
        DoubleArrayObject deserialized = serializer.deserialize(bytes, DoubleArrayObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertArrayEquals(original.values, deserialized.values, 0.0001);
    }

    @Test
    void roundTrip_floatArray_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(FloatArrayObject.class);

        FloatArrayObject original = new FloatArrayObject(new float[]{1.1f, 2.2f, 3.3f, (float)Math.PI});
        byte[] bytes = serializer.serialize(original);
        FloatArrayObject deserialized = serializer.deserialize(bytes, FloatArrayObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertArrayEquals(original.values, deserialized.values, 0.0001f);
    }

    @Test
    void roundTrip_shortArray_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(ShortArrayObject.class);

        ShortArrayObject original = new ShortArrayObject(new short[]{1, 2, 3, Short.MAX_VALUE});
        byte[] bytes = serializer.serialize(original);
        ShortArrayObject deserialized = serializer.deserialize(bytes, ShortArrayObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertArrayEquals(original.values, deserialized.values);
    }

    @Test
    void roundTrip_byteArray_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(ByteArrayObject.class);

        ByteArrayObject original = new ByteArrayObject(new byte[]{1, 2, 3, Byte.MAX_VALUE});
        byte[] bytes = serializer.serialize(original);
        ByteArrayObject deserialized = serializer.deserialize(bytes, ByteArrayObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertArrayEquals(original.values, deserialized.values);
    }

    @Test
    void roundTrip_booleanArray_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(BooleanArrayObject.class);

        BooleanArrayObject original = new BooleanArrayObject(new boolean[]{true, false, true, true, false});
        byte[] bytes = serializer.serialize(original);
        BooleanArrayObject deserialized = serializer.deserialize(bytes, BooleanArrayObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertArrayEquals(original.values, deserialized.values);
    }

    @Test
    void roundTrip_charArray_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(CharArrayObject.class);

        CharArrayObject original = new CharArrayObject(new char[]{'a', 'b', 'c', 'Z'});
        byte[] bytes = serializer.serialize(original);
        CharArrayObject deserialized = serializer.deserialize(bytes, CharArrayObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertArrayEquals(original.values, deserialized.values);
    }

    @Test
    void roundTrip_emptyArray_preservesEmpty() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(IntArrayObject.class);

        IntArrayObject original = new IntArrayObject(new int[]{});
        byte[] bytes = serializer.serialize(original);
        IntArrayObject deserialized = serializer.deserialize(bytes, IntArrayObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertEquals(0, deserialized.values.length);
    }

    @Test
    void roundTrip_nullArray_preservesNull() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(IntArrayObject.class);

        IntArrayObject original = new IntArrayObject(null);
        byte[] bytes = serializer.serialize(original);
        IntArrayObject deserialized = serializer.deserialize(bytes, IntArrayObject.class);

        assertNotNull(deserialized);
        assertNull(deserialized.values);
    }

    @Test
    void roundTrip_allPrimitiveArrays_preservesAllValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(AllPrimitiveArraysObject.class);

        AllPrimitiveArraysObject original = new AllPrimitiveArraysObject(
            new int[]{1, 2, 3},
            new long[]{4L, 5L, 6L},
            new double[]{1.1, 2.2, 3.3},
            new float[]{4.4f, 5.5f, 6.6f},
            new short[]{7, 8, 9},
            new byte[]{10, 11, 12},
            new boolean[]{true, false, true},
            new char[]{'x', 'y', 'z'}
        );

        byte[] bytes = serializer.serialize(original);
        AllPrimitiveArraysObject deserialized = serializer.deserialize(bytes, AllPrimitiveArraysObject.class);

        assertNotNull(deserialized);
        assertArrayEquals(original.intArray, deserialized.intArray);
        assertArrayEquals(original.longArray, deserialized.longArray);
        assertArrayEquals(original.doubleArray, deserialized.doubleArray, 0.0001);
        assertArrayEquals(original.floatArray, deserialized.floatArray, 0.0001f);
        assertArrayEquals(original.shortArray, deserialized.shortArray);
        assertArrayEquals(original.byteArray, deserialized.byteArray);
        assertArrayEquals(original.booleanArray, deserialized.booleanArray);
        assertArrayEquals(original.charArray, deserialized.charArray);
    }

    // ========== Map Tests ==========

    @Test
    void roundTrip_stringIntegerMap_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(StringIntegerMapObject.class);

        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        StringIntegerMapObject original = new StringIntegerMapObject(map);
        byte[] bytes = serializer.serialize(original);
        StringIntegerMapObject deserialized = serializer.deserialize(bytes, StringIntegerMapObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.map);
        assertEquals(3, deserialized.map.size());
        assertEquals(1, deserialized.map.get("one"));
        assertEquals(2, deserialized.map.get("two"));
        assertEquals(3, deserialized.map.get("three"));
    }

    @Test
    void roundTrip_integerStringMap_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(IntegerStringMapObject.class);

        Map<Integer, String> map = new HashMap<>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        IntegerStringMapObject original = new IntegerStringMapObject(map);
        byte[] bytes = serializer.serialize(original);
        IntegerStringMapObject deserialized = serializer.deserialize(bytes, IntegerStringMapObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.map);
        assertEquals(3, deserialized.map.size());
        assertEquals("one", deserialized.map.get(1));
        assertEquals("two", deserialized.map.get(2));
        assertEquals("three", deserialized.map.get(3));
    }

    @Test
    void roundTrip_integerIntegerMap_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(IntegerIntegerMapObject.class);

        Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 100);
        map.put(2, 200);
        map.put(3, 300);

        IntegerIntegerMapObject original = new IntegerIntegerMapObject(map);
        byte[] bytes = serializer.serialize(original);
        IntegerIntegerMapObject deserialized = serializer.deserialize(bytes, IntegerIntegerMapObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.map);
        assertEquals(3, deserialized.map.size());
        assertEquals(100, deserialized.map.get(1));
        assertEquals(200, deserialized.map.get(2));
        assertEquals(300, deserialized.map.get(3));
    }

    @Test
    void roundTrip_longDoubleMap_preservesValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(LongDoubleMapObject.class);

        Map<Long, Double> map = new HashMap<>();
        map.put(1L, 1.5);
        map.put(2L, 2.5);
        map.put(3L, 3.5);

        LongDoubleMapObject original = new LongDoubleMapObject(map);
        byte[] bytes = serializer.serialize(original);
        LongDoubleMapObject deserialized = serializer.deserialize(bytes, LongDoubleMapObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.map);
        assertEquals(3, deserialized.map.size());
        assertEquals(1.5, deserialized.map.get(1L), 0.0001);
        assertEquals(2.5, deserialized.map.get(2L), 0.0001);
        assertEquals(3.5, deserialized.map.get(3L), 0.0001);
    }

    @Test
    void roundTrip_emptyMap_preservesEmpty() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(StringIntegerMapObject.class);

        StringIntegerMapObject original = new StringIntegerMapObject(new HashMap<>());
        byte[] bytes = serializer.serialize(original);
        StringIntegerMapObject deserialized = serializer.deserialize(bytes, StringIntegerMapObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.map);
        assertEquals(0, deserialized.map.size());
    }

    @Test
    void roundTrip_nullMap_preservesNull() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(StringIntegerMapObject.class);

        StringIntegerMapObject original = new StringIntegerMapObject(null);
        byte[] bytes = serializer.serialize(original);
        StringIntegerMapObject deserialized = serializer.deserialize(bytes, StringIntegerMapObject.class);

        assertNotNull(deserialized);
        assertNull(deserialized.map);
    }

    @Test
    void roundTrip_mixedPrimitiveAndMap_preservesAllValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(MixedPrimitiveAndMapObject.class);

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 10);
        map.put("b", 20);
        map.put("c", 30);

        MixedPrimitiveAndMapObject original = new MixedPrimitiveAndMapObject(42, map);
        byte[] bytes = serializer.serialize(original);
        MixedPrimitiveAndMapObject deserialized = serializer.deserialize(bytes, MixedPrimitiveAndMapObject.class);

        assertNotNull(deserialized);
        assertEquals(42, deserialized.intValue);
        assertNotNull(deserialized.map);
        assertEquals(3, deserialized.map.size());
        assertEquals(10, deserialized.map.get("a"));
        assertEquals(20, deserialized.map.get("b"));
        assertEquals(30, deserialized.map.get("c"));
    }

    // ========== Nested Object Tests ==========

    @Test
    void roundTrip_simpleNestedObject_preservesAllValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(SimpleNestedObject.class);

        IntObject nested = new IntObject(42);
        SimpleNestedObject original = new SimpleNestedObject(100, nested);

        byte[] bytes = serializer.serialize(original);
        SimpleNestedObject deserialized = serializer.deserialize(bytes, SimpleNestedObject.class);

        assertNotNull(deserialized);
        assertEquals(100, deserialized.value);
        assertNotNull(deserialized.nested);
        assertEquals(42, deserialized.nested.value);
    }

    @Test
    void roundTrip_nullNestedObject_preservesNull() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(SimpleNestedObject.class);

        SimpleNestedObject original = new SimpleNestedObject(100, null);

        byte[] bytes = serializer.serialize(original);
        SimpleNestedObject deserialized = serializer.deserialize(bytes, SimpleNestedObject.class);

        assertNotNull(deserialized);
        assertEquals(100, deserialized.value);
        assertNull(deserialized.nested);
    }

    @Test
    void roundTrip_multiNestedObject_preservesAllValues() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(MultiNestedObject.class);

        IntObject intNested = new IntObject(42);
        DoubleObject doubleNested = new DoubleObject(3.14159);
        MultiNestedObject original = new MultiNestedObject(1, intNested, doubleNested, 1234567890L);

        byte[] bytes = serializer.serialize(original);
        MultiNestedObject deserialized = serializer.deserialize(bytes, MultiNestedObject.class);

        assertNotNull(deserialized);
        assertEquals(1, deserialized.id);
        assertNotNull(deserialized.intNested);
        assertEquals(42, deserialized.intNested.value);
        assertNotNull(deserialized.doubleNested);
        assertEquals(3.14159, deserialized.doubleNested.value, 0.0001);
        assertEquals(1234567890L, deserialized.timestamp);
    }

    @Test
    void roundTrip_rectangle_preservesNestedPoints() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(Rectangle.class);

        Point topLeft = new Point(10, 20);
        Point bottomRight = new Point(100, 200);
        Rectangle original = new Rectangle(topLeft, bottomRight, 0xFF0000);

        byte[] bytes = serializer.serialize(original);
        Rectangle deserialized = serializer.deserialize(bytes, Rectangle.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.topLeft);
        assertEquals(10, deserialized.topLeft.x);
        assertEquals(20, deserialized.topLeft.y);
        assertNotNull(deserialized.bottomRight);
        assertEquals(100, deserialized.bottomRight.x);
        assertEquals(200, deserialized.bottomRight.y);
        assertEquals(0xFF0000, deserialized.color);
    }

    @Test
    void roundTrip_deepNested_preservesThreeLevels() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(DeepNestedLevel1.class);

        DeepNestedLevel3 level3 = new DeepNestedLevel3(300);
        DeepNestedLevel2 level2 = new DeepNestedLevel2(200, level3);
        DeepNestedLevel1 original = new DeepNestedLevel1(100, level2);

        byte[] bytes = serializer.serialize(original);
        DeepNestedLevel1 deserialized = serializer.deserialize(bytes, DeepNestedLevel1.class);

        assertNotNull(deserialized);
        assertEquals(100, deserialized.level1Value);
        assertNotNull(deserialized.level2);
        assertEquals(200, deserialized.level2.level2Value);
        assertNotNull(deserialized.level2.level3);
        assertEquals(300, deserialized.level2.level3.level3Value);
    }

    @Test
    void roundTrip_deepNestedWithNullMiddle_preservesStructure() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(DeepNestedLevel1.class);

        DeepNestedLevel2 level2 = new DeepNestedLevel2(200, null);
        DeepNestedLevel1 original = new DeepNestedLevel1(100, level2);

        byte[] bytes = serializer.serialize(original);
        DeepNestedLevel1 deserialized = serializer.deserialize(bytes, DeepNestedLevel1.class);

        assertNotNull(deserialized);
        assertEquals(100, deserialized.level1Value);
        assertNotNull(deserialized.level2);
        assertEquals(200, deserialized.level2.level2Value);
        assertNull(deserialized.level2.level3);
    }

    // ============================================================================
    // TypeReference Tests
    // ============================================================================

    @Test
    void typeReference_capturesListType() {
        TypeReference<List<Integer>> typeRef = new TypeReference<List<Integer>>() {};

        assertEquals(List.class, typeRef.getRawType());
        assertNotNull(typeRef.getType());
        assertTrue(typeRef.getType().toString().contains("java.util.List"));
        assertTrue(typeRef.getType().toString().contains("Integer"));
    }

    @Test
    void typeReference_capturesMapType() {
        TypeReference<Map<String, Integer>> typeRef = new TypeReference<Map<String, Integer>>() {};

        assertEquals(Map.class, typeRef.getRawType());
        assertNotNull(typeRef.getType());
        assertTrue(typeRef.getType().toString().contains("java.util.Map"));
    }

    @Test
    void typeReference_equalityBasedOnType() {
        TypeReference<List<Integer>> ref1 = new TypeReference<List<Integer>>() {};
        TypeReference<List<Integer>> ref2 = new TypeReference<List<Integer>>() {};
        TypeReference<List<String>> ref3 = new TypeReference<List<String>>() {};

        assertEquals(ref1, ref2, "Same generic types should be equal");
        assertNotEquals(ref1, ref3, "Different generic types should not be equal");
        assertEquals(ref1.hashCode(), ref2.hashCode(), "Equal types should have same hashCode");
    }

    @Test
    void typeReference_toStringContainsTypeInfo() {
        TypeReference<List<Integer>> typeRef = new TypeReference<List<Integer>>() {};

        String str = typeRef.toString();
        assertTrue(str.contains("TypeReference"));
        assertTrue(str.contains("List") || str.contains("java.util.List"));
    }

    // ============================================================================
    // Generic Serialization Tests with TypeReference
    // ============================================================================

    @Test
    void typeReference_canBeUsedToCheckRegistration() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();

        TypeReference<IntegerListObject> typeRef = new TypeReference<IntegerListObject>() {};

        assertFalse(serializer.isRegistered(typeRef), "Should not be registered initially");
        assertFalse(serializer.isRegistered(IntegerListObject.class), "Class should not be registered initially");

        serializer.register(IntegerListObject.class);

        assertTrue(serializer.isRegistered(IntegerListObject.class),
            "IntegerListObject.class should be registered after registering");
    }

    @Test
    void roundTrip_integerListObject_withTypeInformation() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(IntegerListObject.class);

        List<Integer> values = Arrays.asList(1, 2, 3, 4, 5);
        IntegerListObject original = new IntegerListObject(values);

        byte[] bytes = serializer.serialize(original);
        IntegerListObject deserialized = serializer.deserialize(bytes, IntegerListObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.values);
        assertEquals(original.values, deserialized.values);
    }

    @Test
    void roundTrip_stringListObject_withTypeInformation() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(StringListObject.class);

        List<String> strings = Arrays.asList("hello", "world", "test");
        StringListObject original = new StringListObject(strings);

        byte[] bytes = serializer.serialize(original);
        StringListObject deserialized = serializer.deserialize(bytes, StringListObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.strings);
        assertEquals(original.strings, deserialized.strings);
    }

    @Test
    void roundTrip_mapObject_withGenericTypes() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(StringIntegerMapObject.class);

        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        StringIntegerMapObject original = new StringIntegerMapObject(map);
        byte[] bytes = serializer.serialize(original);
        StringIntegerMapObject deserialized = serializer.deserialize(bytes, StringIntegerMapObject.class);

        assertNotNull(deserialized);
        assertNotNull(deserialized.map);
        assertEquals(original.map.size(), deserialized.map.size());
    }

    @Test
    void register_typeReference_storesTypeInformation() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();

        TypeReference<IntegerListObject> typeRef = new TypeReference<IntegerListObject>() {};

        serializer.register(typeRef);

        assertTrue(serializer.isRegistered(typeRef));
        assertTrue(serializer.isRegistered(IntegerListObject.class));
    }

    @Test
    void deserialize_withTypeReference_worksForRegisteredTypes() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();

        TypeReference<IntegerListObject> typeRef = new TypeReference<IntegerListObject>() {};
        serializer.register(typeRef);

        List<Integer> values = Arrays.asList(10, 20, 30);
        IntegerListObject original = new IntegerListObject(values);

        byte[] bytes = serializer.serialize(original);
        IntegerListObject deserialized = serializer.deserialize(bytes, typeRef);

        assertNotNull(deserialized);
        assertEquals(original.values, deserialized.values);
    }

    @Test
    void deserialize_unregisteredTypeReference_throwsException() {
        KalynxSerializer serializer = new KalynxSerializer();

        byte[] bytes = new byte[]{1, 2, 3};
        TypeReference<IntegerListObject> typeRef = new TypeReference<IntegerListObject>() {};

        assertThrows(IllegalStateException.class, () ->
            serializer.deserialize(bytes, typeRef),
            "Should throw exception for unregistered type");
    }

    @Test
    void typeReference_differentGenericTypes_notEqual() {
        TypeReference<List<Integer>> ref1 = new TypeReference<List<Integer>>() {};
        TypeReference<List<String>> ref2 = new TypeReference<List<String>>() {};

        assertNotEquals(ref1, ref2, "Different generic types should not be equal");
        assertNotEquals(ref1.getType(), ref2.getType());
    }

    // ============================================================================
    // Multi-Class Registration Tests
    // ============================================================================

    @Test
    void multiClass_fluentRegistration() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();

        // Register multiple classes using fluent API
        serializer
                .register(Person.class)
                .register(Address.class)
                .register(Company.class);

        // Verify all are registered
        assertTrue(serializer.isRegistered(Person.class));
        assertTrue(serializer.isRegistered(Address.class));
        assertTrue(serializer.isRegistered(Company.class));
        assertFalse(serializer.isRegistered(String.class));

        // Create and serialize different types
        Person person = new Person("Alice", 28, "alice@example.com");
        Address address = new Address("123 Main St", "Springfield", "12345");
        Company company = new Company("TechCorp", 500);

        byte[] personBytes = serializer.serialize(person);
        byte[] addressBytes = serializer.serialize(address);
        byte[] companyBytes = serializer.serialize(company);

        // Deserialize and verify
        Person deserializedPerson = serializer.deserialize(personBytes, Person.class);
        Address deserializedAddress = serializer.deserialize(addressBytes, Address.class);
        Company deserializedCompany = serializer.deserialize(companyBytes, Company.class);

        assertEquals(person.name, deserializedPerson.name);
        assertEquals(person.age, deserializedPerson.age);
        assertEquals(person.email, deserializedPerson.email);

        assertEquals(address.street, deserializedAddress.street);
        assertEquals(address.city, deserializedAddress.city);
        assertEquals(address.zipCode, deserializedAddress.zipCode);

        assertEquals(company.name, deserializedCompany.name);
        assertEquals(company.employeeCount, deserializedCompany.employeeCount);
    }

    @Test
    void multiClass_separateRegistration() throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();

        // Register classes separately
        serializer.register(Person.class);
        serializer.register(Address.class);

        Person person = new Person("Bob", 35, "bob@test.com");
        byte[] bytes = serializer.serialize(person);
        Person result = serializer.deserialize(bytes, Person.class);

        assertEquals("Bob", result.name);
        assertEquals(35, result.age);
    }

    // Helper classes for multi-class tests
    public static class Person {
        public String name;
        public int age;
        public String email;

        public Person() {}

        public Person(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }
    }

    public static class Address {
        public String street;
        public String city;
        public String zipCode;

        public Address() {}

        public Address(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }
    }

    public static class Company {
        public String name;
        public int employeeCount;

        public Company() {}

        public Company(String name, int employeeCount) {
            this.name = name;
            this.employeeCount = employeeCount;
        }
    }
}

