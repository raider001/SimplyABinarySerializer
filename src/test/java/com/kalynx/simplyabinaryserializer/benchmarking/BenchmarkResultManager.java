package com.kalynx.simplyabinaryserializer.benchmarking;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class BenchmarkResultManager {

    private final Map<String, Map<String, List<SerializationResult>>> benchmarkResults = new LinkedHashMap<>();

    public BenchmarkResultManager(String[] libraries, String[] objectTypes) {
        for (String library : libraries) {
            Map<String, List<SerializationResult>> objectTypeResults = new LinkedHashMap<>();
            for (String objectType : objectTypes) {
                objectTypeResults.put(objectType, new ArrayList<>());
            }
            benchmarkResults.put(library, objectTypeResults);
        }
    }

    public void recordResult(String library, String objectType, SerializationResult result) {
        benchmarkResults.get(library).get(objectType).add(result);
    }

    public int countTotalRuns() {
        int count = 0;
        for (Map<String, List<SerializationResult>> libraryResults : benchmarkResults.values()) {
            for (List<SerializationResult> results : libraryResults.values()) {
                count += results.size();
            }
        }
        return count;
    }

    public void generateFinalReport(String[] libraries, String[] objectTypes) throws Throwable {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("GENERATING FINAL BENCHMARK REPORT");
        System.out.println("=".repeat(100));

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String mdFilename = "benchmark_comparison_" + timestamp + ".md";

        generateMarkdownReport(mdFilename, libraries, objectTypes);

        System.out.println("\n✅ Markdown report generated: " + mdFilename);
        System.out.println("✅ Location: " + new java.io.File(mdFilename).getAbsolutePath());
        System.out.println("=".repeat(100) + "\n");
    }

    private void generateMarkdownReport(String filename, String[] libraries, String[] objectTypes) throws Throwable {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("# Serialization Benchmark Results");
            writer.println();
            writer.println("**Date:** " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println();
            writer.println("**Total test runs:** " + countTotalRuns());
            writer.println();

            // Generate comparison tables for each object type
            for (String objectType : objectTypes) {
                writer.println("## " + objectType);
                writer.println();

                // Collect stats for all libraries
                Map<String, LibraryStats> statsMap = new LinkedHashMap<>();
                for (String library : libraries) {
                    List<SerializationResult> results = benchmarkResults.get(library).get(objectType);
                    if (!results.isEmpty()) {
                        statsMap.put(library, calculateLibraryStats(results));
                    }
                }

                if (statsMap.isEmpty()) {
                    writer.println("*No results available*");
                    writer.println();
                    continue;
                }

                // Find best values for each metric
                double bestSerialize = statsMap.values().stream().mapToDouble(s -> s.serMean).min().orElse(Double.MAX_VALUE);
                double bestDeserialize = statsMap.values().stream().mapToDouble(s -> s.desMean).min().orElse(Double.MAX_VALUE);
                double bestRoundTrip = statsMap.values().stream().mapToDouble(s -> s.rtMean).min().orElse(Double.MAX_VALUE);
                int bestSize = statsMap.entrySet().stream()
                    .mapToInt(e -> benchmarkResults.get(e.getKey()).get(objectType).get(0).binarySize)
                    .min().orElse(Integer.MAX_VALUE);

                // Sort libraries by overall performance (round-trip time, best to worst)
                List<Map.Entry<String, LibraryStats>> sortedEntries = new ArrayList<>(statsMap.entrySet());
                sortedEntries.sort(Comparator.comparingDouble(e -> e.getValue().rtMean));

                // Generate table
                writer.println("| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |");
                writer.println("|---------|------------------:|--------------------:|-------------------:|--------------------:|");

                for (Map.Entry<String, LibraryStats> entry : sortedEntries) {
                    String library = entry.getKey();
                    LibraryStats stats = entry.getValue();
                    int binarySize = benchmarkResults.get(library).get(objectType).get(0).binarySize;

                    writer.printf("| %s | %s | %s | %s | %s |%n",
                        library,
                        formatValue(stats.serMean, bestSerialize),
                        formatValue(stats.desMean, bestDeserialize),
                        formatValue(stats.rtMean, bestRoundTrip),
                        formatIntValue(binarySize, bestSize));
                }

                writer.println();

                // Add throughput table
                writer.println("### Throughput (ops/sec)");
                writer.println();
                writer.println("| Library | Serialize | Deserialize | Round-Trip |");
                writer.println("|---------|----------:|------------:|-----------:|");

                double bestSerThroughput = statsMap.values().stream().mapToDouble(s -> 1_000_000_000.0 / s.serMean).max().orElse(0);
                double bestDesThroughput = statsMap.values().stream().mapToDouble(s -> 1_000_000_000.0 / s.desMean).max().orElse(0);
                double bestRtThroughput = statsMap.values().stream().mapToDouble(s -> 1_000_000_000.0 / s.rtMean).max().orElse(0);

                for (Map.Entry<String, LibraryStats> entry : sortedEntries) {
                    String library = entry.getKey();
                    LibraryStats stats = entry.getValue();

                    double serThroughput = 1_000_000_000.0 / stats.serMean;
                    double desThroughput = 1_000_000_000.0 / stats.desMean;
                    double rtThroughput = 1_000_000_000.0 / stats.rtMean;

                    writer.printf("| %s | %s | %s | %s |%n",
                        library,
                        formatThroughput(serThroughput, bestSerThroughput),
                        formatThroughput(desThroughput, bestDesThroughput),
                        formatThroughput(rtThroughput, bestRtThroughput));
                }

                writer.println();
            }

            // Summary section
            writer.println("## Summary - Fastest Library by Category");
            writer.println();

            for (String objectType : objectTypes) {
                writer.println("### " + objectType);
                writer.println();

                double fastestSer = Double.MAX_VALUE;
                double fastestDes = Double.MAX_VALUE;
                double fastestRt = Double.MAX_VALUE;
                int smallestSize = Integer.MAX_VALUE;
                String serWinner = "";
                String desWinner = "";
                String rtWinner = "";
                String sizeWinner = "";

                for (String library : libraries) {
                    List<SerializationResult> results = benchmarkResults.get(library).get(objectType);
                    if (!results.isEmpty()) {
                        LibraryStats stats = calculateLibraryStats(results);
                        int size = results.get(0).binarySize;

                        if (stats.serMean < fastestSer) {
                            fastestSer = stats.serMean;
                            serWinner = library;
                        }
                        if (stats.desMean < fastestDes) {
                            fastestDes = stats.desMean;
                            desWinner = library;
                        }
                        if (stats.rtMean < fastestRt) {
                            fastestRt = stats.rtMean;
                            rtWinner = library;
                        }
                        if (size < smallestSize) {
                            smallestSize = size;
                            sizeWinner = library;
                        }
                    }
                }

                writer.println("- **Fastest Serialization:** " + serWinner + " (" + String.format("%.2f", fastestSer) + " ns/op)");
                writer.println("- **Fastest Deserialization:** " + desWinner + " (" + String.format("%.2f", fastestDes) + " ns/op)");
                writer.println("- **Fastest Round-Trip:** " + rtWinner + " (" + String.format("%.2f", fastestRt) + " ns/op)");
                writer.println("- **Smallest Binary Size:** " + sizeWinner + " (" + smallestSize + " bytes)");
                writer.println();
            }
        }
    }

    private String formatValue(double value, double bestValue) {
        String formatted = String.format("%.2f", value);
        if (Math.abs(value - bestValue) < 0.01) {
            return "**" + formatted + "**";
        }
        return formatted;
    }

    private String formatIntValue(int value, int bestValue) {
        String formatted = String.valueOf(value);
        if (value == bestValue) {
            return "**" + formatted + "**";
        }
        return formatted;
    }

    private String formatThroughput(double value, double bestValue) {
        String formatted = String.format("%,.0f", value);
        if (Math.abs(value - bestValue) < 1.0) {
            return "**" + formatted + "**";
        }
        return formatted;
    }

    public void printResult(String library, String objectType, SerializationResult result) {
        double avgSer = result.avgSerializeNs();
        double avgDes = result.avgDeserializeNs();
        double avgRt = avgSer + avgDes;

        System.out.printf("Library: %s\n", library);
        System.out.printf("  Serialize:   %.1f ns/op\n", avgSer);
        System.out.printf("  Deserialize: %.1f ns/op\n", avgDes);
        System.out.printf("  Round-trip:  %.1f ns/op\n", avgRt);
        System.out.printf("  Binary size: %d bytes\n", result.binarySize);
        System.out.printf("  Throughput:  %.0f ops/sec (serialize)  |  %.0f ops/sec (deserialize)\n",
            1_000_000_000.0 / avgSer, 1_000_000_000.0 / avgDes);
    }

    private LibraryStats calculateLibraryStats(List<SerializationResult> results) {
        LibraryStats stats = new LibraryStats();

        List<Double> serTimes = new ArrayList<>();
        List<Double> desTimes = new ArrayList<>();
        List<Double> rtTimes = new ArrayList<>();

        for (SerializationResult result : results) {
            serTimes.add(result.avgSerializeNs());
            desTimes.add(result.avgDeserializeNs());
            rtTimes.add(result.avgSerializeNs() + result.avgDeserializeNs());
        }

        stats.serMean = mean(serTimes);
        stats.serMedian = median(serTimes);
        stats.serMin = Collections.min(serTimes);
        stats.serMax = Collections.max(serTimes);
        stats.serStdDev = stdDev(serTimes, stats.serMean);

        stats.desMean = mean(desTimes);
        stats.desMedian = median(desTimes);
        stats.desMin = Collections.min(desTimes);
        stats.desMax = Collections.max(desTimes);
        stats.desStdDev = stdDev(desTimes, stats.desMean);

        stats.rtMean = mean(rtTimes);
        stats.rtMedian = median(rtTimes);
        stats.rtMin = Collections.min(rtTimes);
        stats.rtMax = Collections.max(rtTimes);
        stats.rtStdDev = stdDev(rtTimes, stats.rtMean);

        return stats;
    }

    private double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    private double stdDev(List<Double> values, double mean) {
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }

    public static class LibraryStats {
        double serMean, serMedian, serMin, serMax, serStdDev;
        double desMean, desMedian, desMin, desMax, desStdDev;
        double rtMean, rtMedian, rtMin, rtMax, rtStdDev;
    }

    public static class SerializationResult {
        final long serializeTimeNs;
        final long deserializeTimeNs;
        final int binarySize;
        final int iterations;

        public SerializationResult(long serializeTimeNs, long deserializeTimeNs, int binarySize, int iterations) {
            this.serializeTimeNs = serializeTimeNs;
            this.deserializeTimeNs = deserializeTimeNs;
            this.binarySize = binarySize;
            this.iterations = iterations;
        }

        double avgSerializeNs() {
            return (double) serializeTimeNs / iterations;
        }

        double avgDeserializeNs() {
            return (double) deserializeTimeNs / iterations;
        }
    }
}

