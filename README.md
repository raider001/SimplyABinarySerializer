# SimplyABinarySerializer

A high-performance, zero-overhead binary serialization library for Java that uses bytecode generation to achieve native speed.

## 🚀 Features

- **🔥 Blazingly Fast** - Uses bytecode generation for zero-overhead serialization
- **📦 Multi-Class Support** - Register and serialize multiple classes in a single serializer instance
- **🎯 Type-Safe Generics** - Full support for generic types via TypeReference pattern
- **⚡ Zero Reflection at Runtime** - All serialization logic generated at registration time
- **🔄 Complete Type Support** - Primitives, collections, maps, arrays, nested objects
- **🧵 Thread-Safe** - Safe for concurrent use
- **💾 Compact Binary Format** - Smaller than JSON, competitive with Protocol Buffers
- **🎨 Fluent API** - Clean, chainable interface

## 📊 Performance

Based on 100,000 iterations benchmark:

| Feature | Performance |
|---------|-------------|
| vs Kryo | **2-3x faster** on average |
| vs Apache Fury | **Competitive** (within 10%) |
| vs JSON | **5-10x faster** |
| TypeReference Overhead | **0% for serialization, -30% for deserialization** (faster!) |

See [PERFORMANCE_SUMMARY.md](PERFORMANCE_SUMMARY.md) for detailed benchmarks.

## 📦 Installation

### Maven

```xml
<dependency>
    <groupId>com.github.raider001</groupId>
    <artifactId>simplyabinaryserializer</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.github.raider001:simplyabinaryserializer:1.0.0'
```

## 🎯 Quick Start

### Basic Usage

```java
import com.kalynx.simplyabinaryserializer.KalynxSerializer;

// Create a serializer
KalynxSerializer serializer = new KalynxSerializer();

// Register your class
serializer.register(User.class);

// Serialize
User user = new User("Alice", 28, "alice@example.com");
byte[] bytes = serializer.serialize(user);

// Deserialize
User deserialized = serializer.deserialize(bytes, User.class);
```

### Multi-Class Registration

```java
// Fluent API for multiple classes
KalynxSerializer serializer = new KalynxSerializer()
    .register(User.class)
    .register(Product.class)
    .register(Order.class);

// Serialize different types with the same serializer
byte[] userBytes = serializer.serialize(user);
byte[] productBytes = serializer.serialize(product);
byte[] orderBytes = serializer.serialize(order);

// Deserialize
User u = serializer.deserialize(userBytes, User.class);
Product p = serializer.deserialize(productBytes, Product.class);
Order o = serializer.deserialize(orderBytes, Order.class);
```

### Generic Types with TypeReference

For classes with generic type parameters, use `TypeReference` for type-safe serialization:

```java
import com.kalynx.simplyabinaryserializer.TypeReference;

// Register generic type
serializer.register(new TypeReference<List<Integer>>() {});

// Use with wrapper classes
class DataWrapper {
    List<Integer> numbers;
    Map<String, User> userMap;
}

serializer.register(DataWrapper.class);

DataWrapper data = new DataWrapper();
data.numbers = Arrays.asList(1, 2, 3, 4, 5);
data.userMap = Map.of("alice", alice, "bob", bob);

byte[] bytes = serializer.serialize(data);
DataWrapper result = serializer.deserialize(bytes, DataWrapper.class);
```

## 📚 Supported Types

### Primitives
- `byte`, `short`, `int`, `long`
- `float`, `double`
- `boolean`, `char`
- `String`

### Collections
- `List<T>` - ArrayList, LinkedList, etc.
- `Set<T>` - HashSet, TreeSet, etc.
- `Map<K,V>` - HashMap, LinkedHashMap, TreeMap, etc.

### Arrays
- Primitive arrays: `int[]`, `long[]`, `double[]`, etc.
- Object arrays: `String[]`, `User[]`, etc.

### Complex Types
- Nested objects
- Generic types (via TypeReference)
- Collections of collections
- Maps with complex values

## 🎨 Advanced Usage

### Nested Objects

```java
class Address {
    String street;
    String city;
    String zipCode;
}

class Person {
    String name;
    int age;
    Address address;  // Nested object
}

// Register both classes
serializer.register(Address.class);
serializer.register(Person.class);

// Serialization handles nesting automatically
Person person = new Person("Bob", 35, new Address("123 Main St", "Boston", "02101"));
byte[] bytes = serializer.serialize(person);
Person result = serializer.deserialize(bytes, Person.class);
```

### Collections with Generic Types

```java
class DataContainer {
    List<Integer> numbers;
    List<String> names;
    Map<String, List<Integer>> dataMap;
}

serializer.register(DataContainer.class);

DataContainer container = new DataContainer();
container.numbers = Arrays.asList(1, 2, 3);
container.names = Arrays.asList("Alice", "Bob");
container.dataMap = Map.of(
    "primes", Arrays.asList(2, 3, 5, 7),
    "fibonacci", Arrays.asList(1, 1, 2, 3, 5)
);

byte[] bytes = serializer.serialize(container);
DataContainer result = serializer.deserialize(bytes, DataContainer.class);
```

### Handling Null Values

The serializer properly handles null values in:
- Object fields
- Collection elements
- Map keys and values (where supported)

```java
class NullableData {
    String name;        // Can be null
    List<String> items; // Can be null or contain nulls
}

NullableData data = new NullableData();
data.name = null;
data.items = Arrays.asList("a", null, "c");

byte[] bytes = serializer.serialize(data);
NullableData result = serializer.deserialize(bytes, NullableData.class);
// result.name is null
// result.items contains ["a", null, "c"]
```

### Registration Check

```java
// Check if a class is registered
if (!serializer.isRegistered(User.class)) {
    serializer.register(User.class);
}

// Check with TypeReference
TypeReference<List<Integer>> typeRef = new TypeReference<List<Integer>>() {};
if (!serializer.isRegistered(typeRef)) {
    serializer.register(typeRef);
}
```

## ⚡ Performance Tips

### 1. Register Classes Once
Registration triggers bytecode generation. Do it once at startup, not per serialization:

```java
// ✅ Good - register once
KalynxSerializer serializer = new KalynxSerializer();
serializer.register(User.class);

for (User user : users) {
    byte[] bytes = serializer.serialize(user);
}

// ❌ Bad - registering in loop
for (User user : users) {
    KalynxSerializer serializer = new KalynxSerializer();
    serializer.register(User.class);  // Wasteful!
    byte[] bytes = serializer.serialize(user);
}
```

### 2. Reuse Serializer Instances
KalynxSerializer is thread-safe. Create once, use everywhere:

```java
public class SerializerFactory {
    private static final KalynxSerializer INSTANCE = new KalynxSerializer()
        .register(User.class)
        .register(Product.class)
        .register(Order.class);
    
    public static KalynxSerializer getInstance() {
        return INSTANCE;
    }
}
```

### 3. Use Primitive Types When Possible
Primitive types (int, long, etc.) are faster than wrapper types (Integer, Long):

```java
// ✅ Faster
class FastData {
    int count;
    long timestamp;
    double value;
}

// ❌ Slower (boxing/unboxing overhead)
class SlowData {
    Integer count;
    Long timestamp;
    Double value;
}
```

## 🔧 Best Practices

### Class Design for Serialization

1. **Use public fields or provide getters/setters**
2. **Provide a no-arg constructor**
3. **Keep classes simple and focused**
4. **Avoid circular references**

```java
public class GoodExample {
    // Public fields work
    public String name;
    public int age;
    
    // No-arg constructor required
    public GoodExample() {}
    
    public GoodExample(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

### Error Handling

```java
try {
    byte[] bytes = serializer.serialize(user);
    User result = serializer.deserialize(bytes, User.class);
} catch (IllegalStateException e) {
    // Class not registered
    System.err.println("Class not registered: " + e.getMessage());
} catch (IllegalArgumentException e) {
    // Null input or empty bytes
    System.err.println("Invalid input: " + e.getMessage());
} catch (Throwable e) {
    // Serialization/deserialization error
    System.err.println("Serialization error: " + e.getMessage());
}
```

## 📖 API Reference

### KalynxSerializer

#### Methods

| Method | Description |
|--------|-------------|
| `register(Class<T> clazz)` | Register a class for serialization |
| `register(TypeReference<T> typeRef)` | Register a generic type |
| `isRegistered(Class<?> clazz)` | Check if class is registered |
| `isRegistered(TypeReference<?> typeRef)` | Check if generic type is registered |
| `serialize(T obj)` | Serialize an object to bytes |
| `deserialize(byte[] bytes, Class<T> clazz)` | Deserialize bytes to object |
| `deserialize(byte[] bytes, TypeReference<T> typeRef)` | Deserialize bytes with type info |

#### Exceptions

- `IllegalStateException` - Thrown when class is not registered
- `IllegalArgumentException` - Thrown for null objects or empty bytes
- `Throwable` - General serialization/deserialization errors

### TypeReference

Used to capture generic type information at compile time:

```java
// Create TypeReference for List<Integer>
TypeReference<List<Integer>> typeRef = new TypeReference<List<Integer>>() {};

// Methods
Type type = typeRef.getType();        // Full generic type
Class<T> raw = typeRef.getRawType();   // Raw class (List.class)
```

## 🔍 Troubleshooting

### "Class not registered" Error

**Problem:** `IllegalStateException: Class not registered: com.example.User`

**Solution:** Register the class before serializing:
```java
serializer.register(User.class);
```

### "Cannot serialize null object" Error

**Problem:** `IllegalArgumentException: Cannot serialize null object`

**Solution:** Check for null before serializing:
```java
if (user != null) {
    byte[] bytes = serializer.serialize(user);
}
```

### TypeReference Not Working

**Problem:** Generic type not properly captured

**Solution:** Use anonymous inner class syntax:
```java
// ❌ Wrong - no anonymous class
TypeReference<List<Integer>> ref = new TypeReference<List<Integer>>();

// ✅ Correct - anonymous class with {}
TypeReference<List<Integer>> ref = new TypeReference<List<Integer>>() {};
```

### Nested Object Serialization Fails

**Problem:** Nested objects not serializing

**Solution:** Register all nested classes:
```java
serializer.register(Address.class);  // Register nested class first
serializer.register(Person.class);   // Then register parent class
```

## 🧪 Testing

Run the test suite:

```bash
# All tests
mvn test

# Unit tests only (excludes benchmarks)
mvn test -Dtest=!KalynxBenchmarkRunnerTests

# Specific test class
mvn test -Dtest=KalynxSerializerTest
```

Run benchmarks:

```bash
mvn test -Dtest=KalynxBenchmarkRunnerTests
```

## 📊 Benchmarks

The library includes comprehensive benchmarks comparing performance against Kryo and Apache Fury:

- 22 different object types tested
- 100,000 iterations per test
- Measures serialization, deserialization, and binary size
- Generates detailed Markdown reports

See [PERFORMANCE_SUMMARY.md](PERFORMANCE_SUMMARY.md) for latest results.

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass (`mvn test`)
5. Submit a pull request

## 📄 License

MIT License

Copyright (c) 2026 SimplyABinarySerializer Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.



