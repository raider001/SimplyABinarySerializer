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
    private static final String STRING_INTEGER_MAP = "StringIntegerMapObject";
    private static final String INTEGER_STRING_MAP = "IntegerStringMapObject";
    private static final String INTEGER_INTEGER_MAP = "IntegerIntegerMapObject";
    private static final String LONG_DOUBLE_MAP = "LongDoubleMapObject";
    private static final String INT_ARRAY = "IntArrayObject";
    private static final String LONG_ARRAY = "LongArrayObject";
    private static final String DOUBLE_ARRAY = "DoubleArrayObject";
    private static final String ALL_PRIMITIVE_ARRAYS = "AllPrimitiveArraysObject";
    private static final String SIMPLE_NESTED = "SimpleNestedObject";
    private static final String RECTANGLE = "Rectangle";
    private static final String DEEP_NESTED = "DeepNestedLevel1";
    private static final String LARGE_STRING = "LargeStringObject";
    private static final String LARGE_STRING_LIST = "LargeStringListObject";
    private static final String MIXED_SIZE_STRING_LIST = "MixedSizeStringListObject";
    private static final String DOCUMENT = "DocumentObject";

    private static final String[] LIBRARIES = {KALYNX_SERIALIZER, KRYO, FURY};
    private static final String[] OBJECT_TYPES = {
        ALL_PRIMITIVES,
        INTEGER_LIST,
        STRING_LIST,
        LONG_LIST,
        DOUBLE_LIST,
        MIXED_PRIMITIVE_LIST,
        ALL_PRIMITIVES_WITH_LISTS,
        STRING_INTEGER_MAP,
        INTEGER_STRING_MAP,
        INTEGER_INTEGER_MAP,
        LONG_DOUBLE_MAP,
        INT_ARRAY,
        LONG_ARRAY,
        DOUBLE_ARRAY,
        ALL_PRIMITIVE_ARRAYS,
        SIMPLE_NESTED,
        RECTANGLE,
        DEEP_NESTED,
        LARGE_STRING,
        LARGE_STRING_LIST,
        MIXED_SIZE_STRING_LIST,
        DOCUMENT
    };

    private static BenchmarkResultManager resultManager;

    // KalynxSerializer instances
    private KalynxSerializer kalynxAllPrimitives;
    private KalynxSerializer kalynxIntegerList;
    private KalynxSerializer kalynxStringList;
    private KalynxSerializer kalynxLongList;
    private KalynxSerializer kalynxDoubleList;
    private KalynxSerializer kalynxMixedPrimitiveList;
    private KalynxSerializer kalynxAllPrimitivesWithLists;
    private KalynxSerializer kalynxStringIntegerMap;
    private KalynxSerializer kalynxIntegerStringMap;
    private KalynxSerializer kalynxIntegerIntegerMap;
    private KalynxSerializer kalynxLongDoubleMap;
    private KalynxSerializer kalynxIntArray;
    private KalynxSerializer kalynxLongArray;
    private KalynxSerializer kalynxDoubleArray;
    private KalynxSerializer kalynxAllPrimitiveArrays;
    private KalynxSerializer kalynxSimpleNested;
    private KalynxSerializer kalynxRectangle;
    private KalynxSerializer kalynxDeepNested;
    private KalynxSerializer kalynxLargeString;
    private KalynxSerializer kalynxLargeStringList;
    private KalynxSerializer kalynxMixedSizeStringList;
    private KalynxSerializer kalynxDocument;

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
        KalynxSerializer ks = new KalynxSerializer();
ks.register(AllPrimitivesObject.class);

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
            ks.deserialize(kdata, AllPrimitivesObject.class);



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
        kalynxAllPrimitives = new KalynxSerializer();
kalynxAllPrimitives.register(AllPrimitivesObject.class);
        kalynxIntegerList = new KalynxSerializer();
kalynxIntegerList.register(IntegerListObject.class);
        kalynxStringList = new KalynxSerializer();
kalynxStringList.register(StringListObject.class);
        kalynxLongList = new KalynxSerializer();
kalynxLongList.register(LongListObject.class);
        kalynxDoubleList = new KalynxSerializer();
kalynxDoubleList.register(DoubleListObject.class);
        kalynxMixedPrimitiveList = new KalynxSerializer();
kalynxMixedPrimitiveList.register(MixedPrimitiveAndListObject.class);
        kalynxAllPrimitivesWithLists = new KalynxSerializer();
kalynxAllPrimitivesWithLists.register(AllPrimitivesWithListsObject.class);
        kalynxStringIntegerMap = new KalynxSerializer();
kalynxStringIntegerMap.register(StringIntegerMapObject.class);
        kalynxIntegerStringMap = new KalynxSerializer();
kalynxIntegerStringMap.register(IntegerStringMapObject.class);
        kalynxIntegerIntegerMap = new KalynxSerializer();
kalynxIntegerIntegerMap.register(IntegerIntegerMapObject.class);
        kalynxLongDoubleMap = new KalynxSerializer();
kalynxLongDoubleMap.register(LongDoubleMapObject.class);
        kalynxIntArray = new KalynxSerializer();
kalynxIntArray.register(IntArrayObject.class);
        kalynxLongArray = new KalynxSerializer();
kalynxLongArray.register(LongArrayObject.class);
        kalynxDoubleArray = new KalynxSerializer();
kalynxDoubleArray.register(DoubleArrayObject.class);
        kalynxAllPrimitiveArrays = new KalynxSerializer();
kalynxAllPrimitiveArrays.register(AllPrimitiveArraysObject.class);
        kalynxSimpleNested = new KalynxSerializer();
kalynxSimpleNested.register(SimpleNestedObject.class);
        kalynxRectangle = new KalynxSerializer();
kalynxRectangle.register(Rectangle.class);
        kalynxDeepNested = new KalynxSerializer();
kalynxDeepNested.register(DeepNestedLevel1.class);
        kalynxLargeString = new KalynxSerializer();
kalynxLargeString.register(LargeStringObject.class);
        kalynxLargeStringList = new KalynxSerializer();
kalynxLargeStringList.register(LargeStringListObject.class);
        kalynxMixedSizeStringList = new KalynxSerializer();
kalynxMixedSizeStringList.register(MixedSizeStringListObject.class);
        kalynxDocument = new KalynxSerializer();
kalynxDocument.register(DocumentObject.class);

        // Initialize Kryo
        kryoThreadLocal = ThreadLocal.withInitial(() -> {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryo.register(ArrayList.class);
            kryo.register(HashMap.class);
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

    private static StringIntegerMapObject createStringIntegerMapObject(int seed) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            map.put("key_" + seed + "_" + i, seed * 100 + i);
        }
        return new StringIntegerMapObject(map);
    }

    private static IntegerStringMapObject createIntegerStringMapObject(int seed) {
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            map.put(seed * 100 + i, "value_" + seed + "_" + i);
        }
        return new IntegerStringMapObject(map);
    }

    private static IntegerIntegerMapObject createIntegerIntegerMapObject(int seed) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            map.put(seed + i, (seed + i) * 10);
        }
        return new IntegerIntegerMapObject(map);
    }

    private static LongDoubleMapObject createLongDoubleMapObject(int seed) {
        Map<Long, Double> map = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            map.put(1000000L * seed + i, 3.14159 * seed + i * 0.5);
        }
        return new LongDoubleMapObject(map);
    }

    private static IntArrayObject createIntArrayObject(int seed) {
        int[] array = new int[10];
        for (int i = 0; i < 10; i++) {
            array[i] = seed * 100 + i;
        }
        return new IntArrayObject(array);
    }

    private static LongArrayObject createLongArrayObject(int seed) {
        long[] array = new long[10];
        for (int i = 0; i < 10; i++) {
            array[i] = (long)seed * 1000 + i;
        }
        return new LongArrayObject(array);
    }

    private static DoubleArrayObject createDoubleArrayObject(int seed) {
        double[] array = new double[10];
        for (int i = 0; i < 10; i++) {
            array[i] = seed * 3.14159 + i * 0.1;
        }
        return new DoubleArrayObject(array);
    }

    private static AllPrimitiveArraysObject createAllPrimitiveArraysObject(int seed) {
        return new AllPrimitiveArraysObject(
            new int[]{seed, seed + 1, seed + 2},
            new long[]{(long)seed, (long)seed + 1, (long)seed + 2},
            new double[]{seed * 1.1, seed * 2.2, seed * 3.3},
            new float[]{seed * 1.1f, seed * 2.2f, seed * 3.3f},
            new short[]{(short)seed, (short)(seed + 1), (short)(seed + 2)},
            new byte[]{(byte)seed, (byte)(seed + 1), (byte)(seed + 2)},
            new boolean[]{seed % 2 == 0, seed % 3 == 0, seed % 5 == 0},
            new char[]{(char)('A' + seed % 26), (char)('B' + seed % 26), (char)('C' + seed % 26)}
        );
    }

    private static SimpleNestedObject createSimpleNestedObject(int seed) {
        IntObject nested = new IntObject(seed * 100);
        return new SimpleNestedObject(seed, nested);
    }

    private static Rectangle createRectangle(int seed) {
        Point topLeft = new Point(seed, seed * 2);
        Point bottomRight = new Point(seed + 100, seed * 2 + 100);
        return new Rectangle(topLeft, bottomRight, seed * 0xFF);
    }

    private static DeepNestedLevel1 createDeepNestedLevel1(int seed) {
        DeepNestedLevel3 level3 = new DeepNestedLevel3(seed * 300);
        DeepNestedLevel2 level2 = new DeepNestedLevel2(seed * 200, level3);
        return new DeepNestedLevel1(seed * 100, level2);
    }

    // Large string object creation methods

    private static LargeStringObject createLargeStringObject(int seed) {
        String content = generateLargeString(150, seed);
        return new LargeStringObject(content);
    }

    private static LargeStringListObject createLargeStringListObject(int seed) {
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            strings.add(generateLargeString(120 + i * 20, seed + i));
        }
        return new LargeStringListObject(strings);
    }

    private static MixedSizeStringListObject createMixedSizeStringListObject(int seed) {
        List<String> shortStrings = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            shortStrings.add("Short_" + seed + "_" + i);
        }

        List<String> mediumStrings = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            mediumStrings.add(generateLargeString(50, seed + i));
        }

        List<String> longStrings = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            longStrings.add(generateLargeString(200, seed + i));
        }

        return new MixedSizeStringListObject(shortStrings, mediumStrings, longStrings);
    }

    private static DocumentObject createDocumentObject(int seed) {
        String title = "Document Title " + seed + " - Performance Analysis Report";
        String author = "Author Name " + seed;
        String summary = generateRealisticText(150, seed);
        String content = generateRealisticText(500, seed + 100);

        List<String> tags = new ArrayList<>();
        tags.add("performance");
        tags.add("benchmark");
        tags.add("serialization");
        tags.add("tag_" + seed);

        return new DocumentObject(title, author, summary, content, tags, System.currentTimeMillis() + seed);
    }

    // Helper methods for generating large strings

    private static String generateLargeString(int length, int seed) {
        StringBuilder sb = new StringBuilder(length);
        String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ";

        for (int i = 0; i < length; i++) {
            int index = (seed + i) % base.length();
            sb.append(base.charAt(index));
        }

        return sb.toString();
    }

    private static String generateRealisticText(int approximateLength, int seed) {
        String[] sentences = {
            "The quick brown fox jumps over the lazy dog in the middle of a sunny afternoon.",
            "Performance optimization is crucial for building scalable distributed systems.",
            "Machine learning algorithms require extensive training data to achieve accuracy.",
            "Database indexing significantly improves query performance for large datasets.",
            "Microservices architecture enables independent deployment and scaling of components.",
            "Cloud computing provides elastic infrastructure for modern applications.",
            "Security best practices include encryption, authentication, and authorization.",
            "Continuous integration and deployment pipelines automate software delivery.",
            "Modern web applications leverage reactive programming for better responsiveness.",
            "Distributed systems face challenges with consistency, availability, and partition tolerance."
        };

        StringBuilder sb = new StringBuilder(approximateLength);
        int sentenceIndex = seed;

        while (sb.length() < approximateLength) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(sentences[sentenceIndex % sentences.length]);
            sentenceIndex++;
        }

        return sb.substring(0, Math.min(approximateLength, sb.length()));
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

    @Test
    public void benchmarkKalynxSerializer_StringIntegerMapObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - StringIntegerMapObject ===");

        StringIntegerMapObject[] objects = new StringIntegerMapObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createStringIntegerMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxStringIntegerMap, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, STRING_INTEGER_MAP, result);
        resultManager.printResult(KALYNX_SERIALIZER, STRING_INTEGER_MAP, result);
    }

    @Test
    public void benchmarkKalynxSerializer_IntegerStringMapObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - IntegerStringMapObject ===");

        IntegerStringMapObject[] objects = new IntegerStringMapObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createIntegerStringMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxIntegerStringMap, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, INTEGER_STRING_MAP, result);
        resultManager.printResult(KALYNX_SERIALIZER, INTEGER_STRING_MAP, result);
    }

    @Test
    public void benchmarkKalynxSerializer_IntegerIntegerMapObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - IntegerIntegerMapObject ===");

        IntegerIntegerMapObject[] objects = new IntegerIntegerMapObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createIntegerIntegerMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxIntegerIntegerMap, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, INTEGER_INTEGER_MAP, result);
        resultManager.printResult(KALYNX_SERIALIZER, INTEGER_INTEGER_MAP, result);
    }

    @Test
    public void benchmarkKalynxSerializer_LongDoubleMapObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - LongDoubleMapObject ===");

        LongDoubleMapObject[] objects = new LongDoubleMapObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLongDoubleMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxLongDoubleMap, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, LONG_DOUBLE_MAP, result);
        resultManager.printResult(KALYNX_SERIALIZER, LONG_DOUBLE_MAP, result);
    }

    @Test
    public void benchmarkKalynxSerializer_IntArrayObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - IntArrayObject ===");

        IntArrayObject[] objects = new IntArrayObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createIntArrayObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxIntArray, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, INT_ARRAY, result);
        resultManager.printResult(KALYNX_SERIALIZER, INT_ARRAY, result);
    }

    @Test
    public void benchmarkKalynxSerializer_LongArrayObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - LongArrayObject ===");

        LongArrayObject[] objects = new LongArrayObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLongArrayObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxLongArray, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, LONG_ARRAY, result);
        resultManager.printResult(KALYNX_SERIALIZER, LONG_ARRAY, result);
    }

    @Test
    public void benchmarkKalynxSerializer_DoubleArrayObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - DoubleArrayObject ===");

        DoubleArrayObject[] objects = new DoubleArrayObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createDoubleArrayObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxDoubleArray, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, DOUBLE_ARRAY, result);
        resultManager.printResult(KALYNX_SERIALIZER, DOUBLE_ARRAY, result);
    }

    @Test
    public void benchmarkKalynxSerializer_AllPrimitiveArraysObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - AllPrimitiveArraysObject ===");

        AllPrimitiveArraysObject[] objects = new AllPrimitiveArraysObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createAllPrimitiveArraysObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxAllPrimitiveArrays, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, ALL_PRIMITIVE_ARRAYS, result);
        resultManager.printResult(KALYNX_SERIALIZER, ALL_PRIMITIVE_ARRAYS, result);
    }

    @Test
    public void benchmarkKalynxSerializer_SimpleNestedObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - SimpleNestedObject ===");

        SimpleNestedObject[] objects = new SimpleNestedObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createSimpleNestedObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxSimpleNested, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, SIMPLE_NESTED, result);
        resultManager.printResult(KALYNX_SERIALIZER, SIMPLE_NESTED, result);
    }

    @Test
    public void benchmarkKalynxSerializer_Rectangle() throws Throwable {
        System.out.println("\n=== KalynxSerializer - Rectangle ===");

        Rectangle[] objects = new Rectangle[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createRectangle(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxRectangle, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, RECTANGLE, result);
        resultManager.printResult(KALYNX_SERIALIZER, RECTANGLE, result);
    }

    @Test
    public void benchmarkKalynxSerializer_DeepNestedLevel1() throws Throwable {
        System.out.println("\n=== KalynxSerializer - DeepNestedLevel1 ===");

        DeepNestedLevel1[] objects = new DeepNestedLevel1[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createDeepNestedLevel1(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxDeepNested, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, DEEP_NESTED, result);
        resultManager.printResult(KALYNX_SERIALIZER, DEEP_NESTED, result);
    }

    @Test
    public void benchmarkKalynxSerializer_LargeStringObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - LargeStringObject (150 chars) ===");

        LargeStringObject[] objects = new LargeStringObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLargeStringObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxLargeString, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, LARGE_STRING, result);
        resultManager.printResult(KALYNX_SERIALIZER, LARGE_STRING, result);
    }

    @Test
    public void benchmarkKalynxSerializer_LargeStringListObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - LargeStringListObject (5 strings, 120-200 chars each) ===");

        LargeStringListObject[] objects = new LargeStringListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLargeStringListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxLargeStringList, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, LARGE_STRING_LIST, result);
        resultManager.printResult(KALYNX_SERIALIZER, LARGE_STRING_LIST, result);
    }

    @Test
    public void benchmarkKalynxSerializer_MixedSizeStringListObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - MixedSizeStringListObject (short/medium/long strings) ===");

        MixedSizeStringListObject[] objects = new MixedSizeStringListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createMixedSizeStringListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxMixedSizeStringList, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, MIXED_SIZE_STRING_LIST, result);
        resultManager.printResult(KALYNX_SERIALIZER, MIXED_SIZE_STRING_LIST, result);
    }

    @Test
    public void benchmarkKalynxSerializer_DocumentObject() throws Throwable {
        System.out.println("\n=== KalynxSerializer - DocumentObject (realistic document with 500+ char content) ===");

        DocumentObject[] objects = new DocumentObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createDocumentObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKalynxSerializer(kalynxDocument, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KALYNX_SERIALIZER, DOCUMENT, result);
        resultManager.printResult(KALYNX_SERIALIZER, DOCUMENT, result);
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

    @Test
    public void benchmarkKryo_StringIntegerMapObject() throws Throwable {
        System.out.println("\n=== Kryo - StringIntegerMapObject ===");

        StringIntegerMapObject[] objects = new StringIntegerMapObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createStringIntegerMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, StringIntegerMapObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, STRING_INTEGER_MAP, result);
        resultManager.printResult(KRYO, STRING_INTEGER_MAP, result);
    }

    @Test
    public void benchmarkKryo_IntegerStringMapObject() throws Throwable {
        System.out.println("\n=== Kryo - IntegerStringMapObject ===");

        IntegerStringMapObject[] objects = new IntegerStringMapObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createIntegerStringMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, IntegerStringMapObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, INTEGER_STRING_MAP, result);
        resultManager.printResult(KRYO, INTEGER_STRING_MAP, result);
    }

    @Test
    public void benchmarkKryo_IntegerIntegerMapObject() throws Throwable {
        System.out.println("\n=== Kryo - IntegerIntegerMapObject ===");

        IntegerIntegerMapObject[] objects = new IntegerIntegerMapObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createIntegerIntegerMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, IntegerIntegerMapObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, INTEGER_INTEGER_MAP, result);
        resultManager.printResult(KRYO, INTEGER_INTEGER_MAP, result);
    }

    @Test
    public void benchmarkKryo_LongDoubleMapObject() throws Throwable {
        System.out.println("\n=== Kryo - LongDoubleMapObject ===");

        LongDoubleMapObject[] objects = new LongDoubleMapObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLongDoubleMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, LongDoubleMapObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, LONG_DOUBLE_MAP, result);
        resultManager.printResult(KRYO, LONG_DOUBLE_MAP, result);
    }

    @Test
    public void benchmarkKryo_IntArrayObject() throws Throwable {
        System.out.println("\n=== Kryo - IntArrayObject ===");

        IntArrayObject[] objects = new IntArrayObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createIntArrayObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, IntArrayObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, INT_ARRAY, result);
        resultManager.printResult(KRYO, INT_ARRAY, result);
    }

    @Test
    public void benchmarkKryo_LongArrayObject() throws Throwable {
        System.out.println("\n=== Kryo - LongArrayObject ===");

        LongArrayObject[] objects = new LongArrayObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLongArrayObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, LongArrayObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, LONG_ARRAY, result);
        resultManager.printResult(KRYO, LONG_ARRAY, result);
    }

    @Test
    public void benchmarkKryo_DoubleArrayObject() throws Throwable {
        System.out.println("\n=== Kryo - DoubleArrayObject ===");

        DoubleArrayObject[] objects = new DoubleArrayObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createDoubleArrayObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, DoubleArrayObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, DOUBLE_ARRAY, result);
        resultManager.printResult(KRYO, DOUBLE_ARRAY, result);
    }

    @Test
    public void benchmarkKryo_AllPrimitiveArraysObject() throws Throwable {
        System.out.println("\n=== Kryo - AllPrimitiveArraysObject ===");

        AllPrimitiveArraysObject[] objects = new AllPrimitiveArraysObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createAllPrimitiveArraysObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, AllPrimitiveArraysObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, ALL_PRIMITIVE_ARRAYS, result);
        resultManager.printResult(KRYO, ALL_PRIMITIVE_ARRAYS, result);
    }

    @Test
    public void benchmarkKryo_SimpleNestedObject() throws Throwable {
        System.out.println("\n=== Kryo - SimpleNestedObject ===");

        SimpleNestedObject[] objects = new SimpleNestedObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createSimpleNestedObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, SimpleNestedObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, SIMPLE_NESTED, result);
        resultManager.printResult(KRYO, SIMPLE_NESTED, result);
    }

    @Test
    public void benchmarkKryo_Rectangle() throws Throwable {
        System.out.println("\n=== Kryo - Rectangle ===");

        Rectangle[] objects = new Rectangle[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createRectangle(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, Rectangle.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, RECTANGLE, result);
        resultManager.printResult(KRYO, RECTANGLE, result);
    }

    @Test
    public void benchmarkKryo_DeepNestedLevel1() throws Throwable {
        System.out.println("\n=== Kryo - DeepNestedLevel1 ===");

        DeepNestedLevel1[] objects = new DeepNestedLevel1[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createDeepNestedLevel1(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, DeepNestedLevel1.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, DEEP_NESTED, result);
        resultManager.printResult(KRYO, DEEP_NESTED, result);
    }

    @Test
    public void benchmarkKryo_LargeStringObject() throws Throwable {
        System.out.println("\n=== Kryo - LargeStringObject ===");

        LargeStringObject[] objects = new LargeStringObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLargeStringObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, LargeStringObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, LARGE_STRING, result);
        resultManager.printResult(KRYO, LARGE_STRING, result);
    }

    @Test
    public void benchmarkKryo_LargeStringListObject() throws Throwable {
        System.out.println("\n=== Kryo - LargeStringListObject ===");

        LargeStringListObject[] objects = new LargeStringListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLargeStringListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, LargeStringListObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, LARGE_STRING_LIST, result);
        resultManager.printResult(KRYO, LARGE_STRING_LIST, result);
    }

    @Test
    public void benchmarkKryo_MixedSizeStringListObject() throws Throwable {
        System.out.println("\n=== Kryo - MixedSizeStringListObject ===");

        MixedSizeStringListObject[] objects = new MixedSizeStringListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createMixedSizeStringListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, MixedSizeStringListObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, MIXED_SIZE_STRING_LIST, result);
        resultManager.printResult(KRYO, MIXED_SIZE_STRING_LIST, result);
    }

    @Test
    public void benchmarkKryo_DocumentObject() throws Throwable {
        System.out.println("\n=== Kryo - DocumentObject ===");

        DocumentObject[] objects = new DocumentObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createDocumentObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, DocumentObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, DOCUMENT, result);
        resultManager.printResult(KRYO, DOCUMENT, result);
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

    @Test
    public void benchmarkFury_StringIntegerMapObject() throws Throwable {
        System.out.println("\n=== Fury - StringIntegerMapObject ===");

        StringIntegerMapObject[] objects = new StringIntegerMapObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createStringIntegerMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, STRING_INTEGER_MAP, result);
        resultManager.printResult(FURY, STRING_INTEGER_MAP, result);
    }

    @Test
    public void benchmarkFury_IntegerStringMapObject() throws Throwable {
        System.out.println("\n=== Fury - IntegerStringMapObject ===");

        IntegerStringMapObject[] objects = new IntegerStringMapObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createIntegerStringMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, INTEGER_STRING_MAP, result);
        resultManager.printResult(FURY, INTEGER_STRING_MAP, result);
    }

    @Test
    public void benchmarkFury_IntegerIntegerMapObject() throws Throwable {
        System.out.println("\n=== Fury - IntegerIntegerMapObject ===");

        IntegerIntegerMapObject[] objects = new IntegerIntegerMapObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createIntegerIntegerMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, INTEGER_INTEGER_MAP, result);
        resultManager.printResult(FURY, INTEGER_INTEGER_MAP, result);
    }

    @Test
    public void benchmarkFury_LongDoubleMapObject() throws Throwable {
        System.out.println("\n=== Fury - LongDoubleMapObject ===");

        LongDoubleMapObject[] objects = new LongDoubleMapObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLongDoubleMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, LONG_DOUBLE_MAP, result);
        resultManager.printResult(FURY, LONG_DOUBLE_MAP, result);
    }

    @Test
    public void benchmarkFury_IntArrayObject() throws Throwable {
        System.out.println("\n=== Fury - IntArrayObject ===");

        IntArrayObject[] objects = new IntArrayObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createIntArrayObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, INT_ARRAY, result);
        resultManager.printResult(FURY, INT_ARRAY, result);
    }

    @Test
    public void benchmarkFury_LongArrayObject() throws Throwable {
        System.out.println("\n=== Fury - LongArrayObject ===");

        LongArrayObject[] objects = new LongArrayObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLongArrayObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, LONG_ARRAY, result);
        resultManager.printResult(FURY, LONG_ARRAY, result);
    }

    @Test
    public void benchmarkFury_DoubleArrayObject() throws Throwable {
        System.out.println("\n=== Fury - DoubleArrayObject ===");

        DoubleArrayObject[] objects = new DoubleArrayObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createDoubleArrayObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, DOUBLE_ARRAY, result);
        resultManager.printResult(FURY, DOUBLE_ARRAY, result);
    }

    @Test
    public void benchmarkFury_AllPrimitiveArraysObject() throws Throwable {
        System.out.println("\n=== Fury - AllPrimitiveArraysObject ===");

        AllPrimitiveArraysObject[] objects = new AllPrimitiveArraysObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createAllPrimitiveArraysObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, ALL_PRIMITIVE_ARRAYS, result);
        resultManager.printResult(FURY, ALL_PRIMITIVE_ARRAYS, result);
    }

    @Test
    public void benchmarkFury_SimpleNestedObject() throws Throwable {
        System.out.println("\n=== Fury - SimpleNestedObject ===");

        SimpleNestedObject[] objects = new SimpleNestedObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createSimpleNestedObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, SIMPLE_NESTED, result);
        resultManager.printResult(FURY, SIMPLE_NESTED, result);
    }

    @Test
    public void benchmarkFury_Rectangle() throws Throwable {
        System.out.println("\n=== Fury - Rectangle ===");

        Rectangle[] objects = new Rectangle[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createRectangle(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, RECTANGLE, result);
        resultManager.printResult(FURY, RECTANGLE, result);
    }

    @Test
    public void benchmarkFury_DeepNestedLevel1() throws Throwable {
        System.out.println("\n=== Fury - DeepNestedLevel1 ===");

        DeepNestedLevel1[] objects = new DeepNestedLevel1[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createDeepNestedLevel1(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, DEEP_NESTED, result);
        resultManager.printResult(FURY, DEEP_NESTED, result);
    }

    @Test
    public void benchmarkFury_LargeStringObject() throws Throwable {
        System.out.println("\n=== Fury - LargeStringObject ===");

        LargeStringObject[] objects = new LargeStringObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLargeStringObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, LARGE_STRING, result);
        resultManager.printResult(FURY, LARGE_STRING, result);
    }

    @Test
    public void benchmarkFury_LargeStringListObject() throws Throwable {
        System.out.println("\n=== Fury - LargeStringListObject ===");

        LargeStringListObject[] objects = new LargeStringListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createLargeStringListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, LARGE_STRING_LIST, result);
        resultManager.printResult(FURY, LARGE_STRING_LIST, result);
    }

    @Test
    public void benchmarkFury_MixedSizeStringListObject() throws Throwable {
        System.out.println("\n=== Fury - MixedSizeStringListObject ===");

        MixedSizeStringListObject[] objects = new MixedSizeStringListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createMixedSizeStringListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, MIXED_SIZE_STRING_LIST, result);
        resultManager.printResult(FURY, MIXED_SIZE_STRING_LIST, result);
    }

    @Test
    public void benchmarkFury_DocumentObject() throws Throwable {
        System.out.println("\n=== Fury - DocumentObject ===");

        DocumentObject[] objects = new DocumentObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createDocumentObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, DOCUMENT, result);
        resultManager.printResult(FURY, DOCUMENT, result);
    }

    // ========== Benchmark Helper Methods ==========

    private <T> BenchmarkResultManager.SerializationResult benchmarkKalynxSerializer(
            KalynxSerializer serializer, T[] objects, int iterations) throws Throwable {

        long startSerialize = System.nanoTime();
        byte[] lastSerialized = null;
        for (int i = 0; i < iterations; i++) {
            lastSerialized = serializer.serialize(objects[i]);
        }
        long serializeTime = System.nanoTime() - startSerialize;

        long startDeserialize = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            serializer.deserialize(lastSerialized, objects[0].getClass());
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

    // ========== TypeReference Performance Tests ==========

    @Test
    void benchmark_typeReference_registration() throws Throwable {
        System.out.println("\n=== TypeReference Registration Benchmark ===");

        // Benchmark Class-based registration
        long classStart = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            KalynxSerializer serializer = new KalynxSerializer();
            serializer.register(IntegerListObject.class);
        }
        long classTime = (System.nanoTime() - classStart) / 1_000_000;

        // Benchmark TypeReference-based registration
        long typeRefStart = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            KalynxSerializer serializer = new KalynxSerializer();
            serializer.register(new com.kalynx.simplyabinaryserializer.TypeReference<IntegerListObject>() {});
        }
        long typeRefTime = (System.nanoTime() - typeRefStart) / 1_000_000;

        System.out.println("Class-based registration:       " + classTime + " ms");
        System.out.println("TypeReference-based registration: " + typeRefTime + " ms");
        System.out.println("Overhead: " + (typeRefTime - classTime) + " ms (" +
            String.format("%.1f%%", ((double)(typeRefTime - classTime) / classTime * 100)) + ")");
    }

    @Test
    void benchmark_typeReference_serialization() throws Throwable {
        System.out.println("\n=== TypeReference Serialization Benchmark ===");

        List<Integer> values = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        IntegerListObject[] objects = new IntegerListObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = new IntegerListObject(values);
        }

        // Benchmark Class-based serialization
        KalynxSerializer classSerializer = new KalynxSerializer();
        classSerializer.register(IntegerListObject.class);

        long classStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            classSerializer.serialize(objects[i]);
        }
        long classTime = (System.nanoTime() - classStart) / 1_000_000;

        // Benchmark TypeReference-based serialization
        KalynxSerializer typeRefSerializer = new KalynxSerializer();
        typeRefSerializer.register(new com.kalynx.simplyabinaryserializer.TypeReference<IntegerListObject>() {});

        long typeRefStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            typeRefSerializer.serialize(objects[i]);
        }
        long typeRefTime = (System.nanoTime() - typeRefStart) / 1_000_000;

        System.out.println("Class-based serialization:       " + classTime + " ms");
        System.out.println("TypeReference-based serialization: " + typeRefTime + " ms");
        System.out.println("Difference: " + Math.abs(typeRefTime - classTime) + " ms (" +
            String.format("%.1f%%", (Math.abs((double)(typeRefTime - classTime)) / classTime * 100)) + ")");
        System.out.println("Conclusion: " + (Math.abs(typeRefTime - classTime) < classTime * 0.05 ?
            "✅ TypeReference has negligible overhead" :
            "⚠️ TypeReference may have measurable overhead"));
    }

    @Test
    void benchmark_typeReference_deserialization() throws Throwable {
        System.out.println("\n=== TypeReference Deserialization Benchmark ===");

        List<Integer> values = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        IntegerListObject obj = new IntegerListObject(values);

        // Prepare serializers and serialized data
        KalynxSerializer classSerializer = new KalynxSerializer();
        classSerializer.register(IntegerListObject.class);
        byte[] bytes = classSerializer.serialize(obj);

        KalynxSerializer typeRefSerializer = new KalynxSerializer();
        com.kalynx.simplyabinaryserializer.TypeReference<IntegerListObject> typeRef =
            new com.kalynx.simplyabinaryserializer.TypeReference<IntegerListObject>() {};
        typeRefSerializer.register(typeRef);

        // Benchmark Class-based deserialization
        long classStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            classSerializer.deserialize(bytes, IntegerListObject.class);
        }
        long classTime = (System.nanoTime() - classStart) / 1_000_000;

        // Benchmark TypeReference-based deserialization
        long typeRefStart = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            typeRefSerializer.deserialize(bytes, typeRef);
        }
        long typeRefTime = (System.nanoTime() - typeRefStart) / 1_000_000;

        System.out.println("Class-based deserialization:       " + classTime + " ms");
        System.out.println("TypeReference-based deserialization: " + typeRefTime + " ms");
        System.out.println("Difference: " + Math.abs(typeRefTime - classTime) + " ms (" +
            String.format("%.1f%%", (Math.abs((double)(typeRefTime - classTime)) / classTime * 100)) + ")");
        System.out.println("Conclusion: " + (Math.abs(typeRefTime - classTime) < classTime * 0.05 ?
            "✅ TypeReference has negligible overhead" :
            "⚠️ TypeReference may have measurable overhead"));
    }

    @Test
    void benchmark_multiClass_registration() throws Throwable {
        System.out.println("\n=== Multi-Class Registration Benchmark ===");

        // Benchmark individual registration
        long individualStart = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            KalynxSerializer serializer = new KalynxSerializer();
            serializer.register(AllPrimitivesObject.class);
            serializer.register(IntegerListObject.class);
            serializer.register(StringIntegerMapObject.class);
        }
        long individualTime = (System.nanoTime() - individualStart) / 1_000_000;

        // Benchmark fluent registration
        long fluentStart = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            new KalynxSerializer()
                .register(AllPrimitivesObject.class)
                .register(IntegerListObject.class)
                .register(StringIntegerMapObject.class);
        }
        long fluentTime = (System.nanoTime() - fluentStart) / 1_000_000;

        System.out.println("Individual registration: " + individualTime + " ms");
        System.out.println("Fluent registration:     " + fluentTime + " ms");
        System.out.println("Performance: " + (individualTime == fluentTime ? "Identical" :
            (fluentTime < individualTime ? "Fluent is faster" : "Individual is faster")) +
            " (diff: " + Math.abs(fluentTime - individualTime) + " ms)");
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

