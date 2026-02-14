package com.kalynx.simplyabinaryserializer;

import com.kalynx.simplyabinaryserializer.testutil.TestDataClasses.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for KalynxSerializer demonstrating both serialization and deserialization.
 * Each test performs a complete round-trip: serialize → deserialize → verify.
 */
public class KalynxSerializerTest {


    @Test
    void roundTrip_byteType_preservesValue() throws Throwable {
        KalynxSerializer<ByteObject> kalynxSerializer = new KalynxSerializer<>(ByteObject.class);

        ByteObject original = new ByteObject((byte) 42);
        byte[] bytes = kalynxSerializer.serialize(original);
        ByteObject deserialized = kalynxSerializer.deserialize(bytes);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value);
        assertEquals(1, bytes.length, "byte should serialize to 1 byte");
    }

    @Test
    void roundTrip_byteType_extremeValues() throws Throwable {
        KalynxSerializer<ByteObject> kalynxSerializer = new KalynxSerializer<>(ByteObject.class);

        // Test MIN_VALUE
        ByteObject minOriginal = new ByteObject(Byte.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        ByteObject minDeserialized = kalynxSerializer.deserialize(minBytes);
        assertEquals(Byte.MIN_VALUE, minDeserialized.value, "MIN_VALUE should round-trip correctly");

        // Test MAX_VALUE
        ByteObject maxOriginal = new ByteObject(Byte.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        ByteObject maxDeserialized = kalynxSerializer.deserialize(maxBytes);
        assertEquals(Byte.MAX_VALUE, maxDeserialized.value, "MAX_VALUE should round-trip correctly");
    }

    @Test
    void roundTrip_shortType_preservesValue() throws Throwable {
        KalynxSerializer<ShortObject> kalynxSerializer = new KalynxSerializer<>(ShortObject.class);

        ShortObject original = new ShortObject((short) 1000);
        byte[] bytes = kalynxSerializer.serialize(original);
        ShortObject deserialized = kalynxSerializer.deserialize(bytes);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value);
        assertEquals(2, bytes.length, "short should serialize to 2 bytes");
    }

    @Test
    void roundTrip_shortType_extremeValues() throws Throwable {
        KalynxSerializer<ShortObject> kalynxSerializer = new KalynxSerializer<>(ShortObject.class);

        // Test MIN_VALUE
        ShortObject minOriginal = new ShortObject(Short.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        ShortObject minDeserialized = kalynxSerializer.deserialize(minBytes);
        assertEquals(Short.MIN_VALUE, minDeserialized.value);

        // Test MAX_VALUE
        ShortObject maxOriginal = new ShortObject(Short.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        ShortObject maxDeserialized = kalynxSerializer.deserialize(maxBytes);
        assertEquals(Short.MAX_VALUE, maxDeserialized.value);
    }

    @Test
    void roundTrip_intType_preservesValue() throws Throwable {
        KalynxSerializer<IntObject> kalynxSerializer = new KalynxSerializer<>(IntObject.class);

        IntObject original = new IntObject(123456);
        byte[] bytes = kalynxSerializer.serialize(original);
        IntObject deserialized = kalynxSerializer.deserialize(bytes);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value);
        assertEquals(4, bytes.length, "int should serialize to 4 bytes");
    }

    @Test
    void roundTrip_intType_extremeValues() throws Throwable {
        KalynxSerializer<IntObject> kalynxSerializer = new KalynxSerializer<>(IntObject.class);

        // Test MIN_VALUE
        IntObject minOriginal = new IntObject(Integer.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        IntObject minDeserialized = kalynxSerializer.deserialize(minBytes);
        assertEquals(Integer.MIN_VALUE, minDeserialized.value);

        // Test MAX_VALUE
        IntObject maxOriginal = new IntObject(Integer.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        IntObject maxDeserialized = kalynxSerializer.deserialize(maxBytes);
        assertEquals(Integer.MAX_VALUE, maxDeserialized.value);
    }

    @Test
    void roundTrip_longType_preservesValue() throws Throwable {
        KalynxSerializer<LongObject> kalynxSerializer = new KalynxSerializer<>(LongObject.class);

        LongObject original = new LongObject(9876543210L);
        byte[] bytes = kalynxSerializer.serialize(original);
        LongObject deserialized = kalynxSerializer.deserialize(bytes);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value);
        assertEquals(8, bytes.length, "long should serialize to 8 bytes");
    }

    @Test
    void roundTrip_longType_extremeValues() throws Throwable {
        KalynxSerializer<LongObject> kalynxSerializer = new KalynxSerializer<>(LongObject.class);

        // Test MIN_VALUE
        LongObject minOriginal = new LongObject(Long.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        LongObject minDeserialized = kalynxSerializer.deserialize(minBytes);
        assertEquals(Long.MIN_VALUE, minDeserialized.value);

        // Test MAX_VALUE
        LongObject maxOriginal = new LongObject(Long.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        LongObject maxDeserialized = kalynxSerializer.deserialize(maxBytes);
        assertEquals(Long.MAX_VALUE, maxDeserialized.value);
    }

    @Test
    void roundTrip_floatType_preservesValue() throws Throwable {
        KalynxSerializer<FloatObject> kalynxSerializer = new KalynxSerializer<>(FloatObject.class);

        FloatObject original = new FloatObject(3.14159f);
        byte[] bytes = kalynxSerializer.serialize(original);
        FloatObject deserialized = kalynxSerializer.deserialize(bytes);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value, 0.00001f);
        assertEquals(4, bytes.length, "float should serialize to 4 bytes");
    }

    @Test
    void roundTrip_floatType_extremeValues() throws Throwable {
        KalynxSerializer<FloatObject> kalynxSerializer = new KalynxSerializer<>(FloatObject.class);

        // Test MIN_VALUE
        FloatObject minOriginal = new FloatObject(Float.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        FloatObject minDeserialized = kalynxSerializer.deserialize(minBytes);
        assertEquals(Float.MIN_VALUE, minDeserialized.value, 0.0f);

        // Test MAX_VALUE
        FloatObject maxOriginal = new FloatObject(Float.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        FloatObject maxDeserialized = kalynxSerializer.deserialize(maxBytes);
        assertEquals(Float.MAX_VALUE, maxDeserialized.value, 0.0f);
    }

    @Test
    void roundTrip_doubleType_preservesValue() throws Throwable {
        KalynxSerializer<DoubleObject> kalynxSerializer = new KalynxSerializer<>(DoubleObject.class);

        DoubleObject original = new DoubleObject(2.718281828459045);
        byte[] bytes = kalynxSerializer.serialize(original);
        DoubleObject deserialized = kalynxSerializer.deserialize(bytes);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value, 0.000000000001);
        assertEquals(8, bytes.length, "double should serialize to 8 bytes");
    }

    @Test
    void roundTrip_doubleType_extremeValues() throws Throwable {
        KalynxSerializer<DoubleObject> kalynxSerializer = new KalynxSerializer<>(DoubleObject.class);

        // Test MIN_VALUE
        DoubleObject minOriginal = new DoubleObject(Double.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        DoubleObject minDeserialized = kalynxSerializer.deserialize(minBytes);
        assertEquals(Double.MIN_VALUE, minDeserialized.value, 0.0);

        // Test MAX_VALUE
        DoubleObject maxOriginal = new DoubleObject(Double.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        DoubleObject maxDeserialized = kalynxSerializer.deserialize(maxBytes);
        assertEquals(Double.MAX_VALUE, maxDeserialized.value, 0.0);
    }

    @Test
    void roundTrip_booleanType_preservesValue() throws Throwable {
        KalynxSerializer<BooleanObject> kalynxSerializer = new KalynxSerializer<>(BooleanObject.class);

        // Test true
        BooleanObject trueOriginal = new BooleanObject(true);
        byte[] trueBytes = kalynxSerializer.serialize(trueOriginal);
        BooleanObject trueDeserialized = kalynxSerializer.deserialize(trueBytes);
        assertTrue(trueDeserialized.value);
        assertEquals(1, trueBytes.length, "boolean should serialize to 1 byte");

        // Test false
        BooleanObject falseOriginal = new BooleanObject(false);
        byte[] falseBytes = kalynxSerializer.serialize(falseOriginal);
        BooleanObject falseDeserialized = kalynxSerializer.deserialize(falseBytes);
        assertFalse(falseDeserialized.value);
        assertEquals(1, falseBytes.length, "boolean should serialize to 1 byte");
    }

    @Test
    void roundTrip_charType_preservesValue() throws Throwable {
        KalynxSerializer<CharObject> kalynxSerializer = new KalynxSerializer<>(CharObject.class);

        CharObject original = new CharObject('X');
        byte[] bytes = kalynxSerializer.serialize(original);
        CharObject deserialized = kalynxSerializer.deserialize(bytes);

        assertNotNull(deserialized);
        assertEquals(original.value, deserialized.value);
        assertEquals(2, bytes.length, "char should serialize to 2 bytes");
    }

    @Test
    void roundTrip_charType_extremeValues() throws Throwable {
        KalynxSerializer<CharObject> kalynxSerializer = new KalynxSerializer<>(CharObject.class);

        // Test MIN_VALUE
        CharObject minOriginal = new CharObject(Character.MIN_VALUE);
        byte[] minBytes = kalynxSerializer.serialize(minOriginal);
        CharObject minDeserialized = kalynxSerializer.deserialize(minBytes);
        assertEquals(Character.MIN_VALUE, minDeserialized.value);

        // Test MAX_VALUE
        CharObject maxOriginal = new CharObject(Character.MAX_VALUE);
        byte[] maxBytes = kalynxSerializer.serialize(maxOriginal);
        CharObject maxDeserialized = kalynxSerializer.deserialize(maxBytes);
        assertEquals(Character.MAX_VALUE, maxDeserialized.value);
    }


    @Test
    void roundTrip_allPrimitiveTypes_preservesAllValues() throws Throwable {
        KalynxSerializer<AllPrimitivesObject> kalynxSerializer = new KalynxSerializer<>(AllPrimitivesObject.class);

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
        AllPrimitivesObject deserialized = kalynxSerializer.deserialize(bytes);

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
        KalynxSerializer<AllPrimitivesObject> kalynxSerializer = new KalynxSerializer<>(AllPrimitivesObject.class);

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
        AllPrimitivesObject deserialized = kalynxSerializer.deserialize(bytes);

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
        KalynxSerializer<AllPrimitivesObject> kalynxSerializer = new KalynxSerializer<>(AllPrimitivesObject.class);

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
        AllPrimitivesObject deserialized = kalynxSerializer.deserialize(bytes);

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
        KalynxSerializer<AllPrimitivesObject> kalynxSerializer = new KalynxSerializer<>(AllPrimitivesObject.class);

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
        AllPrimitivesObject deserialized = kalynxSerializer.deserialize(bytes);

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
    void roundTrip_nullInput_handlesGracefully() throws Throwable {
        KalynxSerializer<IntObject> kalynxSerializer = new KalynxSerializer<>(IntObject.class);

        byte[] bytes = kalynxSerializer.serialize(null);
        IntObject result = kalynxSerializer.deserialize(bytes);

        assertNotNull(bytes);
        assertEquals(0, bytes.length, "null object should serialize to empty array");
        assertNull(result, "empty array should deserialize to null");
    }

    @Test
    void roundTrip_multipleSerializations_produceConsistentResults() throws Throwable {
        KalynxSerializer<IntObject> kalynxSerializer = new KalynxSerializer<>(IntObject.class);

        IntObject original = new IntObject(999);

        byte[] bytes1 = kalynxSerializer.serialize(original);
        byte[] bytes2 = kalynxSerializer.serialize(original);
        byte[] bytes3 = kalynxSerializer.serialize(original);

        // All serializations should produce identical bytes
        assertArrayEquals(bytes1, bytes2, "First and second serialization should match");
        assertArrayEquals(bytes2, bytes3, "Second and third serialization should match");

        // All deserializations should produce identical values
        IntObject result1 = kalynxSerializer.deserialize(bytes1);
        IntObject result2 = kalynxSerializer.deserialize(bytes2);
        IntObject result3 = kalynxSerializer.deserialize(bytes3);

        assertEquals(999, result1.value);
        assertEquals(999, result2.value);
        assertEquals(999, result3.value);
    }

    @Test
    void roundTrip_multipleInstances_independent() throws Throwable {
        KalynxSerializer<IntObject> kalynxSerializer = new KalynxSerializer<>(IntObject.class);

        IntObject obj1 = new IntObject(100);
        IntObject obj2 = new IntObject(200);
        IntObject obj3 = new IntObject(300);

        byte[] bytes1 = kalynxSerializer.serialize(obj1);
        byte[] bytes2 = kalynxSerializer.serialize(obj2);
        byte[] bytes3 = kalynxSerializer.serialize(obj3);

        // Deserialize in different order
        IntObject result3 = kalynxSerializer.deserialize(bytes3);
        IntObject result1 = kalynxSerializer.deserialize(bytes1);
        IntObject result2 = kalynxSerializer.deserialize(bytes2);

        assertEquals(100, result1.value);
        assertEquals(200, result2.value);
        assertEquals(300, result3.value);
    }
}

