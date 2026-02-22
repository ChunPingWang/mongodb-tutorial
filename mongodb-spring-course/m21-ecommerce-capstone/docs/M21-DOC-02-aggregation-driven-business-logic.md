# M21 — 聚合管線驅動業務邏輯

## 概述

M21 的核心創新在於**使用 MongoDB Aggregation Pipeline 作為業務邏輯的一部分**，而非僅用於報表查詢。這種模式讓 Saga 步驟和 DDD Specification 能夠基於 CQRS 讀模型的即時分析結果做出業務決策。

| 使用場景 | 元件 | 管線操作 | 業務決策 |
|---------|------|---------|---------|
| Saga 驗證 | ValidateStockStep | `$match` + `$count` | 偵測大量採購，阻止可疑訂單 |
| DDD 規格 | CategoryProfitabilitySpec | `$match` + `$group` + `$sum` + `$avg` | 評估類別獲利能力，決定商品上架 |

## 1. ValidateStockStep — 大量採購偵測

### 業務規則

> 同一客戶在 24 小時內對同一商品類別下單超過 3 次，判定為大量採購，拒絕訂單。

### 實作

```java
// ValidateStockStep.execute() 中的聚合管線
List<String> orderCategories = order.getLines().stream()
        .map(OrderLine::category)
        .distinct()
        .toList();

var twentyFourHoursAgo = Date.from(Instant.now().minus(24, ChronoUnit.HOURS));
var pipeline = List.of(
    new Document("$match", new Document("customerId", order.getCustomerId())
        .append("categories", new Document("$in", orderCategories))
        .append("lastUpdatedAt", new Document("$gte", twentyFourHoursAgo))),
    new Document("$count", "recentOrders")
);

var results = mongoTemplate.getCollection("m21_order_dashboard")
        .aggregate(pipeline).into(new ArrayList<>());

if (!results.isEmpty() && results.getFirst().getInteger("recentOrders") > 3) {
    throw new IllegalStateException("Bulk purchase limit exceeded");
}
```

### 管線解析

| 階段 | 操作 | 說明 |
|------|------|------|
| `$match` | 篩選條件 | 同一客戶 + 類別交集 + 24 小時內 |
| `$count` | 計數 | 符合條件的訂單總數 |

### 為何查詢 CQRS 讀模型？

- `m21_order_dashboard` 已有 `customerId`、`categories`、`lastUpdatedAt` 等扁平化欄位
- 比查詢 Event Store 再重播更高效
- 讀模型有 `{customerId:1, status:1}` 和 `{lastUpdatedAt:-1}` 索引支援

### Saga 補償

ValidateStockStep 是唯讀步驟，失敗時不需要補償（compensate 為空操作）。但 Saga 會停止後續步驟執行。

## 2. CategoryProfitabilitySpec — 類別獲利分析

### 業務規則

商品上架審核時，檢查該類別的歷史表現：

| 條件 | 閾值 | 不滿足時 |
|------|------|---------|
| 歷史訂單數 | ≥ 5 筆 | 拒絕（訂單歷史不足） |
| 平均訂單金額 | ≥ 500 TWD | 拒絕（平均金額過低） |
| 取消率 | < 30% | 拒絕（取消率過高） |

### 聚合管線實作

```java
// OrderQueryService.computeCategoryMetrics()
var pipeline = List.of(
    new Document("$match", new Document("categories", category)),
    new Document("$group", new Document("_id", (Object) null)
        .append("totalOrders", new Document("$sum", 1))
        .append("cancelledOrders", new Document("$sum",
            new Document("$cond", List.of(
                new Document("$eq", List.of("$status", "CANCELLED")), 1, 0))))
        .append("avgOrderValue", new Document("$avg", "$totalAmount"))
        .append("totalRevenue", new Document("$sum", "$totalAmount")))
);
```

### 管線解析

| 階段 | 操作 | 說明 |
|------|------|------|
| `$match` | `categories: category` | 篩選包含該類別的訂單 |
| `$group` | `_id: null` | 全部聚合為一筆 |
| | `$sum: 1` | 計算總訂單數 |
| | `$cond + $sum` | 條件計算取消訂單數 |
| | `$avg: $totalAmount` | 計算平均訂單金額 |
| | `$sum: $totalAmount` | 計算總營收 |

### CategoryMetrics DTO

```java
public record CategoryMetrics(
    String category,
    int totalOrders,
    int cancelledOrders,
    BigDecimal avgOrderValue,
    BigDecimal totalRevenue
) {}
```

### 規格判斷邏輯

```java
public boolean isSatisfiedBy(CategoryMetrics metrics) {
    if (metrics.totalOrders() < 5) return false;           // 訂單歷史不足
    if (metrics.avgOrderValue().compareTo(500) < 0) return false;  // 平均金額太低

    BigDecimal cancelRate = cancelledOrders / totalOrders;
    if (cancelRate >= 0.3) return false;                    // 取消率過高

    return true;
}
```

## 3. 與 M19/M20 的對比

### M19 Banking — LoanApplication Spec

```java
// IncomeToPaymentRatioSpec — 純數學計算
// 年金公式: P×r×(1+r)^n / ((1+r)^n - 1)
// 不查詢任何 MongoDB 集合
```

### M20 Insurance — ClaimHistoryRiskSpec

```java
// ClaimQueryService.countPaidClaimsByCategory() — 簡單查詢
var query = Query.query(Criteria.where("_id").is(category));
var stats = mongoTemplate.findOne(query, Document.class, "m20_claim_statistics");
return stats.getInteger("paidCount");
```

### M21 E-commerce — CategoryProfitabilitySpec

```java
// OrderQueryService.computeCategoryMetrics() — 聚合管線
var pipeline = List.of(
    new Document("$match", ...),
    new Document("$group", new Document()
        .append("totalOrders", new Document("$sum", 1))
        .append("cancelledOrders", new Document("$sum", new Document("$cond", ...)))
        .append("avgOrderValue", new Document("$avg", "$totalAmount"))
        .append("totalRevenue", new Document("$sum", "$totalAmount")))
);
```

| 面向 | M19 | M20 | M21 |
|------|-----|-----|-----|
| 資料來源 | 無（純計算） | CQRS 讀模型（findOne） | CQRS 讀模型（聚合管線） |
| 查詢複雜度 | 無 | O(1) 單筆查詢 | O(n) 聚合運算 |
| 業務指標 | 單一（償債比） | 單一（理賠次數） | 多維（訂單數 + 取消率 + 平均金額 + 營收） |

## 4. 技術重點

### Decimal128 處理

- `totalAmount` 在 `m21_order_dashboard` 標記為 `@Field(targetType = FieldType.DECIMAL128)`
- 聚合管線中 `$avg` 和 `$sum` 對 Decimal128 自動運算
- 結果透過 `new BigDecimal(r.get("field").toString())` 轉換

### 索引支援

- `{customerId:1, status:1}` 支援 ValidateStockStep 的 `$match`
- `{lastUpdatedAt:-1}` 支援 24 小時時間範圍查詢

### 空結果處理

```java
if (results.isEmpty()) {
    return new CategoryMetrics(category, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
}
```

當類別無任何訂單時，回傳零值 DTO，讓 Spec 判斷「訂單歷史不足」。

## 5. 測試驗證

### BDD 場景

```gherkin
Scenario: 高利潤類別商品上架成功
  Given 類別 "Electronics" 有 10 筆訂單平均金額 2000 元取消率 10%
  When 提交商品上架 SKU "NEW-001" 名稱 "新商品" 類別 "Electronics" 價格 1500 元庫存 50 件
  And 執行上架審核
  Then 上架申請狀態為 "APPROVED"
  And 商品集合中存在 SKU "NEW-001"
```

### 整合測試

- `bulkPurchaseBlocked`: 預插入 4 筆訂單 → Saga COMPENSATED
- `approvedWhenProfitable`: 預插入 10 筆高價訂單 → Listing APPROVED
- `rejectedWhenUnprofitable`: 預插入 3 筆低價 + 高取消率訂單 → Listing REJECTED
