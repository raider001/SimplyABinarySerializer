package com.kalynx.simplyabinaryserializer.deserializer;

import com.kalynx.simplyabinaryserializer.serializer.FastByteWriter;
import com.kalynx.simplyabinaryserializer.testutil.TestDataClasses.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReaderGenerator - validates primitive type deserialization.
 */
class ReaderGeneratorTest {


    @Test
    void generatePrimitiveReader_singleInt_deserializesCorrectly() throws Throwable {
        ReaderGenerator generator = new ReaderGenerator();

        var fields = SingleIntObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var reader = generator.generatePrimitiveReader(SingleIntObject.class, fields);

        // Create test data
        FastByteWriter writer = new FastByteWriter();
        writer.reset(100);
        writer.writeInt(42);
        byte[] data = writer.toByteArray();

        // Deserialize
        FastByteReader byteReader = new FastByteReader();
        byteReader.setData(data);
        SingleIntObject result = reader.read(byteReader);

        assertNotNull(result);
        assertEquals(42, result.value);
    }

    @Test
    void generatePrimitiveReader_multipleInts_deserializesInOrder() throws Throwable {
        ReaderGenerator generator = new ReaderGenerator();

        var fields = MultipleIntsObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var reader = generator.generatePrimitiveReader(MultipleIntsObject.class, fields);

        // Create test data
        FastByteWriter writer = new FastByteWriter();
        writer.reset(100);
        writer.writeInt(1);
        writer.writeInt(2);
        writer.writeInt(3);
        byte[] data = writer.toByteArray();

        // Deserialize
        FastByteReader byteReader = new FastByteReader();
        byteReader.setData(data);
        MultipleIntsObject result = reader.read(byteReader);

        assertNotNull(result);
        assertEquals(1, result.first);
        assertEquals(2, result.second);
        assertEquals(3, result.third);
    }

    @Test
    void generatePrimitiveReader_longValues_deserializesCorrectly() throws Throwable {
        ReaderGenerator generator = new ReaderGenerator();

        var fields = LongValuesObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var reader = generator.generatePrimitiveReader(LongValuesObject.class, fields);

        // Create test data
        FastByteWriter writer = new FastByteWriter();
        writer.reset(100);
        writer.writeLong(Long.MIN_VALUE);
        writer.writeLong(Long.MAX_VALUE);
        writer.writeLong(0L);
        byte[] data = writer.toByteArray();

        // Deserialize
        FastByteReader byteReader = new FastByteReader();
        byteReader.setData(data);
        LongValuesObject result = reader.read(byteReader);

        assertNotNull(result);
        assertEquals(Long.MIN_VALUE, result.min);
        assertEquals(Long.MAX_VALUE, result.max);
        assertEquals(0L, result.zero);
    }

    @Test
    void generatePrimitiveReader_floatValues_deserializesCorrectly() throws Throwable {
        ReaderGenerator generator = new ReaderGenerator();

        var fields = FloatValuesObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var reader = generator.generatePrimitiveReader(FloatValuesObject.class, fields);

        // Create test data
        FastByteWriter writer = new FastByteWriter();
        writer.reset(100);
        writer.writeFloat(Float.MAX_VALUE);
        writer.writeDouble(Double.MIN_VALUE);
        byte[] data = writer.toByteArray();

        // Deserialize
        FastByteReader byteReader = new FastByteReader();
        byteReader.setData(data);
        FloatValuesObject result = reader.read(byteReader);

        assertNotNull(result);
        assertEquals(Float.MAX_VALUE, result.floatValue, 0.001f);
        assertEquals(Double.MIN_VALUE, result.doubleValue, 0.001);
    }

    @Test
    void generatePrimitiveReader_booleanValues_deserializesCorrectly() throws Throwable {
        ReaderGenerator generator = new ReaderGenerator();

        var fields = BooleanValuesObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var reader = generator.generatePrimitiveReader(BooleanValuesObject.class, fields);

        // Create test data
        FastByteWriter writer = new FastByteWriter();
        writer.reset(100);
        writer.writeBoolean(true);
        writer.writeBoolean(false);
        byte[] data = writer.toByteArray();

        // Deserialize
        FastByteReader byteReader = new FastByteReader();
        byteReader.setData(data);
        BooleanValuesObject result = reader.read(byteReader);

        assertNotNull(result);
        assertTrue(result.trueValue);
        assertFalse(result.falseValue);
    }

    @Test
    void generatePrimitiveReader_allPrimitives_deserializesCorrectly() throws Throwable {
        ReaderGenerator generator = new ReaderGenerator();

        var fields = AllPrimitivesObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var reader = generator.generatePrimitiveReader(AllPrimitivesObject.class, fields);

        // Create test data with all primitive types
        FastByteWriter writer = new FastByteWriter();
        writer.reset(100);
        writer.writeByte(42);
        writer.writeShort((short) 1000);
        writer.writeInt(123456);
        writer.writeLong(9876543210L);
        writer.writeFloat(3.14f);
        writer.writeDouble(2.718281828);
        writer.writeBoolean(true);
        writer.writeShort((short) 'A'); // char as short
        byte[] data = writer.toByteArray();

        // Deserialize
        FastByteReader byteReader = new FastByteReader();
        byteReader.setData(data);
        AllPrimitivesObject result = reader.read(byteReader);

        assertNotNull(result);
        assertEquals(42, result.byteVal);
        assertEquals(1000, result.shortVal);
        assertEquals(123456, result.intVal);
        assertEquals(9876543210L, result.longVal);
        assertEquals(3.14f, result.floatVal, 0.001f);
        assertEquals(2.718281828, result.doubleVal, 0.000001);
        assertTrue(result.boolVal);
        assertEquals('A', result.charVal);
    }

    @Test
    void generatePrimitiveReader_nonPrimitiveField_throwsException() {
        ReaderGenerator generator = new ReaderGenerator();

        var fields = MixedFieldsObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        assertThrows(IllegalArgumentException.class,
            () -> generator.generatePrimitiveReader(MixedFieldsObject.class, fields));
    }

    @Test
    void generatePrimitiveReader_extremeValues_deserializesCorrectly() throws Throwable {
        ReaderGenerator generator = new ReaderGenerator();

        var fields = AllPrimitivesObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var reader = generator.generatePrimitiveReader(AllPrimitivesObject.class, fields);

        // Create test data with extreme values
        FastByteWriter writer = new FastByteWriter();
        writer.reset(100);
        writer.writeByte(Byte.MAX_VALUE);
        writer.writeShort(Short.MIN_VALUE);
        writer.writeInt(Integer.MAX_VALUE);
        writer.writeLong(Long.MIN_VALUE);
        writer.writeFloat(Float.MIN_VALUE);
        writer.writeDouble(Double.MAX_VALUE);
        writer.writeBoolean(false);
        writer.writeShort((short) Character.MAX_VALUE);
        byte[] data = writer.toByteArray();

        // Deserialize
        FastByteReader byteReader = new FastByteReader();
        byteReader.setData(data);
        AllPrimitivesObject result = reader.read(byteReader);

        assertNotNull(result);
        assertEquals(Byte.MAX_VALUE, result.byteVal);
        assertEquals(Short.MIN_VALUE, result.shortVal);
        assertEquals(Integer.MAX_VALUE, result.intVal);
        assertEquals(Long.MIN_VALUE, result.longVal);
        assertEquals(Float.MIN_VALUE, result.floatVal, 0.0f);
        assertEquals(Double.MAX_VALUE, result.doubleVal, 0.0);
        assertFalse(result.boolVal);
        assertEquals(Character.MAX_VALUE, result.charVal);
    }
}

