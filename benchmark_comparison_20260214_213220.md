# Serialization Benchmark Results

**Date:** 2026-02-14 21:32:20

**Total test runs:** 54

## Summary: Round-Trip Performance (ns/op)

*Lower is better. Best result per scenario is highlighted in **bold**.*

| Scenario | KalynxSerializer | Kryo | Apache Fury |
|----------|----------:|----------:|----------:|
| AllPrimitivesObject | **57.91** | 777.85 | 79.91 |
| IntegerListObject | **160.25** | 1583.77 | 344.64 |
| StringListObject | 566.16 | 1497.79 | **321.70** |
| LongListObject | **200.08** | 782.21 | 268.59 |
| DoubleListObject | **262.31** | 773.40 | 483.35 |
| MixedPrimitiveAndListObject | **171.92** | 1273.53 | 259.05 |
| AllPrimitivesWithListsObject | **197.15** | 1021.49 | 476.10 |
| StringIntegerMapObject | 999.68 | 2208.88 | **760.49** |
| IntegerStringMapObject | **691.27** | 1476.45 | 1346.51 |
| IntegerIntegerMapObject | **285.34** | 1834.88 | 736.01 |
| LongDoubleMapObject | **355.56** | 1218.30 | 610.25 |
| IntArrayObject | **105.28** | 774.42 | 143.53 |
| LongArrayObject | **114.08** | 977.19 | 192.08 |
| DoubleArrayObject | **136.23** | 747.05 | 155.34 |
| AllPrimitiveArraysObject | 459.90 | 1033.28 | **312.57** |
| SimpleNestedObject | 372.70 | 986.36 | **301.30** |
| Rectangle | **146.98** | 1020.83 | 291.48 |
| DeepNestedLevel1 | **334.58** | 1153.35 | 336.58 |

## AllPrimitivesObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **28.66** | **29.25** | **57.91** | 30 |
| Apache Fury | 40.29 | 39.62 | 79.91 | 103 |
| Kryo | 332.81 | 445.04 | 777.85 | **26** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **34,896,706** | **34,188,034** | **17,269,368** |
| Apache Fury | 24,818,207 | 25,241,052 | 12,513,922 |
| Kryo | 3,004,708 | 2,246,974 | 1,285,588 |

## IntegerListObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **54.54** | **105.71** | **160.25** | 54 |
| Apache Fury | 229.77 | 114.87 | 344.64 | 116 |
| Kryo | 793.91 | 789.85 | 1583.77 | **42** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **18,336,512** | **9,459,753** | **6,240,366** |
| Apache Fury | 4,352,140 | 8,705,796 | 2,901,595 |
| Kryo | 1,259,582 | 1,266,060 | 631,406 |

## StringListObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | **230.94** | **90.76** | **321.70** | 150 |
| KalynxSerializer | 250.18 | 315.98 | 566.16 | 94 |
| Kryo | 748.05 | 749.74 | 1497.79 | **72** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | **4,330,185** | **11,018,312** | **3,108,534** |
| KalynxSerializer | 3,997,122 | 3,164,727 | 1,766,276 |
| Kryo | 1,336,818 | 1,333,794 | 667,652 |

## LongListObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **63.85** | 136.22 | **200.08** | 76 |
| Apache Fury | 184.35 | **84.24** | 268.59 | 146 |
| Kryo | 359.10 | 423.11 | 782.21 | **58** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **15,660,726** | 7,341,012 | **4,998,126** |
| Apache Fury | 5,424,347 | **11,871,268** | 3,723,133 |
| Kryo | 2,784,740 | 2,363,474 | 1,278,436 |

## DoubleListObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **84.72** | 177.60 | **262.31** | 58 |
| Apache Fury | 351.79 | **131.55** | 483.35 | 123 |
| Kryo | 381.65 | 391.75 | 773.40 | **50** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **11,804,285** | 5,630,726 | **3,812,254** |
| Apache Fury | 2,842,581 | **7,601,499** | 2,068,911 |
| Kryo | 2,620,209 | 2,552,629 | 1,292,989 |

## MixedPrimitiveAndListObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **63.01** | 108.92 | **171.92** | 33 |
| Apache Fury | 174.56 | **84.49** | 259.05 | 101 |
| Kryo | 661.83 | 611.70 | 1273.53 | **20** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **15,871,001** | 9,181,472 | **5,816,557** |
| Apache Fury | 5,728,788 | **11,835,440** | 3,860,274 |
| Kryo | 1,510,960 | 1,634,794 | 785,220 |

## AllPrimitivesWithListsObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **96.86** | **100.29** | **197.15** | 81 |
| Apache Fury | 330.18 | 145.92 | 476.10 | 142 |
| Kryo | 455.52 | 565.97 | 1021.49 | **55** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **10,324,286** | **9,970,686** | **5,072,203** |
| Apache Fury | 3,028,651 | 6,853,023 | 2,100,395 |
| Kryo | 2,195,293 | 1,766,869 | 978,959 |

## StringIntegerMapObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 534.16 | **226.33** | **760.49** | 259 |
| KalynxSerializer | **487.01** | 512.66 | 999.68 | 174 |
| Kryo | 1252.32 | 956.56 | 2208.88 | **162** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 1,872,098 | **4,418,386** | **1,314,947** |
| KalynxSerializer | **2,053,329** | 1,950,603 | 1,000,324 |
| Kryo | 798,519 | 1,045,412 | 452,718 |

## IntegerStringMapObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **433.91** | **257.36** | **691.27** | 194 |
| Apache Fury | 974.45 | 372.06 | 1346.51 | 279 |
| Kryo | 873.52 | 602.93 | 1476.45 | **182** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **2,304,652** | **3,885,608** | **1,446,623** |
| Apache Fury | 1,026,219 | 2,687,739 | 742,660 |
| Kryo | 1,144,797 | 1,658,556 | 677,300 |

## IntegerIntegerMapObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **122.81** | **162.54** | **285.34** | 84 |
| Apache Fury | 451.33 | 284.68 | 736.01 | 159 |
| Kryo | 908.31 | 926.58 | 1834.88 | **82** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **8,142,925** | **6,152,407** | **3,504,542** |
| Apache Fury | 2,215,688 | 3,512,679 | 1,358,677 |
| Kryo | 1,100,952 | 1,079,242 | 544,994 |

## LongDoubleMapObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **146.03** | **209.53** | **355.56** | 164 |
| Apache Fury | 328.86 | 281.39 | 610.25 | 267 |
| Kryo | 691.36 | 526.94 | 1218.30 | **162** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **6,847,861** | **4,772,586** | **2,812,457** |
| Apache Fury | 3,040,808 | 3,553,787 | 1,638,673 |
| Kryo | 1,446,416 | 1,897,764 | 820,816 |

## IntArrayObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **45.11** | 60.17 | **105.28** | 44 |
| Apache Fury | 108.27 | **35.26** | 143.53 | 112 |
| Kryo | 427.94 | 346.48 | 774.42 | **41** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **22,167,542** | 16,620,130 | **9,498,570** |
| Apache Fury | 9,236,084 | **28,363,966** | 6,967,330 |
| Kryo | 2,336,760 | 2,886,169 | 1,291,284 |

## LongArrayObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **47.79** | 66.29 | **114.08** | 84 |
| Apache Fury | 157.82 | **34.26** | 192.08 | 153 |
| Kryo | 653.61 | 323.57 | 977.19 | **41** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **20,925,318** | 15,085,687 | **8,766,009** |
| Apache Fury | 6,336,533 | **29,187,706** | 5,206,273 |
| Kryo | 1,529,960 | 3,090,493 | 1,023,348 |

## DoubleArrayObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **62.56** | 73.68 | **136.23** | 84 |
| Apache Fury | 126.47 | **28.87** | 155.34 | 154 |
| Kryo | 327.43 | 419.62 | 747.05 | **81** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **15,985,421** | 13,573,125 | **7,340,419** |
| Apache Fury | 7,906,826 | **34,639,232** | 6,437,409 |
| Kryo | 3,054,088 | 2,383,091 | 1,338,593 |

## AllPrimitiveArraysObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 206.86 | **105.71** | **312.57** | 183 |
| KalynxSerializer | **188.83** | 271.07 | 459.90 | 122 |
| Kryo | 501.84 | 531.45 | 1033.28 | **80** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 4,834,094 | **9,460,201** | **3,199,283** |
| KalynxSerializer | **5,295,825** | 3,689,043 | 2,174,381 |
| Kryo | 1,992,679 | 1,881,652 | 967,787 |

## SimpleNestedObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 218.06 | **83.24** | **301.30** | 110 |
| KalynxSerializer | **199.49** | 173.21 | 372.70 | **13** |
| Kryo | 437.04 | 549.32 | 986.36 | 78 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 4,585,894 | **12,012,878** | **3,318,907** |
| KalynxSerializer | **5,012,808** | 5,773,472 | 2,683,159 |
| Kryo | 2,288,125 | 1,820,429 | 1,013,829 |

## Rectangle

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **66.64** | **80.34** | **146.98** | **30** |
| Apache Fury | 205.68 | 85.79 | 291.48 | 105 |
| Kryo | 433.03 | 587.80 | 1020.83 | 85 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **15,006,903** | **12,447,255** | **6,803,878** |
| Apache Fury | 4,861,851 | 11,655,827 | 3,430,802 |
| Kryo | 2,309,298 | 1,701,265 | 979,595 |

## DeepNestedLevel1

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | **96.98** | 237.60 | **334.58** | **22** |
| Apache Fury | 271.16 | **65.42** | 336.58 | 156 |
| Kryo | 430.02 | 723.33 | 1153.35 | 168 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | **10,311,404** | 4,208,683 | **2,988,786** |
| Apache Fury | 3,687,819 | **15,285,845** | 2,971,035 |
| Kryo | 2,325,468 | 1,382,501 | 867,041 |

## Summary - Fastest Library by Category

### AllPrimitivesObject

- **Fastest Serialization:** KalynxSerializer (28.66 ns/op)
- **Fastest Deserialization:** KalynxSerializer (29.25 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (57.91 ns/op)
- **Smallest Binary Size:** Kryo (26 bytes)

### IntegerListObject

- **Fastest Serialization:** KalynxSerializer (54.54 ns/op)
- **Fastest Deserialization:** KalynxSerializer (105.71 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (160.25 ns/op)
- **Smallest Binary Size:** Kryo (42 bytes)

### StringListObject

- **Fastest Serialization:** Apache Fury (230.94 ns/op)
- **Fastest Deserialization:** Apache Fury (90.76 ns/op)
- **Fastest Round-Trip:** Apache Fury (321.70 ns/op)
- **Smallest Binary Size:** Kryo (72 bytes)

### LongListObject

- **Fastest Serialization:** KalynxSerializer (63.85 ns/op)
- **Fastest Deserialization:** Apache Fury (84.24 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (200.08 ns/op)
- **Smallest Binary Size:** Kryo (58 bytes)

### DoubleListObject

- **Fastest Serialization:** KalynxSerializer (84.72 ns/op)
- **Fastest Deserialization:** Apache Fury (131.55 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (262.31 ns/op)
- **Smallest Binary Size:** Kryo (50 bytes)

### MixedPrimitiveAndListObject

- **Fastest Serialization:** KalynxSerializer (63.01 ns/op)
- **Fastest Deserialization:** Apache Fury (84.49 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (171.92 ns/op)
- **Smallest Binary Size:** Kryo (20 bytes)

### AllPrimitivesWithListsObject

- **Fastest Serialization:** KalynxSerializer (96.86 ns/op)
- **Fastest Deserialization:** KalynxSerializer (100.29 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (197.15 ns/op)
- **Smallest Binary Size:** Kryo (55 bytes)

### StringIntegerMapObject

- **Fastest Serialization:** KalynxSerializer (487.01 ns/op)
- **Fastest Deserialization:** Apache Fury (226.33 ns/op)
- **Fastest Round-Trip:** Apache Fury (760.49 ns/op)
- **Smallest Binary Size:** Kryo (162 bytes)

### IntegerStringMapObject

- **Fastest Serialization:** KalynxSerializer (433.91 ns/op)
- **Fastest Deserialization:** KalynxSerializer (257.36 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (691.27 ns/op)
- **Smallest Binary Size:** Kryo (182 bytes)

### IntegerIntegerMapObject

- **Fastest Serialization:** KalynxSerializer (122.81 ns/op)
- **Fastest Deserialization:** KalynxSerializer (162.54 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (285.34 ns/op)
- **Smallest Binary Size:** Kryo (82 bytes)

### LongDoubleMapObject

- **Fastest Serialization:** KalynxSerializer (146.03 ns/op)
- **Fastest Deserialization:** KalynxSerializer (209.53 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (355.56 ns/op)
- **Smallest Binary Size:** Kryo (162 bytes)

### IntArrayObject

- **Fastest Serialization:** KalynxSerializer (45.11 ns/op)
- **Fastest Deserialization:** Apache Fury (35.26 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (105.28 ns/op)
- **Smallest Binary Size:** Kryo (41 bytes)

### LongArrayObject

- **Fastest Serialization:** KalynxSerializer (47.79 ns/op)
- **Fastest Deserialization:** Apache Fury (34.26 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (114.08 ns/op)
- **Smallest Binary Size:** Kryo (41 bytes)

### DoubleArrayObject

- **Fastest Serialization:** KalynxSerializer (62.56 ns/op)
- **Fastest Deserialization:** Apache Fury (28.87 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (136.23 ns/op)
- **Smallest Binary Size:** Kryo (81 bytes)

### AllPrimitiveArraysObject

- **Fastest Serialization:** KalynxSerializer (188.83 ns/op)
- **Fastest Deserialization:** Apache Fury (105.71 ns/op)
- **Fastest Round-Trip:** Apache Fury (312.57 ns/op)
- **Smallest Binary Size:** Kryo (80 bytes)

### SimpleNestedObject

- **Fastest Serialization:** KalynxSerializer (199.49 ns/op)
- **Fastest Deserialization:** Apache Fury (83.24 ns/op)
- **Fastest Round-Trip:** Apache Fury (301.30 ns/op)
- **Smallest Binary Size:** KalynxSerializer (13 bytes)

### Rectangle

- **Fastest Serialization:** KalynxSerializer (66.64 ns/op)
- **Fastest Deserialization:** KalynxSerializer (80.34 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (146.98 ns/op)
- **Smallest Binary Size:** KalynxSerializer (30 bytes)

### DeepNestedLevel1

- **Fastest Serialization:** KalynxSerializer (96.98 ns/op)
- **Fastest Deserialization:** Apache Fury (65.42 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (334.58 ns/op)
- **Smallest Binary Size:** KalynxSerializer (22 bytes)

