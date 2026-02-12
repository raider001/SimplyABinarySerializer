# Serialization Benchmark Results

**Date:** 2026-02-12 22:29:20

**Total test runs:** 18

## SimpleObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| TypedSerializer | 54.22 | 52.86 | 107.08 | 36 |
| GeneratedSerializer | **30.73** | 39.14 | 69.87 | 36 |
| Kryo | 445.41 | 298.13 | 743.54 | **25** |
| Jackson | 355.64 | 457.86 | 813.50 | 112 |
| Gson | 597.70 | 524.38 | 1122.08 | 112 |
| Apache Fury | 42.13 | **25.25** | **67.38** | 31 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| TypedSerializer | 18,442,835 | 18,918,183 | 9,338,742 |
| GeneratedSerializer | **32,537,890** | 25,551,138 | 14,312,171 |
| Kryo | 2,245,101 | 3,354,263 | 1,344,913 |
| Jackson | 2,811,857 | 2,184,077 | 1,229,262 |
| Gson | 1,673,072 | 1,907,017 | 891,200 |
| Apache Fury | 23,734,872 | **39,609,294** | **14,841,486** |

## ComplexObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| TypedSerializer | 141.35 | 220.31 | 361.65 | 68 |
| GeneratedSerializer | **118.82** | 177.24 | 296.06 | 69 |
| Kryo | 658.35 | 646.89 | 1305.24 | 135 |
| Jackson | 316.78 | 503.75 | 820.52 | 122 |
| Gson | 741.75 | 1023.36 | 1765.11 | 122 |
| Apache Fury | 141.17 | **124.04** | **265.21** | **61** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| TypedSerializer | 7,074,888 | 4,539,100 | 2,765,081 |
| GeneratedSerializer | **8,416,269** | 5,642,083 | 3,377,728 |
| Kryo | 1,518,956 | 1,545,850 | 766,142 |
| Jackson | 3,156,810 | 1,985,116 | 1,218,733 |
| Gson | 1,348,158 | 977,178 | 566,538 |
| Apache Fury | 7,083,608 | **8,061,948** | **3,770,590** |

## DeepObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| TypedSerializer | 179.86 | 224.10 | 403.96 | 125 |
| GeneratedSerializer | **176.99** | 221.64 | 398.62 | 129 |
| Kryo | 415.00 | 416.92 | 831.92 | **90** |
| Jackson | 529.90 | 974.68 | 1504.58 | 244 |
| Gson | 1620.89 | 1188.99 | 2809.88 | 231 |
| Apache Fury | 223.58 | **109.50** | **333.08** | 112 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| TypedSerializer | 5,559,911 | 4,462,294 | 2,475,499 |
| GeneratedSerializer | **5,650,196** | 4,511,862 | 2,508,636 |
| Kryo | 2,409,650 | 2,398,559 | 1,202,046 |
| Jackson | 1,887,149 | 1,025,978 | 664,637 |
| Gson | 616,945 | 841,049 | 355,887 |
| Apache Fury | 4,472,772 | **9,132,337** | **3,002,318** |

## Summary - Fastest Library by Category

### SimpleObject

- **Fastest Serialization:** GeneratedSerializer (30.73 ns/op)
- **Fastest Deserialization:** Apache Fury (25.25 ns/op)
- **Fastest Round-Trip:** Apache Fury (67.38 ns/op)
- **Smallest Binary Size:** Kryo (25 bytes)

### ComplexObject

- **Fastest Serialization:** GeneratedSerializer (118.82 ns/op)
- **Fastest Deserialization:** Apache Fury (124.04 ns/op)
- **Fastest Round-Trip:** Apache Fury (265.21 ns/op)
- **Smallest Binary Size:** Apache Fury (61 bytes)

### DeepObject

- **Fastest Serialization:** GeneratedSerializer (176.99 ns/op)
- **Fastest Deserialization:** Apache Fury (109.50 ns/op)
- **Fastest Round-Trip:** Apache Fury (333.08 ns/op)
- **Smallest Binary Size:** Kryo (90 bytes)

