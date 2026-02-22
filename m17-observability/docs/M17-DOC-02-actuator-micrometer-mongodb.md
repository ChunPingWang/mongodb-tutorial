# M17 DOC-02：Spring Boot Actuator + Micrometer + MongoDB

## 概述

Spring Boot 對 MongoDB 的可觀測性支援分為三個層次：

1. **自動配置指標**（Micrometer）— 零配置即可取得命令計時與連線池監控
2. **健康檢查**（Actuator HealthIndicator）— 應用程式就緒狀態的標準化端點
3. **診斷服務**（自訂 Service）— 深入 MongoDB 內部狀態的進階查詢

本文件涵蓋這三個層次的原理、實作方式與測試策略。

## MongoMetricsAutoConfiguration 原理

### 自動配置機制

當 classpath 同時存在 `spring-boot-starter-data-mongodb` 與 `spring-boot-starter-actuator` 時，Spring Boot 會啟用 `MongoMetricsAutoConfiguration`，自動註冊兩個關鍵監聽器：

| 監聽器 | 職責 | 對應指標 |
|--------|------|----------|
| `MongoMetricsCommandListener` | 攔截每個 MongoDB 命令的執行時間 | `mongodb.driver.commands`（Timer） |
| `MongoMetricsConnectionPoolListener` | 監控連線池的大小與狀態 | `mongodb.driver.pool.size`（Gauge） |

這兩個監聽器透過 `MongoClientSettingsBuilderCustomizer` 注入到 MongoDB Java Driver 的 `MongoClientSettings` 中，與應用程式自訂的 Customizer **共存**而非互斥。

### 指標詳細說明

#### `mongodb.driver.commands`（Timer）

記錄每個 MongoDB 命令的執行時間，包含以下 Tag：

| Tag | 說明 | 範例值 |
|-----|------|--------|
| `command` | 命令名稱 | `find`, `insert`, `update`, `aggregate` |
| `status` | 執行結果 | `SUCCESS`, `FAILED` |
| `cluster.id` | 叢集識別碼 | 自動產生 |
| `server.address` | 伺服器位址 | `localhost:27017` |

#### `mongodb.driver.pool.size`（Gauge）

即時反映連線池的連線數量。Tag 包含 `server.address`，可區分多個 MongoDB 節點。

### 本模組的依賴配置

```kotlin
// build.gradle.kts
plugins {
    id("course.spring-module")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}
```

`course.spring-module` 已包含 `spring-boot-starter-data-mongodb`，加上 `actuator` 即啟用完整指標收集。

## MongoClientSettingsBuilderCustomizer 模式

### 核心概念

Spring Boot 使用 `MongoClientSettingsBuilderCustomizer` 來擴展 `MongoClientSettings`。所有註冊的 Customizer Bean 會**依序執行**，每個都能在同一個 Builder 上疊加設定。這表示自訂的 `CommandListener` 可以與自動配置的 `MongoMetricsCommandListener` 並存。

### 本模組的實作：SlowQueryDetector

```java
@Configuration
public class MongoObservabilityConfig {

    @Bean
    SlowQueryDetector slowQueryDetector() {
        return new SlowQueryDetector(100);  // 閾值 100ms
    }

    @Bean
    MongoClientSettingsBuilderCustomizer slowQueryDetectorCustomizer(SlowQueryDetector detector) {
        return builder -> builder.addCommandListener(detector);
    }
}
```

關鍵點：

- 使用 `addCommandListener()` 而非 `commandListenerList()`。前者是**追加**，後者是**覆蓋**
- `SlowQueryDetector` 同時是 Spring Bean 和 `CommandListener`，可在測試中直接注入查詢結果
- 自動配置的 `MongoMetricsCommandListener` 不受影響，兩個 Listener 各自獨立運作

### SlowQueryDetector 設計

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

    @Override
    public void commandFailed(CommandFailedEvent event) {
        captureIfSlow(event.getCommandName(),
                TimeUnit.NANOSECONDS.toMillis(event.getElapsedTime(TimeUnit.NANOSECONDS)),
                event.getDatabaseName());
    }

    private void captureIfSlow(String commandName, long durationMs, String databaseName) {
        if (!TRACKED_COMMANDS.contains(commandName)) return;
        if (durationMs >= thresholdMs) {
            capturedQueries.add(new SlowQueryEntry(commandName, durationMs, databaseName, Instant.now()));
        }
    }
}
```

設計要點：

- **`CopyOnWriteArrayList`**：執行緒安全，適用於寫少讀多的慢查詢收集
- **`volatile thresholdMs`**：支援執行時期動態調整閾值
- **`TRACKED_COMMANDS` 白名單**：過濾掉 `hello`、`ismaster` 等內部命令，只追蹤業務相關操作
- **`SlowQueryEntry` 記錄**：使用 Java record 封裝命令名稱、耗時、資料庫名稱與擷取時間

```java
public record SlowQueryEntry(
        String commandName,
        long durationMs,
        String databaseName,
        Instant capturedAt
) {}
```

## 自訂 HealthIndicator 實作

### 為什麼需要自訂？

Spring Boot 預設的 `MongoHealthIndicator` 僅執行 `ping` 命令，回傳 `UP` 或 `DOWN`。在生產環境中，我們通常需要更豐富的診斷資訊：版本號、資料庫名稱、集合數量等。

### MongoDetailedHealthIndicator

```java
@Component
public class MongoDetailedHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;

    public MongoDetailedHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Health health() {
        try {
            // 1. 基本連線測試
            mongoTemplate.getDb().runCommand(new Document("ping", 1));

            // 2. 取得版本資訊
            var buildInfo = mongoTemplate.getDb().runCommand(new Document("buildInfo", 1));
            var version = buildInfo.getString("version");

            // 3. 取得資料庫名稱
            var databaseName = mongoTemplate.getDb().getName();

            // 4. 計算集合數量
            var collectionNames = mongoTemplate.getDb().listCollectionNames();
            int collectionsCount = 0;
            for (var ignored : collectionNames) {
                collectionsCount++;
            }

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

### 回應範例

當 `/actuator/health` 端點被存取時，此 HealthIndicator 會回傳類似以下的結構：

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

### 設計考量

- **例外捕獲**：任何 MongoDB 通訊失敗都會回傳 `Health.down(ex)`，Actuator 會自動將應用程式標記為不健康
- **`listCollectionNames()` 的迭代**：回傳的是 `MongoIterable`，不提供 `size()` 方法，需手動迭代計數
- **`buildInfo` 命令**：不需要 admin 權限，任何已認證使用者都可以執行

## MongoDiagnosticService 設計

### 職責劃分

`MongoDiagnosticService` 負責將 MongoDB 的管理命令封裝為型別安全的 DTO，提供三個層次的診斷查詢：

| 方法 | 對應命令 | 回傳型別 | 用途 |
|------|----------|----------|------|
| `getServerStatus()` | `serverStatus` | `ServerStatusReport` | 伺服器整體狀態 |
| `getDatabaseStats()` | `dbStats` | `DatabaseStatsReport` | 資料庫層級統計 |
| `getCollectionStats()` | `$collStats` 聚合 | `CollectionStatsReport` | 集合層級統計 |

### 型別化 DTO（Java Record）

```java
public record ServerStatusReport(
        String version,
        long uptimeSeconds,
        int currentConnections,
        int availableConnections,
        String replicaSetName
) {}

public record DatabaseStatsReport(
        String databaseName,
        int collections,
        long documents,
        long dataSizeBytes,
        long storageSizeBytes
) {}

public record CollectionStatsReport(
        String collectionName,
        long documentCount,
        long totalSizeBytes,
        long avgDocSizeBytes
) {}
```

### serverStatus 與 dbStats

```java
public ServerStatusReport getServerStatus() {
    var result = mongoTemplate.getDb().runCommand(new Document("serverStatus", 1));

    var version = result.getString("version");
    var uptime = result.get("uptimeEstimate", Number.class).longValue();

    var connections = result.get("connections", Document.class);
    var current = connections.getInteger("current");
    var available = connections.getInteger("available");

    var repl = result.get("repl", Document.class);
    var replicaSetName = repl != null ? repl.getString("setName") : null;

    return new ServerStatusReport(version, uptime, current, available, replicaSetName);
}

public DatabaseStatsReport getDatabaseStats() {
    var result = mongoTemplate.getDb().runCommand(new Document("dbStats", 1));

    return new DatabaseStatsReport(
            result.getString("db"),
            result.getInteger("collections"),
            result.get("objects", Number.class).longValue(),
            result.get("dataSize", Number.class).longValue(),
            result.get("storageSize", Number.class).longValue()
    );
}
```

> **注意**：`serverStatus` 回傳的 `connections.current` 包含當前所有連線（含應用程式自身），`connections.available` 則是剩餘可用連線數。兩者加總約等於 `maxIncomingConnections`。

### $collStats 聚合階段

#### 為什麼不用 `collStats` 命令？

MongoDB 6.2 起，`collStats` 命令已被標記為 deprecated。官方建議改用 `$collStats` 聚合階段，原因包括：

- `$collStats` 可與其他聚合階段組合使用
- 更一致的 API 設計（所有統計都透過聚合管線）
- 未來版本可能移除 `collStats` 命令

#### 實作方式

```java
public CollectionStatsReport getCollectionStats(String collectionName) {
    var pipeline = List.of(
            new Document("$collStats", new Document("storageStats", new Document()))
    );

    var results = mongoTemplate.getDb()
            .getCollection(collectionName)
            .aggregate(pipeline)
            .first();

    if (results == null) {
        return new CollectionStatsReport(collectionName, 0, 0, 0);
    }

    var storageStats = results.get("storageStats", Document.class);
    return new CollectionStatsReport(
            collectionName,
            storageStats.get("count", Number.class).longValue(),
            storageStats.get("size", Number.class).longValue(),
            storageStats.get("avgObjSize", Number.class).longValue()
    );
}
```

`$collStats` 必須是聚合管線的**第一個階段**，且直接在集合上執行（而非透過 `runCommand`）。傳入 `new Document("storageStats", new Document())` 即啟用儲存統計。回傳結果包含 `storageStats` 子文件，其中有 `count`、`size`、`avgObjSize` 等欄位。

## 測試策略

### SimpleMeterRegistry：無需 Prometheus

在測試環境中，Spring Boot 會自動注入 `SimpleMeterRegistry`（記憶體內實作），無需引入 `micrometer-registry-prometheus` 或任何外部監控系統。直接 `@Autowired MeterRegistry` 即可查詢指標。

```java
@SpringBootTest
@Import(SharedContainersConfig.class)
class AutoConfiguredMetricsTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void commandMetrics_timerExists() {
        // 觸發一次 MongoDB 操作
        transactionService.create("ACC-METRIC-001", 1000, "DEPOSIT");
        transactionService.findAll();

        // 驗證 Timer 指標已記錄
        var timers = meterRegistry.find("mongodb.driver.commands").timers();
        assertThat(timers).isNotEmpty();
    }

    @Test
    void commandMetrics_hasCommandTag() {
        transactionService.findAll();

        // 驗證特定 command tag 存在
        var timer = meterRegistry.find("mongodb.driver.commands")
                .tag("command", "find")
                .timer();
        assertThat(timer).isNotNull();
    }

    @Test
    void connectionPoolMetrics_gaugeExists() {
        // 連線池指標在 MongoClient 建立後即可用
        var gauge = meterRegistry.find("mongodb.driver.pool.size").gauge();
        assertThat(gauge).isNotNull();
    }
}
```

### HealthIndicator 直接注入測試

`MongoDetailedHealthIndicator` 是一個普通的 Spring Bean，可直接注入測試類別並呼叫 `health()` 方法：

```java
@SpringBootTest
@Import(SharedContainersConfig.class)
class MongoDetailedHealthIndicatorTest {

    @Autowired
    private MongoDetailedHealthIndicator healthIndicator;

    @Autowired
    private ProductService productService;

    @Test
    void health_returnsUpWithDetails() {
        var health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("version");
        assertThat(health.getDetails()).containsKey("databaseName");
    }

    @Test
    void health_detailsContainCollectionsCount() {
        productService.create("Health Check Product", "test", 500);

        var health = healthIndicator.health();

        assertThat(health.getDetails()).containsKey("collections");
        assertThat((int) health.getDetails().get("collections")).isGreaterThan(0);
    }
}
```

這種測試方式的優點：
- 不需要啟動 HTTP 伺服器（不用 `@SpringBootTest(webEnvironment = RANDOM_PORT)`）
- 不需要透過 REST 端點存取（不用 `TestRestTemplate`）
- 直接驗證 `Health` 物件的結構和內容

### MongoDiagnosticService 整合測試

```java
@SpringBootTest
@Import(SharedContainersConfig.class)
class MongoDiagnosticServiceTest {

    @Autowired
    private MongoDiagnosticService diagnosticService;

    @Autowired
    private ProductService productService;

    @Test
    void serverStatus_returnsVersion() {
        var report = diagnosticService.getServerStatus();
        assertThat(report.version()).startsWith("8.0");
    }

    @Test
    void collectionStats_matchesInsertedCount() {
        for (int i = 1; i <= 5; i++) {
            productService.create("Product " + i, "electronics", i * 100L);
        }

        var report = diagnosticService.getCollectionStats("m17_products");
        assertThat(report.documentCount()).isEqualTo(5);
    }
}
```

## 架構總覽

```
MongoClientSettings
  ├── MongoMetricsCommandListener      ← 自動配置（Actuator）
  ├── MongoMetricsConnectionPoolListener ← 自動配置（Actuator）
  └── SlowQueryDetector                 ← 自訂（MongoClientSettingsBuilderCustomizer）

Actuator Endpoints
  ├── /actuator/health
  │     ├── MongoHealthIndicator          ← 自動配置（ping only）
  │     └── MongoDetailedHealthIndicator  ← 自訂（version + collections）
  └── /actuator/metrics
        ├── mongodb.driver.commands       ← Timer（自動配置）
        └── mongodb.driver.pool.size      ← Gauge（自動配置）

MongoDiagnosticService
  ├── getServerStatus()    → serverStatus 命令 → ServerStatusReport
  ├── getDatabaseStats()   → dbStats 命令     → DatabaseStatsReport
  └── getCollectionStats() → $collStats 聚合  → CollectionStatsReport
```

## 重點整理

1. **零配置指標**：引入 `spring-boot-starter-actuator` 後，`mongodb.driver.commands` 和 `mongodb.driver.pool.size` 即自動啟用
2. **Customizer 追加模式**：`addCommandListener()` 不會覆蓋自動配置的監聽器，多個 Customizer Bean 依序執行
3. **$collStats 取代 collStats**：MongoDB 6.2+ 建議使用聚合階段而非管理命令來取得集合統計
4. **型別化 DTO**：使用 Java Record 將原始 `Document` 封裝為強型別物件，提升可讀性與安全性
5. **測試不需 Prometheus**：`SimpleMeterRegistry` 在測試中自動可用，直接查詢記憶體中的指標即可
6. **HealthIndicator 直接測試**：作為普通 Bean 注入，呼叫 `health()` 方法即可驗證，無需 HTTP 端點
