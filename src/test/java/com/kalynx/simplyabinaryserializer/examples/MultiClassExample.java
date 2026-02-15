package com.kalynx.simplyabinaryserializer.examples;

import com.kalynx.simplyabinaryserializer.KalynxSerializer;

/**
 * Example demonstrating multi-class registration and usage of KalynxSerializer.
 */
public class MultiClassExample {

    public static void main(String[] args) throws Throwable {
        // Create a single serializer instance
        KalynxSerializer serializer = new KalynxSerializer();

        // Register multiple classes (fluent API)
        serializer
                .register(Person.class)
                .register(Address.class)
                .register(Company.class);

        // Create test objects
        Person person = new Person("Alice", 28, "alice@example.com");
        Address address = new Address("123 Main St", "Springfield", "12345");
        Company company = new Company("TechCorp", 500);

        // Serialize different types
        byte[] personBytes = serializer.serialize(person);
        byte[] addressBytes = serializer.serialize(address);
        byte[] companyBytes = serializer.serialize(company);

        System.out.println("Serialized Person: " + personBytes.length + " bytes");
        System.out.println("Serialized Address: " + addressBytes.length + " bytes");
        System.out.println("Serialized Company: " + companyBytes.length + " bytes");

        // Deserialize back to objects
        Person deserializedPerson = serializer.deserialize(personBytes, Person.class);
        Address deserializedAddress = serializer.deserialize(addressBytes, Address.class);
        Company deserializedCompany = serializer.deserialize(companyBytes, Company.class);

        // Verify correctness
        System.out.println("\nDeserialized Person: " + deserializedPerson);
        System.out.println("Deserialized Address: " + deserializedAddress);
        System.out.println("Deserialized Company: " + deserializedCompany);

        // Check registration status
        System.out.println("\nIs Person registered? " + serializer.isRegistered(Person.class));
        System.out.println("Is String registered? " + serializer.isRegistered(String.class));
    }

    static class Person {
        String name;
        int age;
        String email;

        public Person() {}

        public Person(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + ", email='" + email + "'}";
        }
    }

    static class Address {
        String street;
        String city;
        String zipCode;

        public Address() {}

        public Address(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }

        @Override
        public String toString() {
            return "Address{street='" + street + "', city='" + city + "', zipCode='" + zipCode + "'}";
        }
    }

    static class Company {
        String name;
        int employeeCount;

        public Company() {}

        public Company(String name, int employeeCount) {
            this.name = name;
            this.employeeCount = employeeCount;
        }

        @Override
        public String toString() {
            return "Company{name='" + name + "', employeeCount=" + employeeCount + "}";
        }
    }
}

