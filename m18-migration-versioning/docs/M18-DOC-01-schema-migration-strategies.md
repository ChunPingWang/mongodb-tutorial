# M18-DOC-01: MongoDB Schema Migration 策略

## 概述

MongoDB 的彈性 Schema 並不代表不需要遷移——它改變了遷移的方式。本文件介紹兩種互補的遷移策略：**Eager Migration（即時批次遷移）** 與 **Lazy Migration（惰性讀取遷移）**，以及如何在零停機的前提下安全演進文件結構。

---

## 策略比較表

| 面向 | Eager Migration（批次） | Lazy Migration（讀取時） |
|------|------------------------|------------------------|
| **工具** | Mongock ChangeUnit | Spring Data `@ReadingConverter` |
| **執行時機** | 應用啟動時或排程執行 | 每次讀取文件時 |
| **資料庫影響** | 一次性更新所有文件 | 文件保持原始版本直到被讀寫 |
| **停機需求** | 可能需要維護窗口（大量資料時） | 零停機 |
| **一致性** | 遷移後所有文件版本一致 | 多版本長期共存 |
| **回滾** | Mongock `@RollbackExecution` | 不需要（原始文件未修改） |
| **適用場景** | 結構性變更、索引依賴變更 | 新增欄位、預設值填充 |
| **效能影響** | 遷移期間寫入負載高 | 每次讀取有微小轉換開銷 |

---

## Eager Migration：Mongock

### Mongock 簡介

Mongock 是 MongoDB 專用的資料庫遷移工具，類似於 Flyway/Liquibase 在關聯式資料庫中的角色。核心概念：

- **ChangeUnit**：一個遷移單元，包含 `@Execution`（正向遷移）與 `@RollbackExecution`（回滾）
- **mongockChangeLog**：自動建立的稽核集合，記錄所有已執行的遷移
- **mongockLock**：分散式鎖定集合，確保同時只有一個實例執行遷移

### ChangeUnit 結構

```java
@ChangeUnit(id = "v002-add-risk-score", order = "002", author = "m18")
public class V002_AddRiskScoreChangeUnit {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        // 正向遷移邏輯
        mongoTemplate.getCollection("policies").updateMany(
            Filters.eq("type", "AUTO"),
            Updates.combine(
                Updates.set("riskScore", 50),
                Updates.set("schemaVersion", 2)
            )
        );
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        // 回滾邏輯
        mongoTemplate.getCollection("policies").updateMany(
            Filters.exists("riskScore"),
            Updates.combine(
                Updates.unset("riskScore"),
                Updates.set("schemaVersion", 1)
            )
        );
    }
}
```

### 關鍵設定

```properties
# application.properties
mongock.migration-scan-package=com.mongodb.course.m18.insurance.changeunit
mongock.transaction-enabled=false  # 大量文件遷移避免 16MB 交易限制
```

### Mongock 在 Spring Boot 中的啟用

```java
@SpringBootApplication
@EnableMongock
public class M18Application { }
```

---

## Lazy Migration：@ReadingConverter

### 原理

讀取時期遷移透過 Spring Data 的 `@ReadingConverter` 實作。當應用程式從 MongoDB 讀取文件時，Converter 檢查 `schemaVersion` 並在記憶體中將文件升級到最新版本。**原始 MongoDB 文件不會被修改**，只有在明確儲存時才透過 `@WritingConverter` 寫回最新版本。

### Converter Chain 範例

```java
@ReadingConverter
public class CustomerReadConverter implements Converter<Document, Customer> {
    public Customer convert(Document source) {
        int version = source.getInteger("schemaVersion", 1);
        if (version < 2) migrateV1toV2(source);  // 扁平地址 → 嵌入式
        if (version < 3) migrateV2toV3(source);  // 加入 loyaltyTier
        return mapToCustomer(source);
    }
}
```

### 搭配 @WritingConverter 確保寫入最新版本

```java
@WritingConverter
public class CustomerWriteConverter implements Converter<Customer, Document> {
    public Document convert(Customer source) {
        // 永遠寫入 schemaVersion=3
    }
}
```

### 註冊 Custom Conversions

```java
@Configuration
public class MongoConverterConfig {
    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
            new CustomerReadConverter(),
            new CustomerWriteConverter()
        ));
    }
}
```

> **注意**：使用 `@Configuration` + `@Bean`，不要繼承 `AbstractMongoClientConfiguration`，以保留 Spring Boot 自動配置。

---

## 零停機遷移三階段策略

### 第一階段：部署支援多版本讀取的新版應用

1. 部署包含 `@ReadingConverter` 的新版應用
2. 所有版本的文件都能正確讀取
3. 新寫入的文件自動使用最新版本

### 第二階段：背景批次遷移

```java
@Service
public class CustomerMigrationService {
    public int migrateAllToLatest() {
        // 找出所有 schemaVersion < 3 的文件
        // 讀取時自動透過 ReadingConverter 升級
        // 儲存時透過 WritingConverter 寫回 V3
        return migratedCount;
    }
}
```

### 第三階段：清理

1. 驗證所有文件已升級到最新版本
2. 可選：移除 Converter 中的舊版本處理邏輯
3. 可選：更新 Schema Validation 規則

---

## 策略選擇決策樹

```
需要遷移 Schema？
├── 是否為結構性變更（重新命名欄位、拆分/合併文件）？
│   ├── 是 → Eager Migration（Mongock）
│   └── 否 → 是否需要立即一致性？
│       ├── 是 → Eager Migration（Mongock）
│       └── 否 → Lazy Migration（@ReadingConverter）
│           └── 搭配背景批次遷移最終達到一致性
```

---

## M08 vs M18 對照

| 面向 | M08 | M18 |
|------|-----|-----|
| **重點** | Schema Validation（$jsonSchema + Jakarta） | Schema Migration 工具與策略 |
| **遷移方式** | `DocumentMigrator` 介面 + `MigrationService` | Mongock ChangeUnit + `@ReadingConverter` chain |
| **轉換層** | 應用層明確呼叫 | Spring Data Converter 自動透明轉換 |
| **回滾** | 未涵蓋 | Mongock `@RollbackExecution` |
| **稽核** | 未涵蓋 | Mongock `mongockChangeLog` 自動稽核 |
| **多版本共存** | `moderate` validation level | `@ReadingConverter` 版本鏈 |
