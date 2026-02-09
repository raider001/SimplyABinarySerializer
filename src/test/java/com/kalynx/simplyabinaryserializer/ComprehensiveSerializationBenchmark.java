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

        // Force new Kryo instance with all registrations
        kryoThreadLocal = ThreadLocal.withInitial(() -> {
            Kryo kryo = new Kryo();
            // CRITICAL: Must be first - allows unregistered classes
            kryo.setRegistrationRequired(false);
            // Register common classes for better performance
            kryo.register(SimpleObject.class);
            kryo.register(ComplexObject.class);
            kryo.register(NestedObject.class);
            kryo.register(ListObject.class);
            kryo.register(MapObject.class);
            kryo.register(DeepObject.class);
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

    @Test
    public void benchmark_ListSerialization_AllLibraries() throws Exception {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("LIST SERIALIZATION BENCHMARK - Various list types (10K unique objects)");
        System.out.println("=".repeat(100));

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS / 5; i++) {
            ListObject obj = createListObject(i);
            warmupAllList(obj);
        }

        // Benchmark
        BenchmarkResult binary = benchmarkBinaryList();
        BenchmarkResult jackson = benchmarkJacksonList();
        BenchmarkResult kryo = benchmarkKryoList();
        BenchmarkResult msgpack = benchmarkMessagePackList();

        printTable("ListObject (3 lists: Integer, String, Boolean)", binary, jackson, kryo, msgpack);
    }

    @Test
    public void benchmark_MapSerialization_AllLibraries() throws Exception {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("MAP SERIALIZATION BENCHMARK - Various map types (10K unique objects)");
        System.out.println("=".repeat(100));

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS / 5; i++) {
            MapObject obj = createMapObject(i);
            warmupAllMap(obj);
        }

        // Benchmark
        BenchmarkResult binary = benchmarkBinaryMap();
        BenchmarkResult jackson = benchmarkJacksonMap();
        BenchmarkResult kryo = benchmarkKryoMap();
        BenchmarkResult msgpack = benchmarkMessagePackMap();

        printTable("MapObject (3 maps: String->Integer, String->String, Integer->String)", binary, jackson, kryo, msgpack);
    }

    @Test
    public void benchmark_DeepNestedObject_AllLibraries() throws Exception {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("DEEP NESTED OBJECT BENCHMARK - 5 levels deep (5K unique objects)");
        System.out.println("=".repeat(100));

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS / 10; i++) {
            DeepObject obj = createDeepObject(i, 5);
            warmupAllDeep(obj);
        }

        // Benchmark
        BenchmarkResult binary = benchmarkBinaryDeep();
        BenchmarkResult jackson = benchmarkJacksonDeep();
        BenchmarkResult kryo = benchmarkKryoDeep();
        BenchmarkResult msgpack = benchmarkMessagePackDeep();

        printTable("DeepObject (5 levels of nesting)", binary, jackson, kryo, msgpack);
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

    // List benchmarks
    private BenchmarkResult benchmarkBinaryList() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 5;
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = binarySerializer.serialize(createListObject(i), ListObject.class);
        }
        long serTime = System.nanoTime() - serStart;

        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            binaryDeserializer.deserialize(data[i], ListObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;

        return new BenchmarkResult("BinarySerializer", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkJacksonList() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 5;
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = jacksonMapper.writeValueAsBytes(createListObject(i));
        }
        long serTime = System.nanoTime() - serStart;

        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            jacksonMapper.readValue(data[i], ListObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;

        return new BenchmarkResult("Jackson (JSON)", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkKryoList() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 5;
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output output = new Output(baos);
            kryoThreadLocal.get().writeObject(output, createListObject(i));
            output.close();
            data[i] = baos.toByteArray();
        }
        long serTime = System.nanoTime() - serStart;

        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Input input = new Input(new ByteArrayInputStream(data[i]));
            kryoThreadLocal.get().readObject(input, ListObject.class);
            input.close();
        }
        long deserTime = System.nanoTime() - deserStart;

        return new BenchmarkResult("Kryo", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkMessagePackList() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 5;
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = msgpackMapper.writeValueAsBytes(createListObject(i));
        }
        long serTime = System.nanoTime() - serStart;

        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            msgpackMapper.readValue(data[i], ListObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;

        return new BenchmarkResult("MessagePack", serTime, deserTime, data[0].length);
    }

    // Map benchmarks
    private BenchmarkResult benchmarkBinaryMap() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 5;
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = binarySerializer.serialize(createMapObject(i), MapObject.class);
        }
        long serTime = System.nanoTime() - serStart;

        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            binaryDeserializer.deserialize(data[i], MapObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;

        return new BenchmarkResult("BinarySerializer", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkJacksonMap() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 5;
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = jacksonMapper.writeValueAsBytes(createMapObject(i));
        }
        long serTime = System.nanoTime() - serStart;

        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            jacksonMapper.readValue(data[i], MapObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;

        return new BenchmarkResult("Jackson (JSON)", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkKryoMap() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 5;
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output output = new Output(baos);
            kryoThreadLocal.get().writeObject(output, createMapObject(i));
            output.close();
            data[i] = baos.toByteArray();
        }
        long serTime = System.nanoTime() - serStart;

        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Input input = new Input(new ByteArrayInputStream(data[i]));
            kryoThreadLocal.get().readObject(input, MapObject.class);
            input.close();
        }
        long deserTime = System.nanoTime() - deserStart;

        return new BenchmarkResult("Kryo", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkMessagePackMap() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 5;
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = msgpackMapper.writeValueAsBytes(createMapObject(i));
        }
        long serTime = System.nanoTime() - serStart;

        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            msgpackMapper.readValue(data[i], MapObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;

        return new BenchmarkResult("MessagePack", serTime, deserTime, data[0].length);
    }

    // Deep nested object benchmarks
    private BenchmarkResult benchmarkBinaryDeep() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 10;
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = binarySerializer.serialize(createDeepObject(i, 5), DeepObject.class);
        }
        long serTime = System.nanoTime() - serStart;

        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            binaryDeserializer.deserialize(data[i], DeepObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;

        return new BenchmarkResult("BinarySerializer", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkJacksonDeep() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 10;
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = jacksonMapper.writeValueAsBytes(createDeepObject(i, 5));
        }
        long serTime = System.nanoTime() - serStart;

        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            jacksonMapper.readValue(data[i], DeepObject.class);
        }
        long deserTime = System.nanoTime() - deserStart;

        return new BenchmarkResult("Jackson (JSON)", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkKryoDeep() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 10;
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output output = new Output(baos);
            kryoThreadLocal.get().writeObject(output, createDeepObject(i, 5));
            output.close();
            data[i] = baos.toByteArray();
        }
        long serTime = System.nanoTime() - serStart;

        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Input input = new Input(new ByteArrayInputStream(data[i]));
            kryoThreadLocal.get().readObject(input, DeepObject.class);
            input.close();
        }
        long deserTime = System.nanoTime() - deserStart;

        return new BenchmarkResult("Kryo", serTime, deserTime, data[0].length);
    }

    private BenchmarkResult benchmarkMessagePackDeep() throws Exception {
        int iterations = BENCHMARK_ITERATIONS / 10;
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = msgpackMapper.writeValueAsBytes(createDeepObject(i, 5));
        }
        long serTime = System.nanoTime() - serStart;

        long deserStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            msgpackMapper.readValue(data[i], DeepObject.class);
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

    private void warmupAllList(ListObject obj) throws Exception {
        binarySerializer.serialize(obj, ListObject.class);
        jacksonMapper.writeValueAsBytes(obj);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryoThreadLocal.get().writeObject(output, obj);
        output.close();
        msgpackMapper.writeValueAsBytes(obj);
    }

    private void warmupAllMap(MapObject obj) throws Exception {
        binarySerializer.serialize(obj, MapObject.class);
        jacksonMapper.writeValueAsBytes(obj);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryoThreadLocal.get().writeObject(output, obj);
        output.close();
        msgpackMapper.writeValueAsBytes(obj);
    }

    private void warmupAllDeep(DeepObject obj) throws Exception {
        binarySerializer.serialize(obj, DeepObject.class);
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

    private ListObject createListObject(int seed) {
        ListObject obj = new ListObject();
        obj.integers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            obj.integers.add(seed + i);
        }
        obj.strings = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            obj.strings.add("str" + seed + "_" + i);
        }
        obj.booleans = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            obj.booleans.add((seed + i) % 2 == 0);
        }
        return obj;
    }

    private MapObject createMapObject(int seed) {
        MapObject obj = new MapObject();
        obj.map1 = new HashMap<>();
        obj.map1.put("key1_" + seed, seed + 1);
        obj.map1.put("key2_" + seed, seed + 2);
        obj.map1.put("key3_" + seed, seed + 3);

        obj.map2 = new HashMap<>();
        obj.map2.put("name_" + seed, "value_" + seed);
        obj.map2.put("type_" + seed, "data_" + seed);

        obj.map3 = new HashMap<>();
        obj.map3.put(seed + 1, "val1");
        obj.map3.put(seed + 2, "val2");
        obj.map3.put(seed + 3, "val3");
        return obj;
    }

    private DeepObject createDeepObject(int seed, int depth) {
        DeepObject obj = new DeepObject();
        obj.id = seed;
        obj.name = "Level" + depth + "_" + seed;
        obj.value = 1.5 * depth + seed;

        if (depth > 1) {
            obj.child = createDeepObject(seed, depth - 1);
        }

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

    public static class ListObject {
        public java.util.List<Integer> integers;
        public java.util.List<String> strings;
        public java.util.List<Boolean> booleans;
        public ListObject() {}
    }

    public static class MapObject {
        public Map<String, Integer> map1;
        public Map<String, String> map2;
        public Map<Integer, String> map3;
        public MapObject() {}
    }

    public static class DeepObject {
        public int id;
        public String name;
        public double value;
        public DeepObject child;
        public DeepObject() {}
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










