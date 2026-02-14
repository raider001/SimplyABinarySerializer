package com.kalynx.simplyabinaryserializer.serializer;

import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Protected constants used during serialization bytecode generation.
 * Contains ClassDesc references and counters needed for writer generation.
 */
public class SerializerConstants {

    public static final AtomicLong CLASS_COUNTER = new AtomicLong(0);

    public static final ClassDesc CD_FastByteWriter = ClassDesc.of("com.kalynx.simplyabinaryserializer.serializer.FastByteWriter");
    public static final ClassDesc CD_FastByteReader = ClassDesc.of("com.kalynx.simplyabinaryserializer.deserializer.FastByteReader");
    public static final ClassDesc CD_OptimizedSerializer = ClassDesc.of("com.kalynx.simplyabinaryserializer.OptimizedSerializer");
    public static final ClassDesc CD_Object = ConstantDescs.CD_Object;
    public static final ClassDesc CD_String = ConstantDescs.CD_String;
    public static final ClassDesc CD_List = ClassDesc.of("java.util.List");
    public static final ClassDesc CD_Map = ClassDesc.of("java.util.Map");
    public static final ClassDesc CD_ArrayList = ClassDesc.of("java.util.ArrayList");
    public static final ClassDesc CD_HashMap = ClassDesc.of("java.util.HashMap");

    // Wrapper class descriptors
    public static final ClassDesc CD_Integer = ClassDesc.of("java.lang.Integer");
    public static final ClassDesc CD_Long = ClassDesc.of("java.lang.Long");
    public static final ClassDesc CD_Double = ClassDesc.of("java.lang.Double");
    public static final ClassDesc CD_Float = ClassDesc.of("java.lang.Float");
    public static final ClassDesc CD_Short = ClassDesc.of("java.lang.Short");
    public static final ClassDesc CD_Byte = ClassDesc.of("java.lang.Byte");
    public static final ClassDesc CD_Boolean = ClassDesc.of("java.lang.Boolean");
    public static final ClassDesc CD_Character = ClassDesc.of("java.lang.Character");

    // Charset descriptors
    public static final ClassDesc CD_StandardCharsets = ClassDesc.of(StandardCharsets.class.getName());
    public static final ClassDesc CD_Charset = ClassDesc.of(Charset.class.getName());
    public static final ClassDesc CD_byte_array = ConstantDescs.CD_byte.arrayType();

    // MethodTypeDesc for FastByteWriter methods
    public static final MethodTypeDesc MTD_void_int = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int);
    public static final MethodTypeDesc MTD_void_long = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_long);
    public static final MethodTypeDesc MTD_void_double = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_double);
    public static final MethodTypeDesc MTD_void_float = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_float);
    public static final MethodTypeDesc MTD_void_short = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_short);
    public static final MethodTypeDesc MTD_void_byte = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_byte);
    public static final MethodTypeDesc MTD_void_boolean = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_boolean);
    public static final MethodTypeDesc MTD_void_byteArray = MethodTypeDesc.of(ConstantDescs.CD_void, CD_byte_array);

    // MethodTypeDesc for FastByteReader methods
    public static final MethodTypeDesc MTD_int = MethodTypeDesc.of(ConstantDescs.CD_int);
    public static final MethodTypeDesc MTD_long = MethodTypeDesc.of(ConstantDescs.CD_long);
    public static final MethodTypeDesc MTD_double = MethodTypeDesc.of(ConstantDescs.CD_double);
    public static final MethodTypeDesc MTD_float = MethodTypeDesc.of(ConstantDescs.CD_float);
    public static final MethodTypeDesc MTD_short = MethodTypeDesc.of(ConstantDescs.CD_short);
    public static final MethodTypeDesc MTD_byte = MethodTypeDesc.of(ConstantDescs.CD_byte);
    public static final MethodTypeDesc MTD_boolean = MethodTypeDesc.of(ConstantDescs.CD_boolean);
    public static final MethodTypeDesc MTD_String = MethodTypeDesc.of(ConstantDescs.CD_String);
    public static final MethodTypeDesc MTD_byte_array = MethodTypeDesc.of(CD_byte_array);
    public static final MethodTypeDesc MTD_byte_array_int = MethodTypeDesc.of(CD_byte_array, ConstantDescs.CD_int);

    public static final int ACC_PUBLIC_FINAL_SYNTHETIC = ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;
    public static final int ACC_PUBLIC = ClassFile.ACC_PUBLIC;
    public static final int ACC_FINAL = ClassFile.ACC_FINAL;
    public static final int ACC_SYNTHETIC = ClassFile.ACC_SYNTHETIC;

    private SerializerConstants() {
    }
}

