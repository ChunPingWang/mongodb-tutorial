# M06-DOC-01: Criteria API 深入解析

## 前言

在 M05 中，我們學會了使用 `MongoRepository` 的 Derived Queries 和 `@Query` 註解進行宣告式查詢。但當查詢條件變得複雜、需要動態組合時，宣告式查詢往往力不從心。本模組介紹 Spring Data MongoDB 的 **Criteria API** — 一套程式化查詢建構工具，讓你用 Java 程式碼組合任意複雜的查詢條件。

## 1. Criteria 基礎

### 1.1 核心類別

| 類別 | 用途 |
|------|------|
| `Criteria` | 查詢條件建構器，對應 MongoDB 的查詢運算子 |
| `Query` | 將 Criteria 包裝成完整查詢，支援排序、分頁、投影 |
| `MongoTemplate` | 執行查詢的核心元件 |

### 1.2 基本查詢

```java
// 等值查詢：找出所有 ACTIVE 帳戶
Query query = new Query(Criteria.where("status").is(AccountStatus.ACTIVE));
List<BankAccount> results = mongoTemplate.find(query, BankAccount.class);
```

對應的 MongoDB 查詢：
```json
{ "status": "ACTIVE" }
```

### 1.3 鏈式 AND 查詢

```java
// 多欄位 AND：type = SAVINGS 且 status = ACTIVE
Query query = new Query(
    Criteria.where("type").is(AccountType.SAVINGS)
            .and("status").is(AccountStatus.ACTIVE)
);
```

> **注意**：鏈式 `.and("field")` 只能用於**不同欄位**。若需要對同一欄位設定多個條件（如 `gte` + `lte`），請直接串接在同一個 Criteria 上。

## 2. 組合查詢

### 2.1 OR 查詢

```java
// SAVINGS 帳戶 OR FROZEN 狀態
new Criteria().orOperator(
    Criteria.where("type").is(AccountType.SAVINGS),
    Criteria.where("status").is(AccountStatus.FROZEN)
);
```

### 2.2 AND 組合器

```java
// 使用 andOperator 組合多個 Criteria
new Criteria().andOperator(
    Criteria.where("status").is(PolicyStatus.ACTIVE),
    Criteria.where("coverageAmount").gte(new Decimal128(minCoverage))
);
```

### 2.3 巢狀 AND + OR

```java
// (TERM_LIFE OR WHOLE_LIFE) AND ACTIVE
new Criteria().andOperator(
    new Criteria().orOperator(
        Criteria.where("policyType").is(PolicyType.TERM_LIFE),
        Criteria.where("policyType").is(PolicyType.WHOLE_LIFE)
    ),
    Criteria.where("status").is(PolicyStatus.ACTIVE)
);
```

## 3. 比較與範圍查詢

### 3.1 範圍查詢

```java
// 餘額在 10,000 到 60,000 之間
Criteria.where("balance").gte(new Decimal128(min)).lte(new Decimal128(max))
```

### 3.2 Decimal128 欄位的重要提醒

當 MongoDB 欄位使用 `@Field(targetType = FieldType.DECIMAL128)` 儲存 BigDecimal 時，Criteria API 的 `gte()`、`lte()`、`gt()`、`lt()` 運算子需要**明確轉換為 `org.bson.types.Decimal128`**：

```java
// ✅ 正確：明確轉換
Criteria.where("balance").gte(new Decimal128(min)).lte(new Decimal128(max))

// ❌ 錯誤：直接傳入 BigDecimal，MongoDB 無法正確比較
Criteria.where("balance").gte(min).lte(max)
```

這是因為 Spring Data MongoDB 的 Criteria API 在建構查詢時，不會自動根據 `@Field(targetType)` 註解轉換運算子參數的型別。

### 3.3 集合運算子

```java
// $in：匹配指定類型
Criteria.where("policyType").in(List.of(PolicyType.TERM_LIFE, PolicyType.WHOLE_LIFE))

// $nin：排除指定類型
Criteria.where("policyType").nin(List.of(PolicyType.AUTO))

// $ne：不等於
Criteria.where("status").ne(PolicyStatus.CANCELLED)
```

## 4. 正則表達式查詢

```java
// 包含 "Wang" 的持有人名稱
Criteria.where("holderName").regex(".*Wang.*")

// 以 "A" 開頭
Criteria.where("holderName").regex("^A")

// 不區分大小寫
Criteria.where("name").regex(pattern, "i")
```

## 5. Projection（投影）

Projection 讓你只回傳所需欄位，減少網路傳輸量：

```java
Query query = new Query();
query.fields()
    .include("accountNumber")  // 只回傳 accountNumber
    .exclude("id");            // 排除 _id

List<BankAccount> results = mongoTemplate.find(query, BankAccount.class);
// results 中只有 accountNumber 有值，其他欄位為 null/預設值
```

### 5.1 Projection + Criteria 組合

```java
Query query = new Query(Criteria.where("type").is(AccountType.CHECKING));
query.fields().include("accountNumber", "holderName");
```

## 6. Distinct 查詢

```java
// 取得不重複的持有人名稱
List<String> names = mongoTemplate.findDistinct(
    new Query(),           // 可加 Criteria 過濾
    "holderName",          // 欄位名稱
    BankAccount.class,     // 實體類別
    String.class           // 回傳型別
);
```

## 7. 排序與分頁

### 7.1 排序

```java
Query query = new Query(Criteria.where("status").is(AccountStatus.ACTIVE));
query.with(Sort.by(Sort.Direction.ASC, "balance"));
```

### 7.2 分頁

```java
Query query = new Query(Criteria.where("status").is(AccountStatus.ACTIVE));
query.with(PageRequest.of(0, 10));  // 第 0 頁，每頁 10 筆
```

### 7.3 Limit

```java
Query query = new Query().limit(5);  // 最多回傳 5 筆
```

### 7.4 Count

```java
long count = mongoTemplate.count(
    new Query(Criteria.where("status").is(AccountStatus.ACTIVE)),
    BankAccount.class
);
```

## 8. 動態查詢建構

Criteria API 最大的優勢是**動態查詢**：根據參數有無，動態組合條件。

```java
public List<InsurancePolicyDocument> findByMultipleConditions(
        PolicyType type, PolicyStatus status, BigDecimal minPremium) {
    Criteria criteria = new Criteria();
    if (type != null) {
        criteria = criteria.and("policyType").is(type);
    }
    if (status != null) {
        criteria = criteria.and("status").is(status);
    }
    if (minPremium != null) {
        criteria = criteria.and("premium").gte(new Decimal128(minPremium));
    }
    return mongoTemplate.find(new Query(criteria), InsurancePolicyDocument.class);
}
```

這種模式在實務上非常常見 — 搜尋 API 通常有多個可選過濾條件，只有使用者填入的欄位才需要加入查詢。

## 9. Criteria API vs Repository 決策矩陣

| 場景 | 推薦方式 |
|------|---------|
| 固定條件的簡單查詢 | Derived Query / `@Query` |
| 2-3 個固定條件組合 | Derived Query |
| 動態條件組合 | **Criteria API** |
| 複雜的 AND/OR 巢狀 | **Criteria API** |
| 需要投影（部分欄位） | **Criteria API** |
| 全文檢索 | **Criteria API** (TextCriteria) |
| 地理空間查詢 | **Criteria API** (NearQuery) |
| Aggregation Pipeline | **MongoTemplate** |

## 10. 同一欄位的 AND 陷阱

```java
// ❌ 錯誤：第二個 .and("balance") 會覆蓋第一個
Criteria.where("balance").gte(min)
        .and("balance").lte(max)

// ✅ 正確：在同一個 Criteria 串接
Criteria.where("balance").gte(min).lte(max)

// ✅ 正確：使用 andOperator
new Criteria().andOperator(
    Criteria.where("balance").gte(min),
    Criteria.where("balance").lte(max)
)
```

## 小結

Criteria API 是 Spring Data MongoDB 的核心查詢工具，適合需要程式化建構查詢的場景。搭配 `MongoTemplate`，它提供了完整的查詢能力 — 從簡單的等值查詢到複雜的動態組合、投影、排序分頁，無一不能。

下一篇文件將介紹全文檢索與地理空間查詢，這些進階查詢同樣建構在 Criteria API 之上。
