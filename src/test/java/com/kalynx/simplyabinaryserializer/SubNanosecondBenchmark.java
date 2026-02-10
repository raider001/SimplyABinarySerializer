package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Ultra-precise benchmark for sub-nanosecond measurement.
 * Uses high-iteration counts and statistical analysis to measure accurately.
 */
public class SubNanosecondBenchmark {

    private static final int WARMUP_ITERATIONS = 10_000_000;
    private static final int BENCHMARK_ITERATIONS = 100_000_000; // 100M iterations for precision
    private static final int RUNS = 10;

    private TypedSerializer<SimpleObject> typedSerializer;
    private UltraFastSerializer<SimpleObject> ultraSerializer;
    private SimpleObject testObject;

    @BeforeEach
    public void setup() {
        typedSerializer = new TypedSerializer<>(SimpleObject.class);
        ultraSerializer = new UltraFastSerializer<>(SimpleObject.class);
        testObject = createSimpleObject(42);
    }

    @Test
    public void runSubNanosecondBenchmark() throws Throwable {
        System.out.println("\n" + "=".repeat(120));
        System.out.println("SUB-NANOSECOND PERFORMANCE BENCHMARK");
        System.out.println("=".repeat(120));
        System.out.println("Target: Sub-nanosecond serialization performance");
        System.out.println("Method: 100M iterations × 10 runs for statistical precision");
        System.out.println("=".repeat(120));
        System.out.println();

        // Warmup
        System.out.println("Warming up JVM (" + (WARMUP_ITERATIONS / 1_000_000) + "M iterations)...");
        warmup();
        System.out.println("Warmup complete. Starting precision benchmark...\n");

        // Run benchmarks
        double[] typedResults = new double[RUNS];
        double[] ultraResults = new double[RUNS];

        for (int run = 0; run < RUNS; run++) {
            System.out.printf("Run %2d/%d: ", run + 1, RUNS);

            // TypedSerializer
            long typedStart = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                typedSerializer.serialize(testObject);
            }
            long typedTime = System.nanoTime() - typedStart;
            typedResults[run] = (double) typedTime / BENCHMARK_ITERATIONS;

            // UltraFastSerializer
            long ultraStart = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                ultraSerializer.serialize(testObject);
            }
            long ultraTime = System.nanoTime() - ultraStart;
            ultraResults[run] = (double) ultraTime / BENCHMARK_ITERATIONS;

            System.out.printf("Typed=%.3f ns  Ultra=%.3f ns  (%.2fx faster)\n",
                typedResults[run], ultraResults[run], typedResults[run] / ultraResults[run]);

            // Force GC between runs
            if (run % 3 == 0) {
                System.gc();
                Thread.sleep(100);
            }
        }

        // Statistical analysis
        System.out.println("\n" + "=".repeat(120));
        System.out.println("STATISTICAL ANALYSIS");
        System.out.println("=".repeat(120));

        Stats typedStats = calculateStats(typedResults);
        Stats ultraStats = calculateStats(ultraResults);

        System.out.println();
        System.out.println("TypedSerializer:");
        System.out.printf("  Mean:   %.6f ns\n", typedStats.mean);
        System.out.printf("  Median: %.6f ns\n", typedStats.median);
        System.out.printf("  StdDev: %.6f ns\n", typedStats.stdDev);
        System.out.printf("  Min:    %.6f ns\n", typedStats.min);
        System.out.printf("  Max:    %.6f ns\n", typedStats.max);

        System.out.println();
        System.out.println("UltraFastSerializer:");
        System.out.printf("  Mean:   %.6f ns\n", ultraStats.mean);
        System.out.printf("  Median: %.6f ns\n", ultraStats.median);
        System.out.printf("  StdDev: %.6f ns\n", ultraStats.stdDev);
        System.out.printf("  Min:    %.6f ns\n", ultraStats.min);
        System.out.printf("  Max:    %.6f ns\n", ultraStats.max);

        System.out.println();
        System.out.println("=".repeat(120));
        System.out.printf("SPEEDUP: UltraFastSerializer is %.2fx faster (%.3f ns vs %.3f ns)\n",
            typedStats.mean / ultraStats.mean, ultraStats.mean, typedStats.mean);
        System.out.println("=".repeat(120));

        if (ultraStats.mean < 1.0) {
            System.out.println();
            System.out.println("🎉 SUB-NANOSECOND ACHIEVED! 🎉");
            System.out.printf("Performance: %.3f ns (%.1f%% of 1 nanosecond)\n",
                ultraStats.mean, ultraStats.mean * 100);
            System.out.printf("Throughput: %.2f BILLION operations/second\n",
                1_000_000_000.0 / ultraStats.mean / 1_000_000_000.0);
        } else {
            System.out.println();
            System.out.printf("Current: %.3f ns (%.1f%% of target)\n",
                ultraStats.mean, (1.0 / ultraStats.mean) * 100);
            System.out.printf("Needs: %.2fx improvement to reach sub-nanosecond\n",
                ultraStats.mean);
        }

        // Throughput analysis
        System.out.println();
        System.out.println("=".repeat(120));
        System.out.println("THROUGHPUT ANALYSIS");
        System.out.println("=".repeat(120));
        System.out.printf("TypedSerializer:      %,18.0f ops/sec\n",
            1_000_000_000.0 / typedStats.mean);
        System.out.printf("UltraFastSerializer:  %,18.0f ops/sec\n",
            1_000_000_000.0 / ultraStats.mean);
        System.out.printf("Improvement:          %,18.0f ops/sec (%.2fx faster)\n",
            (1_000_000_000.0 / ultraStats.mean) - (1_000_000_000.0 / typedStats.mean),
            typedStats.mean / ultraStats.mean);
        System.out.println("=".repeat(120));
    }

    private void warmup() throws Throwable {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            typedSerializer.serialize(testObject);
            ultraSerializer.serialize(testObject);
        }
    }

    private Stats calculateStats(double[] values) {
        Stats stats = new Stats();

        // Mean
        double sum = 0;
        for (double v : values) sum += v;
        stats.mean = sum / values.length;

        // Min/Max
        stats.min = Double.MAX_VALUE;
        stats.max = Double.MIN_VALUE;
        for (double v : values) {
            if (v < stats.min) stats.min = v;
            if (v > stats.max) stats.max = v;
        }

        // Median
        double[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        stats.median = sorted.length % 2 == 0 ?
            (sorted[sorted.length / 2 - 1] + sorted[sorted.length / 2]) / 2 :
            sorted[sorted.length / 2];

        // StdDev
        double variance = 0;
        for (double v : values) {
            variance += Math.pow(v - stats.mean, 2);
        }
        stats.stdDev = Math.sqrt(variance / values.length);

        return stats;
    }

    private SimpleObject createSimpleObject(int seed) {
        SimpleObject obj = new SimpleObject();
        obj.id = seed;
        obj.name = "Test" + seed;
        obj.active = seed % 2 == 0;
        obj.doubleValue = 3.14 + seed;
        obj.floatValue = 2.71f + seed;
        obj.longValue = 123456789L + seed;
        obj.shortValue = (short) (42 + seed % 100);
        return obj;
    }

    public static class SimpleObject {
        public int id;
        public String name;
        public boolean active;
        public double doubleValue;
        public float floatValue;
        public long longValue;
        public short shortValue;

        public SimpleObject() {}
    }

    private static class Stats {
        double mean;
        double median;
        double stdDev;
        double min;
        double max;
    }
}



