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
import org.apache.fury.config.FuryBuilder;
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

    private static final int BENCHMARK_ITERATIONS = 10_000_000;

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
    private static final String LIST_VARIETY = "ListVarietyObject";
    private static final String COMPLEX_LIST = "ComplexListObject";
    private static final String NULLABLE = "NullableObject";
    private static final String ENUM_OBJECT = "EnumObject";
    private static final String COMPLEX_MAP = "ComplexMapObject";
    private static final String MIXED_COMPLEXITY = "MixedComplexityObject";

    private static final String[] LIBRARIES = {TYPED_SERIALIZER, GENERATED_SERIALIZER, KRYO, FURY};
    private static final String[] OBJECT_TYPES = {
        SIMPLE_OBJECT, COMPLEX_OBJECT, DEEP_OBJECT,
        LIST_VARIETY, COMPLEX_LIST, NULLABLE,
        ENUM_OBJECT, COMPLEX_MAP, MIXED_COMPLEXITY
    };

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
            kryo.register(ListVarietyObject.class);
            kryo.register(ComplexListObject.class);
            kryo.register(NullableObject.class);
            kryo.register(EnumObject.class);
            kryo.register(ComplexMapObject.class);
            kryo.register(MixedComplexityObject.class);
            kryo.register(NestedObject.class);
            kryo.register(Priority.class);
            kryo.register(Status.class);
            kryo.register(HashMap.class);
            kryo.register(ArrayList.class);
            return kryo;
        });

        Fury fury = Fury.builder()
            .withLanguage(Language.JAVA)
            .requireClassRegistration(false)
            .withRefTracking(true)
            .withCodegen(false)  // DISABLE JIT code generation and Unsafe usage
            .build();
        fury.register(SimpleObject.class);
        fury.register(ComplexObject.class);
        fury.register(DeepObject.class);
        fury.register(ListVarietyObject.class);
        fury.register(ComplexListObject.class);
        fury.register(NullableObject.class);
        fury.register(EnumObject.class);
        fury.register(ComplexMapObject.class);
        fury.register(MixedComplexityObject.class);
        fury.register(NestedObject.class);
        fury.register(Priority.class);
        fury.register(Status.class);
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

    private static ListVarietyObject createListVarietyObject(int seed) {
        ListVarietyObject obj = new ListVarietyObject();
        obj.integers = new ArrayList<>();
        for (int i = 0; i < 10; i++) obj.integers.add(seed + i);

        obj.longs = new ArrayList<>();
        for (int i = 0; i < 10; i++) obj.longs.add((long)(seed + i) * 1000);

        obj.strings = new ArrayList<>();
        for (int i = 0; i < 10; i++) obj.strings.add("Item" + (seed + i));

        obj.doubles = new ArrayList<>();
        for (int i = 0; i < 10; i++) obj.doubles.add(3.14 * (seed + i));

        obj.booleans = new ArrayList<>();
        for (int i = 0; i < 10; i++) obj.booleans.add(i % 2 == 0);

        return obj;
    }

    private static ComplexListObject createComplexListObject(int seed) {
        ComplexListObject obj = new ComplexListObject();
        obj.id = "CL" + seed;

        obj.items = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            NestedObject nested = new NestedObject();
            nested.id = seed * 10 + i;
            nested.name = "Nested" + i;
            nested.value = 1.5 * i;
            obj.items.add(nested);
        }

        obj.simpleItems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            obj.simpleItems.add(createSimpleObject(seed * 10 + i));
        }

        return obj;
    }

    private static NullableObject createNullableObject(int seed) {
        NullableObject obj = new NullableObject();
        obj.requiredField = "Required" + seed;

        // Randomly make fields null (50% null rate)
        if (seed % 2 == 0) obj.optionalField1 = "Optional1_" + seed;
        if (seed % 3 == 0) obj.optionalField2 = "Optional2_" + seed;
        if (seed % 4 == 0) obj.optionalInt = seed * 100;

        if (seed % 5 == 0) {
            obj.optionalNested = new NestedObject();
            obj.optionalNested.id = seed;
            obj.optionalNested.name = "OptionalNested";
            obj.optionalNested.value = 42.0;
        }

        if (seed % 6 == 0) {
            obj.optionalList = new ArrayList<>();
            obj.optionalList.add("A");
            obj.optionalList.add("B");
        }

        if (seed % 7 == 0) {
            obj.optionalMap = new HashMap<>();
            obj.optionalMap.put("key1", 1);
            obj.optionalMap.put("key2", 2);
        }

        return obj;
    }

    private static EnumObject createEnumObject(int seed) {
        EnumObject obj = new EnumObject();
        obj.id = seed;
        obj.name = "Task" + seed;
        obj.priority = Priority.values()[seed % Priority.values().length];
        obj.status = Status.values()[seed % Status.values().length];

        obj.priorityHistory = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            obj.priorityHistory.add(Priority.values()[(seed + i) % Priority.values().length]);
        }

        return obj;
    }

    private static ComplexMapObject createComplexMapObject(int seed) {
        ComplexMapObject obj = new ComplexMapObject();
        obj.id = "CM" + seed;

        obj.objectMap = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            NestedObject nested = new NestedObject();
            nested.id = i;
            nested.name = "MapNested" + i;
            nested.value = 2.5 * i;
            obj.objectMap.put("obj" + i, nested);
        }

        obj.listMap = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            List<Integer> list = new ArrayList<>();
            for (int j = 0; j < 4; j++) list.add((i * 10) + j);
            obj.listMap.put("list" + i, list);
        }

        obj.intKeyMap = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            obj.intKeyMap.put(seed * 10 + i, "Value" + i);
        }

        return obj;
    }

    private static MixedComplexityObject createMixedComplexityObject(int seed) {
        MixedComplexityObject obj = new MixedComplexityObject();
        obj.id = seed;
        obj.name = "Mixed" + seed;
        obj.priority = Priority.values()[seed % Priority.values().length];

        obj.nestedList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            NestedObject nested = new NestedObject();
            nested.id = i;
            nested.name = "N" + i;
            nested.value = 1.1 * i;
            obj.nestedList.add(nested);
        }

        obj.simpleMap = new HashMap<>();
        obj.simpleMap.put("a", 1);
        obj.simpleMap.put("b", 2);
        obj.simpleMap.put("c", 3);

        obj.complexMap = new HashMap<>();
        NestedObject n1 = new NestedObject();
        n1.id = 1;
        n1.name = "CM1";
        n1.value = 5.5;
        obj.complexMap.put("first", n1);

        obj.singleNested = new NestedObject();
        obj.singleNested.id = 99;
        obj.singleNested.name = "Single";
        obj.singleNested.value = 3.14;

        // Leave nullableField as null (50% of the time)
        if (seed % 2 == 0) {
            obj.nullableField = "NotNull" + seed;
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
            kryo.register(ListVarietyObject.class);
            kryo.register(ComplexListObject.class);
            kryo.register(NullableObject.class);
            kryo.register(EnumObject.class);
            kryo.register(ComplexMapObject.class);
            kryo.register(MixedComplexityObject.class);
            kryo.register(NestedObject.class);
            kryo.register(Priority.class);
            kryo.register(Status.class);
            kryo.register(HashMap.class);
            kryo.register(ArrayList.class);
            return kryo;
        });


        // Apache Fury setup WITHOUT Unsafe for fair comparison
        fury = Fury.builder()
            .withLanguage(Language.JAVA)
            .requireClassRegistration(false)
            .withRefTracking(true)  // Enable reference tracking to handle nested objects correctly
            .withCodegen(false)     // DISABLE JIT code generation and Unsafe usage for fair comparison
            .build();
        fury.register(SimpleObject.class);
        fury.register(ComplexObject.class);
        fury.register(DeepObject.class);
        fury.register(ListVarietyObject.class);
        fury.register(ComplexListObject.class);
        fury.register(NullableObject.class);
        fury.register(EnumObject.class);
        fury.register(ComplexMapObject.class);
        fury.register(MixedComplexityObject.class);
        fury.register(NestedObject.class);
        fury.register(Priority.class);
        fury.register(Status.class);
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

    // @Test  // Disabled - focusing on high-performance serializers
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

    // @Test  // Disabled - focusing on high-performance serializers
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

    // @Test  // Disabled - focusing on high-performance serializers
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

    // @Test  // Disabled - focusing on high-performance serializers
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

    // @Test  // Disabled - focusing on high-performance serializers
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

    // @Test  // Disabled - focusing on high-performance serializers
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
    // NEW COMPREHENSIVE SCENARIO TESTS (GeneratedSerializer only for speed)
    // ========================================================================

    @Test
    public void benchmarkGeneratedSerializer_ListVarietyObject() throws Throwable {
        System.out.println("\n=== GeneratedSerializer - ListVarietyObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        ListVarietyObject[] objects = new ListVarietyObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createListVarietyObject(i);
        }

        GeneratedSerializer<ListVarietyObject> serializer = new GeneratedSerializer<>(ListVarietyObject.class);
        BenchmarkResultManager.SerializationResult result = benchmarkGeneratedSerializer(serializer, objects, iterations);
        resultManager.recordResult(GENERATED_SERIALIZER, LIST_VARIETY, result);
        resultManager.printResult("GeneratedSerializer", "ListVarietyObject", result);
    }

    @Test
    public void benchmarkTypedSerializer_ListVarietyObject() throws Throwable {
        System.out.println("\n=== TypedSerializer - ListVarietyObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        ListVarietyObject[] objects = new ListVarietyObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createListVarietyObject(i);
        }

        TypedSerializer<ListVarietyObject> serializer = new TypedSerializer<>(ListVarietyObject.class);
        BenchmarkResultManager.SerializationResult result = benchmarkTypedSerializer(serializer, objects, iterations);
        resultManager.recordResult(TYPED_SERIALIZER, LIST_VARIETY, result);
        resultManager.printResult("TypedSerializer", "ListVarietyObject", result);
    }

    @Test
    public void benchmarkKryo_ListVarietyObject() throws Throwable {
        System.out.println("\n=== Kryo - ListVarietyObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        ListVarietyObject[] objects = new ListVarietyObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createListVarietyObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, ListVarietyObject.class, iterations);
        resultManager.recordResult(KRYO, LIST_VARIETY, result);
        resultManager.printResult("Kryo", "ListVarietyObject", result);
    }

    // @Test  // Disabled - focusing on high-performance serializers
    public void benchmarkJackson_ListVarietyObject() throws Throwable {
        System.out.println("\n=== Jackson - ListVarietyObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        ListVarietyObject[] objects = new ListVarietyObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createListVarietyObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkJackson(objects, ListVarietyObject.class, iterations);
        resultManager.recordResult(JACKSON, LIST_VARIETY, result);
        resultManager.printResult("Jackson", "ListVarietyObject", result);
    }

    // @Test  // Disabled - focusing on high-performance serializers
    public void benchmarkGson_ListVarietyObject() throws Throwable {
        System.out.println("\n=== Gson - ListVarietyObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        ListVarietyObject[] objects = new ListVarietyObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createListVarietyObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkGson(objects, ListVarietyObject.class, iterations);
        resultManager.recordResult(GSON, LIST_VARIETY, result);
        resultManager.printResult("Gson", "ListVarietyObject", result);
    }

    @Test
    public void benchmarkFury_ListVarietyObject() throws Throwable {
        System.out.println("\n=== Apache Fury - ListVarietyObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        ListVarietyObject[] objects = new ListVarietyObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createListVarietyObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, iterations);
        resultManager.recordResult(FURY, LIST_VARIETY, result);
        resultManager.printResult("Apache Fury", "ListVarietyObject", result);
    }

    @Test
    public void benchmarkGeneratedSerializer_ComplexListObject() throws Throwable {
        System.out.println("\n=== GeneratedSerializer - ComplexListObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        ComplexListObject[] objects = new ComplexListObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexListObject(i);
        }

        GeneratedSerializer<ComplexListObject> serializer = new GeneratedSerializer<>(ComplexListObject.class);
        BenchmarkResultManager.SerializationResult result = benchmarkGeneratedSerializer(serializer, objects, iterations);
        resultManager.recordResult(GENERATED_SERIALIZER, COMPLEX_LIST, result);
        resultManager.printResult("GeneratedSerializer", "ComplexListObject", result);
    }

    @Test
    public void benchmarkTypedSerializer_ComplexListObject() throws Throwable {
        System.out.println("\n=== TypedSerializer - ComplexListObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        ComplexListObject[] objects = new ComplexListObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexListObject(i);
        }

        TypedSerializer<ComplexListObject> serializer = new TypedSerializer<>(ComplexListObject.class);
        BenchmarkResultManager.SerializationResult result = benchmarkTypedSerializer(serializer, objects, iterations);
        resultManager.recordResult(TYPED_SERIALIZER, COMPLEX_LIST, result);
        resultManager.printResult("TypedSerializer", "ComplexListObject", result);
    }

    @Test
    public void benchmarkKryo_ComplexListObject() throws Throwable {
        System.out.println("\n=== Kryo - ComplexListObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        ComplexListObject[] objects = new ComplexListObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, ComplexListObject.class, iterations);
        resultManager.recordResult(KRYO, COMPLEX_LIST, result);
        resultManager.printResult("Kryo", "ComplexListObject", result);
    }

    // @Test  // Disabled - focusing on high-performance serializers
    public void benchmarkJackson_ComplexListObject() throws Throwable {
        System.out.println("\n=== Jackson - ComplexListObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        ComplexListObject[] objects = new ComplexListObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkJackson(objects, ComplexListObject.class, iterations);
        resultManager.recordResult(JACKSON, COMPLEX_LIST, result);
        resultManager.printResult("Jackson", "ComplexListObject", result);
    }

    // @Test  // Disabled - focusing on high-performance serializers
    public void benchmarkGson_ComplexListObject() throws Throwable {
        System.out.println("\n=== Gson - ComplexListObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        ComplexListObject[] objects = new ComplexListObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkGson(objects, ComplexListObject.class, iterations);
        resultManager.recordResult(GSON, COMPLEX_LIST, result);
        resultManager.printResult("Gson", "ComplexListObject", result);
    }

    @Test
    public void benchmarkFury_ComplexListObject() throws Throwable {
        System.out.println("\n=== Apache Fury - ComplexListObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        ComplexListObject[] objects = new ComplexListObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexListObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, iterations);
        resultManager.recordResult(FURY, COMPLEX_LIST, result);
        resultManager.printResult("Apache Fury", "ComplexListObject", result);
    }

    @Test
    public void benchmarkGeneratedSerializer_NullableObject() throws Throwable {
        System.out.println("\n=== GeneratedSerializer - NullableObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        NullableObject[] objects = new NullableObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createNullableObject(i);
        }

        GeneratedSerializer<NullableObject> serializer = new GeneratedSerializer<>(NullableObject.class);
        BenchmarkResultManager.SerializationResult result = benchmarkGeneratedSerializer(serializer, objects, iterations);
        resultManager.recordResult(GENERATED_SERIALIZER, NULLABLE, result);
        resultManager.printResult("GeneratedSerializer", "NullableObject", result);
    }

    @Test
    public void benchmarkTypedSerializer_NullableObject() throws Throwable {
        System.out.println("\n=== TypedSerializer - NullableObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        NullableObject[] objects = new NullableObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createNullableObject(i);
        }

        TypedSerializer<NullableObject> serializer = new TypedSerializer<>(NullableObject.class);
        BenchmarkResultManager.SerializationResult result = benchmarkTypedSerializer(serializer, objects, iterations);
        resultManager.recordResult(TYPED_SERIALIZER, NULLABLE, result);
        resultManager.printResult("TypedSerializer", "NullableObject", result);
    }

    @Test
    public void benchmarkKryo_NullableObject() throws Throwable {
        System.out.println("\n=== Kryo - NullableObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        NullableObject[] objects = new NullableObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createNullableObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, NullableObject.class, iterations);
        resultManager.recordResult(KRYO, NULLABLE, result);
        resultManager.printResult("Kryo", "NullableObject", result);
    }

    @Test
    public void benchmarkFury_NullableObject() throws Throwable {
        System.out.println("\n=== Apache Fury - NullableObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        NullableObject[] objects = new NullableObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createNullableObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, iterations);
        resultManager.recordResult(FURY, NULLABLE, result);
        resultManager.printResult("Apache Fury", "NullableObject", result);
    }

    @Test
    public void benchmarkGeneratedSerializer_EnumObject() throws Throwable {
        System.out.println("\n=== GeneratedSerializer - EnumObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        EnumObject[] objects = new EnumObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createEnumObject(i);
        }

        GeneratedSerializer<EnumObject> serializer = new GeneratedSerializer<>(EnumObject.class);
        BenchmarkResultManager.SerializationResult result = benchmarkGeneratedSerializer(serializer, objects, iterations);
        resultManager.recordResult(GENERATED_SERIALIZER, ENUM_OBJECT, result);
        resultManager.printResult("GeneratedSerializer", "EnumObject", result);
    }

    @Test
    public void benchmarkTypedSerializer_EnumObject() throws Throwable {
        System.out.println("\n=== TypedSerializer - EnumObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        EnumObject[] objects = new EnumObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createEnumObject(i);
        }

        TypedSerializer<EnumObject> serializer = new TypedSerializer<>(EnumObject.class);
        BenchmarkResultManager.SerializationResult result = benchmarkTypedSerializer(serializer, objects, iterations);
        resultManager.recordResult(TYPED_SERIALIZER, ENUM_OBJECT, result);
        resultManager.printResult("TypedSerializer", "EnumObject", result);
    }

    @Test
    public void benchmarkKryo_EnumObject() throws Throwable {
        System.out.println("\n=== Kryo - EnumObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        EnumObject[] objects = new EnumObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createEnumObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, EnumObject.class, iterations);
        resultManager.recordResult(KRYO, ENUM_OBJECT, result);
        resultManager.printResult("Kryo", "EnumObject", result);
    }

    @Test
    public void benchmarkFury_EnumObject() throws Throwable {
        System.out.println("\n=== Apache Fury - EnumObject ===");

        int iterations = BENCHMARK_ITERATIONS / 5;
        EnumObject[] objects = new EnumObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createEnumObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, iterations);
        resultManager.recordResult(FURY, ENUM_OBJECT, result);
        resultManager.printResult("Apache Fury", "EnumObject", result);
    }

    @Test
    public void benchmarkGeneratedSerializer_ComplexMapObject() throws Throwable {
        System.out.println("\n=== GeneratedSerializer - ComplexMapObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        ComplexMapObject[] objects = new ComplexMapObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexMapObject(i);
        }

        GeneratedSerializer<ComplexMapObject> serializer = new GeneratedSerializer<>(ComplexMapObject.class);
        BenchmarkResultManager.SerializationResult result = benchmarkGeneratedSerializer(serializer, objects, iterations);
        resultManager.recordResult(GENERATED_SERIALIZER, COMPLEX_MAP, result);
        resultManager.printResult("GeneratedSerializer", "ComplexMapObject", result);
    }

    @Test
    public void benchmarkTypedSerializer_ComplexMapObject() throws Throwable {
        System.out.println("\n=== TypedSerializer - ComplexMapObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        ComplexMapObject[] objects = new ComplexMapObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexMapObject(i);
        }

        TypedSerializer<ComplexMapObject> serializer = new TypedSerializer<>(ComplexMapObject.class);
        BenchmarkResultManager.SerializationResult result = benchmarkTypedSerializer(serializer, objects, iterations);
        resultManager.recordResult(TYPED_SERIALIZER, COMPLEX_MAP, result);
        resultManager.printResult("TypedSerializer", "ComplexMapObject", result);
    }

    @Test
    public void benchmarkKryo_ComplexMapObject() throws Throwable {
        System.out.println("\n=== Kryo - ComplexMapObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        ComplexMapObject[] objects = new ComplexMapObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, ComplexMapObject.class, iterations);
        resultManager.recordResult(KRYO, COMPLEX_MAP, result);
        resultManager.printResult("Kryo", "ComplexMapObject", result);
    }

    // @Test  // Disabled - focusing on high-performance serializers
    public void benchmarkJackson_ComplexMapObject() throws Throwable {
        System.out.println("\n=== Jackson - ComplexMapObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        ComplexMapObject[] objects = new ComplexMapObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkJackson(objects, ComplexMapObject.class, iterations);
        resultManager.recordResult(JACKSON, COMPLEX_MAP, result);
        resultManager.printResult("Jackson", "ComplexMapObject", result);
    }

    // @Test  // Disabled - focusing on high-performance serializers
    public void benchmarkGson_ComplexMapObject() throws Throwable {
        System.out.println("\n=== Gson - ComplexMapObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        ComplexMapObject[] objects = new ComplexMapObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkGson(objects, ComplexMapObject.class, iterations);
        resultManager.recordResult(GSON, COMPLEX_MAP, result);
        resultManager.printResult("Gson", "ComplexMapObject", result);
    }

    @Test
    public void benchmarkFury_ComplexMapObject() throws Throwable {
        System.out.println("\n=== Apache Fury - ComplexMapObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        ComplexMapObject[] objects = new ComplexMapObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createComplexMapObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, iterations);
        resultManager.recordResult(FURY, COMPLEX_MAP, result);
        resultManager.printResult("Apache Fury", "ComplexMapObject", result);
    }

    @Test
    public void benchmarkGeneratedSerializer_MixedComplexityObject() throws Throwable {
        System.out.println("\n=== GeneratedSerializer - MixedComplexityObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        MixedComplexityObject[] objects = new MixedComplexityObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createMixedComplexityObject(i);
        }

        GeneratedSerializer<MixedComplexityObject> serializer = new GeneratedSerializer<>(MixedComplexityObject.class);
        BenchmarkResultManager.SerializationResult result = benchmarkGeneratedSerializer(serializer, objects, iterations);
        resultManager.recordResult(GENERATED_SERIALIZER, MIXED_COMPLEXITY, result);
        resultManager.printResult("GeneratedSerializer", "MixedComplexityObject", result);
    }

    @Test
    public void benchmarkTypedSerializer_MixedComplexityObject() throws Throwable {
        System.out.println("\n=== TypedSerializer - MixedComplexityObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        MixedComplexityObject[] objects = new MixedComplexityObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createMixedComplexityObject(i);
        }

        TypedSerializer<MixedComplexityObject> serializer = new TypedSerializer<>(MixedComplexityObject.class);
        BenchmarkResultManager.SerializationResult result = benchmarkTypedSerializer(serializer, objects, iterations);
        resultManager.recordResult(TYPED_SERIALIZER, MIXED_COMPLEXITY, result);
        resultManager.printResult("TypedSerializer", "MixedComplexityObject", result);
    }

    @Test
    public void benchmarkKryo_MixedComplexityObject() throws Throwable {
        System.out.println("\n=== Kryo - MixedComplexityObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        MixedComplexityObject[] objects = new MixedComplexityObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createMixedComplexityObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkKryo(objects, MixedComplexityObject.class, iterations);
        resultManager.recordResult(KRYO, MIXED_COMPLEXITY, result);
        resultManager.printResult("Kryo", "MixedComplexityObject", result);
    }

    @Test
    public void benchmarkFury_MixedComplexityObject() throws Throwable {
        System.out.println("\n=== Apache Fury - MixedComplexityObject ===");

        int iterations = BENCHMARK_ITERATIONS / 10;
        MixedComplexityObject[] objects = new MixedComplexityObject[iterations];
        for (int i = 0; i < iterations; i++) {
            objects[i] = createMixedComplexityObject(i);
        }

        BenchmarkResultManager.SerializationResult result = benchmarkFury(objects, iterations);
        resultManager.recordResult(FURY, MIXED_COMPLEXITY, result);
        resultManager.printResult("Apache Fury", "MixedComplexityObject", result);
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

    // NEW COMPREHENSIVE TEST SCENARIOS

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }

    // Scenario: Lists with different primitive types
    public static class ListVarietyObject {
        public List<Integer> integers;
        public List<Long> longs;
        public List<String> strings;
        public List<Double> doubles;
        public List<Boolean> booleans;
        public ListVarietyObject() {}
    }

    // Scenario: Complex objects in lists
    public static class ComplexListObject {
        public String id;
        public List<NestedObject> items;
        public List<SimpleObject> simpleItems;
        public ComplexListObject() {}
    }

    // Scenario: Null fields (sparse object)
    public static class NullableObject {
        public String requiredField;
        public String optionalField1;
        public String optionalField2;
        public Integer optionalInt;
        public NestedObject optionalNested;
        public List<String> optionalList;
        public Map<String, Integer> optionalMap;
        public NullableObject() {}
    }

    // Scenario: Enums
    public static class EnumObject {
        public int id;
        public String name;
        public Priority priority;
        public Status status;
        public List<Priority> priorityHistory;
        public EnumObject() {}
    }

    // Scenario: Maps with complex values
    public static class ComplexMapObject {
        public String id;
        public Map<String, NestedObject> objectMap;
        public Map<String, List<Integer>> listMap;
        public Map<Integer, String> intKeyMap;
        public ComplexMapObject() {}
    }

    // Scenario: Mixed complexity (combines multiple patterns)
    public static class MixedComplexityObject {
        public int id;
        public String name;
        public Priority priority;
        public List<NestedObject> nestedList;
        public Map<String, Integer> simpleMap;
        public Map<String, NestedObject> complexMap;
        public NestedObject singleNested;
        public String nullableField;
        public MixedComplexityObject() {}
    }
}

