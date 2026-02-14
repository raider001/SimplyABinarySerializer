package com.kalynx.simplyabinaryserializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for circular reference handling in OptimizedSerializer
 */
public class CircularReferenceTest {

    private OptimizedSerializer<CircularNode> serializer;

    @BeforeEach
    public void setUp() {
        serializer = new OptimizedSerializer<>(CircularNode.class);
    }

    @Test
    public void serialize_CircularReference_preservesStructure() throws Throwable {
        // Create circular structure: parent -> child -> parent
        CircularNode parent = new CircularNode();
        parent.name = "Parent";
        parent.value = 42;

        CircularNode child = new CircularNode();
        child.name = "Child";
        child.value = 24;
        child.parent = parent;

        parent.child = child; // This creates the circular reference

        // Serialize
        byte[] data = serializer.serialize(parent);
        assertNotNull(data);
        assertTrue(data.length > 0);

        // Deserialize
        CircularNode deserialized = serializer.deserialize(data);
        assertNotNull(deserialized);

        // Verify structure is preserved
        assertEquals("Parent", deserialized.name);
        assertEquals(42, deserialized.value);
        assertNotNull(deserialized.child);
        assertEquals("Child", deserialized.child.name);
        assertEquals(24, deserialized.child.value);

        // Verify circular reference is preserved
        assertNotNull(deserialized.child.parent);
        assertSame(deserialized, deserialized.child.parent); // Should be the same object (circular reference)

        System.out.println("✅ Circular reference test passed!");
        System.out.println("  Parent: " + deserialized.name + " (" + deserialized.value + ")");
        System.out.println("  Child: " + deserialized.child.name + " (" + deserialized.child.value + ")");
        System.out.println("  Circular reference preserved: " + (deserialized == deserialized.child.parent));
    }

    @Test
    public void serialize_SelfReference_preservesStructure() throws Throwable {
        // Create self-referencing structure
        CircularNode node = new CircularNode();
        node.name = "SelfRef";
        node.value = 100;
        node.parent = node; // Self-reference

        // Serialize
        byte[] data = serializer.serialize(node);
        assertNotNull(data);
        assertTrue(data.length > 0);

        // Deserialize
        CircularNode deserialized = serializer.deserialize(data);
        assertNotNull(deserialized);

        // Verify structure is preserved
        assertEquals("SelfRef", deserialized.name);
        assertEquals(100, deserialized.value);

        // Verify self-reference is preserved
        assertSame(deserialized, deserialized.parent); // Should be the same object (self-reference)

        System.out.println("✅ Self-reference test passed!");
        System.out.println("  Node: " + deserialized.name + " (" + deserialized.value + ")");
        System.out.println("  Self-reference preserved: " + (deserialized == deserialized.parent));
    }

    @Test
    public void serialize_DeepCircularChain_preservesStructure() throws Throwable {
        // Create deeper circular chain: A -> B -> C -> A
        CircularNode nodeA = new CircularNode();
        nodeA.name = "A";
        nodeA.value = 1;

        CircularNode nodeB = new CircularNode();
        nodeB.name = "B";
        nodeB.value = 2;

        CircularNode nodeC = new CircularNode();
        nodeC.name = "C";
        nodeC.value = 3;

        // Create circular chain
        nodeA.child = nodeB;
        nodeB.child = nodeC;
        nodeC.parent = nodeA; // Circular reference

        // Serialize
        byte[] data = serializer.serialize(nodeA);
        assertNotNull(data);

        // Deserialize
        CircularNode deserialized = serializer.deserialize(data);
        assertNotNull(deserialized);

        // Verify structure
        assertEquals("A", deserialized.name);
        assertEquals("B", deserialized.child.name);
        assertEquals("C", deserialized.child.child.name);

        // Verify circular reference
        assertSame(deserialized, deserialized.child.child.parent);

        System.out.println("✅ Deep circular chain test passed!");
        System.out.println("  Chain: A -> B -> C -> A (circular)");
        System.out.println("  Circular reference preserved: " + (deserialized == deserialized.child.child.parent));
    }

    // Test class with circular references
    public static class CircularNode {
        public String name;
        public int value;
        public CircularNode parent;
        public CircularNode child;

        public CircularNode() {}
    }
}
