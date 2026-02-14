package com.kalynx.simplyabinaryserializer.utils;

import com.kalynx.simplyabinaryserializer.ClassObjects.FieldType;
import com.kalynx.simplyabinaryserializer.OptimizedSerializer;

public class PrimitiveSizeFactory {

    static int getPrimitiveSize(FieldType type) {
        return switch (type) {
            case BYTE -> 1;
            case BOOLEAN -> 1;
            case SHORT -> 2;
            case CHAR -> 2;
            case INT -> 4;
            case FLOAT -> 4;
            case LONG -> 8;
            case DOUBLE -> 8;
            case STRING -> 128;
            case LIST -> 0;
            case MAP -> 0;
            case OBJECT -> 0;
            case ENUM -> 1;
            case ARRAY -> 0;
        };
    }
}
