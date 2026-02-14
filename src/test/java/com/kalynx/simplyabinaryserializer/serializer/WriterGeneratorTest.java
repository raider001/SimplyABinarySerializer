package com.kalynx.simplyabinaryserializer.serializer;

import com.kalynx.simplyabinaryserializer.deserializer.FastByteReader;
import com.kalynx.simplyabinaryserializer.testutil.TestDataClasses.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WriterGenerator - validates primitive type serialization.
 */
class WriterGeneratorTest {

    @Test
    void generatePrimitiveWriter_singleInt_serializesCorrectly() throws Throwable {
        WriterGenerator generator = new WriterGenerator();
        SingleIntObject obj = new SingleIntObject(100);

        var fields = SingleIntObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var writer = generator.generatePrimitiveWriter(SingleIntObject.class, fields);
        FastByteWriter byteWriter = new FastByteWriter();
        byteWriter.reset(100);

        writer.write(byteWriter, obj);
        byte[] result = byteWriter.toByteArray();

        assertNotNull(result);
        assertEquals(4, result.length);

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertEquals(100, reader.readInt());
    }

    @Test
    void generatePrimitiveWriter_multipleInts_serializesInOrder() throws Throwable {
        WriterGenerator generator = new WriterGenerator();
        MultipleIntsObject obj = new MultipleIntsObject(1, 2, 3);

        var fields = MultipleIntsObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var writer = generator.generatePrimitiveWriter(MultipleIntsObject.class, fields);
        FastByteWriter byteWriter = new FastByteWriter();
        byteWriter.reset(100);

        writer.write(byteWriter, obj);
        byte[] result = byteWriter.toByteArray();

        assertEquals(12, result.length); // 3 ints = 12 bytes

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertEquals(1, reader.readInt());
        assertEquals(2, reader.readInt());
        assertEquals(3, reader.readInt());
    }

    @Test
    void generatePrimitiveWriter_longValues_serializesCorrectly() throws Throwable {
        WriterGenerator generator = new WriterGenerator();
        LongValuesObject obj = new LongValuesObject(Long.MIN_VALUE, Long.MAX_VALUE, 0L);

        var fields = LongValuesObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var writer = generator.generatePrimitiveWriter(LongValuesObject.class, fields);
        FastByteWriter byteWriter = new FastByteWriter();
        byteWriter.reset(100);

        writer.write(byteWriter, obj);
        byte[] result = byteWriter.toByteArray();

        assertEquals(24, result.length); // 3 longs = 24 bytes

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertEquals(Long.MIN_VALUE, reader.readLong());
        assertEquals(Long.MAX_VALUE, reader.readLong());
        assertEquals(0L, reader.readLong());
    }

    @Test
    void generatePrimitiveWriter_floatValues_serializesCorrectly() throws Throwable {
        WriterGenerator generator = new WriterGenerator();
        FloatValuesObject obj = new FloatValuesObject(Float.MAX_VALUE, Double.MIN_VALUE);

        var fields = FloatValuesObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var writer = generator.generatePrimitiveWriter(FloatValuesObject.class, fields);
        FastByteWriter byteWriter = new FastByteWriter();
        byteWriter.reset(100);

        writer.write(byteWriter, obj);
        byte[] result = byteWriter.toByteArray();

        assertEquals(12, result.length); // float(4) + double(8) = 12 bytes

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertEquals(Float.MAX_VALUE, reader.readFloat(), 0.001f);
        assertEquals(Double.MIN_VALUE, reader.readDouble(), 0.001);
    }

    @Test
    void generatePrimitiveWriter_booleanValues_serializesCorrectly() throws Throwable {
        WriterGenerator generator = new WriterGenerator();
        BooleanValuesObject obj = new BooleanValuesObject(true, false);

        var fields = BooleanValuesObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var writer = generator.generatePrimitiveWriter(BooleanValuesObject.class, fields);
        FastByteWriter byteWriter = new FastByteWriter();
        byteWriter.reset(100);

        writer.write(byteWriter, obj);
        byte[] result = byteWriter.toByteArray();

        assertEquals(2, result.length); // 2 booleans = 2 bytes

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertTrue(reader.readBoolean());
        assertFalse(reader.readBoolean());
    }

    @Test
    void generatePrimitiveWriter_allPrimitives_serializesCorrectly() throws Throwable {
        WriterGenerator generator = new WriterGenerator();
        AllPrimitivesObject obj = new AllPrimitivesObject(
            (byte) 42,
            (short) 1000,
            123456,
            9876543210L,
            3.14f,
            2.718281828,
            true,
            'A'
        );

        var fields = AllPrimitivesObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        var writer = generator.generatePrimitiveWriter(AllPrimitivesObject.class, fields);
        FastByteWriter byteWriter = new FastByteWriter();
        byteWriter.reset(100);

        writer.write(byteWriter, obj);
        byte[] result = byteWriter.toByteArray();

        // byte(1) + short(2) + int(4) + long(8) + float(4) + double(8) + boolean(1) + char(2) = 30 bytes
        assertEquals(30, result.length);

        FastByteReader reader = new FastByteReader();
        reader.setData(result);
        assertEquals(42, reader.readByte());
        assertEquals(1000, reader.readShort());
        assertEquals(123456, reader.readInt());
        assertEquals(9876543210L, reader.readLong());
        assertEquals(3.14f, reader.readFloat(), 0.001f);
        assertEquals(2.718281828, reader.readDouble(), 0.000001);
        assertTrue(reader.readBoolean());
        assertEquals('A', (char) reader.readShort()); // char is written as short
    }

    @Test
    void estimatePrimitiveSize_allPrimitives_returnsCorrectSize() throws Throwable {
        WriterGenerator generator = new WriterGenerator();

        var fields = AllPrimitivesObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        int estimatedSize = generator.estimatePrimitiveSize(fields);

        // byte(1) + short(2) + int(4) + long(8) + float(4) + double(8) + boolean(1) + char(2) = 30
        assertEquals(30, estimatedSize);
    }

    @Test
    void estimatePrimitiveSize_singleInt_returnsFourBytes() throws Throwable {
        WriterGenerator generator = new WriterGenerator();

        var fields = SingleIntObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        int estimatedSize = generator.estimatePrimitiveSize(fields);
        assertEquals(4, estimatedSize);
    }


    @Test
    void generatePrimitiveWriter_nonPrimitiveField_throwsException() {
        WriterGenerator generator = new WriterGenerator();

        var fields = MixedFieldsObject.class.getDeclaredFields();
        for (var field : fields) {
            field.setAccessible(true);
        }

        assertThrows(IllegalArgumentException.class, () -> {
            generator.generatePrimitiveWriter(MixedFieldsObject.class, fields);
        });
    }
}

