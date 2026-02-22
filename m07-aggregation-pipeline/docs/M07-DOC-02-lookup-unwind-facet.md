# M07-DOC-02: $lookup、$unwind 與 $facet

## 目錄

- [簡介](#簡介)
- [$unwind 展開陣列](#unwind-展開陣列)
  - [基本語法](#基本語法)
  - [preserveNullAndEmptyArrays 參數](#preservenullandemptyarrays-參數)
  - [標籤統計範例](#標籤統計範例)
  - [訂單項目展開範例](#訂單項目展開範例)
- [$lookup 關聯查詢](#lookup-關聯查詢)
  - [基本語法 (localField/foreignField)](#基本語法-localfieldforeignfield)
  - [訂單→商品關聯範例](#訂單商品關聯範例)
  - [$lookup 與 $unwind 組合](#lookup-與-unwind-組合)
  - [$lookup Pipeline 變體](#lookup-pipeline-變體)
- [$facet 多面向查詢](#facet-多面向查詢)
  - [基本概念](#基本概念)
  - [商品搜尋儀表板範例](#商品搜尋儀表板範例)
  - [銀行業務儀表板範例](#銀行業務儀表板範例)
  - [分頁 + 統計組合](#分頁--統計組合)
- [實務模式總結](#實務模式總結)
- [小結](#小結)

---

## 簡介

在 M07-DOC-01 中,我們學習了 Aggregation Pipeline 的核心階段 (`$match`、`$group`、`$project` 等)。本文件進入進階主題:

1. **$unwind**: 展開陣列欄位,將單一文件拆分為多個文件
2. **$lookup**: 跨集合關聯查詢 (類似 SQL JOIN)
3. **$facet**: 在單一查詢中執行多個平行的 Pipeline 分支,實現多面向分析

這三個階段是實務中最常用的進階操作,能夠處理複雜的資料結構與業務需求。

---

## $unwind 展開陣列

### 基本語法

`$unwind` 將文件中的陣列欄位「展開」,每個陣列元素生成一個新文件。

**展開前**:
```json
{
  "_id": 1,
  "name": "Wireless Mouse",
  "tags": ["wireless", "mouse", "computer"]
}
```

**展開後** (`$unwind: "tags"`):
```json
{ "_id": 1, "name": "Wireless Mouse", "tags": "wireless" }
{ "_id": 1, "name": "Wireless Mouse", "tags": "mouse" }
{ "_id": 1, "name": "Wireless Mouse", "tags": "computer" }
```

### Spring Data API

```java
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

Aggregation aggregation = newAggregation(
        unwind("tags")  // ← 展開 tags 陣列
);
```

---

### preserveNullAndEmptyArrays 參數

預設情況下,若陣列欄位為 `null` 或空陣列 `[]`,`$unwind` 會**排除**該文件。若要保留這些文件,需設定 `preserveNullAndEmptyArrays: true`。

#### 範例: 保留空陣列文件

```java
public List<Map> unwindPreserveEmpty() {
    Aggregation aggregation = newAggregation(
            unwind("tags", true),  // ← preserveNullAndEmptyArrays = true
            project("name", "tags", "category")
    );
    AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, "m07_products", Map.class);
    return results.getMappedResults();
}
```

**資料範例**:
```json
{ "name": "Product A", "tags": ["tag1", "tag2"] }
{ "name": "Product B", "tags": [] }
{ "name": "Product C", "tags": null }
```

**輸出 (preserveNullAndEmptyArrays = false)**:
```json
{ "name": "Product A", "tags": "tag1" }
{ "name": "Product A", "tags": "tag2" }
// Product B 和 Product C 被排除
```

**輸出 (preserveNullAndEmptyArrays = true)**:
```json
{ "name": "Product A", "tags": "tag1" }
{ "name": "Product A", "tags": "tag2" }
{ "name": "Product B", "tags": null }
{ "name": "Product C", "tags": null }
```

---

### 標籤統計範例

#### 需求: 統計所有標籤的出現次數

```java
@Service
public class UnwindAggregationService {

    private final MongoTemplate mongoTemplate;

    public List<TagCount> countByTag() {
        Aggregation aggregation = newAggregation(
                unwind("tags"),                           // ← 展開 tags 陣列
                group("tags").count().as("count"),        // ← 依 tag 分組計數
                project("count").and("_id").as("tag"),    // ← 重新命名欄位
                sort(Sort.Direction.DESC, "count")        // ← 降序排序
        );
        AggregationResults<TagCount> results = mongoTemplate.aggregate(
                aggregation, "m07_products", TagCount.class);
        return results.getMappedResults();
    }
}
```

**DTO**:
```java
public record TagCount(String tag, long count) {}
```

**輸出範例**:
```
computer: 3
programming: 2
wireless: 1
mouse: 1
keyboard: 1
java: 2
spring: 1
...
```

#### Top N 標籤

```java
public List<TagCount> topTags(int n) {
    Aggregation aggregation = newAggregation(
            unwind("tags"),
            group("tags").count().as("count"),
            project("count").and("_id").as("tag"),
            sort(Sort.Direction.DESC, "count"),
            limit(n)  // ← 限制輸出 N 筆
    );
    AggregationResults<TagCount> results = mongoTemplate.aggregate(
            aggregation, "m07_products", TagCount.class);
    return results.getMappedResults();
}
```

---

### 訂單項目展開範例

#### 需求: 統計每筆訂單的項目數量

**訂單資料結構**:
```json
{
  "orderNumber": "ORD-001",
  "customerName": "Alice",
  "items": [
    { "sku": "SKU-001", "quantity": 2, "unitPrice": 29.99 },
    { "sku": "SKU-002", "quantity": 1, "unitPrice": 89.99 }
  ]
}
```

**實作**:
```java
public List<Map> itemCountByOrder() {
    Aggregation aggregation = newAggregation(
            unwind("items"),                          // ← 展開 items 陣列
            group("orderNumber").count().as("itemCount"),
            project("itemCount").and("_id").as("orderNumber"),
            sort(Sort.Direction.ASC, "orderNumber")
    );
    AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, "m07_orders", Map.class);
    return results.getMappedResults();
}
```

**輸出範例**:
```
ORD-001: itemCount=2
ORD-002: itemCount=3
ORD-003: itemCount=1
```

---

## $lookup 關聯查詢

### 基本語法 (localField/foreignField)

`$lookup` 用於跨集合關聯查詢,類似 SQL 的 `LEFT OUTER JOIN`。

**MongoDB Shell 語法**:
```javascript
{
  $lookup: {
    from: "目標集合",
    localField: "本地欄位",
    foreignField: "目標欄位",
    as: "輸出陣列欄位名稱"
  }
}
```

**Spring Data API**:
```java
lookup("目標集合", "本地欄位", "目標欄位", "輸出欄位")
```

---

### 訂單→商品關聯範例

#### 需求: 訂單項目關聯商品詳細資訊

**資料結構**:
- **訂單集合** (`m07_orders`):
  ```json
  {
    "orderNumber": "ORD-001",
    "items": [
      { "sku": "SKU-001", "quantity": 2 },
      { "sku": "SKU-002", "quantity": 1 }
    ]
  }
  ```

- **商品集合** (`m07_products`):
  ```json
  { "sku": "SKU-001", "name": "Wireless Mouse", "price": 29.99 }
  { "sku": "SKU-002", "name": "Keyboard", "price": 89.99 }
  ```

#### 實作

```java
@Service
public class LookupAggregationService {

    private final MongoTemplate mongoTemplate;

    public List<Map> ordersWithProductDetails() {
        Aggregation aggregation = newAggregation(
                unwind("items"),                                           // ← 展開訂單項目
                lookup("m07_products", "items.sku", "sku", "productDetails"),  // ← 關聯商品
                project("orderNumber", "customerName", "items", "productDetails")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_orders", Map.class);
        return results.getMappedResults();
    }
}
```

**執行流程**:
1. `unwind("items")`: 展開 `items` 陣列,每個訂單項目生成一個文件
2. `lookup(...)`: 將 `items.sku` 與 `m07_products.sku` 比對,結果存入 `productDetails` 陣列
3. `project(...)`: 選擇輸出欄位

**輸出範例**:
```json
{
  "orderNumber": "ORD-001",
  "customerName": "Alice",
  "items": { "sku": "SKU-001", "quantity": 2 },
  "productDetails": [
    { "sku": "SKU-001", "name": "Wireless Mouse", "price": 29.99 }
  ]
}
```

**注意**: `$lookup` 輸出的 `productDetails` 是**陣列**,即使只有一個匹配結果。

---

### $lookup 與 $unwind 組合

#### 需求: 平面化輸出,避免巢狀陣列

```java
public List<Map> lookupWithUnwind() {
    Aggregation aggregation = newAggregation(
            unwind("items"),
            lookup("m07_products", "items.sku", "sku", "product"),  // ← 關聯商品
            unwind("product", true),                                // ← 展開 product 陣列
            project("orderNumber", "customerName")
                    .and("items.sku").as("sku")
                    .and("items.quantity").as("quantity")
                    .and("product.name").as("productName")          // ← 平面化欄位
                    .and("product.price").as("productPrice")
    );
    AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, "m07_orders", Map.class);
    return results.getMappedResults();
}
```

**輸出範例**:
```json
{
  "orderNumber": "ORD-001",
  "customerName": "Alice",
  "sku": "SKU-001",
  "quantity": 2,
  "productName": "Wireless Mouse",
  "productPrice": 29.99
}
```

**關鍵技巧**: 使用 `unwind("product", true)` 展開 `$lookup` 的輸出陣列,並設定 `preserveNullAndEmptyArrays = true`,避免找不到匹配商品時訂單項目消失。

---

### $lookup Pipeline 變體

#### 進階語法: 自訂 Pipeline

Spring Data 支援 `$lookup` 的 Pipeline 變體,允許在關聯集合上執行複雜查詢。

```java
public List<Map> pipelineLookup() {
    Aggregation aggregation = newAggregation(
            unwind("items"),
            lookup()
                    .from("m07_products")
                    .localField("items.sku")
                    .foreignField("sku")
                    .as("productInfo"),
            unwind("productInfo", true),
            project("orderNumber", "customerName")
                    .and("items.sku").as("sku")
                    .and("items.quantity").as("quantity")
                    .and("items.unitPrice").as("unitPrice")
                    .and("productInfo.name").as("productName")
                    .and("productInfo.category").as("productCategory")
    );
    AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, "m07_orders", Map.class);
    return results.getMappedResults();
}
```

**MongoDB 4.4+ 進階 Pipeline 語法** (Spring Data 尚未完整支援,需使用原生 Document):
```java
Document lookupStage = new Document("$lookup", new Document()
        .append("from", "m07_products")
        .append("let", new Document("orderSku", "$items.sku"))
        .append("pipeline", List.of(
                new Document("$match", new Document("$expr",
                        new Document("$eq", List.of("$sku", "$$orderSku")))),
                new Document("$project", new Document("name", 1).append("price", 1))
        ))
        .append("as", "productInfo")
);
```

---

## $facet 多面向查詢

### 基本概念

`$facet` 允許在單一 Aggregation 查詢中執行**多個平行的 Pipeline 分支**,每個分支獨立處理資料,最後合併輸出。

**應用場景**:
- 搜尋結果 + 分頁資訊 + 統計摘要 (一次查詢完成)
- 儀表板多維度分析
- 減少網路往返次數

**基本語法**:
```java
facet(
    pipeline1...
).as("分支1名稱")
 .and(
    pipeline2...
).as("分支2名稱")
 .and(
    pipeline3...
).as("分支3名稱")
```

---

### 商品搜尋儀表板範例

#### 需求: 單一查詢返回總筆數 + 分頁資料 + 統計資訊

```java
@Service
public class FacetAggregationService {

    private final MongoTemplate mongoTemplate;

    public Map productSearchFacet(String category) {
        FacetOperation facet = facet(
                count().as("total")                     // ← 分支1: 總筆數
        ).as("totalCount")
                .and(
                        sort(Sort.Direction.ASC, "name"),
                        skip(0L),
                        limit(10)                       // ← 分支2: 分頁資料
                ).as("data")
                .and(
                        group()
                                .avg("price").as("avgPrice")
                                .min("price").as("minPrice")
                                .max("price").as("maxPrice")  // ← 分支3: 價格統計
                ).as("stats");

        Aggregation aggregation;
        if (category != null) {
            aggregation = newAggregation(
                    match(Criteria.where("category").is(category)),  // ← 前置過濾
                    facet
            );
        } else {
            aggregation = newAggregation(facet);
        }

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "m07_products", Map.class);
        return results.getMappedResults().isEmpty() ? Map.of() : results.getMappedResults().getFirst();
    }
}
```

**輸出範例**:
```json
{
  "totalCount": [{ "total": 3 }],
  "data": [
    { "name": "Mechanical Keyboard", "price": 89.99 },
    { "name": "USB-C Hub", "price": 39.99 },
    { "name": "Wireless Mouse", "price": 29.99 }
  ],
  "stats": [
    { "avgPrice": 53.32, "minPrice": 29.99, "maxPrice": 89.99 }
  ]
}
```

**注意**: 每個 `$facet` 分支的輸出都是**陣列**,即使只有一個元素 (如 `totalCount`)。

---

### 銀行業務儀表板範例

#### 需求: 同時統計帳戶狀態分布 + 類型分布 + 餘額統計

```java
public Map bankingDashboard() {
    FacetOperation facet = facet(
            group("status").count().as("count"),
            project("count").and("_id").as("status")
    ).as("statusCounts")
            .and(
                    group("type").count().as("count"),
                    project("count").and("_id").as("type")
            ).as("typeCounts")
            .and(
                    group()
                            .sum("balance").as("totalBalance")
                            .avg("balance").as("avgBalance")
                            .min("balance").as("minBalance")
                            .max("balance").as("maxBalance")
            ).as("balanceStats");

    Aggregation aggregation = newAggregation(facet);
    AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, "m07_bank_accounts", Map.class);
    return results.getMappedResults().isEmpty() ? Map.of() : results.getMappedResults().getFirst();
}
```

**輸出範例**:
```json
{
  "statusCounts": [
    { "status": "ACTIVE", "count": 4 },
    { "status": "CLOSED", "count": 1 }
  ],
  "typeCounts": [
    { "type": "SAVINGS", "count": 3 },
    { "type": "CHECKING", "count": 2 }
  ],
  "balanceStats": [
    {
      "totalBalance": 200000,
      "avgBalance": 40000,
      "minBalance": 15000,
      "maxBalance": 80000
    }
  ]
}
```

**優點**: 單一查詢完成多維度分析,減少 3 次查詢到 1 次查詢。

---

### 分頁 + 統計組合

#### 需求: 支援分頁參數的商品搜尋

```java
public Map facetCombinedSearch(String category, boolean inStockOnly, int page, int pageSize) {
    Criteria criteria = Criteria.where("category").is(category);
    if (inStockOnly) {
        criteria = criteria.and("inStock").is(true);
    }

    FacetOperation facet = facet(
            count().as("total")
    ).as("totalCount")
            .and(
                    sort(Sort.Direction.ASC, "price"),
                    skip((long) page * pageSize),    // ← 分頁 skip
                    limit(pageSize)                  // ← 分頁 limit
            ).as("data")
            .and(
                    group()
                            .count().as("count")
                            .avg("price").as("avgPrice")
                            .min("price").as("minPrice")
                            .max("price").as("maxPrice")
                            .avg("rating").as("avgRating")
            ).as("stats");

    Aggregation aggregation = newAggregation(
            match(criteria),  // ← 前置過濾
            facet
    );

    AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, "m07_products", Map.class);
    return results.getMappedResults().isEmpty() ? Map.of() : results.getMappedResults().getFirst();
}
```

**呼叫範例**:
```java
Map result = service.facetCombinedSearch("Electronics", true, 0, 5);
// category=Electronics, inStock=true, page=0, pageSize=5
```

**輸出範例**:
```json
{
  "totalCount": [{ "total": 3 }],
  "data": [
    { "name": "Wireless Mouse", "price": 29.99 },
    { "name": "Mechanical Keyboard", "price": 89.99 }
  ],
  "stats": [
    {
      "count": 3,
      "avgPrice": 53.32,
      "minPrice": 29.99,
      "maxPrice": 89.99,
      "avgRating": 4.4
    }
  ]
}
```

---

## 實務模式總結

### 1. 陣列標籤統計模式

```
$unwind("tags") → $group("tags").count() → $sort(DESC) → $limit(N)
```

**應用**: 熱門標籤、分類統計、關鍵字分析

---

### 2. 跨集合關聯模式

```
$unwind("items") → $lookup(from, local, foreign, as) → $unwind(as, true) → $project
```

**應用**: 訂單→商品、使用者→訂單、文章→作者

**注意**:
- `$lookup` 輸出為陣列,需 `$unwind` 平面化
- 使用 `preserveNullAndEmptyArrays: true` 避免遺失資料

---

### 3. 搜尋儀表板模式

```
$match(filter) → $facet(
  totalCount: $count,
  data: $sort → $skip → $limit,
  stats: $group(aggregators)
)
```

**應用**: 商品搜尋、報表查詢、管理後台列表

**優點**: 單一查詢返回分頁資料 + 總筆數 + 統計摘要

---

### 4. 多維度分析模式

```
$facet(
  dimension1: $group(field1).count(),
  dimension2: $group(field2).count(),
  summary: $group().sum/avg/min/max
)
```

**應用**: 業務儀表板、數據分析、BI 報表

---

## 小結

本文件深入探討三個進階 Aggregation 階段:

| 階段 | 核心功能 | 常見組合 |
|------|---------|---------|
| `$unwind` | 展開陣列,拆分文件 | `$unwind` → `$group` → `$sort` |
| `$lookup` | 跨集合關聯查詢 | `$unwind` → `$lookup` → `$unwind` → `$project` |
| `$facet` | 多 Pipeline 平行執行 | `$match` → `$facet(totalCount/data/stats)` |

**關鍵要點**:

1. **$unwind 參數**: `preserveNullAndEmptyArrays: true` 保留空陣列文件
2. **$lookup 輸出**: 永遠是陣列,需搭配 `$unwind` 平面化
3. **$facet 輸出**: 每個分支輸出都是陣列
4. **效能最佳化**: `$match` 前置於 `$facet` 之前,減少處理的資料量

**實務建議**:
- 複雜查詢優先考慮 `$facet`,減少查詢次數
- `$lookup` 避免大量關聯,注意記憶體與效能
- `$unwind` 大陣列時注意文件膨脹,可能影響效能

**下一步**: 在 M07-DOC-03 中,我們將學習 `$bucket`/`$bucketAuto` 分桶分析,以及 Pipeline 效能最佳化策略,並總結 M05-M07 的查詢能力回顧。
