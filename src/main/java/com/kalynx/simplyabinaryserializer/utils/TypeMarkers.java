package com.kalynx.simplyabinaryserializer.utils;

public class TypeMarkers {
    // Type markers
    public static final byte TYPE_STRING = 1;
    public static final byte TYPE_INT = 2;
    public static final byte TYPE_LONG = 3;
    public static final byte TYPE_BOOLEAN = 4;
    public static final byte TYPE_DOUBLE = 5;
    public static final byte TYPE_MAP = 6;
    public static final byte TYPE_OBJECT = 7;
    public static final byte TYPE_LIST = 8;
    public static final byte TYPE_SET = 9;
    public static final byte TYPE_ARRAY = 10;
    public static final byte TYPE_NULL = 11;
    public static final byte TYPE_FLOAT = 15;
    public static final byte TYPE_SHORT = 16;

    // Optimized type markers
    public static final byte TYPE_OBJECT_PACKED = 12; // Object with packed field descriptors
    static final byte TYPE_LIST_STRING = 13;   // Homogeneous list of strings
    static final byte TYPE_SCHEMALESS = 14;    // Schema-based (no type markers, class as template)

    static final byte NIBBLE_NULL = 0x0;
    static final byte NIBBLE_STRING = 0x1;
    static final byte NIBBLE_INT = 0x2;
    static final byte NIBBLE_LONG = 0x3;
    static final byte NIBBLE_BOOLEAN = 0x4;
    static final byte NIBBLE_DOUBLE = 0x5;
    static final byte NIBBLE_FLOAT = 0xA;
    static final byte NIBBLE_SHORT = 0xB;
    static final byte NIBBLE_LIST_STRING = 0x6; // Homogeneous string list
    static final byte NIBBLE_LIST_GENERIC = 0x7;
    static final byte NIBBLE_NESTED_OBJECT = 0x8;
    static final byte NIBBLE_MAP = 0x9; // Map type

}
