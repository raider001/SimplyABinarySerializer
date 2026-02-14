package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * Test to measure the overhead of String construction vs raw operations.
 * This helps us understand why String deserialization is slower than primitives.
 */
public class StringConstructorOverheadTest {

    private static final int WARMUP = 100_000;
    private static final int ITERATIONS = 1_000_000;

    @Test
    public void compareStringVsIntegerOverhead() {
        System.out.println("\n=== Comparing String vs Integer Deserialization Overhead ===\n");

        // Test data
        byte[] intData = new byte[4];
        intData[0] = 0;
        intData[1] = 0;
        intData[2] = 0;
        intData[3] = 42;

        String testString = "String_12345_0";
        byte[] stringBytes = testString.getBytes(StandardCharsets.UTF_8);
        byte[] stringData = new byte[4 + stringBytes.length];
        // Write length prefix
        stringData[0] = (byte)((stringBytes.length >> 24) & 0xFF);
        stringData[1] = (byte)((stringBytes.length >> 16) & 0xFF);
        stringData[2] = (byte)((stringBytes.length >> 8) & 0xFF);
        stringData[3] = (byte)(stringBytes.length & 0xFF);
        System.arraycopy(stringBytes, 0, stringData, 4, stringBytes.length);

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            readInt(intData, 0);
            readString(stringData, 0);
        }

        // Benchmark Integer
        long startInt = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            readInt(intData, 0);
        }
        long elapsedInt = System.nanoTime() - startInt;
        double nsPerInt = (double) elapsedInt / ITERATIONS;

        // Benchmark String
        long startString = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            readString(stringData, 0);
        }
        long elapsedString = System.nanoTime() - startString;
        double nsPerString = (double) elapsedString / ITERATIONS;

        System.out.printf("Integer read: %.2f ns/op\n", nsPerInt);
        System.out.printf("String read:  %.2f ns/op\n", nsPerString);
        System.out.printf("String is %.2fx slower\n", nsPerString / nsPerInt);

        System.out.println("\n=== Breaking Down String Construction ===\n");

        // Test 1: Just reading the bytes (no String construction)
        long startByteRead = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            readBytesOnly(stringData, 0);
        }
        long elapsedByteRead = System.nanoTime() - startByteRead;
        double nsPerByteRead = (double) elapsedByteRead / ITERATIONS;

        // Test 2: Reading + String construction
        long startStringFull = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            readString(stringData, 0);
        }
        long elapsedStringFull = System.nanoTime() - startStringFull;
        double nsPerStringFull = (double) elapsedStringFull / ITERATIONS;

        System.out.printf("Just reading bytes:        %.2f ns/op\n", nsPerByteRead);
        System.out.printf("Reading + String creation: %.2f ns/op\n", nsPerStringFull);
        System.out.printf("String constructor overhead: %.2f ns (%.1f%% of total)\n",
            nsPerStringFull - nsPerByteRead,
            ((nsPerStringFull - nsPerByteRead) / nsPerStringFull) * 100);

        System.out.println("\n=== Analysis ===\n");
        System.out.println("The String constructor (UTF-8 decoding + char array creation + String object)");
        System.out.println("is the primary bottleneck, not the raw byte reading.");
        System.out.println("\nFury likely uses:");
        System.out.println("1. Unsafe/VarHandle for zero-copy string creation");
        System.out.println("2. String deduplication/interning");
        System.out.println("3. Pre-compiled codegen specific to each class");
    }

    private int readInt(byte[] buf, int pos) {
        return ((buf[pos] & 0xFF) << 24) |
               ((buf[pos + 1] & 0xFF) << 16) |
               ((buf[pos + 2] & 0xFF) << 8) |
               (buf[pos + 3] & 0xFF);
    }

    private String readString(byte[] buf, int pos) {
        int len = readInt(buf, pos);
        return new String(buf, pos + 4, len, StandardCharsets.UTF_8);
    }

    private int readBytesOnly(byte[] buf, int pos) {
        // Just read the length and advance position (simulate reading bytes without String creation)
        int len = readInt(buf, pos);
        // Simulate touching the bytes
        int checksum = 0;
        for (int i = 0; i < len; i++) {
            checksum += buf[pos + 4 + i];
        }
        return checksum;
    }
}

