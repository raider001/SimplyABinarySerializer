package com.kalynx.simplyabinaryserializer.serializer;

import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Protected constants used during serialization bytecode generation.
 * Contains ClassDesc references and counters needed for writer generation.
 */
public class SerializerConstants {

    public static final AtomicLong CLASS_COUNTER = new AtomicLong(0);

    public static final ClassDesc CD_FastByteWriter = ClassDesc.of("com.kalynx.simplyabinaryserializer.serializer.FastByteWriter");
    public static final ClassDesc CD_OptimizedSerializer = ClassDesc.of("com.kalynx.simplyabinaryserializer.OptimizedSerializer");
    public static final ClassDesc CD_Object = ConstantDescs.CD_Object;
    public static final ClassDesc CD_String = ConstantDescs.CD_String;
    public static final ClassDesc CD_List = ClassDesc.of("java.util.List");
    public static final ClassDesc CD_Map = ClassDesc.of("java.util.Map");
    public static final ClassDesc CD_ArrayList = ClassDesc.of("java.util.ArrayList");
    public static final ClassDesc CD_HashMap = ClassDesc.of("java.util.HashMap");

    public static final int ACC_PUBLIC_FINAL_SYNTHETIC = ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SYNTHETIC;

    private SerializerConstants() {
    }
}

