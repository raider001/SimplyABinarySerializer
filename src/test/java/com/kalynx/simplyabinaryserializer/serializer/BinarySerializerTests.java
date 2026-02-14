package com.kalynx.simplyabinaryserializer.serializer;

import com.kalynx.simplyabinaryserializer.deserializer.FastByteReader;
import com.kalynx.simplyabinaryserializer.testutil.TestDataClasses.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for BinarySerializer covering all primitive types.
 * Each test verifies that serialization produces correct byte arrays.
 */
public class BinarySerializerTests {


    @Test
    void serialize_byteType_producesCorrectValue() throws Throwable {
        // Test with value 42
        BinarySerializer<ByteObject> serializer = new BinarySerializer<>(ByteObject.class);
        ByteObject obj = new ByteObject((byte) 42);

        byte[] result = serializer.serialize(obj);

        assertNotNull(result);
        assertEquals(1, result.length, "byte should serialize to 1 byte");

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertEquals(42, reader.readByte());
    }

    @Test
    void serialize_byteType_extremeValues_producesCorrectValues() throws Throwable {
        BinarySerializer<ByteObject> serializer = new BinarySerializer<>(ByteObject.class);

        // Test MIN_VALUE
        ByteObject minObj = new ByteObject(Byte.MIN_VALUE);
        byte[] minResult = serializer.serialize(minObj);
        FastByteReader minReader = new FastByteReader();
        minReader.setData(minResult);
        assertEquals(Byte.MIN_VALUE, minReader.readByte(), "MIN_VALUE should serialize correctly");

        // Test MAX_VALUE
        ByteObject maxObj = new ByteObject(Byte.MAX_VALUE);
        byte[] maxResult = serializer.serialize(maxObj);
        FastByteReader maxReader = new FastByteReader();
        maxReader.setData(maxResult);
        assertEquals(Byte.MAX_VALUE, maxReader.readByte(), "MAX_VALUE should serialize correctly");
    }

    @Test
    void serialize_shortType_producesCorrectValue() throws Throwable {
        BinarySerializer<ShortObject> serializer = new BinarySerializer<>(ShortObject.class);
        ShortObject obj = new ShortObject((short) 1000);

        byte[] result = serializer.serialize(obj);

        assertNotNull(result);
        assertEquals(2, result.length, "short should serialize to 2 bytes");

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertEquals(1000, reader.readShort());
    }

    @Test
    void serialize_shortType_extremeValues_producesCorrectValues() throws Throwable {
        BinarySerializer<ShortObject> serializer = new BinarySerializer<>(ShortObject.class);

        // Test MIN_VALUE
        ShortObject minObj = new ShortObject(Short.MIN_VALUE);
        byte[] minResult = serializer.serialize(minObj);
        FastByteReader minReader = new FastByteReader();
        minReader.setData(minResult);
        assertEquals(Short.MIN_VALUE, minReader.readShort(), "MIN_VALUE should serialize correctly");

        // Test MAX_VALUE
        ShortObject maxObj = new ShortObject(Short.MAX_VALUE);
        byte[] maxResult = serializer.serialize(maxObj);
        FastByteReader maxReader = new FastByteReader();
        maxReader.setData(maxResult);
        assertEquals(Short.MAX_VALUE, maxReader.readShort(), "MAX_VALUE should serialize correctly");
    }

    @Test
    void serialize_intType_producesCorrectValue() throws Throwable {
        BinarySerializer<IntObject> serializer = new BinarySerializer<>(IntObject.class);
        IntObject obj = new IntObject(123456);

        byte[] result = serializer.serialize(obj);

        assertNotNull(result);
        assertEquals(4, result.length, "int should serialize to 4 bytes");

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertEquals(123456, reader.readInt());
    }

    @Test
    void serialize_intType_extremeValues_producesCorrectValues() throws Throwable {
        BinarySerializer<IntObject> serializer = new BinarySerializer<>(IntObject.class);

        // Test MIN_VALUE
        IntObject minObj = new IntObject(Integer.MIN_VALUE);
        byte[] minResult = serializer.serialize(minObj);
        FastByteReader minReader = new FastByteReader();
        minReader.setData(minResult);
        assertEquals(Integer.MIN_VALUE, minReader.readInt(), "MIN_VALUE should serialize correctly");

        // Test MAX_VALUE
        IntObject maxObj = new IntObject(Integer.MAX_VALUE);
        byte[] maxResult = serializer.serialize(maxObj);
        FastByteReader maxReader = new FastByteReader();
        maxReader.setData(maxResult);
        assertEquals(Integer.MAX_VALUE, maxReader.readInt(), "MAX_VALUE should serialize correctly");
    }

    @Test
    void serialize_longType_producesCorrectValue() throws Throwable {
        BinarySerializer<LongObject> serializer = new BinarySerializer<>(LongObject.class);
        LongObject obj = new LongObject(9876543210L);

        byte[] result = serializer.serialize(obj);

        assertNotNull(result);
        assertEquals(8, result.length, "long should serialize to 8 bytes");

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertEquals(9876543210L, reader.readLong());
    }

    @Test
    void serialize_longType_extremeValues_producesCorrectValues() throws Throwable {
        BinarySerializer<LongObject> serializer = new BinarySerializer<>(LongObject.class);

        // Test MIN_VALUE
        LongObject minObj = new LongObject(Long.MIN_VALUE);
        byte[] minResult = serializer.serialize(minObj);
        FastByteReader minReader = new FastByteReader();
        minReader.setData(minResult);
        assertEquals(Long.MIN_VALUE, minReader.readLong(), "MIN_VALUE should serialize correctly");

        // Test MAX_VALUE
        LongObject maxObj = new LongObject(Long.MAX_VALUE);
        byte[] maxResult = serializer.serialize(maxObj);
        FastByteReader maxReader = new FastByteReader();
        maxReader.setData(maxResult);
        assertEquals(Long.MAX_VALUE, maxReader.readLong(), "MAX_VALUE should serialize correctly");
    }

    @Test
    void serialize_floatType_producesCorrectValue() throws Throwable {
        BinarySerializer<FloatObject> serializer = new BinarySerializer<>(FloatObject.class);
        FloatObject obj = new FloatObject(3.14159f);

        byte[] result = serializer.serialize(obj);

        assertNotNull(result);
        assertEquals(4, result.length, "float should serialize to 4 bytes");

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertEquals(3.14159f, reader.readFloat(), 0.00001f);
    }

    @Test
    void serialize_floatType_extremeValues_producesCorrectValues() throws Throwable {
        BinarySerializer<FloatObject> serializer = new BinarySerializer<>(FloatObject.class);

        // Test MIN_VALUE
        FloatObject minObj = new FloatObject(Float.MIN_VALUE);
        byte[] minResult = serializer.serialize(minObj);
        FastByteReader minReader = new FastByteReader();
        minReader.setData(minResult);
        assertEquals(Float.MIN_VALUE, minReader.readFloat(), 0.0f, "MIN_VALUE should serialize correctly");

        // Test MAX_VALUE
        FloatObject maxObj = new FloatObject(Float.MAX_VALUE);
        byte[] maxResult = serializer.serialize(maxObj);
        FastByteReader maxReader = new FastByteReader();
        maxReader.setData(maxResult);
        assertEquals(Float.MAX_VALUE, maxReader.readFloat(), 0.0f, "MAX_VALUE should serialize correctly");
    }

    @Test
    void serialize_doubleType_producesCorrectValue() throws Throwable {
        BinarySerializer<DoubleObject> serializer = new BinarySerializer<>(DoubleObject.class);
        DoubleObject obj = new DoubleObject(2.718281828459045);

        byte[] result = serializer.serialize(obj);

        assertNotNull(result);
        assertEquals(8, result.length, "double should serialize to 8 bytes");

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertEquals(2.718281828459045, reader.readDouble(), 0.000000000001);
    }

    @Test
    void serialize_doubleType_extremeValues_producesCorrectValues() throws Throwable {
        BinarySerializer<DoubleObject> serializer = new BinarySerializer<>(DoubleObject.class);

        // Test MIN_VALUE
        DoubleObject minObj = new DoubleObject(Double.MIN_VALUE);
        byte[] minResult = serializer.serialize(minObj);
        FastByteReader minReader = new FastByteReader();
        minReader.setData(minResult);
        assertEquals(Double.MIN_VALUE, minReader.readDouble(), 0.0, "MIN_VALUE should serialize correctly");

        // Test MAX_VALUE
        DoubleObject maxObj = new DoubleObject(Double.MAX_VALUE);
        byte[] maxResult = serializer.serialize(maxObj);
        FastByteReader maxReader = new FastByteReader();
        maxReader.setData(maxResult);
        assertEquals(Double.MAX_VALUE, maxReader.readDouble(), 0.0, "MAX_VALUE should serialize correctly");
    }

    @Test
    void serialize_booleanType_producesCorrectValue() throws Throwable {
        BinarySerializer<BooleanObject> serializer = new BinarySerializer<>(BooleanObject.class);

        // Test true
        BooleanObject trueObj = new BooleanObject(true);
        byte[] trueResult = serializer.serialize(trueObj);
        assertNotNull(trueResult);
        assertEquals(1, trueResult.length, "boolean should serialize to 1 byte");

        FastByteReader trueReader = new FastByteReader();
        trueReader.setData(trueResult);
        assertTrue(trueReader.readBoolean(), "true should serialize correctly");

        // Test false
        BooleanObject falseObj = new BooleanObject(false);
        byte[] falseResult = serializer.serialize(falseObj);
        assertNotNull(falseResult);
        assertEquals(1, falseResult.length, "boolean should serialize to 1 byte");

        FastByteReader falseReader = new FastByteReader();
        falseReader.setData(falseResult);
        assertFalse(falseReader.readBoolean(), "false should serialize correctly");
    }

    @Test
    void serialize_charType_producesCorrectValue() throws Throwable {
        BinarySerializer<CharObject> serializer = new BinarySerializer<>(CharObject.class);
        CharObject obj = new CharObject('X');

        byte[] result = serializer.serialize(obj);

        assertNotNull(result);
        assertEquals(2, result.length, "char should serialize to 2 bytes");

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertEquals('X', (char) reader.readShort(), "char should serialize as short");
    }

    @Test
    void serialize_charType_extremeValues_producesCorrectValues() throws Throwable {
        BinarySerializer<CharObject> serializer = new BinarySerializer<>(CharObject.class);

        // Test MIN_VALUE
        CharObject minObj = new CharObject(Character.MIN_VALUE);
        byte[] minResult = serializer.serialize(minObj);
        FastByteReader minReader = new FastByteReader();
        minReader.setData(minResult);
        assertEquals(Character.MIN_VALUE, (char) minReader.readShort(), "MIN_VALUE should serialize correctly");

        // Test MAX_VALUE
        CharObject maxObj = new CharObject(Character.MAX_VALUE);
        byte[] maxResult = serializer.serialize(maxObj);
        FastByteReader maxReader = new FastByteReader();
        maxReader.setData(maxResult);
        assertEquals(Character.MAX_VALUE, (char) maxReader.readShort(), "MAX_VALUE should serialize correctly");
    }


    @Test
    void serialize_allPrimitiveTypes_producesCorrectValues() throws Throwable {
        BinarySerializer<AllPrimitivesObject> serializer = new BinarySerializer<>(AllPrimitivesObject.class);
        AllPrimitivesObject obj = new AllPrimitivesObject(
            (byte) 42,
            (short) 1000,
            123456,
            9876543210L,
            3.14f,
            2.718281828,
            true,
            'Z'
        );

        byte[] result = serializer.serialize(obj);

        assertNotNull(result);
        // byte(1) + short(2) + int(4) + long(8) + float(4) + double(8) + bool(1) + char(2) = 30 bytes
        assertEquals(30, result.length, "All primitives should serialize to 30 bytes");

        FastByteReader reader = new FastByteReader();
        reader.setData(result);

        assertEquals(42, reader.readByte(), "byte value mismatch");
        assertEquals(1000, reader.readShort(), "short value mismatch");
        assertEquals(123456, reader.readInt(), "int value mismatch");
        assertEquals(9876543210L, reader.readLong(), "long value mismatch");
        assertEquals(3.14f, reader.readFloat(), 0.001f, "float value mismatch");
        assertEquals(2.718281828, reader.readDouble(), 0.000001, "double value mismatch");
        assertTrue(reader.readBoolean(), "boolean value mismatch");
        assertEquals('Z', (char) reader.readShort(), "char value mismatch");
    }

    @Test
    void serialize_zeroValues_producesCorrectValues() throws Throwable {
        BinarySerializer<AllPrimitivesObject> serializer = new BinarySerializer<>(AllPrimitivesObject.class);
        AllPrimitivesObject obj = new AllPrimitivesObject(
            (byte) 0,
            (short) 0,
            0,
            0L,
            0.0f,
            0.0,
            false,
            '\0'
        );

        byte[] result = serializer.serialize(obj);

        FastByteReader reader = new FastByteReader();
        reader.setData(result);

        assertEquals(0, reader.readByte(), "zero byte should serialize correctly");
        assertEquals(0, reader.readShort(), "zero short should serialize correctly");
        assertEquals(0, reader.readInt(), "zero int should serialize correctly");
        assertEquals(0L, reader.readLong(), "zero long should serialize correctly");
        assertEquals(0.0f, reader.readFloat(), 0.0f, "zero float should serialize correctly");
        assertEquals(0.0, reader.readDouble(), 0.0, "zero double should serialize correctly");
        assertFalse(reader.readBoolean(), "false should serialize correctly");
        assertEquals('\0', (char) reader.readShort(), "null char should serialize correctly");
    }

    @Test
    void serialize_negativeValues_producesCorrectValues() throws Throwable {
        BinarySerializer<AllPrimitivesObject> serializer = new BinarySerializer<>(AllPrimitivesObject.class);
        AllPrimitivesObject obj = new AllPrimitivesObject(
            (byte) -42,
            (short) -1000,
            -123456,
            -9876543210L,
            -3.14f,
            -2.718281828,
            false,
            'A'
        );

        byte[] result = serializer.serialize(obj);

        FastByteReader reader = new FastByteReader();
        reader.setData(result);

        assertEquals(-42, reader.readByte(), "negative byte should serialize correctly");
        assertEquals(-1000, reader.readShort(), "negative short should serialize correctly");
        assertEquals(-123456, reader.readInt(), "negative int should serialize correctly");
        assertEquals(-9876543210L, reader.readLong(), "negative long should serialize correctly");
        assertEquals(-3.14f, reader.readFloat(), 0.001f, "negative float should serialize correctly");
        assertEquals(-2.718281828, reader.readDouble(), 0.000001, "negative double should serialize correctly");
    }

    @Test
    void serialize_nullObject_returnsEmptyArray() throws Throwable {
        BinarySerializer<IntObject> serializer = new BinarySerializer<>(IntObject.class);

        byte[] result = serializer.serialize(null);

        assertNotNull(result);
        assertEquals(0, result.length, "null object should serialize to empty array");
    }

    @Test
    void serialize_multipleTimes_producesConsistentResults() throws Throwable {
        BinarySerializer<IntObject> serializer = new BinarySerializer<>(IntObject.class);
        IntObject obj = new IntObject(999);

        byte[] result1 = serializer.serialize(obj);
        byte[] result2 = serializer.serialize(obj);
        byte[] result3 = serializer.serialize(obj);

        assertArrayEquals(result1, result2, "First and second serialization should match");
        assertArrayEquals(result2, result3, "Second and third serialization should match");
    }
}
