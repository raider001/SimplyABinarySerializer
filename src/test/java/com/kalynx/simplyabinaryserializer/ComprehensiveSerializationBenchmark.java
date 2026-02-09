package com.kalynx.simplyabinaryserializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive performance comparison of multiple serialization libraries:
 * - BinarySerializer (our custom implementation)
 * - Jackson (JSON)
 * - Kryo (popular binary serialization)
 * - MessagePack (binary JSON-like format)
 * 
 * Each test uses unique objects to avoid caching artifacts.
 */
public class ComprehensiveSerializationBenchmark {

    private static final int WARMUP_ITERATIONS = 5000;
    private static final int BENCHMARK_ITERATIONS = 50000;

    private BinarySerializer binarySerializer;
    private BinaryDeserializer binaryDeserializer;
    private ObjectMapper jacksonMapper;
    private ThreadLocal<Kryo> kryoThreadLocal;
    private ObjectMapper msgpackMapper;

    @BeforeEach
    public void setup() {
        binarySerializer = new BinarySerializer();
        binaryDeserializer = new BinaryDeserializer();
        jacksonMapper = new ObjectMapper();
        
        kryoThreadLocal = ThreadLocal.withInitial(() -> {
            Kryo kryo = new Kryo();
            kryo.register(SimpleObject.class);
            kryo.register(ComplexObject.class);
            kryo.register(NestedObject.class);
            kryo.register(HashMap.class);
            kryo.register(ArrayList.class);
            return kryo;
        });
        
        msgpackMapper = new ObjectMapper(new MessagePackFactory());
    }

    @Test
    public void benchmark_SimpleObject_AllLibraries() throws Exception {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("SIMPLE OBJECT BENCHMARK - Serialization + Deserialization (50K unique objects)");
        System.out.println("=".repeat(100));
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            SimpleObject obj = createSimpleObject(i);
            warmupAll(obj);
        }
        
        // Benchmark
        BenchmarkResult binary = benchmarkBinary();
        BenchmarkResult jackson = benchmarkJackson();
        BenchmarkResult kryo = benchmarkKryo();
        BenchmarkResult msgpack = benchmarkMessagePack();
        
        printTable("SimpleObject", binary, jackson, kryo, msgpack);
    }

    @Test
    public void benchmark_ComplexObject_AllLibraries() throws Exception {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("COMPLEX OBJECT BENCHMARK - Serialization + Deserialization (10K unique objects)");
        System.out.println("=".repeat(100));
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS / 5; i++) {
            ComplexObject obj = createComplexObject(i);
            warmupAllComplex(obj);
        }
        
        // Benchmark
        BenchmarkResult binary = benchmarkBinaryComplex();
        BenchmarkResult jackson = benchmarkJacksonComplex();
        BenchmarkResult kryo = benchmarkKryoComplex();
        BenchmarkResult msgpack = benchmarkMessagePackComplex();
        
        printTable("ComplexObject", binary, jackson, kryo, msgpack);
    }

    private BenchmarkResult benchmarkBinary() throws Exception {
        byte[][] data = new byte[BENCHMARK_ITERATIONS][];
        
        long serStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            data[i] = binarySerializer.serialize(createSimpleObject(i), SimpleObject.class);
        }
        long serTime = System.nanoTime() - serStart;
        
        long deserStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            binaryDeserializer.deserialize(data[i], SimpleObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;
        
        return new BenchmarkResult("BinarySerializer", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkJackson() throws Exception {
        byte[][] data = new byte[BENCHMARK_ITERATIONS][];
        
        long serStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            data[i] = jacksonMapper.writeValueAsBytes(createSimpleObject(i));
        }
        long serTime = System.nanoTime() - serStart;
        
        long deserStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            jacksonMapper.readValue(data[i], SimpleObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;
        
        return new BenchmarkResult("Jackson (JSON)", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkKryo() throws Exception {
        byte[][] data = new byte[BENCHMARK_ITERATIONS][];
        
        long serStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output output = new Output(baos);
            kryoThreadLocal.get().writeObject(output, createSimpleObject(i));
            output.close();
            data[i] = baos.toByteArray();
        }
        long serTime = System.nanoTime() - serStart;
        
        long deserStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Input input = new Input(new ByteArrayInputStream(data[i]));
            kryoThreadLocal.get().readObject(input, SimpleObject.class);
            input.close();
        }
        long deserTime = System.nanoTime() - deserStart;
        
        return new BenchmarkResult("Kryo", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkMessagePack() throws Exception {
        byte[][] data = new byte[BENCHMARK_ITERATIONS][];
        
        long serStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            data[i] = msgpackMapper.writeValueAsBytes(createSimpleObject(i));
        }
        long serTime = System.nanoTime() - serStart;
        
        long deserStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            msgpackMapper.readValue(data[i], SimpleObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;
        
        return new BenchmarkResult("MessagePack", serTime, deserTime, data[0].length);
    }

    // Complex object benchmarks
    private BenchmarkResult benchmarkBinaryComplex() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 5;
        byte[][] data = new byte[iterations][];
        
        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = binarySerializer.serialize(createComplexObject(i), ComplexObject.class);
        }
        long serTime = System.nanoTime() - serStart;
        
        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            binaryDeserializer.deserialize(data[i], ComplexObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;
        
        return new BenchmarkResult("BinarySerializer", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkJacksonComplex() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 5;
        byte[][] data = new byte[iterations][];
        
        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = jacksonMapper.writeValueAsBytes(createComplexObject(i));
        }
        long serTime = System.nanoTime() - serStart;
        
        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            jacksonMapper.readValue(data[i], ComplexObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;
        
        return new BenchmarkResult("Jackson (JSON)", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkKryoComplex() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 5;
        byte[][] data = new byte[iterations][];
        
        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output output = new Output(baos);
            kryoThreadLocal.get().writeObject(output, createComplexObject(i));
            output.close();
            data[i] = baos.toByteArray();
        }
        long serTime = System.nanoTime() - serStart;
        
        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Input input = new Input(new ByteArrayInputStream(data[i]));
            kryoThreadLocal.get().readObject(input, ComplexObject.class);
            input.close();
        }
        long deserTime = System.nanoTime() - deserStart;
        
        return new BenchmarkResult("Kryo", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkMessagePackComplex() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 5;
        byte[][] data = new byte[iterations][];
        
        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = msgpackMapper.writeValueAsBytes(createComplexObject(i));
        }
        long serTime = System.nanoTime() - serStart;
        
        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            msgpackMapper.readValue(data[i], ComplexObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;
        
        return new BenchmarkResult("MessagePack", serTime, deserTime, data[0].length);
    }

    private void warmupAll(SimpleObject obj) throws Exception {
        binarySerializer.serialize(obj, SimpleObject.class);
        jacksonMapper.writeValueAsBytes(obj);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryoThreadLocal.get().writeObject(output, obj);
        output.close();
        msgpackMapper.writeValueAsBytes(obj);
    }

    private void warmupAllComplex(ComplexObject obj) throws Exception {
        binarySerializer.serialize(obj, ComplexObject.class);
        jacksonMapper.writeValueAsBytes(obj);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryoThreadLocal.get().writeObject(output, obj);
        output.close();
        msgpackMapper.writeValueAsBytes(obj);
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

    private ComplexObject createComplexObject(int seed) {
        ComplexObject obj = new ComplexObject();
        obj.id = 42 + seed;
        obj.name = "Complex" + seed;
        obj.active = seed % 2 == 0;
        obj.nested = new NestedObject();
        obj.nested.id = 100 + seed;
        obj.nested.name = "Nested" + seed;
        obj.nested.value = 1.23 + seed;
        obj.data = new HashMap<>();
        obj.data.put("count", 5 + seed);
        obj.data.put("version", 2 + seed);
        return obj;
    }

    private void printTable(String name, BenchmarkResult... results) {
        System.out.println("\n" + name + " Results:");
        System.out.println("-".repeat(110));
        System.out.printf("%-20s %15s %15s %15s %12s %15s\n", 
            "Library", "Serialize (ms)", "Deserialize (ms)", "Total (ms)", "Size (bytes)", "Size vs Binary");
        System.out.println("-".repeat(110));
        
        double bestSer = Double.MAX_VALUE;
        double bestDeser = Double.MAX_VALUE;
        int bestSize = Integer.MAX_VALUE;
        
        for (BenchmarkResult r : results) {
            bestSer = Math.min(bestSer, r.serMs);
            bestDeser = Math.min(bestDeser, r.desMs);
            bestSize = Math.min(bestSize, r.size);
        }
        
        for (int i = 0; i < results.length; i++) {
            BenchmarkResult r = results[i];
            String serMark = r.serMs == bestSer ? " ⚡" : "";
            String desMark = r.desMs == bestDeser ? " ⚡" : "";
            String sizeMark = r.size == bestSize ? " 💾" : "";
            double total = r.serMs + r.desMs;
            String sizeVs = i == 0 ? "-" : String.format("%.2fx", (results[0].size / (double) r.size));
            
            System.out.printf("%-20s %12.2f%s %12.2f%s %12.2f    %9d%s %15s\n",
                r.name, r.serMs, serMark, r.desMs, desMark, total, r.size, sizeMark, sizeVs);
        }
        
        System.out.println("-".repeat(110));
        System.out.println("⚡ = Fastest  💾 = Smallest\n");
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

    public static class ComplexObject {
        public int id;
        public String name;
        public boolean active;
        public NestedObject nested;
        public Map<String, Integer> data;
        public ComplexObject() {}
    }

    public static class NestedObject {
        public int id;
        public String name;
        public double value;
        public NestedObject() {}
    }

    private static class BenchmarkResult {
        String name;
        double serMs;
        double desMs;
        int size;
        
        BenchmarkResult(String name, long serNs, long desNs, int size) {
            this.name = name;
            this.serMs = serNs / 1_000_000.0;
            this.desMs = desNs / 1_000_000.0;
            this.size = size;
        }
    }
}

