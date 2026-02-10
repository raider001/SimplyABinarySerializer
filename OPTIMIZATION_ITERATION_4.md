# TypedSerializer Optimization - Iteration 4 (Null Bitmap)

## Goal
Shave off another 10ns from both serialization and deserialization to achieve sub-40ns performance.

## Optimization Applied

### Null Bitmap Optimization

**Problem:** In Iteration 3, every object-type field wrote a 1-byte null marker (0 or 1), adding overhead:
- **Cost per field**: 1 byte write + 1 conditional check = ~2-3 cycles
- **For SimpleObject**: 1 String field = ~2-3ns overhead

**Solution:** Use a null bitmap to track all null fields upfront:
- Write a single bitmap (1 byte for up to 8 object fields)
- Primitives always written (never null)
- Objects checked via bitmap bit

**Implementation:**

```java
// Constructor - calculate bitmap size
int objectFieldCount = 0;
for (FieldSchema f : schema.fields) {
    if (!f.type.isPrimitive()) objectFieldCount++;
}
this.nullBitmapBytes = (objectFieldCount + 7) / 8;

// Serialization - build and write bitmap once
byte[] nullBitmap = new byte[nullBitmapBytes];
int nullableIdx = 0;
for (int i = 0; i < len; i++) {
    FieldSchema f = fields[i];
    if (!f.type.isPrimitive()) {
        Object v = f.getter != null ? f.getter.invoke(obj) : f.field.get(obj);
        if (v == null) {
            nullBitmap[nullableIdx / 8] |= (1 << (nullableIdx % 8));
        }
        nullableIdx++;
    }
}
w.writeBytes(nullBitmap); // Single write

// Then write fields (skip reading null objects)
nullableIdx = 0;
for (int i = 0; i < len; i++) {
    FieldSchema f = fields[i];
    if (f.type.isPrimitive()) {
        // Direct write
        w.writeInt(f.field.getInt(obj));
    } else {
        // Check bitmap
        boolean isNull = (nullBitmap[nullableIdx / 8] & (1 << (nullableIdx % 8))) != 0;
        nullableIdx++;
        if (!isNull) {
            // Write field
        }
    }
}
```

**Benefits:**
- **Eliminates per-field null markers** - 1 bit instead of 1 byte per object field
- **Single bitmap write** - More cache-friendly than scattered writes
- **Branch prediction** - Bitmap check is more predictable than scattered nulls

**Expected Impact:**
- **SimpleObject**: ~3-5ns improvement (1 String field)
- **ComplexObject**: ~5-8ns improvement (multiple object fields)
- **Binary size**: Slightly smaller for objects with few object fields

## Code Structure

### Added Fields
```java
private final int nullBitmapBytes; // Null bitmap size
```

### Added Helper Method
```java
boolean isPrimitive() {
    return this == INT || this == LONG || this == BOOLEAN ||
           this == DOUBLE || this == FLOAT || this == SHORT;
}
```

### Modified Methods
- `TypedSerializer()` constructor - calculates nullBitmapBytes
- `writeObject()` - builds and writes null bitmap
- `readObject()` - reads and uses null bitmap
- `readObjectGeneric()` - reads and uses null bitmap

## Expected Results

### Target Performance (vs Iteration 3)

| Metric | Iteration 3 | Target (Iteration 4) | Improvement |
|--------|-------------|----------------------|-------------|
| SimpleObject Serialize | 50.4 ns | **~45 ns** | **5 ns faster** |
| SimpleObject Deserialize | 60.6 ns | **~55 ns** | **5 ns faster** |
| ComplexObject Serialize | 123.7 ns | **~115 ns** | **8 ns faster** |
| ComplexObject Deserialize | 201.7 ns | **~193 ns** | **8 ns faster** |

### Confidence Level
**75%** - Null bitmap optimization is proven, but JIT behavior can vary. Actual improvement depends on:
- Branch prediction effectiveness
- Cache line alignment
- JIT inlining decisions

## Verification Steps

1. ✅ Code compiles successfully
2. ⏳ Run ExtremeBenchmarkRunner (50 runs × 1M iterations)
3. ⏳ Compare results to Iteration 3 baseline
4. ⏳ Verify correctness with all test types
5. ⏳ Check binary size changes

## Next Steps if Goal Not Achieved

If we don't reach the target 10ns improvement:

### Additional Optimizations to Try:

1. **VarHandle instead of MethodHandle** - Newer API, potentially faster
2. **Pre-allocate nested writers** - Reduce allocation overhead
3. **Inline small strings** - Avoid length byte for very short strings
4. **Collection capacity hints** - Better pre-sizing for Lists/Maps
5. **Field ordering optimization** - Reorder fields by type for better cache locality

---

*Status: ⏳ Running benchmark...*
*Date: February 10, 2026*

