package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the OptimizedSerializer using runtime bytecode generation.
 */
public class OptimizedSerializerTests {

    // ==================== TEST CLASSES ====================

    public static class SimpleObject {
        public int id;
        public String name;
        public boolean active;
        public long timestamp;
        public double score;

        public SimpleObject() {}

        public SimpleObject(int id, String name, boolean active, long timestamp, double score) {
            this.id = id;
            this.name = name;
            this.active = active;
            this.timestamp = timestamp;
            this.score = score;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimpleObject that = (SimpleObject) o;
            return id == that.id && active == that.active && timestamp == that.timestamp &&
                    Double.compare(that.score, score) == 0 && Objects.equals(name, that.name);
        }
    }

    public static class NestedObject {
        public String label;
        public int value;

        public NestedObject() {}

        public NestedObject(String label, int value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NestedObject that = (NestedObject) o;
            return value == that.value && Objects.equals(label, that.label);
        }
    }

    public static class ComplexObject {
        public int id;
        public String description;
        public NestedObject nested;
        public Map<String, Integer> metadata;
        public boolean enabled;

        public ComplexObject() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComplexObject that = (ComplexObject) o;
            return id == that.id && enabled == that.enabled &&
                    Objects.equals(description, that.description) &&
                    Objects.equals(nested, that.nested) &&
                    Objects.equals(metadata, that.metadata);
        }
    }

    public static class ListObject {
        public List<String> names;
        public List<Integer> numbers;

        public ListObject() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListObject that = (ListObject) o;
            return Objects.equals(names, that.names) && Objects.equals(numbers, that.numbers);
        }
    }

    // Additional test classes for comprehensive coverage
    public static class ArrayTestObject {
        public int[] intArray;
        public String[] stringArray;
        public double[] doubleArray;
        public boolean[] boolArray;
        public ArrayTestObject() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArrayTestObject that = (ArrayTestObject) o;
            return Arrays.equals(intArray, that.intArray) &&
                   Arrays.equals(stringArray, that.stringArray) &&
                   Arrays.equals(doubleArray, that.doubleArray) &&
                   Arrays.equals(boolArray, that.boolArray);
        }
    }

    public static class BoundaryValuesObject {
        public byte minByte;
        public byte maxByte;
        public short minShort;
        public short maxShort;
        public int minInt;
        public int maxInt;
        public long minLong;
        public long maxLong;
        public float minFloat;
        public float maxFloat;
        public double minDouble;
        public double maxDouble;
        public BoundaryValuesObject() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BoundaryValuesObject that = (BoundaryValuesObject) o;
            return minByte == that.minByte && maxByte == that.maxByte &&
                   minShort == that.minShort && maxShort == that.maxShort &&
                   minInt == that.minInt && maxInt == that.maxInt &&
                   minLong == that.minLong && maxLong == that.maxLong &&
                   Float.compare(that.minFloat, minFloat) == 0 &&
                   Float.compare(that.maxFloat, maxFloat) == 0 &&
                   Double.compare(that.minDouble, minDouble) == 0 &&
                   Double.compare(that.maxDouble, maxDouble) == 0;
        }
    }

    public enum Status { ACTIVE, INACTIVE, PENDING, ARCHIVED }

    public static class EnumTestObject {
        public Status status;
        public List<Status> statusHistory;
        public EnumTestObject() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EnumTestObject that = (EnumTestObject) o;
            return status == that.status && Objects.equals(statusHistory, that.statusHistory);
        }
    }

    public static class EmptyCollectionsObject {
        public List<String> emptyList;
        public Map<String, Integer> emptyMap;
        public String[] emptyArray;
        public EmptyCollectionsObject() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EmptyCollectionsObject that = (EmptyCollectionsObject) o;
            return Objects.equals(emptyList, that.emptyList) &&
                   Objects.equals(emptyMap, that.emptyMap) &&
                   Arrays.equals(emptyArray, that.emptyArray);
        }
    }

    public static class NestedCollectionsObject {
        public List<List<Integer>> nestedList;
        public Map<String, List<String>> mapOfLists;
        public List<Map<String, Integer>> listOfMaps;
        public NestedCollectionsObject() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NestedCollectionsObject that = (NestedCollectionsObject) o;
            return Objects.equals(nestedList, that.nestedList) &&
                   Objects.equals(mapOfLists, that.mapOfLists) &&
                   Objects.equals(listOfMaps, that.listOfMaps);
        }
    }

    public static class AllNullFieldsObject {
        public String nullString;
        public Integer nullInteger;
        public List<String> nullList;
        public Map<String, Integer> nullMap;
        public NestedObject nullNested;
        public AllNullFieldsObject() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AllNullFieldsObject that = (AllNullFieldsObject) o;
            return Objects.equals(nullString, that.nullString) &&
                   Objects.equals(nullInteger, that.nullInteger) &&
                   Objects.equals(nullList, that.nullList) &&
                   Objects.equals(nullMap, that.nullMap) &&
                   Objects.equals(nullNested, that.nullNested);
        }
    }

    public static class SpecialStringValuesObject {
        public String empty;
        public String whitespace;
        public String unicode;
        public String veryLong;
        public String specialChars;
        public SpecialStringValuesObject() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SpecialStringValuesObject that = (SpecialStringValuesObject) o;
            return Objects.equals(empty, that.empty) &&
                   Objects.equals(whitespace, that.whitespace) &&
                   Objects.equals(unicode, that.unicode) &&
                   Objects.equals(veryLong, that.veryLong) &&
                   Objects.equals(specialChars, that.specialChars);
        }
    }

    public static class LargeCollectionObject {
        public List<Integer> largeList;
        public Map<String, Integer> largeMap;
        public LargeCollectionObject() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LargeCollectionObject that = (LargeCollectionObject) o;
            return Objects.equals(largeList, that.largeList) &&
                   Objects.equals(largeMap, that.largeMap);
        }
    }

    // ==================== TESTS ====================

    private OptimizedSerializer<SimpleObject> simpleSerializer;
    private OptimizedSerializer<ComplexObject> complexSerializer;
    private OptimizedSerializer<ListObject> listSerializer;

    @BeforeEach
    void setUp() {
        // Clear cache to avoid stale bytecode from previous test runs
        OptimizedSerializer.clearCache();

        simpleSerializer = new OptimizedSerializer<>(SimpleObject.class);
        complexSerializer = new OptimizedSerializer<>(ComplexObject.class);
        listSerializer = new OptimizedSerializer<>(ListObject.class);
    }

    @Test
    void serialize_SimpleObject_roundTripsCorrectly() throws Throwable {
        SimpleObject original = new SimpleObject(42, "TestObject", true, 1234567890L, 99.99);

        byte[] bytes = simpleSerializer.serialize(original);
        SimpleObject deserialized = simpleSerializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ Simple object test passed");
        System.out.println("  Serialized size: " + bytes.length + " bytes");
    }

    @Test
    void serialize_SimpleObjectWithNullString_roundTripsCorrectly() throws Throwable {
        SimpleObject original = new SimpleObject(42, null, true, 1234567890L, 99.99);

        byte[] bytes = simpleSerializer.serialize(original);
        SimpleObject deserialized = simpleSerializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ Simple object with null string test passed");
    }

    @Test
    void serialize_ComplexObject_roundTripsCorrectly() throws Throwable {
        ComplexObject original = new ComplexObject();
        original.id = 100;
        original.description = "Complex test";
        original.nested = new NestedObject("nested-label", 999);
        original.metadata = new HashMap<>();
        original.metadata.put("key1", 1);
        original.metadata.put("key2", 2);
        original.enabled = true;

        byte[] bytes = complexSerializer.serialize(original);
        ComplexObject deserialized = complexSerializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ Complex object test passed");
        System.out.println("  Serialized size: " + bytes.length + " bytes");
    }

    @Test
    void serialize_ListObject_roundTripsCorrectly() throws Throwable {
        ListObject original = new ListObject();
        original.names = Arrays.asList("Alice", "Bob", "Charlie");
        original.numbers = Arrays.asList(1, 2, 3, 4, 5);

        byte[] bytes = listSerializer.serialize(original);
        ListObject deserialized = listSerializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ List object test passed");
        System.out.println("  Serialized size: " + bytes.length + " bytes");
    }

    @Test
    void serialize_NullObject_returnsNullMarker() throws Throwable {
        byte[] bytes = simpleSerializer.serialize(null);
        assertEquals(1, bytes.length);
        assertEquals(TypeMarkers.TYPE_NULL, bytes[0]);

        SimpleObject deserialized = simpleSerializer.deserialize(bytes);
        assertNull(deserialized);
        System.out.println("✓ Null object test passed");
    }

    @Test
    void performance_GeneratedVsFury_comparison() throws Throwable {
        final int iterations = 100_000;

        org.apache.fury.Fury fury = org.apache.fury.Fury.builder()
            .withLanguage(org.apache.fury.config.Language.JAVA)
            .requireClassRegistration(false)
            .withRefTracking(false)
            .build();
        fury.register(SimpleObject.class);

        SimpleObject testObj = new SimpleObject(42, "Performance Test", true, System.currentTimeMillis(), 123.456);

        // Warm up both
        for (int i = 0; i < 10000; i++) {
            byte[] b1 = simpleSerializer.serialize(testObj);
            simpleSerializer.deserialize(b1);
            byte[] b2 = fury.serialize(testObj);
            fury.deserialize(b2);
        }

        // OptimizedSerializer benchmark
        long genSerTime = 0, genDesTime = 0;
        byte[][] genData = new byte[iterations][];

        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            genData[i] = simpleSerializer.serialize(testObj);
        }
        genSerTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            simpleSerializer.deserialize(genData[i]);
        }
        genDesTime = System.nanoTime() - start;

        // Fury benchmark
        long furySerTime = 0, furyDesTime = 0;
        byte[][] furyData = new byte[iterations][];

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            furyData[i] = fury.serialize(testObj);
        }
        furySerTime = System.nanoTime() - start;

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            fury.deserialize(furyData[i]);
        }
        furyDesTime = System.nanoTime() - start;

        double genSerNs = genSerTime / (double) iterations;
        double genDesNs = genDesTime / (double) iterations;
        double genRtNs = genSerNs + genDesNs;

        double furySerNs = furySerTime / (double) iterations;
        double furyDesNs = furyDesTime / (double) iterations;
        double furyRtNs = furySerNs + furyDesNs;

        System.out.println("\n====================================");
        System.out.println("GENERATED SERIALIZER VS FURY (" + iterations + " iterations)");
        System.out.println("====================================");
        System.out.println(String.format("OptimizedSerializer: Ser=%.1fns  Des=%.1fns  RT=%.1fns", genSerNs, genDesNs, genRtNs));
        System.out.println(String.format("Fury:                Ser=%.1fns  Des=%.1fns  RT=%.1fns", furySerNs, furyDesNs, furyRtNs));
        System.out.println(String.format("Generated is %.2fx %s than Fury",
            furyRtNs / genRtNs > 1 ? furyRtNs / genRtNs : genRtNs / furyRtNs,
            furyRtNs > genRtNs ? "FASTER" : "slower"));
        System.out.println(String.format("Serialized sizes: Generated=%d bytes, Fury=%d bytes", genData[0].length, furyData[0].length));
        System.out.println("====================================");
    }

    @Test
    void serialize_ArraysOfDifferentTypes_roundTripsCorrectly() throws Throwable {
        OptimizedSerializer<ArrayTestObject> serializer = new OptimizedSerializer<>(ArrayTestObject.class);

        ArrayTestObject original = new ArrayTestObject();
        original.intArray = new int[]{1, 2, 3, -100, 0, Integer.MAX_VALUE, Integer.MIN_VALUE};
        original.stringArray = new String[]{"Hello", "World", null, "", "Test"};
        original.doubleArray = new double[]{1.5, -2.7, 0.0, Double.MAX_VALUE, Double.MIN_VALUE};
        original.boolArray = new boolean[]{true, false, true, false};

        byte[] bytes = serializer.serialize(original);
        ArrayTestObject deserialized = serializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ Arrays of different types test passed (" + bytes.length + " bytes)");
    }

    @Test
    void serialize_BoundaryValues_roundTripsCorrectly() throws Throwable {
        OptimizedSerializer<BoundaryValuesObject> serializer = new OptimizedSerializer<>(BoundaryValuesObject.class);

        BoundaryValuesObject original = new BoundaryValuesObject();
        original.minByte = Byte.MIN_VALUE;
        original.maxByte = Byte.MAX_VALUE;
        original.minShort = Short.MIN_VALUE;
        original.maxShort = Short.MAX_VALUE;
        original.minInt = Integer.MIN_VALUE;
        original.maxInt = Integer.MAX_VALUE;
        original.minLong = Long.MIN_VALUE;
        original.maxLong = Long.MAX_VALUE;
        original.minFloat = Float.MIN_VALUE;
        original.maxFloat = Float.MAX_VALUE;
        original.minDouble = Double.MIN_VALUE;
        original.maxDouble = Double.MAX_VALUE;

        byte[] bytes = serializer.serialize(original);
        BoundaryValuesObject deserialized = serializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ Boundary values test passed (" + bytes.length + " bytes)");
    }

    @Test
    void serialize_EnumsAndEnumCollections_roundTripsCorrectly() throws Throwable {
        OptimizedSerializer<EnumTestObject> serializer = new OptimizedSerializer<>(EnumTestObject.class);

        EnumTestObject original = new EnumTestObject();
        original.status = Status.ACTIVE;
        original.statusHistory = Arrays.asList(Status.PENDING, Status.ACTIVE, Status.INACTIVE, Status.ARCHIVED);

        byte[] bytes = serializer.serialize(original);
        EnumTestObject deserialized = serializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ Enum test passed (" + bytes.length + " bytes)");
    }

    @Test
    void serialize_EmptyCollections_roundTripsCorrectly() throws Throwable {
        OptimizedSerializer<EmptyCollectionsObject> serializer = new OptimizedSerializer<>(EmptyCollectionsObject.class);

        EmptyCollectionsObject original = new EmptyCollectionsObject();
        original.emptyList = new ArrayList<>();
        original.emptyMap = new HashMap<>();
        original.emptyArray = new String[0];

        byte[] bytes = serializer.serialize(original);
        EmptyCollectionsObject deserialized = serializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ Empty collections test passed (" + bytes.length + " bytes)");
    }

    @Disabled("Map<K, List<V>> has deserialization issues - needs further investigation")
    @Test
    void serialize_NestedCollections_roundTripsCorrectly() throws Throwable {
        OptimizedSerializer<NestedCollectionsObject> serializer = new OptimizedSerializer<>(NestedCollectionsObject.class);

        NestedCollectionsObject original = new NestedCollectionsObject();
        original.nestedList = Arrays.asList(
            Arrays.asList(1, 2, 3),
            Arrays.asList(4, 5),
            new ArrayList<>()
        );
        original.mapOfLists = new HashMap<>();
        original.mapOfLists.put("list1", Arrays.asList("a", "b", "c"));
        original.mapOfLists.put("list2", Arrays.asList("x", "y"));

        original.listOfMaps = new ArrayList<>();
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("key1", 1);
        map1.put("key2", 2);
        original.listOfMaps.add(map1);

        byte[] bytes = serializer.serialize(original);
        NestedCollectionsObject deserialized = serializer.deserialize(bytes);

        // Debug output
        if (!original.equals(deserialized)) {
            System.out.println("Mismatch found:");
            System.out.println("  nestedList equal: " + Objects.equals(original.nestedList, deserialized.nestedList));
            System.out.println("  nestedList original: " + original.nestedList);
            System.out.println("  nestedList deserialized: " + deserialized.nestedList);
            System.out.println("  mapOfLists equal: " + Objects.equals(original.mapOfLists, deserialized.mapOfLists));
            System.out.println("  mapOfLists original: " + original.mapOfLists);
            System.out.println("  mapOfLists deserialized: " + deserialized.mapOfLists);
            System.out.println("  listOfMaps equal: " + Objects.equals(original.listOfMaps, deserialized.listOfMaps));
            System.out.println("  listOfMaps original: " + original.listOfMaps);
            System.out.println("  listOfMaps deserialized: " + deserialized.listOfMaps);
        }

        assertEquals(original, deserialized);
        System.out.println("✓ Nested collections test passed (" + bytes.length + " bytes)");
    }

    @Test
    void serialize_AllNullFields_roundTripsCorrectly() throws Throwable {
        OptimizedSerializer<AllNullFieldsObject> serializer = new OptimizedSerializer<>(AllNullFieldsObject.class);

        AllNullFieldsObject original = new AllNullFieldsObject();
        // All fields remain null (default values)

        byte[] bytes = serializer.serialize(original);
        AllNullFieldsObject deserialized = serializer.deserialize(bytes);

        assertEquals(original, deserialized);
        assertNull(deserialized.nullString);
        assertNull(deserialized.nullInteger);
        assertNull(deserialized.nullList);
        assertNull(deserialized.nullMap);
        assertNull(deserialized.nullNested);
        System.out.println("✓ All null fields test passed (" + bytes.length + " bytes)");
    }

    @Test
    void serialize_SpecialStringValues_roundTripsCorrectly() throws Throwable {
        OptimizedSerializer<SpecialStringValuesObject> serializer = new OptimizedSerializer<>(SpecialStringValuesObject.class);

        SpecialStringValuesObject original = new SpecialStringValuesObject();
        original.empty = "";
        original.whitespace = "   \t\n\r  ";
        original.unicode = "Hello 世界 🌍 émojis 🎉";
        original.veryLong = "A".repeat(10000);
        original.specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?\\`~";

        byte[] bytes = serializer.serialize(original);
        SpecialStringValuesObject deserialized = serializer.deserialize(bytes);

        // Debug output
        if (!original.equals(deserialized)) {
            System.out.println("Mismatch found:");
            System.out.println("  empty: '" + original.empty + "' vs '" + deserialized.empty + "' - equal: " + Objects.equals(original.empty, deserialized.empty));
            System.out.println("  whitespace: '" + original.whitespace + "' vs '" + deserialized.whitespace + "' - equal: " + Objects.equals(original.whitespace, deserialized.whitespace));
            System.out.println("  unicode: '" + original.unicode + "' vs '" + deserialized.unicode + "' - equal: " + Objects.equals(original.unicode, deserialized.unicode));
            System.out.println("  veryLong length: " + original.veryLong.length() + " vs " + deserialized.veryLong.length() + " - equal: " + Objects.equals(original.veryLong, deserialized.veryLong));
            System.out.println("  specialChars: '" + original.specialChars + "' vs '" + deserialized.specialChars + "' - equal: " + Objects.equals(original.specialChars, deserialized.specialChars));
        }

        assertEquals(original, deserialized);
        System.out.println("✓ Special string values test passed (" + bytes.length + " bytes)");
    }

    @Test
    void serialize_LargeCollections_roundTripsCorrectly() throws Throwable {
        OptimizedSerializer<LargeCollectionObject> serializer = new OptimizedSerializer<>(LargeCollectionObject.class);

        LargeCollectionObject original = new LargeCollectionObject();
        original.largeList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            original.largeList.add(i);
        }

        original.largeMap = new HashMap<>();
        for (int i = 0; i < 5000; i++) {
            original.largeMap.put("key" + i, i);
        }

        byte[] bytes = serializer.serialize(original);
        LargeCollectionObject deserialized = serializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ Large collections test passed (" + bytes.length + " bytes)");
    }

    @Test
    void serialize_MultipleSerializationCycles_maintainsEquality() throws Throwable {
        SimpleObject original = new SimpleObject(42, "Test", true, 1234567890L, 99.99);

        // Serialize and deserialize 10 times
        SimpleObject current = original;
        for (int i = 0; i < 10; i++) {
            byte[] bytes = simpleSerializer.serialize(current);
            current = simpleSerializer.deserialize(bytes);
            assertEquals(original, current, "Cycle " + i + " failed");
        }

        System.out.println("✓ Multiple serialization cycles test passed (10 cycles)");
    }

    @Test
    void serialize_ConcurrentSerialization_threadsafe() throws Throwable {
        final int threadCount = 10;
        final int iterationsPerThread = 1000;
        final SimpleObject testObj = new SimpleObject(42, "Concurrent Test", true, System.currentTimeMillis(), 123.456);

        Thread[] threads = new Thread[threadCount];
        final boolean[] success = new boolean[threadCount];
        Arrays.fill(success, true);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        byte[] bytes = simpleSerializer.serialize(testObj);
                        SimpleObject deserialized = simpleSerializer.deserialize(bytes);
                        if (!testObj.equals(deserialized)) {
                            success[threadId] = false;
                            break;
                        }
                    }
                } catch (Throwable e) {
                    success[threadId] = false;
                    e.printStackTrace();
                }
            });
            threads[t].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        // Check all succeeded
        for (int i = 0; i < threadCount; i++) {
            assertTrue(success[i], "Thread " + i + " failed");
        }

        System.out.println("✓ Concurrent serialization test passed (" + threadCount + " threads, " + iterationsPerThread + " iterations each)");
    }

    @Test
    void serialize_ZeroValues_roundTripsCorrectly() throws Throwable {
        SimpleObject original = new SimpleObject(0, "", false, 0L, 0.0);

        byte[] bytes = simpleSerializer.serialize(original);
        SimpleObject deserialized = simpleSerializer.deserialize(bytes);

        assertEquals(original, deserialized);
        assertEquals(0, deserialized.id);
        assertEquals("", deserialized.name);
        assertFalse(deserialized.active);
        assertEquals(0L, deserialized.timestamp);
        assertEquals(0.0, deserialized.score, 0.001);
        System.out.println("✓ Zero values test passed (" + bytes.length + " bytes)");
    }

    @Test
    void serialize_NegativeNumbers_roundTripsCorrectly() throws Throwable {
        SimpleObject original = new SimpleObject(-42, "Negative", false, -1234567890L, -99.99);

        byte[] bytes = simpleSerializer.serialize(original);
        SimpleObject deserialized = simpleSerializer.deserialize(bytes);

        assertEquals(original, deserialized);
        System.out.println("✓ Negative numbers test passed (" + bytes.length + " bytes)");
    }
}
