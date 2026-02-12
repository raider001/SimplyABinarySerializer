# Serialization Benchmark Results

**Date:** 2026-02-12 22:22:41

**Total test runs:** 18

## SimpleObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| TypedSerializer | 52.75 | 72.22 | 124.97 | 36 |
| GeneratedSerializer | 106.94 | 65.80 | 172.73 | 36 |
| Kryo | 402.05 | 310.72 | 712.77 | **25** |
| Jackson | 328.92 | 477.98 | 806.90 | 112 |
| Gson | 618.49 | 562.55 | 1181.04 | 112 |
| Apache Fury | **46.12** | **22.75** | **68.87** | 31 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| TypedSerializer | 18,955,944 | 13,846,542 | 8,001,658 |
| GeneratedSerializer | 9,351,440 | 15,198,007 | 5,789,265 |
| Kryo | 2,487,241 | 3,218,377 | 1,402,982 |
| Jackson | 3,040,294 | 2,092,117 | 1,239,310 |
| Gson | 1,616,842 | 1,777,614 | 846,711 |
| Apache Fury | **21,683,366** | **43,951,407** | **14,519,963** |

## ComplexObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| TypedSerializer | 161.83 | 272.56 | 434.39 | 68 |
| GeneratedSerializer | 210.57 | 194.92 | 405.49 | 73 |
| Kryo | 664.94 | 654.39 | 1319.33 | 135 |
| Jackson | 281.64 | 518.72 | 800.35 | 122 |
| Gson | 737.45 | 894.20 | 1631.65 | 122 |
| Apache Fury | **143.96** | **115.86** | **259.82** | **61** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| TypedSerializer | 6,179,190 | 3,668,984 | 2,302,087 |
| GeneratedSerializer | 4,748,958 | 5,130,363 | 2,466,149 |
| Kryo | 1,503,898 | 1,528,137 | 757,960 |
| Jackson | 3,550,676 | 1,927,835 | 1,249,449 |
| Gson | 1,356,020 | 1,118,319 | 612,876 |
| Apache Fury | **6,946,253** | **8,631,330** | **3,848,826** |

## DeepObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| TypedSerializer | **178.62** | 226.56 | 405.18 | 125 |
| GeneratedSerializer | 333.86 | 361.53 | 695.39 | 145 |
| Kryo | 405.05 | 404.75 | 809.80 | **90** |
| Jackson | 689.25 | 1127.70 | 1816.95 | 244 |
| Gson | 1648.71 | 1449.67 | 3098.38 | 231 |
| Apache Fury | 218.95 | **97.94** | **316.89** | 112 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| TypedSerializer | **5,598,509** | 4,413,803 | 2,468,033 |
| GeneratedSerializer | 2,995,250 | 2,766,053 | 1,438,046 |
| Kryo | 2,468,843 | 2,470,673 | 1,234,879 |
| Jackson | 1,450,861 | 886,760 | 550,374 |
| Gson | 606,535 | 689,813 | 322,750 |
| Apache Fury | 4,567,169 | **10,210,541** | **3,155,649** |

## Summary - Fastest Library by Category

### SimpleObject

- **Fastest Serialization:** Apache Fury (46.12 ns/op)
- **Fastest Deserialization:** Apache Fury (22.75 ns/op)
- **Fastest Round-Trip:** Apache Fury (68.87 ns/op)
- **Smallest Binary Size:** Kryo (25 bytes)

### ComplexObject

- **Fastest Serialization:** Apache Fury (143.96 ns/op)
- **Fastest Deserialization:** Apache Fury (115.86 ns/op)
- **Fastest Round-Trip:** Apache Fury (259.82 ns/op)
- **Smallest Binary Size:** Apache Fury (61 bytes)

### DeepObject

- **Fastest Serialization:** TypedSerializer (178.62 ns/op)
- **Fastest Deserialization:** Apache Fury (97.94 ns/op)
- **Fastest Round-Trip:** Apache Fury (316.89 ns/op)
- **Smallest Binary Size:** Kryo (90 bytes)

