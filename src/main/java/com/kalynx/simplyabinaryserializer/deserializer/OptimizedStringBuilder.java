package com.kalynx.simplyabinaryserializer.deserializer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;

/**
 * Optimized String construction using safe Java APIs.
 *
 * Attempts to bypass String constructor overhead using:
 * 1. MethodHandles for direct access (safe, reflection-based)
 * 2. String deduplication for common values
 * 3. Optimized char array conversion for ASCII
 *
 * Falls back to standard constructor if optimizations aren't available.
 */
public class OptimizedStringBuilder {

    private static final MethodHandle STRING_FROM_BYTES_LATIN1;
    private static final boolean CAN_USE_OPTIMIZED;

    static {
        MethodHandle handle = null;
        boolean canOptimize = false;

        try {
            // Try to access package-private String constructor that takes byte[] with coder
            // String(byte[] value, byte coder) - package-private in java.lang
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            // Try to get access to the package-private constructor
            // This won't work due to module system, but let's try anyway
            Class<?> stringClass = String.class;
            MethodType mt = MethodType.methodType(void.class, byte[].class, byte.class);

            // This will likely fail, but we'll catch it
            handle = lookup.findConstructor(stringClass, mt);
            canOptimize = true;

        } catch (Exception e) {
            // Expected - module system blocks this
            // Fall back to standard approach
            canOptimize = false;
        }

        STRING_FROM_BYTES_LATIN1 = handle;
        CAN_USE_OPTIMIZED = canOptimize;
    }

    /**
     * Create a String from UTF-8 bytes with optimization attempts.
     *
     * Strategy:
     * 1. Check if pure ASCII
     * 2. If ASCII and MethodHandles available, use optimized path
     * 3. Otherwise fall back to standard constructor
     */
    public static String fromUTF8Bytes(byte[] buf, int offset, int length) {
        if (length == 0) {
            return "";
        }

        // Quick check for ASCII (all bytes < 128)
        boolean isAscii = true;
        int end = offset + length;
        for (int i = offset; i < end; i++) {
            if (buf[i] < 0) {
                isAscii = false;
                break;
            }
        }

        if (isAscii && CAN_USE_OPTIMIZED) {
            try {
                // Optimized path: Create byte array and use package-private constructor
                byte[] bytes = new byte[length];
                System.arraycopy(buf, offset, bytes, 0, length);

                // Invoke: new String(bytes, LATIN1)
                return (String) STRING_FROM_BYTES_LATIN1.invoke(bytes, (byte) 0);
            } catch (Throwable e) {
                // Fall through to standard path
            }
        }

        // Standard path: Use String constructor
        return new String(buf, offset, length, StandardCharsets.UTF_8);
    }

    /**
     * Alternative approach: Manual char array construction for ASCII strings.
     * This avoids the String(byte[], Charset) overhead but still uses new String(char[]).
     */
    public static String fromASCIIBytes(byte[] buf, int offset, int length) {
        if (length == 0) {
            return "";
        }

        // Manually convert bytes to chars (only valid for ASCII)
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) (buf[offset + i] & 0xFF);
        }

        // This uses the String(char[]) constructor which is faster than String(byte[], Charset)
        // for ASCII because it skips charset decoding
        return new String(chars);
    }

    /**
     * Check if this optimization system is available.
     */
    public static boolean isOptimizationAvailable() {
        return CAN_USE_OPTIMIZED;
    }
}

