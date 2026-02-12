package com.kalynx.simplyabinaryserializer.benchmarking;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.kalynx.simplyabinaryserializer.GeneratedSerializer;
import com.kalynx.simplyabinaryserializer.TypedSerializer;
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
 * Extreme benchmark runner - executes 25 runs of 1M iterations each.
 * Generates comprehensive performance report with statistics.
 */
public class ExtremeBenchmarkRunnerTests {

    private static final int BENCHMARK_ITERATIONS = 1_000_000;

    // Library names
    private static final String TYPED_SERIALIZER = "TypedSerializer";
    private static final String GENERATED_SERIALIZER = "GeneratedSerializer";
    private static final String KRYO = "Kryo";
    private static final String JACKSON = "Jackson";
    private static final String GSON = "Gson";
    private static final String FURY = "Apache Fury";

    // Object type names
    private static final String SIMPLE_OBJECT = "SimpleObject";
    private static final String COMPLEX_OBJECT = "ComplexObject";
    private static final String DEEP_OBJECT = "DeepObject";

    private static final String[] LIBRARIES = {TYPED_SERIALIZER, GENERATED_SERIALIZER, KRYO, JACKSON, GSON, FURY};
    private static final String[] OBJECT_TYPES = {SIMPLE_OBJECT, COMPLEX_OBJECT, DEEP_OBJECT};

    private static BenchmarkResultManager resultManager;

    private TypedSerializer<SimpleObject> typedSimpleSerializer;
    private TypedSerializer<ComplexObject> typedComplexSerializer;
    private TypedSerializer<DeepObject> typedDeepSerializer;
    private GeneratedSerializer<SimpleObject> genSimpleSerializer;
    private GeneratedSerializer<ComplexObject> genComplexSerializer;
    private GeneratedSerializer<DeepObject> genDeepSerializer;
    private ObjectMapper jacksonMapper;
    private Gson gson;
    private ThreadLocal<Kryo> kryoThreadLocal;
    private Fury fury;

    @BeforeAll
    public static void initializeResultsCollection() throws Throwable {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("INITIALIZING BENCHMARK RESULTS COLLECTION");
        System.out.println("=".repeat(100));

        resultManager = new BenchmarkResultManager(LIBRARIES, OBJECT_TYPES);

        System.out.println("Initialized results collection for " + LIBRARIES.length + " libraries");
        System.out.println("Object types: " + SIMPLE_OBJECT + ", " + COMPLEX_OBJECT + ", " + DEEP_OBJECT);
        System.out.println("\n" + "=".repeat(100));
        System.out.println("WARMING UP JVM - This will take a moment...");
        System.out.println("=".repeat(100));

        performComprehensiveWarmup();

        System.out.println("Warmup complete!");
        System.out.println("=".repeat(100) + "\n");
    }

    private static void performComprehensiveWarmup() throws Throwable {
        // Create temporary serializers for warmup
        TypedSerializer<SimpleObject> typedSimple = new TypedSerializer<>(SimpleObject.class);
        TypedSerializer<ComplexObject> typedComplex = new TypedSerializer<>(ComplexObject.class);
        TypedSerializer<DeepObject> typedDeep = new TypedSerializer<>(DeepObject.class);
        GeneratedSerializer<SimpleObject> genSimple = new GeneratedSerializer<>(SimpleObject.class);
        GeneratedSerializer<ComplexObject> genComplex = new GeneratedSerializer<>(ComplexObject.class);
        GeneratedSerializer<DeepObject> genDeep = new GeneratedSerializer<>(DeepObject.class);

        ObjectMapper jackson = new ObjectMapper();
        Gson gson = new Gson();

        ThreadLocal<Kryo> kryoTL = ThreadLocal.withInitial(() -> {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryo.register(SimpleObject.class);
            kryo.register(ComplexObject.class);
            kryo.register(DeepObject.class);
            kryo.register(HashMap.class);
            kryo.register(ArrayList.class);
            return kryo;
        });

        Fury fury = Fury.builder()
            .withLanguage(Language.JAVA)
            .requireClassRegistration(false)
            .withRefTracking(true)
            .build();
        fury.register(SimpleObject.class);
        fury.register(ComplexObject.class);
        fury.register(DeepObject.class);
        fury.register(NestedObject.class);

        System.out.println("  Warming up with SimpleObject (10k iterations)...");
        for (int i = 0; i < 10_000; i++) {
            SimpleObject obj = createSimpleObject(i);

            // Warm up all serializers
            byte[] data = typedSimple.serialize(obj);
            typedSimple.deserialize(data);

            data = genSimple.serialize(obj);
            genSimple.deserialize(data);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.esotericsoftware.kryo.io.Output output = new com.esotericsoftware.kryo.io.Output(baos);
            kryoTL.get().writeObject(output, obj);
            output.close();
            com.esotericsoftware.kryo.io.Input input = new com.esotericsoftware.kryo.io.Input(new ByteArrayInputStream(baos.toByteArray()));
            kryoTL.get().readObject(input, SimpleObject.class);
            input.close();

            byte[] jdata = jackson.writeValueAsBytes(obj);
            jackson.readValue(jdata, SimpleObject.class);

            String gdata = gson.toJson(obj);
            gson.fromJson(gdata, SimpleObject.class);

            byte[] fdata = fury.serialize(obj);
            fury.deserialize(fdata);
        }

        System.out.println("  Warming up with ComplexObject (5k iterations)...");
        for (int i = 0; i < 5_000; i++) {
            ComplexObject obj = createComplexObject(i);

            byte[] data = typedComplex.serialize(obj);
            typedComplex.deserialize(data);

            data = genComplex.serialize(obj);
            genComplex.deserialize(data);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.esotericsoftware.kryo.io.Output output = new com.esotericsoftware.kryo.io.Output(baos);
            kryoTL.get().writeObject(output, obj);
            output.close();
            com.esotericsoftware.kryo.io.Input input = new com.esotericsoftware.kryo.io.Input(new ByteArrayInputStream(baos.toByteArray()));
            kryoTL.get().readObject(input, ComplexObject.class);
            input.close();

            byte[] jdata = jackson.writeValueAsBytes(obj);
            jackson.readValue(jdata, ComplexObject.class);

            String gdata = gson.toJson(obj);
            gson.fromJson(gdata, ComplexObject.class);

            byte[] fdata = fury.serialize(obj);
            fury.deserialize(fdata);
        }

        System.out.println("  Warming up with DeepObject (2k iterations)...");
        for (int i = 0; i < 2_000; i++) {
            DeepObject obj = createDeepObject(i, 5);

            byte[] data = typedDeep.serialize(obj);
            typedDeep.deserialize(data);

            data = genDeep.serialize(obj);
            genDeep.deserialize(data);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.esotericsoftware.kryo.io.Output output = new com.esotericsoftware.kryo.io.Output(baos);
            kryoTL.get().writeObject(output, obj);
            output.close();
            com.esotericsoftware.kryo.io.Input input = new com.esotericsoftware.kryo.io.Input(new ByteArrayInputStream(baos.toByteArray()));
            kryoTL.get().readObject(input, DeepObject.class);
            input.close();

            byte[] jdata = jackson.writeValueAsBytes(obj);
            jackson.readValue(jdata, DeepObject.class);

            String gdata = gson.toJson(obj);
            gson.fromJson(gdata, DeepObject.class);

            byte[] fdata = fury.serialize(obj);
            fury.deserialize(fdata);
        }

        System.gc();
        Thread.sleep(100);
    }

    private static SimpleObject createSimpleObject(int seed) {
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

    private static ComplexObject createComplexObject(int seed) {
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

    private static DeepObject createDeepObject(int seed, int depth) {
        DeepObject obj = new DeepObject();
        obj.id = seed + depth * 10;
        obj.name = "Level" + depth + "_" + seed;
        obj.value = 1.5 * depth + seed;

        if (depth > 1) {
            obj.child = createDeepObject(seed, depth - 1);
        }

        return obj;
    }

    @BeforeEach
    public void setup() {
        typedSimpleSerializer = new TypedSerializer<>(SimpleObject.class);
        typedComplexSerializer = new TypedSerializer<>(ComplexObject.class);
        typedDeepSerializer = new TypedSerializer<>(DeepObject.class);
        genSimpleSerializer = new GeneratedSerializer<>(SimpleObject.class);
        genComplexSerializer = new GeneratedSerializer<>(ComplexObject.class);
        genDeepSerializer = new GeneratedSerializer<>(DeepObject.class);
        jacksonMapper = new ObjectMapper();
        gson = new Gson();

        kryoThreadLocal = ThreadLocal.withInitial(() -> {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryo.register(SimpleObject.class);
            kryo.register(ComplexObject.class);
            kryo.register(DeepObject.class);
            kryo.register(HashMap.class);
            kryo.register(ArrayList.class);
            return kryo;
        });


        // Apache Fury setup with FFM enabled for maximum performance
        fury = Fury.builder()
            .withLanguage(Language.JAVA)
            .requireClassRegistration(false)
            .withRefTracking(true)  // Enable reference tracking to handle nested objects correctly
            .build();
        fury.register(SimpleObject.class);
        fury.register(ComplexObject.class);
        fury.register(DeepObject.class);
        fury.register(NestedObject.class);
    }

    // ========================================================================
    // REUSABLE SERIALIZATION/DESERIALIZATION METHODS
    // ========================================================================

    private <T> BenchmarkResultManager.SerializationResult benchmarkTypedSerializer(TypedSerializer<T> serializer, T[] objects, int iterations) throws Throwable {
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = serializer.serialize(objects[i]);
        }
        long serTime = System.nanoTime() - serStart;

        long desStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            serializer.deserialize(data[i]);
        }
        long desTime = System.nanoTime() - desStart;

        return new BenchmarkResultManager.SerializationResult(serTime, desTime, data[0].length, iterations);
    }

    private <T> BenchmarkResultManager.SerializationResult benchmarkGeneratedSerializer(GeneratedSerializer<T> serializer, T[] objects, int iterations) throws Throwable {
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = serializer.serialize(objects[i]);
        }
        long serTime = System.nanoTime() - serStart;

        long desStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            serializer.deserialize(data[i]);
        }
        long desTime = System.nanoTime() - desStart;

        return new BenchmarkResultManager.SerializationResult(serTime, desTime, data[0].length, iterations);
    }

    private <T> BenchmarkResultManager.SerializationResult benchmarkKryo(T[] objects, Class<T> clazz, int iterations) throws Throwable {
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Output output = new Output(baos);
            kryoThreadLocal.get().writeObject(output, objects[i]);
            output.close();
            data[i] = baos.toByteArray();
        }
        long serTime = System.nanoTime() - serStart;

        long desStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Input input = new Input(new ByteArrayInputStream(data[i]));
            kryoThreadLocal.get().readObject(input, clazz);
            input.close();
        }
        long desTime = System.nanoTime() - desStart;

        return new BenchmarkResultManager.SerializationResult(serTime, desTime, data[0].length, iterations);
    }

    private <T> BenchmarkResultManager.SerializationResult benchmarkJackson(T[] objects, Class<T> clazz, int iterations) throws Throwable {
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = jacksonMapper.writeValueAsBytes(objects[i]);
        }
        long serTime = System.nanoTime() - serStart;

        long desStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            jacksonMapper.readValue(data[i], clazz);
        }
        long desTime = System.nanoTime() - desStart;

        return new BenchmarkResultManager.SerializationResult(serTime, desTime, data[0].length, iterations);
    }

    private <T> BenchmarkResultManager.SerializationResult benchmarkGson(T[] objects, Class<T> clazz, int iterations) throws Throwable {
        String[] data = new String[iterations];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = gson.toJson(objects[i]);
        }
        long serTime = System.nanoTime() - serStart;

        long desStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            gson.fromJson(data[i], clazz);
        }
        long desTime = System.nanoTime() - desStart;

        return new BenchmarkResultManager.SerializationResult(serTime, desTime, data[0].getBytes().length, iterations);
    }

    private <T> BenchmarkResultManager.SerializationResult benchmarkFury(T[] objects, int iterations) throws Throwable {
        byte[][] data = new byte[iterations][];

        long serStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            data[i] = fury.serialize(objects[i]);
        }
        long serTime = System.nanoTime() - serStart;

        long desStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            fury.deserialize(data[i]);
        }
        long desTime = System.nanoTime() - desStart;

        return new BenchmarkResultManager.SerializationResult(serTime, desTime, data[0].length, iterations);
    }

    // ========================================================================
    // INDIVIDUAL TEST METHODS (ONE PER LIBRARY PER OBJECT TYPE)
    // ========================================================================

    // ---- TypedSerializer Tests ----

    @Test
    public void benchmarkTypedSerializer_SimpleObject() throws Throwable {
        System.out.println("\n=== TypedSerializer - SimpleObject ===");

        SimpleObject[] objects = new SimpleObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createSimpleObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkTypedSerializer(typedSimpleSerializer, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(TYPED_SERIALIZER, SIMPLE_OBJECT, result);
        resultManager.printResult("TypedSerializer", "SimpleObject", result);
    }

    @Test
    public void benchmarkTypedSerializer_ComplexObject() throws Throwable {
        System.out.println("\n=== TypedSerializer - ComplexObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        ComplexObject[] objects = new ComplexObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkTypedSerializer(typedComplexSerializer, objects, iterations);
        resultManager.recordResult(TYPED_SERIALIZER, COMPLEX_OBJECT, result);
        resultManager.printResult("TypedSerializer", "ComplexObject", result);
    }

    @Test
    public void benchmarkTypedSerializer_DeepObject() throws Throwable {
        System.out.println("\n=== TypedSerializer - DeepObject (5 levels) ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        DeepObject[] objects = new DeepObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createDeepObject(i, 5);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkTypedSerializer(typedDeepSerializer, objects, iterations);
        resultManager.recordResult(TYPED_SERIALIZER, DEEP_OBJECT, result);
        resultManager.printResult("TypedSerializer", "DeepObject", result);
    }

    // ---- GeneratedSerializer Tests ----

    @Test
    public void benchmarkGeneratedSerializer_SimpleObject() throws Throwable {
        System.out.println("\n=== GeneratedSerializer - SimpleObject ===");

        SimpleObject[] objects = new SimpleObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createSimpleObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkGeneratedSerializer(genSimpleSerializer, objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(GENERATED_SERIALIZER, SIMPLE_OBJECT, result);
        resultManager.printResult("GeneratedSerializer", "SimpleObject", result);
    }

    @Test
    public void benchmarkGeneratedSerializer_ComplexObject() throws Throwable {
        System.out.println("\n=== GeneratedSerializer - ComplexObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        ComplexObject[] objects = new ComplexObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkGeneratedSerializer(genComplexSerializer, objects, iterations);
        resultManager.recordResult(GENERATED_SERIALIZER, COMPLEX_OBJECT, result);
        resultManager.printResult("GeneratedSerializer", "ComplexObject", result);
    }

    @Test
    public void benchmarkGeneratedSerializer_DeepObject() throws Throwable {
        System.out.println("\n=== GeneratedSerializer - DeepObject (5 levels) ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        DeepObject[] objects = new DeepObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createDeepObject(i, 5);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkGeneratedSerializer(genDeepSerializer, objects, iterations);
        resultManager.recordResult(GENERATED_SERIALIZER, DEEP_OBJECT, result);
        resultManager.printResult("GeneratedSerializer", "DeepObject", result);
    }

    // ---- Kryo Tests ----

    @Test
    public void benchmarkKryo_SimpleObject() throws Throwable {
        System.out.println("\n=== Kryo - SimpleObject ===");

        SimpleObject[] objects = new SimpleObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createSimpleObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, SimpleObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(KRYO, SIMPLE_OBJECT, result);
        resultManager.printResult("Kryo", "SimpleObject", result);
    }

    @Test
    public void benchmarkKryo_ComplexObject() throws Throwable {
        System.out.println("\n=== Kryo - ComplexObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        ComplexObject[] objects = new ComplexObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, ComplexObject.class, iterations);
        resultManager.recordResult(KRYO, COMPLEX_OBJECT, result);
        resultManager.printResult("Kryo", "ComplexObject", result);
    }

    @Test
    public void benchmarkKryo_DeepObject() throws Throwable {
        System.out.println("\n=== Kryo - DeepObject (5 levels) ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        DeepObject[] objects = new DeepObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createDeepObject(i, 5);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, DeepObject.class, iterations);
        resultManager.recordResult(KRYO, DEEP_OBJECT, result);
        resultManager.printResult("Kryo", "DeepObject", result);
    }

    // ---- Jackson Tests ----

    @Test
    public void benchmarkJackson_SimpleObject() throws Throwable {
        System.out.println("\n=== Jackson - SimpleObject ===");

        SimpleObject[] objects = new SimpleObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createSimpleObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkJackson(objects, SimpleObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(JACKSON, SIMPLE_OBJECT, result);
        resultManager.printResult("Jackson", "SimpleObject", result);
    }

    @Test
    public void benchmarkJackson_ComplexObject() throws Throwable {
        System.out.println("\n=== Jackson - ComplexObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        ComplexObject[] objects = new ComplexObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkJackson(objects, ComplexObject.class, iterations);
        resultManager.recordResult(JACKSON, COMPLEX_OBJECT, result);
        resultManager.printResult("Jackson", "ComplexObject", result);
    }

    @Test
    public void benchmarkJackson_DeepObject() throws Throwable {
        System.out.println("\n=== Jackson - DeepObject (5 levels) ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        DeepObject[] objects = new DeepObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createDeepObject(i, 5);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkJackson(objects, DeepObject.class, iterations);
        resultManager.recordResult(JACKSON, DEEP_OBJECT, result);
        resultManager.printResult("Jackson", "DeepObject", result);
    }

    // ---- Gson Tests ----

    @Test
    public void benchmarkGson_SimpleObject() throws Throwable {
        System.out.println("\n=== Gson - SimpleObject ===");

        SimpleObject[] objects = new SimpleObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createSimpleObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkGson(objects, SimpleObject.class, BENCHMARK_ITERATIONS);
        resultManager.recordResult(GSON, SIMPLE_OBJECT, result);
        resultManager.printResult("Gson", "SimpleObject", result);
    }

    @Test
    public void benchmarkGson_ComplexObject() throws Throwable {
        System.out.println("\n=== Gson - ComplexObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        ComplexObject[] objects = new ComplexObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkGson(objects, ComplexObject.class, iterations);
        resultManager.recordResult(GSON, COMPLEX_OBJECT, result);
        resultManager.printResult("Gson", "ComplexObject", result);
    }

    @Test
    public void benchmarkGson_DeepObject() throws Throwable {
        System.out.println("\n=== Gson - DeepObject (5 levels) ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        DeepObject[] objects = new DeepObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createDeepObject(i, 5);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkGson(objects, DeepObject.class, iterations);
        resultManager.recordResult(GSON, DEEP_OBJECT, result);
        resultManager.printResult("Gson", "DeepObject", result);
    }

    // ---- Fury Tests ----

    @Test
    public void benchmarkFury_SimpleObject() throws Throwable {
        System.out.println("\n=== Apache Fury - SimpleObject ===");

        SimpleObject[] objects = new SimpleObject[BENCHMARK_ITERATIONS];
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            objects[i] = createSimpleObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, BENCHMARK_ITERATIONS);
        resultManager.recordResult(FURY, SIMPLE_OBJECT, result);
        resultManager.printResult("Apache Fury", "SimpleObject", result);
    }

    @Test
    public void benchmarkFury_ComplexObject() throws Throwable {
        System.out.println("\n=== Apache Fury - ComplexObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        ComplexObject[] objects = new ComplexObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, iterations);
        resultManager.recordResult(FURY, COMPLEX_OBJECT, result);
        resultManager.printResult("Apache Fury", "ComplexObject", result);
    }

    @Test
    public void benchmarkFury_DeepObject() throws Throwable {
        System.out.println("\n=== Apache Fury - DeepObject (5 levels) ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        DeepObject[] objects = new DeepObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createDeepObject(i, 5);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, iterations);
        resultManager.recordResult(FURY, DEEP_OBJECT, result);
        resultManager.printResult("Apache Fury", "DeepObject", result);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    @AfterAll
    public static void generateFinalReport() throws Throwable {
        resultManager.generateFinalReport(LIBRARIES, OBJECT_TYPES);
    }


    // Data classes
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

    public static class DeepObject {
        public int id;
        public String name;
        public double value;
        public DeepObject child;
        public DeepObject() {}
    }
}

