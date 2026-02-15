# SimplyABinarySerializer Examples

Quick reference examples for common use cases.

## Basic Serialization

```java
import com.kalynx.simplyabinaryserializer.KalynxSerializer;

public class BasicExample {
    public static void main(String[] args) throws Throwable {
        // Create serializer
        KalynxSerializer serializer = new KalynxSerializer();
        
        // Register class
        serializer.register(User.class);
        
        // Create object
        User user = new User("Alice", 28);
        
        // Serialize
        byte[] bytes = serializer.serialize(user);
        System.out.println("Serialized size: " + bytes.length + " bytes");
        
        // Deserialize
        User restored = serializer.deserialize(bytes, User.class);
        System.out.println("Name: " + restored.name);
        System.out.println("Age: " + restored.age);
    }
    
    public static class User {
        public String name;
        public int age;
        
        public User() {}
        
        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}
```

## Multiple Classes

```java
import com.kalynx.simplyabinaryserializer.KalynxSerializer;

public class MultiClassExample {
    public static void main(String[] args) throws Throwable {
        // Register multiple classes
        KalynxSerializer serializer = new KalynxSerializer()
            .register(User.class)
            .register(Product.class)
            .register(Order.class);
        
        // Serialize different types
        User user = new User("Bob", 35);
        Product product = new Product("Widget", 29.99);
        Order order = new Order(101, user, product);
        
        byte[] userBytes = serializer.serialize(user);
        byte[] productBytes = serializer.serialize(product);
        byte[] orderBytes = serializer.serialize(order);
        
        // Deserialize
        User u = serializer.deserialize(userBytes, User.class);
        Product p = serializer.deserialize(productBytes, Product.class);
        Order o = serializer.deserialize(orderBytes, Order.class);
        
        System.out.println("Restored order #" + o.orderId);
    }
    
    public static class User {
        public String name;
        public int age;
        
        public User() {}
        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
    
    public static class Product {
        public String name;
        public double price;
        
        public Product() {}
        public Product(String name, double price) {
            this.name = name;
            this.price = price;
        }
    }
    
    public static class Order {
        public int orderId;
        public User user;
        public Product product;
        
        public Order() {}
        public Order(int orderId, User user, Product product) {
            this.orderId = orderId;
            this.user = user;
            this.product = product;
        }
    }
}
```

## Collections

```java
import com.kalynx.simplyabinaryserializer.KalynxSerializer;
import java.util.*;

public class CollectionsExample {
    public static void main(String[] args) throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        serializer.register(DataContainer.class);
        
        DataContainer data = new DataContainer();
        data.numbers = Arrays.asList(1, 2, 3, 4, 5);
        data.names = Arrays.asList("Alice", "Bob", "Charlie");
        
        data.scores = new HashMap<>();
        data.scores.put("Alice", 95);
        data.scores.put("Bob", 87);
        data.scores.put("Charlie", 92);
        
        // Serialize
        byte[] bytes = serializer.serialize(data);
        
        // Deserialize
        DataContainer restored = serializer.deserialize(bytes, DataContainer.class);
        
        System.out.println("Numbers: " + restored.numbers);
        System.out.println("Names: " + restored.names);
        System.out.println("Scores: " + restored.scores);
    }
    
    public static class DataContainer {
        public List<Integer> numbers;
        public List<String> names;
        public Map<String, Integer> scores;
        
        public DataContainer() {}
    }
}
```

## Nested Objects

```java
import com.kalynx.simplyabinaryserializer.KalynxSerializer;

public class NestedExample {
    public static void main(String[] args) throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        
        // Register all classes (including nested ones)
        serializer.register(Address.class);
        serializer.register(Person.class);
        
        // Create nested structure
        Address address = new Address("123 Main St", "Boston", "02101");
        Person person = new Person("Alice", 28, address);
        
        // Serialize
        byte[] bytes = serializer.serialize(person);
        
        // Deserialize
        Person restored = serializer.deserialize(bytes, Person.class);
        
        System.out.println(restored.name + " lives at " + 
                          restored.address.street + ", " + 
                          restored.address.city);
    }
    
    public static class Address {
        public String street;
        public String city;
        public String zipCode;
        
        public Address() {}
        
        public Address(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }
    }
    
    public static class Person {
        public String name;
        public int age;
        public Address address;
        
        public Person() {}
        
        public Person(String name, int age, Address address) {
            this.name = name;
            this.age = age;
            this.address = address;
        }
    }
}
```

## Generic Types with TypeReference

```java
import com.kalynx.simplyabinaryserializer.KalynxSerializer;
import com.kalynx.simplyabinaryserializer.TypeReference;
import java.util.*;

public class GenericTypeExample {
    public static void main(String[] args) throws Throwable {
        KalynxSerializer serializer = new KalynxSerializer();
        
        // Register with TypeReference for type safety
        serializer.register(new TypeReference<UserData>() {});
        
        // Create complex data structure
        UserData data = new UserData();
        data.username = "alice";
        data.scores = Arrays.asList(95, 87, 92, 88);
        data.metadata = new HashMap<>();
        data.metadata.put("level", "advanced");
        data.metadata.put("status", "active");
        
        // Serialize
        byte[] bytes = serializer.serialize(data);
        
        // Deserialize with type safety
        UserData restored = serializer.deserialize(bytes, new TypeReference<UserData>() {});
        
        System.out.println("User: " + restored.username);
        System.out.println("Average score: " + 
            restored.scores.stream().mapToInt(Integer::intValue).average().orElse(0));
        System.out.println("Metadata: " + restored.metadata);
    }
    
    public static class UserData {
        public String username;
        public List<Integer> scores;
        public Map<String, String> metadata;
        
        public UserData() {}
    }
}
```

## Performance-Optimized Usage

```java
import com.kalynx.simplyabinaryserializer.KalynxSerializer;

public class PerformanceExample {
    // Create serializer once, reuse everywhere (thread-safe)
    private static final KalynxSerializer SERIALIZER = createSerializer();
    
    private static KalynxSerializer createSerializer() {
        try {
            return new KalynxSerializer()
                .register(User.class)
                .register(Product.class)
                .register(Order.class);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize serializer", e);
        }
    }
    
    public static void main(String[] args) throws Throwable {
        // Process many objects efficiently
        List<User> users = generateUsers(10000);
        
        long start = System.currentTimeMillis();
        
        // Serialize all users
        for (User user : users) {
            byte[] bytes = SERIALIZER.serialize(user);
            // Store bytes...
        }
        
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Serialized " + users.size() + " users in " + elapsed + "ms");
        System.out.println("Throughput: " + (users.size() * 1000 / elapsed) + " objects/sec");
    }
    
    private static List<User> generateUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(new User("User" + i, 20 + (i % 50)));
        }
        return users;
    }
    
    public static class User {
        public String name;
        public int age;
        
        public User() {}
        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}
```

## Error Handling

```java
import com.kalynx.simplyabinaryserializer.KalynxSerializer;

public class ErrorHandlingExample {
    public static void main(String[] args) {
        KalynxSerializer serializer = new KalynxSerializer();
        
        try {
            serializer.register(User.class);
            
            User user = new User("Alice", 28);
            byte[] bytes = serializer.serialize(user);
            User restored = serializer.deserialize(bytes, User.class);
            
            System.out.println("Success: " + restored.name);
            
        } catch (IllegalStateException e) {
            // Class not registered
            System.err.println("Registration error: " + e.getMessage());
            
        } catch (IllegalArgumentException e) {
            // Null object or empty bytes
            System.err.println("Invalid input: " + e.getMessage());
            
        } catch (Throwable e) {
            // General serialization error
            System.err.println("Serialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static class User {
        public String name;
        public int age;
        
        public User() {}
        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}
```

## All Examples

For more examples, see:
- `KalynxSerializerTest.java` - 71 comprehensive unit tests
- `KalynxBenchmarkRunnerTests.java` - Performance benchmarks
- `TestDataClasses.java` - Complex type examples

