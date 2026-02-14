package com.kalynx.simplyabinaryserializer.ClassObjects;

import com.kalynx.simplyabinaryserializer.OptimizedSerializer;

import java.lang.reflect.Field;

public record FieldInfo(Field field, int fieldIndex, FieldType type, FieldType listElementType, FieldType arrayElementType, FieldType mapKeyType, FieldType mapValueType) {
}
