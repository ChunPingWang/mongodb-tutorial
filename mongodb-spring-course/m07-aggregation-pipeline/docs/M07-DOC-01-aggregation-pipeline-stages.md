# M07-DOC-01: Aggregation Pipeline 核心階段

## 目錄

- [簡介](#簡介)
- [Pipeline 概念回顧](#pipeline-概念回顧)
- [核心階段操作](#核心階段操作)
  - [$match 階段](#match-階段)
  - [$group 階段](#group-階段)
  - [$project 階段](#project-階段)
  - [$sort 階段](#sort-階段)
  - [$limit 與 $skip 階段](#limit-與-skip-階段)
  - [$addFields 階段](#addfields-階段)
  - [$count 階段](#count-階段)
- [多重累加器](#多重累加器)
- [Typed DTO 結果映射](#typed-dto-結果映射)
- [Decimal128 處理注意事項](#decimal128-處理注意事項)
- [MongoTemplate 整合方式](#mongotemplate-整合方式)
- [小結](#小結)

---

## 簡介

Aggregation Pipeline 是 MongoDB 最強大的查詢與資料轉換工具,能夠執行複雜的資料分析、報表生成與統計計算。在 M06 Query DSL 模組中,我們初步接觸了 Aggregation 的基本概念;本模組將深入探討 Pipeline 的核心階段、多重累加器、DTO 映射,以及與 Spring Data MongoDB 的整合技巧。

本文件涵蓋:
- Pipeline 階段的執行順序與組合邏輯
- `$match`、`$group`、`$project`、`$sort`、`$limit`、`$skip`、`$addFields`、`$count` 的實務應用
- 多重累加器 (count/sum/avg/min/max) 一次性統計
- 使用 Java 23 Records 作為 Typed DTO
- Decimal128 欄位的映射與處理陷阱
- MongoTemplate 的 Aggregation API 整合模式

---

## Pipeline 概念回顧

Aggregation Pipeline 由多個**階段 (Stage)** 組成,資料從第一個階段開始,依序流經各階段,每個階段對文件進行篩選、轉換或聚合操作:

```
文件集合 → [$match] → [$group] → [$sort] → [$limit] → 結果
```

關鍵特性:
1. **順序執行**: 階段按定義順序執行,前一階段的輸出成為下一階段的輸入
2. **資料轉換**: 每個階段可改變文件結構 (新增/移除/重命名欄位)
3. **效能最佳化**: MongoDB 查詢優化器會自動重排某些階段 (如 `$match` 前置)
4. **索引利用**: Pipeline 開頭的 `$match`/`$sort` 可利用索引

在 Spring Data MongoDB 中,使用 `Aggregation.newAggregation()` 建立 Pipeline,並透過 `MongoTemplate.aggregate()` 執行。

---

## 核心階段操作

### $match 階段

`$match` 用於篩選文件,語法與 `find()` 的查詢條件相同。**最佳實務是將 `$match` 置於 Pipeline 開頭**,以減少後續階段處理的文件數量。

#### 範例: 篩選 ACTIVE 狀態帳戶

```java
@Service
public class BasicAggregationService {

    private final MongoTemplate mongoTemplate;

    public List<TypeCount> countByType(AccountStatus status) {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("status").is(status)),  // ← $match 階段
                group("type").count().as("count"),
                project("count").and("_id").as("type")
        );
        AggregationResults<TypeCount> results = mongoTemplate.aggregate(
                aggregation, "m07_bank_accounts", TypeCount.class);
        return results.getMappedResults();
    }
}
```

**執行流程**:
1. `$match` 過濾出 `status: ACTIVE` 的帳戶
2. `$group` 依 `type` 欄位分組,計算每組數量
3. `$project` 重新命名欄位 (`_id` → `type`)

**對應 MongoDB Shell**:
```javascript
db.m07_bank_accounts.aggregate([
  { $match: { status: "ACTIVE" } },
  { $group: { _id: "$type", count: { $sum: 1 } } },
  { $project: { type: "$_id", count: 1, _id: 0 } }
])
```

---

### $group 階段

`$group` 依指定欄位分組,並使用**累加器 (Accumulator)** 計算統計值。分組欄位會映射到輸出文件的 `_id` 欄位。

#### 基本累加器

| 累加器 | 說明 | Spring Data API |
|--------|------|-----------------|
| `$sum` | 總和 | `sum("field").as("total")` |
| `$avg` | 平均 | `avg("field").as("average")` |
| `$min` | 最小值 | `min("field").as("min")` |
| `$max` | 最大值 | `max("field").as("max")` |
| `$count` | 計數 | `count().as("count")` |
| `$push` | 收集所有值 | `push("field").as("values")` |
| `$addToSet` | 收集唯一值 | `addToSet("field").as("uniqueValues")` |

#### 範例: 各帳戶類型餘額總和

```java
public List<TypeSum> sumBalanceByType() {
    Aggregation aggregation = newAggregation(
            group("type").sum("balance").as("total"),  // ← 依 type 分組,計算 balance 總和
            project("total").and("_id").as("type")
    );
    AggregationResults<TypeSum> results = mongoTemplate.aggregate(
            aggregation, "m07_bank_accounts", TypeSum.class);
    return results.getMappedResults();
}
```

**輸出 DTO**:
```java
public record TypeSum(String type, BigDecimal total) {}
```

**範例資料**:
```
SAVINGS: 50000 + 80000 + 30000 = 160000
CHECKING: 15000 + 25000 = 40000
```

---

### $project 階段

`$project` 控制輸出文件的欄位結構,可以:
- 包含/排除欄位
- 重新命名欄位
- 計算新欄位 (使用運算式)

#### 範例: 欄位重新命名

```java
Aggregation aggregation = newAggregation(
        group("type").avg("balance").as("avgBalance"),
        project("avgBalance").and("_id").as("type")  // ← 將 _id 重新命名為 type
);
```

#### 範例: 計算欄位 (條件運算式)

```java
public List<Map> projectComputedField() {
    Aggregation aggregation = newAggregation(
            match(Criteria.where("status").is(AccountStatus.ACTIVE)),
            project("accountNumber", "holderName", "balance")
                    .andExpression("cond(balance >= 50000, 'HIGH', 'NORMAL')")
                    .as("balanceTier")  // ← 新增計算欄位
    );
    AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, "m07_bank_accounts", Map.class);
    return results.getMappedResults();
}
```

**輸出範例**:
```json
{
  "accountNumber": "ACC-001",
  "holderName": "Alice",
  "balance": 50000,
  "balanceTier": "HIGH"  // ← 依條件計算
}
```

**運算式語法**:
- `cond(condition, trueValue, falseValue)`: 條件運算
- `concat(str1, str2)`: 字串連接
- `subtract(num1, num2)`: 數值減法
- `multiply(num1, num2)`: 數值乘法

---

### $sort 階段

`$sort` 依指定欄位排序,通常用於 `$group` 之後,對聚合結果排序。

#### 範例: 依產品類別數量排序

```java
public List<CategoryStats> sortedCategoryCounts() {
    Aggregation aggregation = newAggregation(
            group("category").count().as("count")
                    .avg("price").as("avgPrice")
                    .avg("rating").as("avgRating"),
            project("count", "avgPrice", "avgRating").and("_id").as("category"),
            sort(Sort.Direction.DESC, "count")  // ← 依 count 降序排序
    );
    AggregationResults<CategoryStats> results = mongoTemplate.aggregate(
            aggregation, "m07_products", CategoryStats.class);
    return results.getMappedResults();
}
```

**輸出範例**:
```
Electronics: count=3
Books: count=2
Furniture: count=1
```

---

### $limit 與 $skip 階段

- `$limit`: 限制輸出文件數量 (類似 SQL `LIMIT`)
- `$skip`: 跳過指定數量的文件 (類似 SQL `OFFSET`)

#### 範例: Top N 查詢

```java
public List<CategoryStats> topNCategories(int n) {
    Aggregation aggregation = newAggregation(
            group("category").count().as("count")
                    .avg("price").as("avgPrice")
                    .avg("rating").as("avgRating"),
            project("count", "avgPrice", "avgRating").and("_id").as("category"),
            sort(Sort.Direction.DESC, "count"),
            limit(n)  // ← 限制輸出 n 筆
    );
    AggregationResults<CategoryStats> results = mongoTemplate.aggregate(
            aggregation, "m07_products", CategoryStats.class);
    return results.getMappedResults();
}
```

**分頁範例**:
```java
int page = 2, pageSize = 10;
Aggregation aggregation = newAggregation(
        match(Criteria.where("status").is("ACTIVE")),
        sort(Sort.Direction.ASC, "createdAt"),
        skip((long) page * pageSize),  // ← 跳過前 20 筆
        limit(pageSize)                // ← 取 10 筆
);
```

---

### $addFields 階段

`$addFields` 新增欄位,但**保留原有欄位** (與 `$project` 的差異)。適合在不破壞原始文件結構的前提下新增計算欄位。

#### 範例: 新增折扣價欄位

```java
Aggregation aggregation = newAggregation(
        match(Criteria.where("category").is("Electronics")),
        addFields().addField("discountPrice")
                .withValue(ArithmeticOperators.Multiply.valueOf("price").multiplyBy(0.9))
                .build()
);
```

**對比 $project**:
- `$project`: 明確列出的欄位才會輸出
- `$addFields`: 保留所有原欄位,額外新增欄位

---

### $count 階段

`$count` 計算 Pipeline 中當前階段的文件總數,輸出單一文件包含指定欄位名稱的計數值。

#### 範例: 計算 ACTIVE 帳戶總數

```java
public long countDocuments(AccountStatus status) {
    Aggregation aggregation = newAggregation(
            match(Criteria.where("status").is(status)),
            count().as("total")  // ← 計數並命名為 total
    );
    AggregationResults<Map> results = mongoTemplate.aggregate(
            aggregation, "m07_bank_accounts", Map.class);
    List<Map> mapped = results.getMappedResults();
    if (mapped.isEmpty()) {
        return 0;
    }
    return ((Number) mapped.getFirst().get("total")).longValue();
}
```

**輸出範例**:
```json
{ "total": 4 }
```

**注意**: `$count` 後無法再接其他階段 (除了 `$out`/`$merge`),因為輸出已是單一文件。

---

## 多重累加器

在單一 `$group` 階段可使用多個累加器,一次性計算各種統計值,避免多次查詢。

#### 範例: 完整統計資訊

```java
public List<TypeStats> statsPerType() {
    Aggregation aggregation = newAggregation(
            group("type")
                    .count().as("count")        // ← 計數
                    .sum("balance").as("total") // ← 總和
                    .avg("balance").as("avg")   // ← 平均
                    .min("balance").as("min")   // ← 最小值
                    .max("balance").as("max"),  // ← 最大值
            project("count", "total", "avg", "min", "max").and("_id").as("type")
    );
    AggregationResults<TypeStats> results = mongoTemplate.aggregate(
            aggregation, "m07_bank_accounts", TypeStats.class);
    return results.getMappedResults();
}
```

**Typed DTO**:
```java
public record TypeStats(
    String type,
    long count,
    BigDecimal total,
    BigDecimal avg,
    BigDecimal min,
    BigDecimal max
) {}
```

**輸出範例**:
```
SAVINGS: {count=3, total=160000, avg=53333.33, min=30000, max=80000}
CHECKING: {count=2, total=40000, avg=20000, min=15000, max=25000}
```

**優點**:
- 單次查詢獲取完整統計
- 減少網路往返次數
- 提升效能與開發效率

---

## Typed DTO 結果映射

使用 **Java 23 Records** 作為 Typed DTO,提供型別安全與簡潔的資料映射。

#### Record 定義

```java
public record TypeCount(String type, long count) {}

public record TypeStats(
    String type,
    long count,
    BigDecimal total,
    BigDecimal avg,
    BigDecimal min,
    BigDecimal max
) {}

public record CategoryStats(
    String category,
    long count,
    double avgPrice,
    double avgRating
) {}
```

#### MongoTemplate 映射

```java
AggregationResults<TypeCount> results = mongoTemplate.aggregate(
        aggregation, "m07_bank_accounts", TypeCount.class);  // ← 指定 DTO 類型
return results.getMappedResults();
```

**映射規則**:
1. **欄位名稱匹配**: DTO 欄位名稱需與 Pipeline 輸出欄位一致
2. **型別轉換**: Spring Data 自動處理型別轉換 (如 `Decimal128` → `BigDecimal`)
3. **Record 不可變性**: Record 的 Canonical Constructor 自動處理參數注入

#### Map 動態映射

對於結構不固定的輸出,可使用 `Map.class`:

```java
AggregationResults<Map> results = mongoTemplate.aggregate(
        aggregation, "m07_bank_accounts", Map.class);
List<Map> mapped = results.getMappedResults();
```

**缺點**: 失去型別安全,需手動型別轉換。

---

## Decimal128 處理注意事項

MongoDB 的 `Decimal128` 型別在 Spring Data 中映射為 `BigDecimal`,但需注意以下陷阱:

### 1. 實體欄位宣告

```java
@Document("m07_bank_accounts")
public class BankAccount {
    @Field(targetType = FieldType.DECIMAL128)  // ← 必須明確宣告
    private BigDecimal balance;
}
```

**若未宣告 `targetType`**: BigDecimal 會儲存為**字串**,導致數值比較失效 (字典序排序)。

### 2. Aggregation 累加器自動轉換

在 `$group` 累加器中,Spring Data 會自動將 `Decimal128` 轉換為 `BigDecimal`:

```java
group("type")
    .sum("balance").as("total")   // ← MongoDB Decimal128 → Java BigDecimal
    .avg("balance").as("avg")
```

### 3. DTO 型別宣告

```java
public record TypeStats(
    String type,
    long count,
    BigDecimal total,   // ← 使用 BigDecimal 接收 Decimal128 聚合結果
    BigDecimal avg,
    BigDecimal min,
    BigDecimal max
) {}
```

### 4. 條件運算式中的 Decimal128

在 `$project` 的運算式中,Decimal128 欄位可直接參與比較:

```java
project("accountNumber", "holderName", "balance")
        .andExpression("cond(balance >= 50000, 'HIGH', 'NORMAL')")
        .as("balanceTier")
```

**注意**: 若實體欄位未宣告 `targetType = DECIMAL128`,此處的數值比較會失效。

---

## MongoTemplate 整合方式

### 基本流程

```java
@Service
public class AggregationService {

    private final MongoTemplate mongoTemplate;

    public AggregationService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<TypeCount> analyze() {
        // 1. 建立 Aggregation Pipeline
        Aggregation aggregation = newAggregation(
                match(Criteria.where("status").is(AccountStatus.ACTIVE)),
                group("type").count().as("count"),
                project("count").and("_id").as("type")
        );

        // 2. 執行 Pipeline
        AggregationResults<TypeCount> results = mongoTemplate.aggregate(
                aggregation,
                "m07_bank_accounts",  // ← 集合名稱
                TypeCount.class       // ← 結果型別
        );

        // 3. 獲取映射結果
        return results.getMappedResults();
    }
}
```

### 靜態匯入

```java
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
```

簡化 API 呼叫:
```java
// 完整寫法
Aggregation.newAggregation(
    Aggregation.match(Criteria.where("status").is("ACTIVE")),
    Aggregation.group("type").count().as("count")
)

// 靜態匯入後
newAggregation(
    match(Criteria.where("status").is("ACTIVE")),
    group("type").count().as("count")
)
```

### TypedAggregation (可選)

```java
TypedAggregation<BankAccount> aggregation = newAggregation(
        BankAccount.class,  // ← 指定輸入型別
        match(Criteria.where("status").is(AccountStatus.ACTIVE)),
        group("type").count().as("count")
);
```

**優點**: Spring Data 會根據 `BankAccount` 的 `@Document` 註解自動推斷集合名稱。

---

## 小結

本文件涵蓋 Aggregation Pipeline 的核心階段:

| 階段 | 用途 | 關鍵提示 |
|------|------|----------|
| `$match` | 篩選文件 | 盡可能前置,利用索引 |
| `$group` | 分組聚合 | 支援多重累加器 |
| `$project` | 欄位投影 | 可計算新欄位 |
| `$sort` | 排序 | 搭配 `$limit` 做 Top N |
| `$limit` / `$skip` | 分頁 | 注意 `$skip` 效能成本 |
| `$addFields` | 新增欄位 | 保留原欄位 |
| `$count` | 計數 | 輸出單一文件 |

**關鍵要點**:
1. **型別安全**: 使用 Java Records 作為 Typed DTO
2. **Decimal128**: 實體欄位必須宣告 `@Field(targetType = DECIMAL128)`
3. **多重累加器**: 一次查詢獲取完整統計
4. **效能最佳化**: `$match` 前置,利用索引

**下一步**: 在 M07-DOC-02 中,我們將探討 `$lookup`、`$unwind` 與 `$facet` 的進階應用,實現跨集合關聯查詢與多面向分析。
