# Serialization Benchmark Results

**Date:** 2026-02-14 23:30:29

**Total test runs:** 66

## Summary: Round-Trip Performance (ns/op)

*Lower is better. Best result per scenario is highlighted in **bold**.*

| Scenario | KalynxSerializer | Kryo | Apache Fury |
|----------|----------:|----------:|----------:|
| AllPrimitivesObject | 55.71 | 612.35 | 🏆 **49.70** |
| IntegerListObject | 🏆 **168.12** | 864.02 | 298.96 |
| StringListObject | 455.94 | 701.61 | 🏆 **327.95** |
| LongListObject | 🏆 **159.87** | 688.05 | 271.49 |
| DoubleListObject | 321.73 | 617.41 | 🏆 **263.61** |
| MixedPrimitiveAndListObject | 🏆 **141.00** | 1026.04 | 240.64 |
| AllPrimitivesWithListsObject | 🏆 **203.30** | 723.00 | 404.19 |
| StringIntegerMapObject | 999.44 | 1660.46 | 🏆 **653.23** |
| IntegerStringMapObject | 🏆 **685.03** | 1192.26 | 827.83 |
| IntegerIntegerMapObject | 🏆 **276.96** | 1855.06 | 603.55 |
| LongDoubleMapObject | 🏆 **317.81** | 1030.44 | 396.08 |
| IntArrayObject | 🏆 **109.58** | 641.75 | 131.05 |
| LongArrayObject | 🏆 **106.29** | 675.65 | 137.89 |
| DoubleArrayObject | 137.36 | 595.93 | 🏆 **113.20** |
| AllPrimitiveArraysObject | 450.04 | 796.41 | 🏆 **351.94** |
| SimpleNestedObject | 361.71 | 830.24 | 🏆 **236.99** |
| Rectangle | 🏆 **148.84** | 967.70 | 241.63 |
| DeepNestedLevel1 | 🏆 **176.05** | 856.11 | 306.00 |
| LargeStringObject | 356.23 | 1171.92 | 🏆 **162.16** |
| LargeStringListObject | 962.20 | 1757.12 | 🏆 **372.89** |
| MixedSizeStringListObject | 🏆 **914.50** | 1848.41 | 1153.83 |
| DocumentObject | 766.13 | 1822.28 | 🏆 **527.87** |

## AllPrimitivesObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 30.55 | 🏆 **19.15** | 🏆 **49.70** | 103 |
| KalynxSerializer | 🏆 **29.54** | 26.17 | 55.71 | 30 |
| Kryo | 297.75 | 314.61 | 612.35 | 🏆 **26** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 32,730,010 | 🏆 **52,230,231** | 🏆 **20,121,129** |
| KalynxSerializer | 🏆 **33,847,820** | 38,217,534 | 17,950,099 |
| Kryo | 3,358,567 | 3,178,579 | 1,633,048 |

## IntegerListObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | 🏆 **77.45** | 🏆 **90.67** | 🏆 **168.12** | 54 |
| Apache Fury | 197.19 | 101.77 | 298.96 | 116 |
| Kryo | 449.64 | 414.37 | 864.02 | 🏆 **42** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | 🏆 **12,910,889** | 🏆 **11,029,250** | 🏆 **5,948,062** |
| Apache Fury | 5,071,148 | 9,826,368 | 3,344,918 |
| Kryo | 2,223,992 | 2,413,285 | 1,157,387 |

## StringListObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 220.38 | 🏆 **107.56** | 🏆 **327.95** | 150 |
| KalynxSerializer | 🏆 **172.39** | 283.55 | 455.94 | 94 |
| Kryo | 318.86 | 382.75 | 701.61 | 🏆 **72** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 4,537,534 | 🏆 **9,297,050** | 🏆 **3,049,292** |
| KalynxSerializer | 🏆 **5,800,834** | 3,526,740 | 2,193,285 |
| Kryo | 3,136,173 | 2,612,671 | 1,425,293 |

## LongListObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | 🏆 **68.56** | 91.31 | 🏆 **159.87** | 76 |
| Apache Fury | 191.99 | 🏆 **79.50** | 271.49 | 146 |
| Kryo | 333.76 | 354.28 | 688.05 | 🏆 **58** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | 🏆 **14,585,339** | 10,952,303 | 🏆 **6,255,200** |
| Apache Fury | 5,208,605 | 🏆 **12,578,616** | 3,683,377 |
| Kryo | 2,996,147 | 2,822,594 | 1,453,391 |

## DoubleListObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 186.24 | 🏆 **77.37** | 🏆 **263.61** | 123 |
| KalynxSerializer | 🏆 **62.30** | 259.43 | 321.73 | 58 |
| Kryo | 297.02 | 320.39 | 617.41 | 🏆 **50** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 5,369,416 | 🏆 **12,925,240** | 🏆 **3,793,512** |
| KalynxSerializer | 🏆 **16,051,622** | 3,854,589 | 3,108,196 |
| Kryo | 3,366,799 | 3,121,177 | 1,619,669 |

## MixedPrimitiveAndListObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | 🏆 **53.86** | 87.14 | 🏆 **141.00** | 33 |
| Apache Fury | 165.34 | 🏆 **75.30** | 240.64 | 101 |
| Kryo | 370.74 | 655.30 | 1026.04 | 🏆 **20** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | 🏆 **18,566,654** | 11,475,259 | 🏆 **7,091,997** |
| Apache Fury | 6,048,253 | 🏆 **13,279,683** | 4,155,585 |
| Kryo | 2,697,301 | 1,526,012 | 974,617 |

## AllPrimitivesWithListsObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | 🏆 **109.46** | 🏆 **93.83** | 🏆 **203.30** | 81 |
| Apache Fury | 248.39 | 155.80 | 404.19 | 142 |
| Kryo | 337.86 | 385.14 | 723.00 | 🏆 **55** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | 🏆 **9,135,507** | 🏆 **10,657,231** | 🏆 **4,918,936** |
| Apache Fury | 4,025,976 | 6,418,485 | 2,474,102 |
| Kryo | 2,959,797 | 2,596,485 | 1,383,132 |

## StringIntegerMapObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 446.85 | 🏆 **206.37** | 🏆 **653.23** | 259 |
| KalynxSerializer | 🏆 **445.34** | 554.10 | 999.44 | 174 |
| Kryo | 979.62 | 680.84 | 1660.46 | 🏆 **162** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 2,237,867 | 🏆 **4,845,619** | 🏆 **1,530,864** |
| KalynxSerializer | 🏆 **2,245,496** | 1,804,722 | 1,000,562 |
| Kryo | 1,020,809 | 1,468,765 | 602,243 |

## IntegerStringMapObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | 🏆 **377.25** | 307.78 | 🏆 **685.03** | 194 |
| Apache Fury | 548.05 | 🏆 **279.78** | 827.83 | 279 |
| Kryo | 688.38 | 503.88 | 1192.26 | 🏆 **182** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | 🏆 **2,650,790** | 3,249,074 | 🏆 **1,459,799** |
| Apache Fury | 1,824,651 | 🏆 **3,574,224** | 1,207,976 |
| Kryo | 1,452,686 | 1,984,607 | 838,745 |

## IntegerIntegerMapObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | 🏆 **153.34** | 🏆 **123.63** | 🏆 **276.96** | 84 |
| Apache Fury | 444.75 | 158.80 | 603.55 | 159 |
| Kryo | 1115.13 | 739.92 | 1855.06 | 🏆 **82** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | 🏆 **6,521,583** | 🏆 **8,088,848** | 🏆 **3,610,578** |
| Apache Fury | 2,248,434 | 6,297,269 | 1,656,855 |
| Kryo | 896,754 | 1,351,494 | 539,068 |

## LongDoubleMapObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | 🏆 **173.83** | 143.98 | 🏆 **317.81** | 164 |
| Apache Fury | 260.56 | 🏆 **135.51** | 396.08 | 267 |
| Kryo | 548.36 | 482.08 | 1030.44 | 🏆 **162** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | 🏆 **5,752,648** | 6,945,506 | 🏆 **3,146,524** |
| Apache Fury | 3,837,829 | 🏆 **7,379,420** | 2,524,768 |
| Kryo | 1,823,616 | 2,074,362 | 970,462 |

## IntArrayObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | 🏆 **49.21** | 60.37 | 🏆 **109.58** | 44 |
| Apache Fury | 97.27 | 🏆 **33.78** | 131.05 | 112 |
| Kryo | 331.89 | 309.86 | 641.75 | 🏆 **41** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | 🏆 **20,320,660** | 16,565,891 | 🏆 **9,126,086** |
| Apache Fury | 10,280,451 | 🏆 **29,605,945** | 7,630,734 |
| Kryo | 3,013,028 | 3,227,295 | 1,558,242 |

## LongArrayObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | 🏆 **44.53** | 61.76 | 🏆 **106.29** | 84 |
| Apache Fury | 105.38 | 🏆 **32.51** | 137.89 | 153 |
| Kryo | 318.06 | 357.59 | 675.65 | 🏆 **41** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | 🏆 **22,457,779** | 16,192,234 | 🏆 **9,408,577** |
| Apache Fury | 9,489,197 | 🏆 **30,759,766** | 7,252,000 |
| Kryo | 3,144,051 | 2,796,499 | 1,480,054 |

## DoubleArrayObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 84.93 | 🏆 **28.27** | 🏆 **113.20** | 154 |
| KalynxSerializer | 🏆 **66.00** | 71.36 | 137.36 | 84 |
| Kryo | 297.04 | 298.89 | 595.93 | 🏆 **81** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 11,774,541 | 🏆 **35,370,685** | 🏆 **8,833,844** |
| KalynxSerializer | 🏆 **15,152,433** | 14,012,667 | 7,280,140 |
| Kryo | 3,366,539 | 3,345,690 | 1,678,041 |

## AllPrimitiveArraysObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 239.68 | 🏆 **112.27** | 🏆 **351.94** | 183 |
| KalynxSerializer | 🏆 **169.31** | 280.74 | 450.04 | 122 |
| Kryo | 351.27 | 445.15 | 796.41 | 🏆 **80** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 4,172,282 | 🏆 **8,907,496** | 🏆 **2,841,377** |
| KalynxSerializer | 🏆 **5,906,465** | 3,562,053 | 2,222,010 |
| Kryo | 2,846,854 | 2,246,439 | 1,255,628 |

## SimpleNestedObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 🏆 **174.07** | 🏆 **62.92** | 🏆 **236.99** | 110 |
| KalynxSerializer | 195.19 | 166.52 | 361.71 | 🏆 **13** |
| Kryo | 360.26 | 469.99 | 830.24 | 78 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 🏆 **5,744,749** | 🏆 **15,894,461** | 🏆 **4,219,641** |
| KalynxSerializer | 5,123,240 | 6,005,140 | 2,764,623 |
| Kryo | 2,775,804 | 2,127,718 | 1,204,467 |

## Rectangle

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | 🏆 **70.51** | 78.34 | 🏆 **148.84** | 🏆 **30** |
| Apache Fury | 168.85 | 🏆 **72.79** | 241.63 | 105 |
| Kryo | 404.49 | 563.21 | 967.70 | 85 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | 🏆 **14,183,190** | 12,765,197 | 🏆 **6,718,443** |
| Apache Fury | 5,922,592 | 🏆 **13,739,095** | 4,138,559 |
| Kryo | 2,472,267 | 1,775,524 | 1,033,377 |

## DeepNestedLevel1

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | 🏆 **80.00** | 96.05 | 🏆 **176.05** | 🏆 **22** |
| Apache Fury | 240.38 | 🏆 **65.63** | 306.00 | 156 |
| Kryo | 348.70 | 507.41 | 856.11 | 168 |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | 🏆 **12,500,469** | 10,411,136 | 🏆 **5,680,269** |
| Apache Fury | 4,160,166 | 🏆 **15,238,095** | 3,267,974 |
| Kryo | 2,867,778 | 1,970,785 | 1,168,069 |

## LargeStringObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 134.27 | 🏆 **27.89** | 🏆 **162.16** | 225 |
| KalynxSerializer | 🏆 **133.93** | 222.31 | 356.23 | 154 |
| Kryo | 516.81 | 655.10 | 1171.92 | 🏆 **152** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 7,447,458 | 🏆 **35,860,288** | 🏆 **6,166,749** |
| KalynxSerializer | 🏆 **7,466,754** | 4,498,284 | 2,807,144 |
| Kryo | 1,934,936 | 1,526,478 | 853,303 |

## LargeStringListObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 🏆 **286.79** | 🏆 **86.10** | 🏆 **372.89** | 889 |
| KalynxSerializer | 849.15 | 113.05 | 962.20 | 824 |
| Kryo | 1162.14 | 594.97 | 1757.12 | 🏆 **812** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 🏆 **3,486,884** | 🏆 **11,613,862** | 🏆 **2,681,734** |
| KalynxSerializer | 1,177,645 | 8,845,878 | 1,039,286 |
| Kryo | 860,479 | 1,680,746 | 569,114 |

## MixedSizeStringListObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| KalynxSerializer | 🏆 **760.06** | 🏆 **154.44** | 🏆 **914.50** | 633 |
| Apache Fury | 792.19 | 361.64 | 1153.83 | 692 |
| Kryo | 1299.33 | 549.08 | 1848.41 | 🏆 **602** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| KalynxSerializer | 🏆 **1,315,693** | 🏆 **6,474,881** | 🏆 **1,093,495** |
| Apache Fury | 1,262,323 | 2,765,211 | 866,682 |
| Kryo | 769,626 | 1,821,228 | 541,005 |

## DocumentObject

| Library | Serialize (ns/op) | Deserialize (ns/op) | Round-Trip (ns/op) | Binary Size (bytes) |
|---------|------------------:|--------------------:|-------------------:|--------------------:|
| Apache Fury | 🏆 **386.22** | 🏆 **141.65** | 🏆 **527.87** | 856 |
| KalynxSerializer | 505.56 | 260.56 | 766.13 | 803 |
| Kryo | 1165.52 | 656.76 | 1822.28 | 🏆 **772** |

### Throughput (ops/sec)

| Library | Serialize | Deserialize | Round-Trip |
|---------|----------:|------------:|-----------:|
| Apache Fury | 🏆 **2,589,205** | 🏆 **7,059,604** | 🏆 **1,894,406** |
| KalynxSerializer | 1,977,989 | 3,837,873 | 1,305,270 |
| Kryo | 857,985 | 1,522,622 | 548,762 |

## Summary - Fastest Library by Category

### AllPrimitivesObject

- **Fastest Serialization:** KalynxSerializer (29.54 ns/op)
- **Fastest Deserialization:** Apache Fury (19.15 ns/op)
- **Fastest Round-Trip:** Apache Fury (49.70 ns/op)
- **Smallest Binary Size:** Kryo (26 bytes)

### IntegerListObject

- **Fastest Serialization:** KalynxSerializer (77.45 ns/op)
- **Fastest Deserialization:** KalynxSerializer (90.67 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (168.12 ns/op)
- **Smallest Binary Size:** Kryo (42 bytes)

### StringListObject

- **Fastest Serialization:** KalynxSerializer (172.39 ns/op)
- **Fastest Deserialization:** Apache Fury (107.56 ns/op)
- **Fastest Round-Trip:** Apache Fury (327.95 ns/op)
- **Smallest Binary Size:** Kryo (72 bytes)

### LongListObject

- **Fastest Serialization:** KalynxSerializer (68.56 ns/op)
- **Fastest Deserialization:** Apache Fury (79.50 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (159.87 ns/op)
- **Smallest Binary Size:** Kryo (58 bytes)

### DoubleListObject

- **Fastest Serialization:** KalynxSerializer (62.30 ns/op)
- **Fastest Deserialization:** Apache Fury (77.37 ns/op)
- **Fastest Round-Trip:** Apache Fury (263.61 ns/op)
- **Smallest Binary Size:** Kryo (50 bytes)

### MixedPrimitiveAndListObject

- **Fastest Serialization:** KalynxSerializer (53.86 ns/op)
- **Fastest Deserialization:** Apache Fury (75.30 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (141.00 ns/op)
- **Smallest Binary Size:** Kryo (20 bytes)

### AllPrimitivesWithListsObject

- **Fastest Serialization:** KalynxSerializer (109.46 ns/op)
- **Fastest Deserialization:** KalynxSerializer (93.83 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (203.30 ns/op)
- **Smallest Binary Size:** Kryo (55 bytes)

### StringIntegerMapObject

- **Fastest Serialization:** KalynxSerializer (445.34 ns/op)
- **Fastest Deserialization:** Apache Fury (206.37 ns/op)
- **Fastest Round-Trip:** Apache Fury (653.23 ns/op)
- **Smallest Binary Size:** Kryo (162 bytes)

### IntegerStringMapObject

- **Fastest Serialization:** KalynxSerializer (377.25 ns/op)
- **Fastest Deserialization:** Apache Fury (279.78 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (685.03 ns/op)
- **Smallest Binary Size:** Kryo (182 bytes)

### IntegerIntegerMapObject

- **Fastest Serialization:** KalynxSerializer (153.34 ns/op)
- **Fastest Deserialization:** KalynxSerializer (123.63 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (276.96 ns/op)
- **Smallest Binary Size:** Kryo (82 bytes)

### LongDoubleMapObject

- **Fastest Serialization:** KalynxSerializer (173.83 ns/op)
- **Fastest Deserialization:** Apache Fury (135.51 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (317.81 ns/op)
- **Smallest Binary Size:** Kryo (162 bytes)

### IntArrayObject

- **Fastest Serialization:** KalynxSerializer (49.21 ns/op)
- **Fastest Deserialization:** Apache Fury (33.78 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (109.58 ns/op)
- **Smallest Binary Size:** Kryo (41 bytes)

### LongArrayObject

- **Fastest Serialization:** KalynxSerializer (44.53 ns/op)
- **Fastest Deserialization:** Apache Fury (32.51 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (106.29 ns/op)
- **Smallest Binary Size:** Kryo (41 bytes)

### DoubleArrayObject

- **Fastest Serialization:** KalynxSerializer (66.00 ns/op)
- **Fastest Deserialization:** Apache Fury (28.27 ns/op)
- **Fastest Round-Trip:** Apache Fury (113.20 ns/op)
- **Smallest Binary Size:** Kryo (81 bytes)

### AllPrimitiveArraysObject

- **Fastest Serialization:** KalynxSerializer (169.31 ns/op)
- **Fastest Deserialization:** Apache Fury (112.27 ns/op)
- **Fastest Round-Trip:** Apache Fury (351.94 ns/op)
- **Smallest Binary Size:** Kryo (80 bytes)

### SimpleNestedObject

- **Fastest Serialization:** Apache Fury (174.07 ns/op)
- **Fastest Deserialization:** Apache Fury (62.92 ns/op)
- **Fastest Round-Trip:** Apache Fury (236.99 ns/op)
- **Smallest Binary Size:** KalynxSerializer (13 bytes)

### Rectangle

- **Fastest Serialization:** KalynxSerializer (70.51 ns/op)
- **Fastest Deserialization:** Apache Fury (72.79 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (148.84 ns/op)
- **Smallest Binary Size:** KalynxSerializer (30 bytes)

### DeepNestedLevel1

- **Fastest Serialization:** KalynxSerializer (80.00 ns/op)
- **Fastest Deserialization:** Apache Fury (65.63 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (176.05 ns/op)
- **Smallest Binary Size:** KalynxSerializer (22 bytes)

### LargeStringObject

- **Fastest Serialization:** KalynxSerializer (133.93 ns/op)
- **Fastest Deserialization:** Apache Fury (27.89 ns/op)
- **Fastest Round-Trip:** Apache Fury (162.16 ns/op)
- **Smallest Binary Size:** Kryo (152 bytes)

### LargeStringListObject

- **Fastest Serialization:** Apache Fury (286.79 ns/op)
- **Fastest Deserialization:** Apache Fury (86.10 ns/op)
- **Fastest Round-Trip:** Apache Fury (372.89 ns/op)
- **Smallest Binary Size:** Kryo (812 bytes)

### MixedSizeStringListObject

- **Fastest Serialization:** KalynxSerializer (760.06 ns/op)
- **Fastest Deserialization:** KalynxSerializer (154.44 ns/op)
- **Fastest Round-Trip:** KalynxSerializer (914.50 ns/op)
- **Smallest Binary Size:** Kryo (602 bytes)

### DocumentObject

- **Fastest Serialization:** Apache Fury (386.22 ns/op)
- **Fastest Deserialization:** Apache Fury (141.65 ns/op)
- **Fastest Round-Trip:** Apache Fury (527.87 ns/op)
- **Smallest Binary Size:** Kryo (772 bytes)

