# M17 — MongoDB 可觀測性（Observability）

> **Phase 4: Operations & Performance** 第三個模組
> M15 索引效能 → M16 Change Streams → **M17 可觀測性** → M18 Schema Migration

## 學習目標

生產環境的 MongoDB 應用程式需要可觀測性。本模組教你如何：

1. 利用 Spring Boot Actuator + Micrometer **自動收集** MongoDB 命令指標與連線池狀態
2. 實作自訂 `CommandListener` 進行**慢查詢偵測**
3. 透過 `serverStatus`、`dbStats`、`$collStats` 取得 **Server-Side 診斷資訊**
4. 建立自訂 `HealthIndicator` 提供**進階健康檢查**

---

## 核心概念

### 可觀測性三支柱

| 支柱 | 說明 | MongoDB 對應 |
|------|------|-------------|
| **Metrics（指標）** | 量化系統行為的數值時序資料 | `mongodb.driver.commands` Timer、`mongodb.driver.pool.size` Gauge |
| **Logging（日誌）** | 離散事件的文字記錄 | `CommandListener` 事件、慢查詢記錄 |
| **Tracing（追蹤）** | 請求在系統中流經的完整路徑 | Micrometer Tracing + MongoDB Driver 整合 |

三者互補：Metrics 告訴你「系統有問題」，Logging 告訴你「發生了什麼」，Tracing 告訴你「問題在哪一段」。

### 架構總覽

```
MongoClientSettings
  ├── MongoMetricsCommandListener        ← Spring Boot 自動配置
  ├── MongoMetricsConnectionPoolListener  ← Spring Boot 自動配置
  └── SlowQueryDetector                   ← 自訂（MongoClientSettingsBuilderCustomizer）

Actuator
  ├── /actuator/health
  │     ├── MongoHealthIndicator            ← 自動配置（ping only）
  │     └── MongoDetailedHealthIndicator    ← 自訂（version + databaseName + collections）
  └── /actuator/metrics
        ├── mongodb.driver.commands         ← Timer（自動配置）
        └── mongodb.driver.pool.size        ← Gauge（自動配置）

MongoDiagnosticService
  ├── getServerStatus()    → serverStatus 命令  → ServerStatusReport
  ├── getDatabaseStats()   → dbStats 命令       → DatabaseStatsReport
  └── getCollectionStats() → $collStats 聚合    → CollectionStatsReport
```

---

## 業務領域

本模組使用兩個領域來示範不同的可觀測性面向：

| 領域 | 集合 | 示範重點 |
|------|------|---------|
| **Banking（銀行）** | `m17_transactions` | 慢查詢偵測、自動指標驗證 |
| **E-commerce（電商）** | `m17_products` | 診斷命令、健康檢查 |

---

## 技術重點

### 1. 自動指標（零配置）

引入 `spring-boot-starter-actuator` 後，Spring Boot 自動註冊：

- **`mongodb.driver.commands`**（Timer）— 每個 MongoDB 命令的執行時間與次數
- **`mongodb.driver.pool.size`**（Gauge）— 連線池即時連線數

```kotlin
// build.gradle.kts — 唯一需要新增的依賴
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}
```

```java
// 在測試中驗證指標存在
var timer = meterRegistry.find("mongodb.driver.commands")
        .tag("command", "find")
        .timer();
assertThat(timer).isNotNull();
```

### 2. 慢查詢偵測（SlowQueryDetector）

自訂 `CommandListener` 攔截所有 MongoDB 命令，超過閾值時記錄到記憶體：

```java
public class SlowQueryDetector implements CommandListener {

    private static final Set<String> TRACKED_COMMANDS = Set.of(
            "find", "insert", "update", "delete", "aggregate",
            "count", "distinct", "findAndModify", "getMore", "createIndexes"
    );

    private volatile long thresholdMs;
    private final CopyOnWriteArrayList<SlowQueryEntry> capturedQueries = new CopyOnWriteArrayList<>();

    @Override
    public void commandSucceeded(CommandSucceededEvent event) {
        captureIfSlow(event.getCommandName(),
                TimeUnit.NANOSECONDS.toMillis(event.getElapsedTime(TimeUnit.NANOSECONDS)),
                event.getDatabaseName());
    }
}
```

**設計要點：**

| 決策 | 原因 |
|------|------|
| `CopyOnWriteArrayList` | CommandListener 在 Driver I/O 執行緒回呼，與主執行緒並行，需要執行緒安全 |
| `volatile thresholdMs` | 支援測試中動態調整閾值（0=全捕獲、999999=不捕獲） |
| 記憶體儲存（非 MongoDB） | 避免寫入 MongoDB 觸發新的 CommandListener 事件造成無限遞迴 |
| 白名單過濾 | 只追蹤使用者命令，忽略 `hello`/`isMaster`/`endSessions` 等內部命令 |

### 3. 註冊方式（MongoClientSettingsBuilderCustomizer）

```java
@Configuration
public class MongoObservabilityConfig {

    @Bean
    SlowQueryDetector slowQueryDetector() {
        return new SlowQueryDetector(100);  // 預設閾值 100ms
    }

    @Bean
    MongoClientSettingsBuilderCustomizer slowQueryDetectorCustomizer(SlowQueryDetector detector) {
        return builder -> builder.addCommandListener(detector);
    }
}
```

`addCommandListener()` 是**追加**而非覆蓋，自動配置的 `MongoMetricsCommandListener` 不受影響。

### 4. Server-Side 診斷（MongoDiagnosticService）

封裝三個 MongoDB 管理命令為型別安全的 Java Record DTO：

| 方法 | 對應命令 | 回傳型別 |
|------|----------|----------|
| `getServerStatus()` | `{ serverStatus: 1 }` | `ServerStatusReport` |
| `getDatabaseStats()` | `{ dbStats: 1 }` | `DatabaseStatsReport` |
| `getCollectionStats(name)` | `$collStats` 聚合階段 | `CollectionStatsReport` |

> **注意**：`collStats` 命令自 MongoDB 6.2 起已棄用，本模組使用 `$collStats` 聚合階段取代。

```java
// $collStats 必須是 Pipeline 的第一個階段
var pipeline = List.of(
        new Document("$collStats", new Document("storageStats", new Document()))
);
var result = mongoTemplate.getDb()
        .getCollection(collectionName)
        .aggregate(pipeline)
        .first();
```

### 5. 自訂健康檢查（MongoDetailedHealthIndicator）

Spring Boot 預設的 `MongoHealthIndicator` 僅執行 `ping`。自訂版本額外提供版本號、資料庫名稱、集合數量：

```java
@Component
public class MongoDetailedHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            mongoTemplate.getDb().runCommand(new Document("ping", 1));
            var buildInfo = mongoTemplate.getDb().runCommand(new Document("buildInfo", 1));
            // ...
            return Health.up()
                    .withDetail("version", version)
                    .withDetail("databaseName", databaseName)
                    .withDetail("collections", collectionsCount)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
```

回應範例：
```json
{
  "status": "UP",
  "details": {
    "version": "8.0.4",
    "databaseName": "test",
    "collections": 3
  }
}
```

---

## 資料模型

### Transaction（`m17_transactions`）

```java
@Document("m17_transactions")
public record Transaction(
        @Id String id,
        String accountId,
        long amount,
        String type,        // "DEPOSIT", "WITHDRAWAL"
        Instant createdAt
) {
    public static Transaction of(String accountId, long amount, String type) {
        return new Transaction(null, accountId, amount, type, Instant.now());
    }
}
```

### Product（`m17_products`）

```java
@Document("m17_products")
public record Product(
        @Id String id,
        String name,
        String category,
        long price,
        Instant createdAt
) {
    public static Product of(String name, String category, long price) {
        return new Product(null, name, category, price, Instant.now());
    }
}
```

### DTO Records（不寫入 MongoDB）

| Record | 用途 |
|--------|------|
| `SlowQueryEntry(commandName, durationMs, databaseName, capturedAt)` | 慢查詢記錄 |
| `ServerStatusReport(version, uptimeSeconds, currentConnections, availableConnections, replicaSetName)` | 伺服器狀態 |
| `DatabaseStatsReport(databaseName, collections, documents, dataSizeBytes, storageSizeBytes)` | 資料庫統計 |
| `CollectionStatsReport(collectionName, documentCount, totalSizeBytes, avgDocSizeBytes)` | 集合統計 |

---

## 專案結構

```
m17-observability/
├── build.gradle.kts                              # +spring-boot-starter-actuator
├── docs/
│   ├── M17-DOC-01-mongodb-observability-pillars.md
│   └── M17-DOC-02-actuator-micrometer-mongodb.md
└── src/
    ├── main/java/com/mongodb/course/m17/
    │   ├── M17Application.java
    │   ├── config/
    │   │   └── MongoObservabilityConfig.java      # 註冊 SlowQueryDetector
    │   ├── observability/
    │   │   ├── SlowQueryDetector.java             # CommandListener 慢查詢偵測
    │   │   ├── SlowQueryEntry.java                # 慢查詢記錄 DTO
    │   │   ├── MongoDetailedHealthIndicator.java   # 自訂健康檢查
    │   │   ├── MongoDiagnosticService.java         # 診斷命令封裝
    │   │   ├── ServerStatusReport.java             # serverStatus DTO
    │   │   ├── DatabaseStatsReport.java            # dbStats DTO
    │   │   └── CollectionStatsReport.java          # $collStats DTO
    │   ├── banking/
    │   │   ├── Transaction.java                   # @Document("m17_transactions")
    │   │   └── TransactionService.java
    │   └── ecommerce/
    │       ├── Product.java                       # @Document("m17_products")
    │       └── ProductService.java
    └── test/
        ├── java/com/mongodb/course/m17/
        │   ├── SharedContainersConfig.java
        │   ├── banking/
        │   │   ├── SlowQueryDetectorTest.java      # 5 tests
        │   │   └── AutoConfiguredMetricsTest.java   # 3 tests
        │   ├── ecommerce/
        │   │   ├── MongoDiagnosticServiceTest.java  # 4 tests
        │   │   └── MongoDetailedHealthIndicatorTest.java  # 2 tests
        │   └── bdd/
        │       ├── CucumberSpringConfig.java
        │       ├── RunCucumberTest.java
        │       ├── CucumberHooks.java
        │       ├── SlowQueryDetectionSteps.java
        │       └── DiagnosticsHealthCheckSteps.java
        └── resources/features/
            ├── slow-query-detection.feature         # 5 scenarios
            └── diagnostics-health-check.feature     # 5 scenarios
```

---

## 測試總覽

### 整合測試（14 tests）

#### SlowQueryDetectorTest（5 tests）

| 測試 | 驗證重點 |
|------|---------|
| `thresholdZero_capturesAllQueries` | 閾值=0ms → 所有命令都被捕獲 |
| `thresholdVeryHigh_capturesNothing` | 閾值=999999ms → 不捕獲任何命令 |
| `capturedEntry_containsCommandName` | 捕獲的記錄包含正確的指令名稱（`find`） |
| `capturedEntry_containsDatabaseName` | 捕獲的記錄包含非空的資料庫名稱 |
| `clear_resetsCapturedQueries` | `clear()` 後記錄歸零 |

#### AutoConfiguredMetricsTest（3 tests）

| 測試 | 驗證重點 |
|------|---------|
| `commandMetrics_timerExists` | `mongodb.driver.commands` Timer 已自動註冊 |
| `commandMetrics_hasCommandTag` | Timer 包含 `command=find` 標籤 |
| `connectionPoolMetrics_gaugeExists` | `mongodb.driver.pool.size` Gauge 已自動註冊 |

#### MongoDiagnosticServiceTest（4 tests）

| 測試 | 驗證重點 |
|------|---------|
| `serverStatus_returnsVersion` | 版本以 `8.0` 開頭 |
| `serverStatus_hasConnections` | 目前連線數 > 0 |
| `databaseStats_collectionsCountPositive` | 插入資料後集合數 > 0、文件數 > 0 |
| `collectionStats_matchesInsertedCount` | 插入 5 筆 → `documentCount == 5` |

#### MongoDetailedHealthIndicatorTest（2 tests）

| 測試 | 驗證重點 |
|------|---------|
| `health_returnsUpWithDetails` | Status=UP，包含 `version`、`databaseName` |
| `health_detailsContainCollectionsCount` | 插入資料後 `collections > 0` |

### BDD 場景（10 scenarios）

#### 銀行交易慢查詢偵測與自動指標（5 scenarios）

```gherkin
Feature: 銀行交易慢查詢偵測與自動指標

  Scenario: 低門檻值捕獲所有查詢
    Given 慢查詢偵測器門檻值設定為 0 毫秒
    And 慢查詢偵測器已清除歷史紀錄
    When 新增一筆帳戶 "ACC-001" 金額 5000 元的交易
    Then 慢查詢偵測器應捕獲至少 1 筆紀錄

  Scenario: 高門檻值不捕獲任何查詢
  Scenario: 捕獲的慢查詢包含指令名稱
  Scenario: 捕獲的慢查詢包含資料庫名稱
  Scenario: 自動指標包含 MongoDB 指令計時器
```

#### 電商系統診斷與健康檢查（5 scenarios）

```gherkin
Feature: 電商系統診斷與健康檢查

  Scenario: 伺服器狀態包含版本資訊
    When 查詢 MongoDB 伺服器狀態
    Then 伺服器版本應以 "8.0" 開頭
    And 目前連線數應大於 0

  Scenario: 資料庫統計包含集合與文件數量
  Scenario: 集合統計匹配已插入文件數
  Scenario: 健康檢查回報 UP 狀態
  Scenario: 健康檢查包含伺服器版本
```

---

## 慢查詢偵測策略比較

| 特性 | MongoDB Profiler | CommandListener | APM 工具 |
|------|-----------------|-----------------|----------|
| **層級** | Server-side | Application-side | 端到端 |
| **設定方式** | `db.setProfilingLevel(1, {slowms: 100})` | Java 程式碼實作 | Agent 或 SDK |
| **資料存放** | `system.profile` 集合 | 應用程式記憶體 | 外部平台 |
| **對效能影響** | 中（寫入 capped collection） | 低（記憶體操作） | 視實作而定 |
| **可自訂性** | 有限（閾值+過濾） | 完全可自訂 | 視工具而定 |
| **適用場景** | DBA 一次性調優 | 開發者嵌入微服務 | 維運團隊全局監控 |

---

## Micrometer 指標對應

| Micrometer 類型 | MongoDB 指標名稱 | 說明 |
|----------------|-----------------|------|
| **Timer** | `mongodb.driver.commands` | 命令執行耗時與次數（tag: `command`, `status`） |
| **Gauge** | `mongodb.driver.pool.size` | 連線池總連線數 |
| **Gauge** | `mongodb.driver.pool.checkedout` | 已借出連線數 |
| **Gauge** | `mongodb.driver.pool.waitqueuesize` | 等待佇列長度 |

> Timer 採用「懶初始化」— 必須先執行至少一次 MongoDB 操作，Timer 才會被建立。

---

## 執行測試

```bash
# 執行所有 M17 測試（14 integration + 10 BDD = 24 tests）
./gradlew :m17-observability:test

# 只跑 BDD 場景
./gradlew :m17-observability:test --tests "*.RunCucumberTest"

# 只跑整合測試
./gradlew :m17-observability:test --tests "*.SlowQueryDetectorTest"
./gradlew :m17-observability:test --tests "*.AutoConfiguredMetricsTest"
./gradlew :m17-observability:test --tests "*.MongoDiagnosticServiceTest"
./gradlew :m17-observability:test --tests "*.MongoDetailedHealthIndicatorTest"
```

---

## 教學文件

| 文件 | 標題 | 重點 |
|------|------|------|
| [DOC-01](docs/M17-DOC-01-mongodb-observability-pillars.md) | MongoDB 可觀測性三支柱 | Metrics/Logging/Tracing 定義、serverStatus/dbStats/$collStats、CommandListener 機制、CopyOnWriteArrayList 執行緒安全、慢查詢偵測策略比較、Micrometer 指標類型 |
| [DOC-02](docs/M17-DOC-02-actuator-micrometer-mongodb.md) | Spring Boot Actuator + Micrometer + MongoDB | MongoMetricsAutoConfiguration 原理、自動指標名稱、MongoClientSettingsBuilderCustomizer 擴展模式、自訂 HealthIndicator、MongoDiagnosticService 設計、測試策略 |

---

## 關鍵設計決策

| 決策 | 原因 |
|------|------|
| 只加 `spring-boot-starter-actuator` | Micrometer core 隨 Actuator 自動引入，不需要額外依賴 |
| 不使用 `spring-boot-starter-web` | 健康檢查直接以 Bean 注入測試，無需 MockMvc 或 HTTP 端點 |
| `SlowQueryDetector` 使用記憶體儲存 | 避免在 `CommandListener` 內寫入 MongoDB 造成無限遞迴 |
| `volatile` + `CopyOnWriteArrayList` | Driver I/O 執行緒與主執行緒並行，需要執行緒安全 |
| 可變閾值 `setThresholdMs()` | 同一個 Spring Context 中測試 threshold=0（全捕獲）與 threshold=999999（不捕獲） |
| 使用 `$collStats` 聚合階段 | `collStats` 命令自 MongoDB 6.2 起已棄用 |
| `Number.class.intValue()` 讀取 `dbStats.collections` | MongoDB 8.0 回傳 `Long` 而非 `Integer`，直接用 `getInteger()` 會 ClassCastException |
| 白名單過濾命令 | 比黑名單穩定，MongoDB 版本更新新增內部命令時不需要維護 |
| `MongoClientSettingsBuilderCustomizer` 註冊 | `addCommandListener()` 追加模式，保留 Spring Boot 自動配置的 Metrics Listener |
| `SimpleMeterRegistry` 測試 | Spring Boot Test 自動提供記憶體內 MeterRegistry，不需要 Prometheus 容器 |

---

## 與其他模組的關係

```
M15（索引效能）──── explain() 分析查詢計畫
       │
       ├── M17 的 SlowQueryDetector 可與 M15 的索引優化互補：
       │   先偵測慢查詢 → 再用 explain() 分析 → 建立適當索引
       │
M16（Change Streams）── 即時事件監聽
       │
       ├── M17 的 CommandListener 與 M16 的 ChangeStream 都是事件驅動模式
       │   但 CommandListener 攔截 Driver 層級，ChangeStream 攔截 Server 層級
       │
M17（可觀測性）──── 指標、慢查詢、診斷、健康檢查
       │
       └── 為 M18（Schema Migration）提供監控基礎：
           遷移過程中可監控命令耗時變化與連線池狀態
```
