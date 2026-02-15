package com.kalynx.simplyabinaryserializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A type reference for capturing generic type information at runtime.
 * This class uses a technique similar to Gson's TypeToken and Jackson's TypeReference.
 *
 * Usage:
 * <pre>
 * // For List&lt;Integer&gt;
 * TypeReference&lt;List&lt;Integer&gt;&gt; ref = new TypeReference&lt;List&lt;Integer&gt;&gt;() {};
 *
 * // For Map&lt;String, User&gt;
 * TypeReference&lt;Map&lt;String, User&gt;&gt; ref = new TypeReference&lt;Map&lt;String, User&gt;&gt;() {};
 * </pre>
 *
 * @param <T> The generic type being referenced
 */
public abstract class TypeReference<T> {

    private final Type type;
    private final Class<T> rawType;

    @SuppressWarnings("unchecked")
    protected TypeReference() {
        Type superclass = getClass().getGenericSuperclass();
        if (superclass instanceof ParameterizedType) {
            this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];

            // Extract the raw type
            if (type instanceof ParameterizedType) {
                this.rawType = (Class<T>) ((ParameterizedType) type).getRawType();
            } else if (type instanceof Class) {
                this.rawType = (Class<T>) type;
            } else {
                throw new IllegalArgumentException("Unsupported type: " + type);
            }
        } else {
            throw new IllegalArgumentException(
                "TypeReference must be instantiated with an anonymous subclass to capture generic type information");
        }
    }

    /**
     * Get the captured type, including generic parameters.
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the raw class type (without generic parameters).
     */
    public Class<T> getRawType() {
        return rawType;
    }

    @Override
    public String toString() {
        return "TypeReference<" + type + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TypeReference)) return false;
        TypeReference<?> other = (TypeReference<?>) obj;
        return type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }
}

