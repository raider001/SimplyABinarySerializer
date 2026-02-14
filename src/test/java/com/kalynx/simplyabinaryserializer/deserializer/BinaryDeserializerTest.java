package com.kalynx.simplyabinaryserializer.deserializer;

import com.kalynx.simplyabinaryserializer.serializer.FastByteWriter;
import com.kalynx.simplyabinaryserializer.testutil.TestDataClasses.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for BinaryDeserializer covering all primitive types.
 * Each test verifies that deserialization produces correct objects from byte arrays.
 */
public class BinaryDeserializerTest {


    @Test
    void deserialize_byteType_producesCorrectValue() throws Throwable {
        BinaryDeserializer<ByteObject> deserializer = new BinaryDeserializer<>(ByteObject.class);

        // Create test data
        FastByteWriter writer = new FastByteWriter();
        writer.reset(10);
        writer.writeByte(127);
        byte[] data = writer.toByteArray();

        ByteObject result = deserializer.deserialize(data);

        assertNotNull(result);
        assertEquals(127, result.value);
    }

    @Test
    void deserialize_byteType_extremeValues_producesCorrectValues() throws Throwable {
        BinaryDeserializer<ByteObject> deserializer = new BinaryDeserializer<>(ByteObject.class);

        // Test MIN_VALUE
        FastByteWriter minWriter = new FastByteWriter();
        minWriter.reset(10);
        minWriter.writeByte(Byte.MIN_VALUE);
        ByteObject minResult = deserializer.deserialize(minWriter.toByteArray());
        assertEquals(Byte.MIN_VALUE, minResult.value, "MIN_VALUE should deserialize correctly");

        // Test MAX_VALUE
        FastByteWriter maxWriter = new FastByteWriter();
        maxWriter.reset(10);
        maxWriter.writeByte(Byte.MAX_VALUE);
        ByteObject maxResult = deserializer.deserialize(maxWriter.toByteArray());
        assertEquals(Byte.MAX_VALUE, maxResult.value, "MAX_VALUE should deserialize correctly");
    }

    @Test
    void deserialize_shortType_producesCorrectValue() throws Throwable {
        BinaryDeserializer<ShortObject> deserializer = new BinaryDeserializer<>(ShortObject.class);

        FastByteWriter writer = new FastByteWriter();
        writer.reset(10);
        writer.writeShort((short) 1000);
        byte[] data = writer.toByteArray();

        ShortObject result = deserializer.deserialize(data);

        assertNotNull(result);
        assertEquals(1000, result.value);
    }

    @Test
    void deserialize_shortType_extremeValues_producesCorrectValues() throws Throwable {
        BinaryDeserializer<ShortObject> deserializer = new BinaryDeserializer<>(ShortObject.class);

        // Test MIN_VALUE
        FastByteWriter minWriter = new FastByteWriter();
        minWriter.reset(10);
        minWriter.writeShort(Short.MIN_VALUE);
        ShortObject minResult = deserializer.deserialize(minWriter.toByteArray());
        assertEquals(Short.MIN_VALUE, minResult.value);

        // Test MAX_VALUE
        FastByteWriter maxWriter = new FastByteWriter();
        maxWriter.reset(10);
        maxWriter.writeShort(Short.MAX_VALUE);
        ShortObject maxResult = deserializer.deserialize(maxWriter.toByteArray());
        assertEquals(Short.MAX_VALUE, maxResult.value);
    }

    @Test
    void deserialize_intType_producesCorrectValue() throws Throwable {
        BinaryDeserializer<IntObject> deserializer = new BinaryDeserializer<>(IntObject.class);

        FastByteWriter writer = new FastByteWriter();
        writer.reset(10);
        writer.writeInt(123456);
        byte[] data = writer.toByteArray();

        IntObject result = deserializer.deserialize(data);

        assertNotNull(result);
        assertEquals(123456, result.value);
    }

    @Test
    void deserialize_intType_extremeValues_producesCorrectValues() throws Throwable {
        BinaryDeserializer<IntObject> deserializer = new BinaryDeserializer<>(IntObject.class);

        // Test MIN_VALUE
        FastByteWriter minWriter = new FastByteWriter();
        minWriter.reset(10);
        minWriter.writeInt(Integer.MIN_VALUE);
        IntObject minResult = deserializer.deserialize(minWriter.toByteArray());
        assertEquals(Integer.MIN_VALUE, minResult.value);

        // Test MAX_VALUE
        FastByteWriter maxWriter = new FastByteWriter();
        maxWriter.reset(10);
        maxWriter.writeInt(Integer.MAX_VALUE);
        IntObject maxResult = deserializer.deserialize(maxWriter.toByteArray());
        assertEquals(Integer.MAX_VALUE, maxResult.value);
    }

    @Test
    void deserialize_longType_producesCorrectValue() throws Throwable {
        BinaryDeserializer<LongObject> deserializer = new BinaryDeserializer<>(LongObject.class);

        FastByteWriter writer = new FastByteWriter();
        writer.reset(10);
        writer.writeLong(9876543210L);
        byte[] data = writer.toByteArray();

        LongObject result = deserializer.deserialize(data);

        assertNotNull(result);
        assertEquals(9876543210L, result.value);
    }

    @Test
    void deserialize_longType_extremeValues_producesCorrectValues() throws Throwable {
        BinaryDeserializer<LongObject> deserializer = new BinaryDeserializer<>(LongObject.class);

        // Test MIN_VALUE
        FastByteWriter minWriter = new FastByteWriter();
        minWriter.reset(10);
        minWriter.writeLong(Long.MIN_VALUE);
        LongObject minResult = deserializer.deserialize(minWriter.toByteArray());
        assertEquals(Long.MIN_VALUE, minResult.value);

        // Test MAX_VALUE
        FastByteWriter maxWriter = new FastByteWriter();
        maxWriter.reset(10);
        maxWriter.writeLong(Long.MAX_VALUE);
        LongObject maxResult = deserializer.deserialize(maxWriter.toByteArray());
        assertEquals(Long.MAX_VALUE, maxResult.value);
    }

    @Test
    void deserialize_floatType_producesCorrectValue() throws Throwable {
        BinaryDeserializer<FloatObject> deserializer = new BinaryDeserializer<>(FloatObject.class);

        FastByteWriter writer = new FastByteWriter();
        writer.reset(10);
        writer.writeFloat(3.14159f);
        byte[] data = writer.toByteArray();

        FloatObject result = deserializer.deserialize(data);

        assertNotNull(result);
        assertEquals(3.14159f, result.value, 0.00001f);
    }

    @Test
    void deserialize_floatType_extremeValues_producesCorrectValues() throws Throwable {
        BinaryDeserializer<FloatObject> deserializer = new BinaryDeserializer<>(FloatObject.class);

        // Test MIN_VALUE
        FastByteWriter minWriter = new FastByteWriter();
        minWriter.reset(10);
        minWriter.writeFloat(Float.MIN_VALUE);
        FloatObject minResult = deserializer.deserialize(minWriter.toByteArray());
        assertEquals(Float.MIN_VALUE, minResult.value, 0.0f);

        // Test MAX_VALUE
        FastByteWriter maxWriter = new FastByteWriter();
        maxWriter.reset(10);
        maxWriter.writeFloat(Float.MAX_VALUE);
        FloatObject maxResult = deserializer.deserialize(maxWriter.toByteArray());
        assertEquals(Float.MAX_VALUE, maxResult.value, 0.0f);
    }

    @Test
    void deserialize_doubleType_producesCorrectValue() throws Throwable {
        BinaryDeserializer<DoubleObject> deserializer = new BinaryDeserializer<>(DoubleObject.class);

        FastByteWriter writer = new FastByteWriter();
        writer.reset(10);
        writer.writeDouble(2.718281828459045);
        byte[] data = writer.toByteArray();

        DoubleObject result = deserializer.deserialize(data);

        assertNotNull(result);
        assertEquals(2.718281828459045, result.value, 0.000000000001);
    }

    @Test
    void deserialize_doubleType_extremeValues_producesCorrectValues() throws Throwable {
        BinaryDeserializer<DoubleObject> deserializer = new BinaryDeserializer<>(DoubleObject.class);

        // Test MIN_VALUE
        FastByteWriter minWriter = new FastByteWriter();
        minWriter.reset(10);
        minWriter.writeDouble(Double.MIN_VALUE);
        DoubleObject minResult = deserializer.deserialize(minWriter.toByteArray());
        assertEquals(Double.MIN_VALUE, minResult.value, 0.0);

        // Test MAX_VALUE
        FastByteWriter maxWriter = new FastByteWriter();
        maxWriter.reset(10);
        maxWriter.writeDouble(Double.MAX_VALUE);
        DoubleObject maxResult = deserializer.deserialize(maxWriter.toByteArray());
        assertEquals(Double.MAX_VALUE, maxResult.value, 0.0);
    }

    @Test
    void deserialize_booleanType_producesCorrectValue() throws Throwable {
        BinaryDeserializer<BooleanObject> deserializer = new BinaryDeserializer<>(BooleanObject.class);

        // Test true
        FastByteWriter trueWriter = new FastByteWriter();
        trueWriter.reset(10);
        trueWriter.writeBoolean(true);
        BooleanObject trueResult = deserializer.deserialize(trueWriter.toByteArray());
        assertTrue(trueResult.value);

        // Test false
        FastByteWriter falseWriter = new FastByteWriter();
        falseWriter.reset(10);
        falseWriter.writeBoolean(false);
        BooleanObject falseResult = deserializer.deserialize(falseWriter.toByteArray());
        assertFalse(falseResult.value);
    }

    @Test
    void deserialize_charType_producesCorrectValue() throws Throwable {
        BinaryDeserializer<CharObject> deserializer = new BinaryDeserializer<>(CharObject.class);

        FastByteWriter writer = new FastByteWriter();
        writer.reset(10);
        writer.writeShort((short) 'X'); // char is written as short
        byte[] data = writer.toByteArray();

        CharObject result = deserializer.deserialize(data);

        assertNotNull(result);
        assertEquals('X', result.value);
    }

    @Test
    void deserialize_charType_extremeValues_producesCorrectValues() throws Throwable {
        BinaryDeserializer<CharObject> deserializer = new BinaryDeserializer<>(CharObject.class);

        // Test MIN_VALUE
        FastByteWriter minWriter = new FastByteWriter();
        minWriter.reset(10);
        minWriter.writeShort((short) Character.MIN_VALUE);
        CharObject minResult = deserializer.deserialize(minWriter.toByteArray());
        assertEquals(Character.MIN_VALUE, minResult.value);

        // Test MAX_VALUE
        FastByteWriter maxWriter = new FastByteWriter();
        maxWriter.reset(10);
        maxWriter.writeShort((short) Character.MAX_VALUE);
        CharObject maxResult = deserializer.deserialize(maxWriter.toByteArray());
        assertEquals(Character.MAX_VALUE, maxResult.value);
    }


    @Test
    void deserialize_allPrimitiveTypes_producesCorrectValues() throws Throwable {
        BinaryDeserializer<AllPrimitivesObject> deserializer = new BinaryDeserializer<>(AllPrimitivesObject.class);

        // Create test data
        FastByteWriter writer = new FastByteWriter();
        writer.reset(50);
        writer.writeByte(42);
        writer.writeShort((short) 1000);
        writer.writeInt(123456);
        writer.writeLong(9876543210L);
        writer.writeFloat(3.14f);
        writer.writeDouble(2.718281828);
        writer.writeBoolean(true);
        writer.writeShort((short) 'Z');
        byte[] data = writer.toByteArray();

        AllPrimitivesObject result = deserializer.deserialize(data);

        assertNotNull(result);
        assertEquals(42, result.byteVal);
        assertEquals(1000, result.shortVal);
        assertEquals(123456, result.intVal);
        assertEquals(9876543210L, result.longVal);
        assertEquals(3.14f, result.floatVal, 0.001f);
        assertEquals(2.718281828, result.doubleVal, 0.000001);
        assertTrue(result.boolVal);
        assertEquals('Z', result.charVal);
    }

    @Test
    void deserialize_zeroValues_producesCorrectValues() throws Throwable {
        BinaryDeserializer<AllPrimitivesObject> deserializer = new BinaryDeserializer<>(AllPrimitivesObject.class);

        // Create test data with all zeros
        FastByteWriter writer = new FastByteWriter();
        writer.reset(50);
        writer.writeByte(0);
        writer.writeShort((short) 0);
        writer.writeInt(0);
        writer.writeLong(0L);
        writer.writeFloat(0.0f);
        writer.writeDouble(0.0);
        writer.writeBoolean(false);
        writer.writeShort((short) '\0');
        byte[] data = writer.toByteArray();

        AllPrimitivesObject result = deserializer.deserialize(data);

        assertNotNull(result);
        assertEquals(0, result.byteVal);
        assertEquals(0, result.shortVal);
        assertEquals(0, result.intVal);
        assertEquals(0L, result.longVal);
        assertEquals(0.0f, result.floatVal, 0.0f);
        assertEquals(0.0, result.doubleVal, 0.0);
        assertFalse(result.boolVal);
        assertEquals('\0', result.charVal);
    }

    @Test
    void deserialize_negativeValues_producesCorrectValues() throws Throwable {
        BinaryDeserializer<AllPrimitivesObject> deserializer = new BinaryDeserializer<>(AllPrimitivesObject.class);

        // Create test data with negative values
        FastByteWriter writer = new FastByteWriter();
        writer.reset(50);
        writer.writeByte(-42);
        writer.writeShort((short) -1000);
        writer.writeInt(-123456);
        writer.writeLong(-9876543210L);
        writer.writeFloat(-3.14f);
        writer.writeDouble(-2.718281828);
        writer.writeBoolean(false);
        writer.writeShort((short) 'A');
        byte[] data = writer.toByteArray();

        AllPrimitivesObject result = deserializer.deserialize(data);

        assertNotNull(result);
        assertEquals(-42, result.byteVal);
        assertEquals(-1000, result.shortVal);
        assertEquals(-123456, result.intVal);
        assertEquals(-9876543210L, result.longVal);
        assertEquals(-3.14f, result.floatVal, 0.001f);
        assertEquals(-2.718281828, result.doubleVal, 0.000001);
    }

    @Test
    void deserialize_nullBytes_returnsNull() throws Throwable {
        BinaryDeserializer<IntObject> deserializer = new BinaryDeserializer<>(IntObject.class);

        IntObject result = deserializer.deserialize(null);
        assertNull(result);
    }

    @Test
    void deserialize_emptyBytes_returnsNull() throws Throwable {
        BinaryDeserializer<IntObject> deserializer = new BinaryDeserializer<>(IntObject.class);

        IntObject result = deserializer.deserialize(new byte[0]);
        assertNull(result);
    }

    @Test
    void deserialize_multipleTimes_producesConsistentResults() throws Throwable {
        BinaryDeserializer<IntObject> deserializer = new BinaryDeserializer<>(IntObject.class);

        FastByteWriter writer = new FastByteWriter();
        writer.reset(10);
        writer.writeInt(999);
        byte[] data = writer.toByteArray();

        IntObject result1 = deserializer.deserialize(data);
        IntObject result2 = deserializer.deserialize(data);
        IntObject result3 = deserializer.deserialize(data);

        assertEquals(999, result1.value);
        assertEquals(999, result2.value);
        assertEquals(999, result3.value);
    }
}

