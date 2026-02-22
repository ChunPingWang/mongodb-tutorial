# M11-DOC-02：Java Sealed Interface + MongoDB 多型實踐

## 概述

Java 17+ 的 **Sealed Interface** 結合 MongoDB 的 `_class` discriminator，可以實現型別安全的多型文件建模。Java 21+ 的 **Pattern Matching** 和 **Guarded Patterns** 進一步強化了多型處理能力。

本文件說明 M11 的完整實作，包含從 M04 到 M11 的演進。

---

## 從 M04 到 M11 的演進

### M04：基礎多型

```java
// M04：基礎 sealed interface，無 @TypeAlias
public sealed interface FinancialProduct permits SavingsAccount, FixedDeposit, InsurancePolicy {
    String id();
    String name();
    BigDecimal value();
}

// M04：使用 FQCN 查詢
private <T> List<T> findByType(Class<T> type) {
    Query query = Query.query(Criteria.where("_class").is(type.getName()));
    return mongoTemplate.find(query, type, COLLECTION);
}
```

### M11：進階多型

```java
// M11：使用 @TypeAlias 控制 _class 值
@Document("m11_financial_products")
@TypeAlias("deposit")
public record Deposit(
    @Id String id,
    String name,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal value,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal annualRate,
    int termMonths
) implements FinancialProduct { }

// M11：Spring Data 自動過濾 _class（不需手動 Criteria）
public <T extends FinancialProduct> List<T> findByType(Class<T> type) {
    return mongoTemplate.find(new Query(), type, COLLECTION);
}
```

**關鍵差異：**
1. `@TypeAlias("deposit")` → `_class` 存短名稱而非 FQCN
2. `findByType()` 不需手動加 `_class` Criteria — Spring Data 會根據 `@TypeAlias` 自動加入過濾條件
3. 新增 Custom Converter、Guarded Pattern Matching

---

## Sealed Interface 定義

### Banking：Record（不可變）

```java
public sealed interface FinancialProduct permits Deposit, Fund, InsuranceProduct {
    String id();
    String name();
    BigDecimal value();
}
```

Record 適用於**值導向**的資料：一旦建立就不會變更。

### Insurance：Class（可變）

```java
public sealed interface Policy permits AutoPolicy, LifePolicy, HealthPolicy {
    String getId();
    String getPolicyNumber();
    String getHolderName();
    BigDecimal getBasePremium();
}
```

Class 適用於**生命週期管理**的資料：保單可能有狀態變更。

---

## @TypeAlias 控制 _class

```java
@TypeAlias("deposit")       // 儲存 _class = "deposit"
@TypeAlias("fund")          // 儲存 _class = "fund"
@TypeAlias("insurance_product")  // 儲存 _class = "insurance_product"
```

**實際 MongoDB 文件：**
```json
{
  "_id": ObjectId("..."),
  "_class": "deposit",
  "name": "一年定存",
  "value": NumberDecimal("100000"),
  "annualRate": NumberDecimal("1.5"),
  "termMonths": 12
}
```

**不使用 @TypeAlias 的話：**
```json
{ "_class": "com.mongodb.course.m11.banking.model.Deposit" }
```

---

## Custom Converter（MongoCustomConversions）

對於複合型別（如 `RiskProfile` record），可以自訂序列化/反序列化邏輯。

### 定義 Value Object

```java
public record RiskProfile(int level, String category) { }
```

### Writing Converter（寫入時）

```java
@WritingConverter
public class RiskProfileWriteConverter implements Converter<RiskProfile, String> {
    @Override
    public String convert(RiskProfile source) {
        return source.level() + ":" + source.category();
    }
}
```

### Reading Converter（讀取時）

```java
@ReadingConverter
public class StringToRiskProfileConverter implements Converter<String, RiskProfile> {
    @Override
    public RiskProfile convert(String source) {
        String[] parts = source.split(":", 2);
        return new RiskProfile(Integer.parseInt(parts[0]), parts[1]);
    }
}
```

### 註冊 Converter

```java
@Configuration
public class MongoConfig {
    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
            new RiskProfileWriteConverter(),
            new StringToRiskProfileConverter()
        ));
    }
}
```

**注意：** 使用 `@Configuration` + `@Bean`，**不要**繼承 `AbstractMongoClientConfiguration`，否則會覆蓋 Spring Boot 自動設定。

### 儲存結果

```json
{
  "_class": "fund",
  "name": "全球股票",
  "riskProfile": "5:AGGRESSIVE"
}
```

---

## Pattern Matching + Guarded Patterns

### 基本 Pattern Matching（switch expression）

```java
public BigDecimal estimateAnnualReturn(FinancialProduct product) {
    return switch (product) {
        case Deposit d -> d.value().multiply(d.annualRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        case Fund f -> f.value().multiply(BigDecimal.valueOf(0.08));
        case InsuranceProduct ip -> ip.coverage()
                .divide(BigDecimal.valueOf(ip.premiumYears()), 2, RoundingMode.HALF_UP);
    };
}
```

**關鍵：** Sealed Interface 保證 switch 是 exhaustive（窮舉的），不需要 `default` 分支。

### Guarded Patterns（Java 21+）

```java
public BigDecimal calculatePremium(Policy policy) {
    return switch (policy) {
        case AutoPolicy auto when auto.getDriverAge() < 25 ->
            auto.getBasePremium().multiply(BigDecimal.valueOf(1.5));     // 年輕駕駛加費
        case AutoPolicy auto when "truck".equals(auto.getVehicleType()) ->
            auto.getBasePremium().multiply(BigDecimal.valueOf(1.3));     // 卡車加費
        case AutoPolicy auto ->
            auto.getBasePremium();                                       // 一般車險
        case LifePolicy life when life.getInsuredAge() > 60 ->
            life.getBasePremium().multiply(BigDecimal.valueOf(2.0));     // 高齡加費
        case LifePolicy life ->
            life.getBasePremium().multiply(
                BigDecimal.ONE.add(BigDecimal.valueOf(life.getInsuredAge())
                    .multiply(new BigDecimal("0.02"))));                 // 年齡因子
        case HealthPolicy health -> {
            var premium = health.getBasePremium();
            if (health.isHasDentalCoverage()) premium = premium.add(BigDecimal.valueOf(500));
            if (health.isHasVisionCoverage()) premium = premium.add(BigDecimal.valueOf(300));
            yield premium;                                               // 附加選項
        }
    };
}
```

**Guarded Pattern 順序重要：**
1. `case AutoPolicy when age < 25` — 最具體的條件先
2. `case AutoPolicy when truck` — 次具體
3. `case AutoPolicy` — 兜底

如果順序反過來，兜底的 `case AutoPolicy` 會先匹配，guarded patterns 永遠不會執行。

---

## 多型反序列化流程

```
MongoDB Document: { "_class": "deposit", "name": "一年定存", ... }
       │
       ▼
Spring Data 讀取 _class = "deposit"
       │
       ▼
查找 @TypeAlias("deposit") → Deposit.class
       │
       ▼
實例化 Deposit record，填入欄位值
       │
       ▼
如果有 Custom Converter（如 riskProfile），呼叫 @ReadingConverter
       │
       ▼
返回 FinancialProduct（實際型別為 Deposit）
```

`mongoTemplate.findAll(FinancialProduct.class, COLLECTION)` 會返回混合型別的 List，每個元素是正確的具體型別。

---

## 測試策略

### 純 Unit Test（不需 Spring/MongoDB）

```java
// Pattern Matching 測試 — 只需 new 物件即可
var deposit = new Deposit("d1", "定存", BigDecimal.valueOf(100000), BigDecimal.valueOf(2.0), 12);
BigDecimal result = service.estimateAnnualReturn(deposit);
assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(2000));
```

### Integration Test（驗證多型持久化）

```java
// 儲存後查詢 — 驗證 _class alias 和反序列化
service.save(new Deposit(...));
service.save(new Fund(...));
List<FinancialProduct> all = service.findAll();
assertThat(all).hasAtLeastOneElementOfType(Deposit.class);
assertThat(all).hasAtLeastOneElementOfType(Fund.class);
```

### BigDecimal 測試注意

永遠使用 `isEqualByComparingTo()`（scale-insensitive）：

```java
// ✅ 正確
assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(2000));

// ❌ 可能失敗（scale 不同：2000 vs 2000.00）
assertThat(result).isEqualTo(BigDecimal.valueOf(2000));
```

---

## 總結

| 特性 | M04 | M11 |
|------|-----|-----|
| Sealed Interface | ✅ | ✅ |
| @TypeAlias | ❌ | ✅ |
| Custom Converter | ❌ | ✅ |
| Guarded Patterns | ❌ | ✅ |
| findByType 方式 | 手動 `_class` Criteria | 自動過濾 |
| 策略比較 | ❌ | ✅ Single vs Multiple |
| Record + Class 混用 | ❌ Record only | ✅ 依場景選擇 |
