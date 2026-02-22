# M20 — 多型與事件溯源整合實踐

## Policy Sealed Hierarchy

M20 使用 Java sealed interface 實現保單多型，搭配 `@TypeAlias` 讓 Spring Data MongoDB 正確序列化/反序列化：

```java
public sealed interface Policy permits AutoPolicy, HealthPolicy, LifePolicy {
    String getId();
    String getPolicyNumber();
    String getHolderName();
    BigDecimal getBasePremium();
    BigDecimal getCoverageAmount();
    BigDecimal getTotalClaimsPaid();
}
```

三個實作類別使用 `final class`（非 record），因為 Spring Data MongoDB 需要 setter 進行多型反序列化：

| 類型 | @TypeAlias | 特有欄位 |
|------|-----------|---------|
| AutoPolicy | `"AutoPolicy"` | vehicleType, accidentCount |
| HealthPolicy | `"HealthPolicy"` | planTier, claimsThisYear |
| LifePolicy | `"LifePolicy"` | beneficiaryName, termYears |

### Schema Validation

`m20_policies` 使用 `$jsonSchema` 驗證必填欄位：

```java
MongoJsonSchema.builder()
    .required("policyNumber", "holderName", "basePremium", "coverageAmount")
    .build();
```

搭配 `strictValidation().failOnValidationError()` 確保資料完整性。

---

## ClaimProcess Event-Sourced Aggregate

### 事件類型

```java
public sealed interface ClaimEvent extends DomainEvent
    permits ClaimFiled, ClaimInvestigated, ClaimAssessed,
            ClaimApproved, ClaimRejected, ClaimPaid;
```

### 狀態轉換圖

```
FILED ──investigate──▶ UNDER_INVESTIGATION ──assess──▶ ASSESSED ──approve──▶ APPROVED ──pay──▶ PAID
  │                          │                            │
  └──reject──▶ REJECTED ◀──reject──────────────◀─reject──┘
```

### apply() Pattern Matching

```java
private void apply(ClaimEvent event) {
    switch (event) {
        case ClaimFiled e -> { ... }
        case ClaimInvestigated e -> { this.fraudRisk = e.fraudRisk(); ... }
        case ClaimAssessed e -> { this.assessedAmount = e.assessedAmount(); ... }
        case ClaimApproved e -> { this.approvedAmount = e.approvedAmount(); ... }
        case ClaimRejected e -> { this.status = ClaimStatus.REJECTED; }
        case ClaimPaid e -> { this.paidAmount = e.paidAmount(); ... }
    }
    this.version = event.version();
}
```

---

## CQRS Projector 投影

### ClaimDashboardProjector

每個事件類型對應不同的投影邏輯：

- `ClaimFiled` → insert 新文件（含初始 timeline entry）
- `ClaimInvestigated` → update status + fraudRisk + push timeline
- `ClaimAssessed` → update assessedAmount + push timeline
- `ClaimApproved/Rejected/Paid` → update status + amount + push timeline

**重要**：BigDecimal 需包裝為 `Decimal128` 才能正確使用 `$inc`。

### ClaimStatisticsProjector

使用 `mongoTemplate.upsert()` 按類別聚合統計：

```java
case ClaimFiled e -> {
    var query = Query.query(Criteria.where("_id").is(e.category()));
    var update = new Update()
        .inc("totalClaims", 1)
        .inc("filedCount", 1)
        .inc("totalClaimedAmount", new Decimal128(e.claimedAmount()));
    mongoTemplate.upsert(query, update, COLLECTION);
}
```

---

## Settlement Saga 多型整合

### UpdatePolicyStep — 多型 Switch

Saga 的 `UpdatePolicyStep` 讀取原始 BSON Document 的 `_class` 欄位，使用 switch 進行型別特定更新：

```java
String typeAlias = rawDoc.getString("_class");
var update = new Update().inc("totalClaimsPaid", new Decimal128(amount));

switch (typeAlias) {
    case "AutoPolicy"   -> update.inc("accidentCount", 1);
    case "HealthPolicy"  -> update.inc("claimsThisYear", 1);
    case "LifePolicy"    -> { /* 壽險無型別特定計數器 */ }
}
```

### FraudCheckStep — CQRS 讀取模型驅動

Saga 的第一步查詢 CQRS 讀取模型進行詐欺檢測：

1. 查詢 `m20_claim_dashboard` 確認個案 fraudRisk ≠ HIGH
2. 查詢 `m20_claim_statistics` 確認類別層級核准率 ≥ 30%（當 filedCount > 5 時）

---

## DDD Underwriting — Specification 查詢 CQRS

UnderwritingApplication 是純領域物件（無 Spring 註解），使用兩個 Specification：

- **AgeEligibilitySpec**：依保單類型檢查年齡範圍（AUTO 18-75, HEALTH 0-80, LIFE 18-65）
- **ClaimHistoryRiskSpec**：查詢 CQRS 讀取模型的已付款理賠數量（> 3 則拒絕）

核保通過後自動建立對應型別的 Policy 文件。

---

## Change Stream on Read Model

與 M19 不同（監聽事件儲存庫），M20 的 Change Stream 監聽 CQRS 讀取模型 `m20_claim_dashboard`：

```java
var csOptions = ChangeStreamOptions.builder().build();
var requestOptions = new ChangeStreamRequestOptions(null, DASHBOARD, csOptions);
var request = new ChangeStreamRequest<>(listener, requestOptions);
container.register(request, Document.class);
```

當理賠狀態變為 APPROVED 或 PAID 時，寫入 `m20_claim_notifications`。

---

## 技術注意事項

- **BigDecimal + Decimal128**：所有金額欄位使用 `@Field(targetType = FieldType.DECIMAL128)` + `$inc` 時包裝 `new Decimal128(bigDecimal)`
- **Record 型別的 null 處理**：ClaimStatisticsDocument 使用 `Integer`（非 `int`）避免 upsert 產生的 null 值導致實例化失敗
- **Snapshot BigDecimal 序列化**：使用 `toPlainString()` / `new BigDecimal(string)` 確保精度
- **findByType 需手動過濾**：`mongoTemplate.find()` 不會自動過濾 `_class`，需明確加入 `Criteria.where("_class").is(aliasValue)`
