package com.kalynx.simplyabinaryserializer.deserializer;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * Test various safe approaches to optimizing String construction.
 * Compares against baseline to see if any safe method can improve performance.
 */
public class OptimizedStringConstructionTest {

    private static final int WARMUP = 100_000;
    private static final int ITERATIONS = 1_000_000;

    @Test
    public void compareStringConstructionApproaches() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TESTING SAFE STRING CONSTRUCTION OPTIMIZATIONS");
        System.out.println("=".repeat(80));

        // Test string: typical ASCII string like "String_12345_0"
        String testString = "String_12345_0";
        byte[] stringBytes = testString.getBytes(StandardCharsets.UTF_8);

        System.out.println("\nTest string: \"" + testString + "\" (" + stringBytes.length + " bytes)");
        System.out.println("OptimizedStringBuilder available: " + OptimizedStringBuilder.isOptimizationAvailable());

        // Approach 1: Baseline - Standard String constructor
        warmup(() -> new String(stringBytes, 0, stringBytes.length, StandardCharsets.UTF_8));
        long baseline = benchmark("Standard String(byte[], Charset)", () ->
            new String(stringBytes, 0, stringBytes.length, StandardCharsets.UTF_8)
        );

        // Approach 2: OptimizedStringBuilder.fromUTF8Bytes
        warmup(() -> OptimizedStringBuilder.fromUTF8Bytes(stringBytes, 0, stringBytes.length));
        long optimized = benchmark("OptimizedStringBuilder.fromUTF8Bytes", () ->
            OptimizedStringBuilder.fromUTF8Bytes(stringBytes, 0, stringBytes.length)
        );

        // Approach 3: Manual char array construction
        warmup(() -> OptimizedStringBuilder.fromASCIIBytes(stringBytes, 0, stringBytes.length));
        long charArray = benchmark("Manual char[] construction", () ->
            OptimizedStringBuilder.fromASCIIBytes(stringBytes, 0, stringBytes.length)
        );

        // Approach 4: String(char[]) with inline conversion
        warmup(() -> stringFromCharsInline(stringBytes, 0, stringBytes.length));
        long inlineChars = benchmark("Inline char[] conversion", () ->
            stringFromCharsInline(stringBytes, 0, stringBytes.length)
        );

        // Approach 5: ISO-8859-1 charset for ASCII
        warmup(() -> new String(stringBytes, 0, stringBytes.length, StandardCharsets.ISO_8859_1));
        long latin1 = benchmark("String(byte[], ISO_8859_1)", () ->
            new String(stringBytes, 0, stringBytes.length, StandardCharsets.ISO_8859_1)
        );

        System.out.println("\n" + "=".repeat(80));
        System.out.println("RESULTS SUMMARY");
        System.out.println("=".repeat(80));

        double baselineNs = (double) baseline / ITERATIONS;
        System.out.printf("\nBaseline: %.2f ns/op\n", baselineNs);

        printComparison("OptimizedStringBuilder", optimized, baseline);
        printComparison("Manual char[]", charArray, baseline);
        printComparison("Inline char[]", inlineChars, baseline);
        printComparison("ISO-8859-1", latin1, baseline);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("ANALYSIS");
        System.out.println("=".repeat(80));

        if (optimized < baseline * 0.95) {
            System.out.println("\n✅ OptimizedStringBuilder is significantly faster!");
            System.out.println("   Recommendation: Use OptimizedStringBuilder.fromUTF8Bytes()");
        } else if (charArray < baseline * 0.95) {
            System.out.println("\n✅ Manual char[] construction is significantly faster!");
            System.out.println("   Recommendation: Use manual char[] for ASCII strings");
        } else if (latin1 < baseline * 0.95) {
            System.out.println("\n✅ ISO-8859-1 charset is significantly faster!");
            System.out.println("   Recommendation: Use ISO-8859-1 for ASCII strings");
        } else {
            System.out.println("\n❌ No safe optimization beats the standard String constructor");
            System.out.println("   The JVM's UTF-8 decoder is already highly optimized");
            System.out.println("   Recommendation: Keep current implementation");
            System.out.println("\n   Why manual optimizations fail:");
            System.out.println("   1. JVM's UTF-8 decoder uses SIMD instructions");
            System.out.println("   2. Intrinsics for String operations are highly tuned");
            System.out.println("   3. Manual loops can't match native code performance");
            System.out.println("   4. Extra allocations (char[]) add overhead");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("CONCLUSION");
        System.out.println("=".repeat(80));
        System.out.println("\nTo significantly beat the standard String constructor, you would need:");
        System.out.println("1. sun.misc.Unsafe (deprecated, blocked by security)");
        System.out.println("2. JNI native code (complex, platform-specific)");
        System.out.println("3. JVM intrinsics (requires JVM modification)");
        System.out.println("\nAll safe Java approaches are either equivalent or slower.");
        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    public void testWithDifferentStringSizes() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("STRING SIZE IMPACT ON OPTIMIZATION");
        System.out.println("=".repeat(80));

        String[] testStrings = {
            "ab",                                    // 2 chars
            "hello",                                 // 5 chars
            "String_12345_0",                        // 15 chars
            "A longer string with more characters",  // 37 chars
            "This is a very long string that contains many more characters and will test performance with larger data sizes" // 114 chars
        };

        for (String test : testStrings) {
            byte[] bytes = test.getBytes(StandardCharsets.UTF_8);

            warmup(() -> new String(bytes, 0, bytes.length, StandardCharsets.UTF_8));
            long baseline = benchmark("", () ->
                new String(bytes, 0, bytes.length, StandardCharsets.UTF_8)
            );

            warmup(() -> OptimizedStringBuilder.fromASCIIBytes(bytes, 0, bytes.length));
            long charArray = benchmark("", () ->
                OptimizedStringBuilder.fromASCIIBytes(bytes, 0, bytes.length)
            );

            double baselineNs = (double) baseline / ITERATIONS;
            double charArrayNs = (double) charArray / ITERATIONS;
            double improvement = ((baseline - charArray) / (double) baseline) * 100;

            System.out.printf("\n%3d chars: Baseline %.2f ns | char[] %.2f ns | ",
                bytes.length, baselineNs, charArrayNs);

            if (improvement > 5) {
                System.out.printf("✅ %.1f%% faster", improvement);
            } else if (improvement < -5) {
                System.out.printf("❌ %.1f%% slower", -improvement);
            } else {
                System.out.printf("≈ Similar (%.1f%% diff)", improvement);
            }
        }

        System.out.println("\n\n" + "=".repeat(80));
    }

    private String stringFromCharsInline(byte[] buf, int offset, int length) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) (buf[offset + i] & 0xFF);
        }
        return new String(chars);
    }

    private void warmup(Runnable task) {
        for (int i = 0; i < WARMUP; i++) {
            task.run();
        }
    }

    private long benchmark(String name, Runnable task) {
        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            task.run();
        }
        long elapsed = System.nanoTime() - start;

        if (!name.isEmpty()) {
            double nsPerOp = (double) elapsed / ITERATIONS;
            System.out.printf("%-35s: %8.2f ns/op\n", name, nsPerOp);
        }

        return elapsed;
    }

    private void printComparison(String name, long time, long baseline) {
        double improvement = ((baseline - time) / (double) baseline) * 100;
        String symbol;
        if (improvement > 10) {
            symbol = "✅ FASTER";
        } else if (improvement > 5) {
            symbol = "✓ Slightly faster";
        } else if (improvement < -5) {
            symbol = "❌ SLOWER";
        } else {
            symbol = "≈ Similar";
        }

        System.out.printf("%-25s: %s (%.1f%%)\n", name, symbol, improvement);
    }
}


