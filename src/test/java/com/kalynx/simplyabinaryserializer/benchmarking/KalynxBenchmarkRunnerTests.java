package com.kalynx.simplyabinaryserializer.benchmarking;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.kalynx.simplyabinaryserializer.KalynxSerializer;
import com.kalynx.simplyabinaryserializer.testutil.TestDataClasses.*;
import org.apache.fury.Fury;
import org.apache.fury.config.Language;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Benchmark suite for KalynxSerializer.
 * Each test method benchmarks one serializer with one object type.
 * Pattern: benchmark{Library}_{ObjectType}()
 */
public class KalynxBenchmarkRunnerTests {

    private static final int BENCHMARK_ITERATIONS = 100_000;

    // Library names
    private static final String KALYNX_SERIALIZER = "KalynxSerializer";
    private static final String KRYO = "Kryo";
    private static final String FURY = "Apache Fury";

    // Object type names
    private static final String ALL_PRIMITIVES = "AllPrimitivesObject";
    private static final String INTEGER_LIST = "IntegerListObject";
    private static final String STRING_LIST = "StringListObject";
    private static final String LONG_LIST = "LongListObject";
    private static final String DOUBLE_LIST = "DoubleListObject";
    private static final String MIXED_PRIMITIVE_LIST = "MixedPrimitiveAndListObject";
    private static final String ALL_PRIMITIVES_WITH_LISTS = "AllPrimitivesWithListsObject";

    private static final String[] LIBRARIES = {KALYNX_SERIALIZER, KRYO, FURY};
    private static final String[] OBJECT_TYPES = {
        ALL_PRIMITIVES,
        INTEGER_LIST,
        STRING_LIST,
        LONG_LIST,
        DOUBLE_LIST,
        MIXED_PRIMITIVE_LIST,
        ALL_PRIMITIVES_WITH_LISTS
    };

    private static BenchmarkResultManager resultManager;

    // KalynxSerializer instances
    private KalynxSerializer<AllPrimitivesObject> kalynxAllPrimitives;
    private KalynxSerializer<IntegerListObject> kalynxIntegerList;
    private KalynxSerializer<StringListObject> kalynxStringList;
    private KalynxSerializer<LongListObject> kalynxLongList;
    private KalynxSerializer<DoubleListObject> kalynxDoubleList;
    private KalynxSerializer<MixedPrimitiveAndListObject> kalynxMixedPrimitiveList;
    private KalynxSerializer<AllPrimitivesWithListsObject> kalynxAllPrimitivesWithLists;

    // Kryo and Fury
    private ThreadLocal<Kryo> kryoThreadLocal;
    private Fury fury;

    @BeforeAll
    public static void initializeResultsCollection() throws Throwable {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("KALYNX SERIALIZER BENCHMARK SUITE");
        System.out.println("=".repeat(100));

        resultManager = new BenchmarkResultManager(LIBRARIES, OBJECT_TYPES);

        System.out.println("Libraries: " + LIBRARIES.length);
        System.out.println("Object Types: " + OBJECT_TYPES.length);
        System.out.println("Total Tests: " + (LIBRARIES.length * OBJECT_TYPES.length));

        System.out.println("\n" + "=".repeat(100));
        System.out.println("WARMING UP JVM...");
        System.out.println("=".repeat(100));

        performWarmup();

        System.out.println("Warmup complete!");
        System.out.println("=".repeat(100) + "\n");
    }

    private static void performWarmup() throws Throwable {
        KalynxSerializer<AllPrimitivesObject> ks = new KalynxSerializer<>(AllPrimitivesObject.class);

        ThreadLocal<Kryo> kryo = ThreadLocal.withInitial(() -> {
            Kryo k = new Kryo();
            k.setRegistrationRequired(false);
            return k;
        });

        Fury fury = Fury.builder().withLanguage(Language.JAVA).requireClassRegistration(false).build();

        System.out.println("  Warming up with 10k iterations...");
        for (int i = 0; i < 10_000; i++) {
            AllPrimitivesObject obj = createAllPrimitivesObject(i);

            byte[] kdata = ks.serialize(obj);
            ks.deserialize(kdata);



            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output output = new Output(baos);
            kryo.get().writeObject(output, obj);
            output.close();
            Input input = new Input(new ByteArrayInputStream(baos.toByteArray()));
            kryo.get().readObject(input, AllPrimitivesObject.class);
            input.close();

            byte[] fdata = fury.serialize(obj);
            fury.deserialize(fdata);
        }

        System.gc();
        Thread.sleep(100);
    }

    @BeforeEach
    public void setUp() throws Throwable {
        // Initialize KalynxSerializer instances
        kalynxAllPrimitives = new KalynxSerializer<>(AllPrimitivesObject.class);
        kalynxIntegerList = new KalynxSerializer<>(IntegerListObject.class);
        kalynxStringList = new KalynxSerializer<>(StringListObject.class);
        kalynxLongList = new KalynxSerializer<>(LongListObject.class);
        kalynxDoubleList = new KalynxSerializer<>(DoubleListObject.class);
        kalynxMixedPrimitiveList = new KalynxSerializer<>(MixedPrimitiveAndListObject.class);
        kalynxAllPrimitivesWithLists = new KalynxSerializer<>(AllPrimitivesWithListsObject.class);

        // Initialize Kryo
        kryoThreadLocal = ThreadLocal.withInitial(() -> {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryo.register(ArrayList.class);
            return kryo;
        });

        // Initialize Fury
        fury = Fury.builder()
                .withLanguage(Language.JAVA)
                .requireClassRegistration(false)
                .build();
    }

    // ========== Object Creation Methods ==========

    private static AllPrimitivesObject createAllPrimitivesObject(int seed) {
        return new AllPrimitivesObject(
            (byte) (seed % 128),
            (short) (1000 + seed % 1000),
            123456 + seed,
            9876543210L + seed,
            3.14f + seed,
            2.718281828 + seed,
            seed % 2 == 0,
            (char) ('A' + seed % 26)
        );
    }

    private static IntegerListObject createIntegerListObject(int seed) {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            values.add(seed * 100 + i);
        }
        return new IntegerListObject(values);
    }

    private static StringListObject createStringListObject(int seed) {
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            strings.add("String_" + seed + "_" + i);
        }
        return new StringListObject(strings);
    }

    private static LongListObject createLongListObject(int seed) {
        List<Long> values = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            values.add(1000000000L * seed + i);
        }
        return new LongListObject(values);
    }

    private static DoubleListObject createDoubleListObject(int seed) {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            values.add(3.14159 * seed + i * 0.1);
        }
        return new DoubleListObject(values);
    }

    private static MixedPrimitiveAndListObject createMixedPrimitiveAndListObject(int seed) {
        List<Integer> intList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            intList.add(seed + i);
        }
        return new MixedPrimitiveAndListObject(42 + seed, intList);
    }

    private static AllPrimitivesWithListsObject createAllPrimitivesWithListsObject(int seed) {
        List<Integer> intList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            intList.add(seed + i);
        }
        List<String> stringList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            stringList.add("item_" + seed + "_" + i);
        }
        return new AllPrimitivesWithListsObject(
            (byte) (seed % 100),
            intList,
            123456789L + seed,
            stringList,
            seed % 2 == 0
        );
    }

    // ========== KalynxSerializer Benchmark Tests ==========

    @Test
    public void benchmarkKalynxSerializer_AllPrimitivesObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - AllPrimitivesObject ===");

        AllPrimitivesObject[] objects = new AllPrimitivesObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createAllPrimitivesObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxAllPrimitives, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, ALL_PRIMITIVES, result);
        resultManager.printResult(KALYNX_SERIALIZER, ALL_PRIMITIVES, result);
    }

    @Test
    public void benchmarkKalynxSerializer_IntegerListObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - IntegerListObject ===");

        IntegerListObject[] objects = new IntegerListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createIntegerListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxIntegerList, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, INTEGER_LIST, result);
        resultManager.printResult(KALYNX_SERIALIZER, INTEGER_LIST, result);
    }

    @Test
    public void benchmarkKalynxSerializer_StringListObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - StringListObject ===");

        StringListObject[] objects = new StringListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createStringListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxStringList, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, STRING_LIST, result);
        resultManager.printResult(KALYNX_SERIALIZER, STRING_LIST, result);
    }

    @Test
    public void benchmarkKalynxSerializer_LongListObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - LongListObject ===");

        LongListObject[] objects = new LongListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLongListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxLongList, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, LONG_LIST, result);
        resultManager.printResult(KALYNX_SERIALIZER, LONG_LIST, result);
    }

    @Test
    public void benchmarkKalynxSerializer_DoubleListObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - DoubleListObject ===");

        DoubleListObject[] objects = new DoubleListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createDoubleListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxDoubleList, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, DOUBLE_LIST, result);
        resultManager.printResult(KALYNX_SERIALIZER, DOUBLE_LIST, result);
    }

    @Test
    public void benchmarkKalynxSerializer_MixedPrimitiveAndListObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - MixedPrimitiveAndListObject ===");

        MixedPrimitiveAndListObject[] objects = new MixedPrimitiveAndListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createMixedPrimitiveAndListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxMixedPrimitiveList, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, MIXED_PRIMITIVE_LIST, result);
        resultManager.printResult(KALYNX_SERIALIZER, MIXED_PRIMITIVE_LIST, result);
    }

    @Test
    public void benchmarkKalynxSerializer_AllPrimitivesWithListsObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - AllPrimitivesWithListsObject ===");

        AllPrimitivesWithListsObject[] objects = new AllPrimitivesWithListsObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createAllPrimitivesWithListsObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxAllPrimitivesWithLists, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, ALL_PRIMITIVES_WITH_LISTS, result);
        resultManager.printResult(KALYNX_SERIALIZER, ALL_PRIMITIVES_WITH_LISTS, result);
    }


    // ========== Kryo Benchmark Tests ==========

    @Test
    public void benchmarkKryo_AllPrimitivesObject() throws Throwable {
        System.out.println("\n=== Kryo - AllPrimitivesObject ===");

        AllPrimitivesObject[] objects = new AllPrimitivesObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createAllPrimitivesObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, AllPrimitivesObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, ALL_PRIMITIVES, result);
        resultManager.printResult(KRYO, ALL_PRIMITIVES, result);
    }

    @Test
    public void benchmarkKryo_IntegerListObject() throws Throwable {
        System.out.println("\n=== Kryo - IntegerListObject ===");

        IntegerListObject[] objects = new IntegerListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createIntegerListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, IntegerListObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, INTEGER_LIST, result);
        resultManager.printResult(KRYO, INTEGER_LIST, result);
    }

    @Test
    public void benchmarkKryo_StringListObject() throws Throwable {
        System.out.println("\n=== Kryo - StringListObject ===");

        StringListObject[] objects = new StringListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createStringListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, StringListObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, STRING_LIST, result);
        resultManager.printResult(KRYO, STRING_LIST, result);
    }

    @Test
    public void benchmarkKryo_LongListObject() throws Throwable {
        System.out.println("\n=== Kryo - LongListObject ===");

        LongListObject[] objects = new LongListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLongListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, LongListObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, LONG_LIST, result);
        resultManager.printResult(KRYO, LONG_LIST, result);
    }

    @Test
    public void benchmarkKryo_DoubleListObject() throws Throwable {
        System.out.println("\n=== Kryo - DoubleListObject ===");

        DoubleListObject[] objects = new DoubleListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createDoubleListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, DoubleListObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, DOUBLE_LIST, result);
        resultManager.printResult(KRYO, DOUBLE_LIST, result);
    }

    @Test
    public void benchmarkKryo_MixedPrimitiveAndListObject() throws Throwable {
        System.out.println("\n=== Kryo - MixedPrimitiveAndListObject ===");

        MixedPrimitiveAndListObject[] objects = new MixedPrimitiveAndListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createMixedPrimitiveAndListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, MixedPrimitiveAndListObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, MIXED_PRIMITIVE_LIST, result);
        resultManager.printResult(KRYO, MIXED_PRIMITIVE_LIST, result);
    }

    @Test
    public void benchmarkKryo_AllPrimitivesWithListsObject() throws Throwable {
        System.out.println("\n=== Kryo - AllPrimitivesWithListsObject ===");

        AllPrimitivesWithListsObject[] objects = new AllPrimitivesWithListsObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createAllPrimitivesWithListsObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, AllPrimitivesWithListsObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, ALL_PRIMITIVES_WITH_LISTS, result);
        resultManager.printResult(KRYO, ALL_PRIMITIVES_WITH_LISTS, result);
    }

    // ========== Fury Benchmark Tests ==========

    @Test
    public void benchmarkFury_AllPrimitivesObject() throws Throwable {
        System.out.println("\n=== Fury - AllPrimitivesObject ===");

        AllPrimitivesObject[] objects = new AllPrimitivesObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createAllPrimitivesObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, ALL_PRIMITIVES, result);
        resultManager.printResult(FURY, ALL_PRIMITIVES, result);
    }

    @Test
    public void benchmarkFury_IntegerListObject() throws Throwable {
        System.out.println("\n=== Fury - IntegerListObject ===");

        IntegerListObject[] objects = new IntegerListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createIntegerListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, INTEGER_LIST, result);
        resultManager.printResult(FURY, INTEGER_LIST, result);
    }

    @Test
    public void benchmarkFury_StringListObject() throws Throwable {
        System.out.println("\n=== Fury - StringListObject ===");

        StringListObject[] objects = new StringListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createStringListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, STRING_LIST, result);
        resultManager.printResult(FURY, STRING_LIST, result);
    }

    @Test
    public void benchmarkFury_LongListObject() throws Throwable {
        System.out.println("\n=== Fury - LongListObject ===");

        LongListObject[] objects = new LongListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLongListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, LONG_LIST, result);
        resultManager.printResult(FURY, LONG_LIST, result);
    }

    @Test
    public void benchmarkFury_DoubleListObject() throws Throwable {
        System.out.println("\n=== Fury - DoubleListObject ===");

        DoubleListObject[] objects = new DoubleListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createDoubleListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, DOUBLE_LIST, result);
        resultManager.printResult(FURY, DOUBLE_LIST, result);
    }

    @Test
    public void benchmarkFury_MixedPrimitiveAndListObject() throws Throwable {
        System.out.println("\n=== Fury - MixedPrimitiveAndListObject ===");

        MixedPrimitiveAndListObject[] objects = new MixedPrimitiveAndListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createMixedPrimitiveAndListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, MIXED_PRIMITIVE_LIST, result);
        resultManager.printResult(FURY, MIXED_PRIMITIVE_LIST, result);
    }

    @Test
    public void benchmarkFury_AllPrimitivesWithListsObject() throws Throwable {
        System.out.println("\n=== Fury - AllPrimitivesWithListsObject ===");

        AllPrimitivesWithListsObject[] objects = new AllPrimitivesWithListsObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createAllPrimitivesWithListsObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, ALL_PRIMITIVES_WITH_LISTS, result);
        resultManager.printResult(FURY, ALL_PRIMITIVES_WITH_LISTS, result);
    }

    // ========== Benchmark Helper Methods ==========

    private <T> BenchmarkResultManager.SerializationResult benchmarkKalynxSerializer(
            KalynxSerializer<T> serializer, T[] objects, int iterations) throws Throwable {

        long startSerialize = System.nanoTime();
        byte[] lastSerialized = null;
        for (int i = 0; i < iterations; i++) {
            lastSerialized = serializer.serialize(objects[i]);
        }
        long serializeTime = System.nanoTime() - startSerialize;

        long startDeserialize = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            serializer.deserialize(lastSerialized);
        }
        long deserializeTime = System.nanoTime() - startDeserialize;

        return new BenchmarkResultManager.SerializationResult(
                serializeTime, deserializeTime, lastSerialized.length, iterations);
    }

    private <T> BenchmarkResultManager.SerializationResult benchmarkKryo(
            T[] objects, Class<T> clazz, int iterations) throws Throwable {

        long startSerialize = System.nanoTime();
        byte[] lastSerialized = null;
        for (int i = 0; i < iterations; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output output = new Output(baos);
            kryoThreadLocal.get().writeObject(output, objects[i]);
            output.close();
            lastSerialized = baos.toByteArray();
        }
        long serializeTime = System.nanoTime() - startSerialize;

        long startDeserialize = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Input input = new Input(new ByteArrayInputStream(lastSerialized));
            kryoThreadLocal.get().readObject(input, clazz);
            input.close();
        }
        long deserializeTime = System.nanoTime() - startDeserialize;

        return new BenchmarkResultManager.SerializationResult(
                serializeTime, deserializeTime, lastSerialized.length, iterations);
    }

    private <T> BenchmarkResultManager.SerializationResult benchmarkFury(
            T[] objects, int iterations) throws Throwable {

        long startSerialize = System.nanoTime();
        byte[] lastSerialized = null;
        for (int i = 0; i < iterations; i++) {
            lastSerialized = fury.serialize(objects[i]);
        }
        long serializeTime = System.nanoTime() - startSerialize;

        long startDeserialize = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            fury.deserialize(lastSerialized);
        }
        long deserializeTime = System.nanoTime() - startDeserialize;

        return new BenchmarkResultManager.SerializationResult(
                serializeTime, deserializeTime, lastSerialized.length, iterations);
    }

    // ========== Final Report ==========

    @AfterAll
    public static void generateFinalReport() throws Throwable {
        System.out.println("\n\n" + "=".repeat(100));
        System.out.println("GENERATING FINAL REPORT");
        System.out.println("=".repeat(100));

        resultManager.generateFinalReport(LIBRARIES, OBJECT_TYPES);
    }
}

