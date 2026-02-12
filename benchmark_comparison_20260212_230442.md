# Serialization Benchmark Results

**Date:** 2026-02-12 23:04:42

**Total test runs:** 18

## SimpleObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| GeneratedSerializer | **23.11** | **24.29** | **47.40** | 34 |
| TypedSerializer | 47.59 | 48.12 | 95.71 | 36 |
| Apache Fury | 83.18 | 57.72 | 140.89 | 31 |
| Kryo | 345.13 | 335.58 | 680.71 | **25** |
| Jackson | 302.73 | 469.68 | 772.41 | 112 |
| Gson | 593.32 | 537.83 | 1131.15 | 112 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| GeneratedSerializer | **43,270,319** | **41,173,850** | **21,098,030** |
| TypedSerializer | 21,011,268 | 20,780,853 | 10,447,713 |
| Apache Fury | 12,022,468 | 17,326,035 | 7,097,524 |
| Kryo | 2,897,468 | 2,979,941 | 1,469,063 |
| Jackson | 3,303,237 | 2,129,106 | 1,294,642 |
| Gson | 1,685,427 | 1,859,318 | 884,054 |

## ComplexObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| GeneratedSerializer | **92.72** | **148.00** | **240.72** | 66 |
| TypedSerializer | 263.55 | 185.99 | 449.54 | 68 |
| Apache Fury | 355.45 | 188.13 | 543.58 | **61** |
| Jackson | 312.00 | 618.53 | 930.53 | 122 |
| Kryo | 637.18 | 756.74 | 1393.91 | 135 |
| Gson | 729.54 | 690.62 | 1420.16 | 122 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| GeneratedSerializer | **10,785,561** | **6,756,686** | **4,154,237** |
| TypedSerializer | 3,794,292 | 5,376,682 | 2,224,486 |
| Apache Fury | 2,813,296 | 5,315,589 | 1,839,653 |
| Jackson | 3,205,131 | 1,616,740 | 1,074,658 |
| Kryo | 1,569,424 | 1,321,461 | 717,404 |
| Gson | 1,370,734 | 1,447,975 | 704,148 |

## DeepObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| GeneratedSerializer | 195.67 | **158.02** | **353.68** | 123 |
| TypedSerializer | **170.55** | 207.90 | 378.46 | 125 |
| Apache Fury | 219.15 | 187.40 | 406.55 | 112 |
| Kryo | 485.94 | 491.12 | 977.06 | **90** |
| Jackson | 529.60 | 1216.61 | 1746.21 | 244 |
| Gson | 1535.47 | 1073.26 | 2608.73 | 231 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| GeneratedSerializer | 5,110,706 | **6,328,477** | **2,827,386** |
| TypedSerializer | **5,863,246** | 4,809,952 | 2,642,313 |
| Apache Fury | 4,563,049 | 5,336,299 | 2,459,737 |
| Kryo | 2,057,885 | 2,036,159 | 1,023,482 |
| Jackson | 1,888,214 | 821,956 | 572,668 |
| Gson | 651,265 | 931,743 | 383,328 |

## Summary - Fastest Library by Category

### SimpleObject

- **Fastest Serialization:** GeneratedSerializer (23.11 ns/op)
- **Fastest Deserialization:** GeneratedSerializer (24.29 ns/op)
- **Fastest Round-Trip:** GeneratedSerializer (47.40 ns/op)
- **Smallest Binary Size:** Kryo (25 bytes)

### ComplexObject

- **Fastest Serialization:** GeneratedSerializer (92.72 ns/op)
- **Fastest Deserialization:** GeneratedSerializer (148.00 ns/op)
- **Fastest Round-Trip:** GeneratedSerializer (240.72 ns/op)
- **Smallest Binary Size:** Apache Fury (61 bytes)

### DeepObject

- **Fastest Serialization:** TypedSerializer (170.55 ns/op)
- **Fastest Deserialization:** GeneratedSerializer (158.02 ns/op)
- **Fastest Round-Trip:** GeneratedSerializer (353.68 ns/op)
- **Smallest Binary Size:** Kryo (90 bytes)

