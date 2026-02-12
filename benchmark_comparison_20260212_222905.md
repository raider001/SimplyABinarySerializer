# Serialization Benchmark Results

**Date:** 2026-02-12 22:29:05

**Total test runs:** 18

## SimpleObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| TypedSerializer | 53.30 | 54.22 | 107.52 | 36 |
| GeneratedSerializer | **24.06** | 37.58 | **61.64** | 36 |
| Kryo | 419.93 | 341.98 | 761.90 | **25** |
| Jackson | 350.64 | 444.25 | 794.88 | 112 |
| Gson | 586.95 | 581.54 | 1168.49 | 112 |
| Apache Fury | 42.31 | **25.95** | 68.26 | 31 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| TypedSerializer | 18,763,451 | 18,443,005 | 9,300,924 |
| GeneratedSerializer | **41,562,069** | 26,606,784 | **16,221,968** |
| Kryo | 2,381,363 | 2,924,189 | 1,312,504 |
| Jackson | 2,851,957 | 2,250,994 | 1,258,044 |
| Gson | 1,703,717 | 1,719,586 | 855,807 |
| Apache Fury | 23,637,253 | **38,535,200** | 14,650,641 |

## ComplexObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| TypedSerializer | 142.06 | 202.00 | 344.05 | 68 |
| GeneratedSerializer | **123.05** | 167.40 | 290.45 | 69 |
| Kryo | 629.15 | 669.58 | 1298.73 | 135 |
| Jackson | 326.11 | 505.96 | 832.07 | 122 |
| Gson | 864.46 | 962.92 | 1827.38 | 122 |
| Apache Fury | 142.47 | **127.26** | **269.73** | **61** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| TypedSerializer | 7,039,329 | 4,950,605 | 2,906,516 |
| GeneratedSerializer | **8,126,943** | 5,973,644 | 3,442,939 |
| Kryo | 1,589,450 | 1,493,466 | 769,982 |
| Jackson | 3,066,422 | 1,976,449 | 1,201,821 |
| Gson | 1,156,786 | 1,038,512 | 547,232 |
| Apache Fury | 7,018,874 | **7,858,145** | **3,707,418** |

## DeepObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| TypedSerializer | 185.82 | 230.01 | 415.83 | 125 |
| GeneratedSerializer | **174.17** | 235.25 | 409.42 | 129 |
| Kryo | 741.32 | 710.65 | 1451.97 | **90** |
| Jackson | 533.13 | 971.84 | 1504.97 | 244 |
| Gson | 1500.44 | 1094.69 | 2595.13 | 231 |
| Apache Fury | 235.15 | **164.99** | **400.14** | 112 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| TypedSerializer | 5,381,610 | 4,347,694 | 2,404,858 |
| GeneratedSerializer | **5,741,418** | 4,250,851 | 2,442,480 |
| Kryo | 1,348,945 | 1,407,157 | 688,718 |
| Jackson | 1,875,715 | 1,028,973 | 664,464 |
| Gson | 666,473 | 913,498 | 385,337 |
| Apache Fury | 4,252,550 | **6,060,973** | **2,499,107** |

## Summary - Fastest Library by Category

### SimpleObject

- **Fastest Serialization:** GeneratedSerializer (24.06 ns/op)
- **Fastest Deserialization:** Apache Fury (25.95 ns/op)
- **Fastest Round-Trip:** GeneratedSerializer (61.64 ns/op)
- **Smallest Binary Size:** Kryo (25 bytes)

### ComplexObject

- **Fastest Serialization:** GeneratedSerializer (123.05 ns/op)
- **Fastest Deserialization:** Apache Fury (127.26 ns/op)
- **Fastest Round-Trip:** Apache Fury (269.73 ns/op)
- **Smallest Binary Size:** Apache Fury (61 bytes)

### DeepObject

- **Fastest Serialization:** GeneratedSerializer (174.17 ns/op)
- **Fastest Deserialization:** Apache Fury (164.99 ns/op)
- **Fastest Round-Trip:** Apache Fury (400.14 ns/op)
- **Smallest Binary Size:** Kryo (90 bytes)

