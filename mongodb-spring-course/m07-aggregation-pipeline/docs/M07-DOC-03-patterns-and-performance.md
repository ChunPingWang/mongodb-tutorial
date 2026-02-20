# M07-DOC-03: $bucket 分桶分析與效能最佳化

## 目錄

- [簡介](#簡介)
- [$bucket 明確邊界分桶](#bucket-明確邊界分桶)
  - [基本語法](#基本語法)
  - [餘額分布分析範例](#餘額分布分析範例)
  - [價格區間分析範例](#價格區間分析範例)
  - [評分分布分析範例](#評分分布分析範例)
- [$bucketAuto 自動分桶](#bucketauto-自動分桶)
  - [基本語法](#基本語法-1)
  - [保費自動分桶範例](#保費自動分桶範例)
  - [Granularity 精細度控制](#granularity-精細度控制)
- [Pipeline 順序最佳化](#pipeline-順序最佳化)
  - [$match 前置原則](#match-前置原則)
  - [Pipeline 重排範例](#pipeline-重排範例)
  - [階段融合最佳化](#階段融合最佳化)
- [索引利用策略](#索引利用策略)
  - [Pipeline 索引使用規則](#pipeline-索引使用規則)
  - [索引覆蓋查詢](#索引覆蓋查詢)
  - [explain() 分析](#explain-分析)
- [M05-M07 查詢能力回顧總覽](#m05-m07-查詢能力回顧總覽)
  - [M05: Spring Data CRUD](#m05-spring-data-crud)
  - [M06: Query DSL](#m06-query-dsl)
  - [M07: Aggregation Pipeline](#m07-aggregation-pipeline)
  - [能力矩陣對照表](#能力矩陣對照表)
- [效能最佳化檢查清單](#效能最佳化檢查清單)
- [銜接 M08: 索引與效能深入](#銜接-m08-索引與效能深入)
- [小結](#小結)

---

## 簡介

本文件涵蓋 M07 的最後兩個進階主題:

1. **$bucket / $bucketAuto**: 資料分桶分析 (直方圖統計)
2. **效能最佳化**: Pipeline 順序最佳化、索引利用策略

並在文件尾段提供 **M05-M07 查詢能力總覽表**,幫助開發者快速選擇適合的查詢工具,為 M08 索引與效能深入模組做好準備。

---

## $bucket 明確邊界分桶

### 基本語法

`$bucket` 將資料依指定欄位的數值範圍分組到不同的「桶 (Bucket)」,類似直方圖 (Histogram) 統計。

**MongoDB Shell 語法**:
```javascript
{
  $bucket: {
    groupBy: "$欄位",           // 分桶依據欄位
    boundaries: [0, 20, 50, 100], // 桶邊界 (左閉右開區間)
    default: "其他",             // 預設桶 (不在範圍內的值)
    output: {                    // 輸出累加器
      count: { $sum: 1 },
      avgValue: { $avg: "$欄位" }
    }
  }
}
```

**Spring Data API**:
```java
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

BucketOperation bucket = bucket("欄位")
        .withBoundaries(0, 20, 50, 100)
        .withDefaultBucket("其他")
        .andOutput("欄位").count().as("count");
```

**邊界說明**:
- `boundaries: [0, 20, 50, 100]` 定義 3 個桶:
  - `[0, 20)`: 0 ≤ 值 < 20
  - `[20, 50)`: 20 ≤ 值 < 50
  - `[50, 100)`: 50 ≤ 值 < 100
- **左閉右開區間** (包含下界,不包含上界)
- `default` 桶接收所有不在範圍內的值 (< 0 或 ≥ 100)

---

### 餘額分布分析範例

#### 需求: 統計帳戶餘額在不同區間的分布

```java
@Service
public class BucketAggregationService {

    private final MongoTemplate mongoTemplate;

    public List<Map> balanceDistribution() {
        BucketOperation bucket = bucket("balance")
                .withBoundaries(0, 20000, 50000, 100000, Integer.MAX_VALUE)
                .withDefaultBucket("other")
                .andOutput("balance").count().as("count");

        Aggregation aggregation = newAggregation(bucket);
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", Map.class);
        return results.getMappedResults();
    }
}
```

**輸出範例**:
```json
[
  { "_id": 0, "count": 0 },           // [0, 20000)
  { "_id": 20000, "count": 2 },       // [20000, 50000): Bob(15000), David(25000)
  { "_id": 50000, "count": 2 },       // [50000, 100000): Alice(50000), Charlie(80000)
  { "_id": 100000, "count": 0 }       // [100000, MAX)
]
```

**注意**: `_id` 欄位存放的是桶的**下界值**。

---

### 價格區間分析範例

#### 需求: 商品價格分層統計 + 平均價格

```java
public List<Map> priceTierDistribution() {
    BucketOperation bucket = bucket("price")
            .withBoundaries(0, 50, 100, 200, 500, Integer.MAX_VALUE)
            .withDefaultBucket("other")
            .andOutput("price").count().as("count")
            .andOutput("price").avg().as("avgPrice");  // ← 計算平均價格

    Aggregation aggregation = newAggregation(bucket);
    AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, "m07_products", Map.class);
    return results.getMappedResults();
}
```

**輸出範例**:
```json
[
  { "_id": 0, "count": 1, "avgPrice": 29.99 },      // [0, 50): Wireless Mouse
  { "_id": 50, "count": 2, "avgPrice": 72.495 },    // [50, 100): Java Book, Keyboard
  { "_id": 100, "count": 0, "avgPrice": null },     // [100, 200)
  { "_id": 200, "count": 1, "avgPrice": 299.99 },   // [200, 500): Standing Desk
  { "_id": 500, "count": 0, "avgPrice": null }      // [500, MAX)
]
```

---

### 評分分布分析範例

#### 需求: 商品評分區間統計 (1-5 星)

```java
public List<Map> ratingDistribution() {
    BucketOperation bucket = bucket("rating")
            .withBoundaries(1.0, 2.0, 3.0, 4.0, 5.01)  // ← 注意上界 5.01 包含 5.0
            .withDefaultBucket("other")
            .andOutput("rating").count().as("count")
            .andOutput("rating").avg().as("avgRating");

    Aggregation aggregation = newAggregation(bucket);
    AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, "m07_products", Map.class);
    return results.getMappedResults();
}
```

**注意**: 由於 `rating: 5.0` 需要被包含,上界設為 `5.01` (因為區間是左閉右開)。

**輸出範例**:
```json
[
  { "_id": 1.0, "count": 0, "avgRating": null },    // [1.0, 2.0)
  { "_id": 2.0, "count": 0, "avgRating": null },    // [2.0, 3.0)
  { "_id": 3.0, "count": 1, "avgRating": 3.9 },     // [3.0, 4.0): USB-C Hub
  { "_id": 4.0, "count": 5, "avgRating": 4.56 }     // [4.0, 5.01): 5 個商品
]
```

---

## $bucketAuto 自動分桶

### 基本語法

`$bucketAuto` 自動計算桶的邊界,依據資料分布均勻分配到指定數量的桶。

**MongoDB Shell 語法**:
```javascript
{
  $bucketAuto: {
    groupBy: "$欄位",
    buckets: 4,             // 桶數量
    granularity: "POWERSOF2", // 可選: 精細度控制
    output: {
      count: { $sum: 1 },
      avgValue: { $avg: "$欄位" }
    }
  }
}
```

**Spring Data API**:
```java
BucketAutoOperation bucketAuto = bucketAuto("欄位", 桶數量)
        .andOutput("欄位").count().as("count");
```

---

### 保費自動分桶範例

#### 需求: 將保費自動分為 3 個桶

```java
public List<Map> premiumBucketAuto(int buckets) {
    BucketAutoOperation bucketAuto = bucketAuto("premium", buckets)
            .andOutput("premium").count().as("count")
            .andOutput("premium").avg().as("avgPremium");

    Aggregation aggregation = newAggregation(bucketAuto);
    AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, "m07_insurance_policies", Map.class);
    return results.getMappedResults();
}
```

**輸出範例** (假設資料: 1000, 2000, 3000, 4000, 5000, 6000):
```json
[
  { "_id": { "min": 1000, "max": 3000 }, "count": 2, "avgPremium": 1500 },
  { "_id": { "min": 3000, "max": 5000 }, "count": 2, "avgPremium": 3500 },
  { "_id": { "min": 5000, "max": 6000 }, "count": 2, "avgPremium": 5500 }
]
```

**注意**: `_id` 欄位包含 `min` 和 `max`,表示桶的範圍。

---

### Granularity 精細度控制

`granularity` 參數控制桶邊界的精細度,遵循特定的數學級數。

#### 可用精細度

| 精細度 | 說明 | 範例 |
|--------|------|------|
| `R5` | Renard 5 級數 | 1, 1.6, 2.5, 4, 6.3, 10 |
| `R10` | Renard 10 級數 | 1, 1.25, 1.6, 2, 2.5, 3.15, 4, 5, 6.3, 8, 10 |
| `R20` | Renard 20 級數 | 更細緻的分級 |
| `R40` | Renard 40 級數 | 最細緻的分級 |
| `R80` | Renard 80 級數 | 極細緻分級 |
| `1-2-5` | 1, 2, 5, 10, 20, 50 | 常用於價格分級 |
| `E6` | E6 級數 | 1, 1.5, 2.2, 3.3, 4.7, 6.8, 10 |
| `E12` | E12 級數 | 電子元件常用 |
| `E24`, `E48`, `E96`, `E192` | 更細緻的 E 級數 | 電子元件常用 |
| `POWERSOF2` | 2 的次方 | 1, 2, 4, 8, 16, 32, 64 |

#### 範例: POWERSOF2 精細度

```java
public List<Map> bucketAutoGranularity(int buckets) {
    BucketAutoOperation bucketAuto = bucketAuto("balance", buckets)
            .withGranularity(BucketAutoOperation.Granularities.POWERSOF2)  // ← 2 的次方
            .andOutput("balance").count().as("count");

    Aggregation aggregation = newAggregation(bucketAuto);
    AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, "m07_bank_accounts", Map.class);
    return results.getMappedResults();
}
```

**輸出範例** (餘額: 15000, 25000, 30000, 50000, 80000):
```json
[
  { "_id": { "min": 8192, "max": 32768 }, "count": 3 },   // ← 邊界對齊 2 的次方
  { "_id": { "min": 32768, "max": 65536 }, "count": 1 },
  { "_id": { "min": 65536, "max": 131072 }, "count": 1 }
]
```

**應用場景**:
- `POWERSOF2`: 記憶體大小、檔案大小分析
- `1-2-5`: 價格區間、預算分級
- `R5/R10`: 工業標準、工程應用

---

## Pipeline 順序最佳化

### $match 前置原則

**最佳實務**: 將 `$match` 階段盡可能放在 Pipeline 開頭,減少後續階段處理的資料量。

#### 低效範例 (避免)

```java
// ❌ 低效: 先 $group 處理所有資料,再 $match 過濾
Aggregation aggregation = newAggregation(
        group("type").count().as("count"),
        match(Criteria.where("count").gte(10))  // ← 後置過濾
);
```

#### 高效範例 (推薦)

```java
// ✅ 高效: 先 $match 過濾,減少 $group 處理的資料量
Aggregation aggregation = newAggregation(
        match(Criteria.where("status").is("ACTIVE")),  // ← 前置過濾
        group("type").count().as("count")
);
```

**效能差異**:
- 低效範例: 處理 100 萬筆 → $group → 過濾到 10 筆
- 高效範例: 過濾到 10 萬筆 → $group 處理 10 萬筆

---

### Pipeline 重排範例

MongoDB 查詢優化器會自動重排某些階段,但手動優化仍然重要。

#### 範例: $match 與 $sort 同時存在

```java
// MongoDB 會自動將 $match 前置
Aggregation aggregation = newAggregation(
        sort(Sort.Direction.DESC, "createdAt"),
        match(Criteria.where("status").is("ACTIVE"))  // ← 會被自動前置
);

// 等同於
Aggregation aggregation = newAggregation(
        match(Criteria.where("status").is("ACTIVE")),  // ← 自動重排後
        sort(Sort.Direction.DESC, "createdAt")
);
```

**自動重排規則**:
- `$match` 會盡可能前置到 Pipeline 開頭
- `$sort` + `$limit` 會融合成 Top-K Sort 演算法
- `$project` 若僅選擇欄位 (無計算),會與 `$match` 合併

---

### 階段融合最佳化

#### 1. $sort + $limit 融合

```java
Aggregation aggregation = newAggregation(
        match(Criteria.where("category").is("Electronics")),
        sort(Sort.Direction.DESC, "price"),
        limit(10)  // ← 融合成 Top-10 演算法,不需完整排序
);
```

**優點**: MongoDB 使用 Heap 資料結構,記憶體消耗 O(N) → O(10)。

#### 2. $match 條件下推

```java
Aggregation aggregation = newAggregation(
        lookup("m07_products", "items.sku", "sku", "product"),
        match(Criteria.where("product.category").is("Electronics"))  // ← 部分條件可下推到 $lookup
);
```

**注意**: 並非所有 `$match` 都能下推,需使用 `explain()` 確認。

---

## 索引利用策略

### Pipeline 索引使用規則

Aggregation Pipeline 的索引利用規則:

1. **僅 Pipeline 開頭的階段可利用索引**
2. **支援索引的階段**: `$match`, `$sort`, `$geoNear`
3. **索引失效情況**:
   - `$match` 後接 `$group` / `$unwind` (無法利用索引)
   - `$match` 使用無索引欄位

#### 範例: 索引有效

```java
// ✅ status 欄位有索引
Aggregation aggregation = newAggregation(
        match(Criteria.where("status").is("ACTIVE")),  // ← 使用索引
        group("type").count().as("count")
);
```

**建議索引**:
```javascript
db.m07_bank_accounts.createIndex({ status: 1 })
```

#### 範例: 索引部分有效

```java
// ⚠️ $match 使用索引,但 $sort 在 $group 之後,無法利用索引
Aggregation aggregation = newAggregation(
        match(Criteria.where("status").is("ACTIVE")),  // ← 使用索引
        group("type").count().as("count"),
        sort(Sort.Direction.DESC, "count")             // ← 無法使用索引 (在 $group 後)
);
```

---

### 索引覆蓋查詢

**覆蓋查詢 (Covered Query)**: 查詢所需欄位完全來自索引,無需讀取文件。

#### 範例: 覆蓋查詢最佳化

```java
// 索引: { status: 1, type: 1, balance: 1 }
Aggregation aggregation = newAggregation(
        match(Criteria.where("status").is("ACTIVE")),
        project("type", "balance"),  // ← 僅選擇索引欄位
        group("type").sum("balance").as("total")
);
```

**效能提升**: 避免讀取整個文件,僅掃描索引。

**限制**: `_id` 欄位若未在索引中,仍需讀取文件 (除非明確排除 `_id`)。

---

### explain() 分析

使用 `explain()` 分析 Pipeline 執行計畫,確認索引使用情況。

#### Spring Data 無直接支援,需使用原生 API

```java
Document pipeline = new Document("$match", new Document("status", "ACTIVE"));
Document command = new Document("aggregate", "m07_bank_accounts")
        .append("pipeline", List.of(pipeline))
        .append("explain", true);

Document result = mongoTemplate.executeCommand(command);
System.out.println(result.toJson());
```

**關鍵欄位**:
- `winningPlan.stage`: 執行階段 (如 `IXSCAN` 表示使用索引)
- `executionStats.totalDocsExamined`: 掃描文件數
- `executionStats.nReturned`: 返回文件數

**優化目標**: `totalDocsExamined` ≈ `nReturned` (掃描數接近返回數)

---

## M05-M07 查詢能力回顧總覽

### M05: Spring Data CRUD

**核心能力**: 基本 CRUD + Derived Queries + @Query 註解 + MongoTemplate 簡單操作

| 操作 | 工具 | 範例 |
|------|------|------|
| 依單一欄位查詢 | Derived Query | `findByStatus(AccountStatus status)` |
| 範圍查詢 | Derived Query | `findByBalanceBetween(BigDecimal min, BigDecimal max)` |
| 排序 + 分頁 | Pageable | `findByStatus(status, PageRequest.of(0, 10))` |
| 欄位更新 | @Query + Update | `@Query("{'id': ?0}") update(String id, Update update)` |
| 陣列操作 | MongoTemplate | `update.push("tags").value("new-tag")` |
| 嵌套文件查詢 | Criteria | `Criteria.where("address.city").is("Taipei")` |

**適用場景**: 單一文件操作、簡單條件查詢、CRUD

---

### M06: Query DSL

**核心能力**: Criteria API + Text Search + Geospatial + Aggregation 初探

| 操作 | 工具 | 範例 |
|------|------|------|
| 複合條件查詢 | Criteria | `Criteria.where("status").is("ACTIVE").and("balance").gte(min)` |
| 文字搜尋 | TextCriteria | `TextCriteria.forDefaultLanguage().matching("MongoDB")` |
| 地理空間查詢 | Criteria.near | `Criteria.where("location").near(point).maxDistance(5000)` |
| 聚合初探 | Aggregation | `newAggregation(match(...), group(...))` |
| 動態查詢構建 | Criteria 組合 | `if (filter != null) criteria.and("field").is(filter)` |

**適用場景**: 複雜條件查詢、全文檢索、地理位置查詢、動態查詢

---

### M07: Aggregation Pipeline

**核心能力**: Pipeline 階段組合 + 跨集合查詢 + 多面向分析

| 操作 | 工具 | 範例 |
|------|------|------|
| 分組統計 | $group | `group("type").count().as("count")` |
| 多重累加器 | $group 累加器 | `group("type").count().sum().avg().min().max()` |
| 陣列展開 | $unwind | `unwind("tags")` |
| 跨集合關聯 | $lookup | `lookup("products", "sku", "sku", "detail")` |
| 多維度分析 | $facet | `facet(...).as("stats").and(...).as("data")` |
| 資料分桶 | $bucket | `bucket("balance").withBoundaries(0, 50000, 100000)` |
| 計算欄位 | $project | `project().andExpression("cond(...)").as("tier")` |

**適用場景**: 資料分析、報表生成、儀表板、統計計算、跨集合查詢

---

### 能力矩陣對照表

| 查詢需求 | M05 | M06 | M07 |
|---------|-----|-----|-----|
| 單一文件 CRUD | ✅ | ✅ | ❌ |
| 簡單條件查詢 | ✅ | ✅ | ⚠️ (過度設計) |
| 複合條件查詢 | ⚠️ (Derived Query 冗長) | ✅ | ⚠️ (過度設計) |
| 分頁 + 排序 | ✅ | ✅ | ✅ |
| 全文檢索 | ❌ | ✅ | ⚠️ (需 $match + $text) |
| 地理空間查詢 | ❌ | ✅ | ✅ ($geoNear) |
| 分組統計 | ❌ | ⚠️ (基本) | ✅ |
| 多重累加器 | ❌ | ❌ | ✅ |
| 跨集合查詢 | ❌ | ❌ | ✅ ($lookup) |
| 陣列展開分析 | ❌ | ❌ | ✅ ($unwind) |
| 多維度儀表板 | ❌ | ❌ | ✅ ($facet) |
| 資料分桶 | ❌ | ❌ | ✅ ($bucket) |
| 計算欄位 | ⚠️ (應用層) | ⚠️ (應用層) | ✅ ($project) |

**圖示說明**:
- ✅ 推薦使用
- ⚠️ 可用但不推薦
- ❌ 不支援或極困難

---

## 效能最佳化檢查清單

### Pipeline 設計檢查

- [ ] `$match` 是否前置於 Pipeline 開頭?
- [ ] `$match` 欄位是否有索引?
- [ ] `$sort` + `$limit` 是否相鄰 (觸發 Top-K 最佳化)?
- [ ] `$project` 是否僅選擇必要欄位?
- [ ] `$lookup` 是否避免大量關聯? (考慮嵌入式設計)
- [ ] `$unwind` 是否處理大陣列? (考慮資料建模最佳化)

### 索引檢查

- [ ] Pipeline 開頭的 `$match` 欄位是否有索引?
- [ ] `$sort` 欄位是否有索引? (僅在 `$match` 後立即 `$sort` 有效)
- [ ] 是否使用複合索引覆蓋查詢?
- [ ] 是否使用 `explain()` 確認索引使用?

### 資料建模檢查

- [ ] 是否過度使用 `$lookup`? (考慮嵌入式文件)
- [ ] 陣列欄位是否過大? (考慮拆分集合)
- [ ] 是否有不必要的嵌套? (考慮平面化)

### 分頁檢查

- [ ] 是否使用 `$skip` + `$limit` 做深度分頁? (考慮 Cursor-based Pagination)
- [ ] `$skip` 值是否過大? (效能隨 skip 值線性下降)

---

## 銜接 M08: 索引與效能深入

M07 提供 Aggregation Pipeline 的完整工具集,但效能最佳化的核心在於**索引設計**。M08 模組將深入探討:

1. **索引類型**: 單欄位索引、複合索引、多鍵索引、文字索引、地理空間索引、TTL 索引
2. **索引策略**: ESR 原則 (Equality, Sort, Range)、索引覆蓋查詢、索引選擇性
3. **效能分析**: `explain()` 詳解、執行計畫解讀、慢查詢日誌分析
4. **效能調校**: Working Set 管理、連線池配置、讀寫分離
5. **實務案例**: 高頻查詢最佳化、報表查詢加速、儀表板效能調校

**M07 與 M08 的關係**:
- M07: 提供**查詢工具** (Pipeline 階段組合)
- M08: 提供**效能保證** (索引設計 + 調校)

**學習建議**: 完成 M07 後,應立即學習 M08,將 Pipeline 設計與索引策略結合,才能發揮 MongoDB 的最大效能。

---

## 小結

本文件完成 M07 Aggregation Pipeline 模組的最後部分:

### $bucket / $bucketAuto

| 階段 | 用途 | 適用場景 |
|------|------|----------|
| `$bucket` | 明確邊界分桶 | 已知區間的直方圖統計 |
| `$bucketAuto` | 自動分桶 | 未知資料分布的均勻分桶 |

**關鍵參數**:
- `boundaries`: 左閉右開區間
- `granularity`: 精細度控制 (POWERSOF2, 1-2-5, R5, E12 等)

### 效能最佳化

| 策略 | 說明 | 效果 |
|------|------|------|
| `$match` 前置 | 減少後續階段處理資料量 | 顯著提升效能 |
| `$sort` + `$limit` 融合 | 觸發 Top-K 演算法 | 減少記憶體消耗 |
| 索引覆蓋查詢 | 僅掃描索引,不讀取文件 | 減少 I/O |
| `explain()` 分析 | 確認索引使用與執行計畫 | 驗證最佳化效果 |

### M05-M07 總覽

- **M05**: CRUD + Derived Queries + @Query (單一文件操作)
- **M06**: Criteria API + Text Search + Geospatial (複雜條件查詢)
- **M07**: Aggregation Pipeline (資料分析 + 跨集合查詢)

**選擇建議**:
1. 單一文件 CRUD → M05
2. 複合條件查詢 → M06
3. 統計分析 + 報表 → M07

**下一步**: M08 索引與效能深入,將 Pipeline 效能發揮到極致!
