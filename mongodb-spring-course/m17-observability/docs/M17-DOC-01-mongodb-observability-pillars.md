# M17 DOC-01：MongoDB 可觀測性三支柱

## 可觀測性（Observability）概述

在分散式系統中，**可觀測性**指的是從系統的外部輸出推斷其內部狀態的能力。可觀測性建立在三個支柱之上：

| 支柱 | 英文 | 用途 | MongoDB 對應 |
|------|------|------|-------------|
| **指標** | Metrics | 量化系統行為的數值時序資料 | `mongodb.driver.commands` Timer、Connection Pool Gauge |
| **日誌** | Logging | 離散事件的文字記錄 | CommandListener 事件、Slow Query 記錄 |
| **追蹤** | Tracing | 請求在系統中流經的完整路徑 | Micrometer Tracing + MongoDB Driver 整合 |

三者互補：Metrics 告訴你「系統有問題」，Logging 告訴你「發生了什麼」，Tracing 告訴你「問題在哪一段」。

## MongoDB Server-Side 診斷命令

MongoDB 內建多個診斷命令，可以透過 `MongoTemplate` 直接呼叫。

### serverStatus

回傳 MongoDB 伺服器的全面狀態報告，包含連線數、記憶體使用、操作計數器等：

```java
var result = mongoTemplate.getDb().runCommand(new Document("serverStatus", 1));

var version = result.getString("version");
var uptime = result.get("uptimeEstimate", Number.class).longValue();

var connections = result.get("connections", Document.class);
var current = connections.getInteger("current");
var available = connections.getInteger("available");
```

實務上建議將結果封裝為 Java Record，提升可讀性：

```java
public record ServerStatusReport(
        String version,
        long uptimeSeconds,
        int currentConnections,
        int availableConnections,
        String replicaSetName
) {}
```

### dbStats

回傳目前資料庫的統計資訊，包含集合數量、文件總數、資料大小：

```java
var result = mongoTemplate.getDb().runCommand(new Document("dbStats", 1));

new DatabaseStatsReport(
        result.getString("db"),
        result.getInteger("collections"),
        result.get("objects", Number.class).longValue(),
        result.get("dataSize", Number.class).longValue(),
        result.get("storageSize", Number.class).longValue()
);
```

### $collStats（取代已棄用的 collStats 命令）

從 MongoDB 6.2 開始，`collStats` 命令已被棄用，官方建議改用 **`$collStats` 聚合階段**。差異在於 `$collStats` 是 Aggregation Pipeline 的一部分，而非獨立的管理命令：

```java
// 已棄用（MongoDB 6.2+）：
// mongoTemplate.getDb().runCommand(new Document("collStats", collectionName));

// 推薦做法：使用 $collStats 聚合階段
var pipeline = List.of(
        new Document("$collStats", new Document("storageStats", new Document()))
);

var results = mongoTemplate.getDb()
        .getCollection(collectionName)
        .aggregate(pipeline)
        .first();

var storageStats = results.get("storageStats", Document.class);
new CollectionStatsReport(
        collectionName,
        storageStats.get("count", Number.class).longValue(),
        storageStats.get("size", Number.class).longValue(),
        storageStats.get("avgObjSize", Number.class).longValue()
);
```

> **注意**：`$collStats` 必須是 Pipeline 的第一個階段，且不需要指定集合過濾條件，因為它直接作用於 `getCollection(name)` 所指定的集合。

## CommandListener 機制

MongoDB Java Driver 提供 `CommandListener` 介面，讓應用程式攔截每一個發送到 MongoDB 的命令事件。這是實作應用層可觀測性的核心機制。

### 介面定義

```java
public interface CommandListener {
    void commandStarted(CommandStartedEvent event);     // 命令開始
    void commandSucceeded(CommandSucceededEvent event);  // 命令成功
    void commandFailed(CommandFailedEvent event);        // 命令失敗
}
```

每個事件包含以下資訊：

| 方法 | 說明 |
|------|------|
| `getCommandName()` | 命令名稱（find、insert、update...） |
| `getDatabaseName()` | 目標資料庫名稱 |
| `getElapsedTime(TimeUnit)` | 命令執行耗時 |
| `getRequestId()` | 請求識別碼 |

### 註冊 CommandListener

透過 Spring Boot 的 `MongoClientSettingsBuilderCustomizer` 註冊：

```java
@Configuration
public class MongoObservabilityConfig {

    @Bean
    SlowQueryDetector slowQueryDetector() {
        return new SlowQueryDetector(100); // 閾值 100ms
    }

    @Bean
    MongoClientSettingsBuilderCustomizer slowQueryDetectorCustomizer(SlowQueryDetector detector) {
        return builder -> builder.addCommandListener(detector);
    }
}
```

> **關鍵設計**：`SlowQueryDetector` 本身不是 Spring Bean 時就需要手動建立。這裡將它宣告為 `@Bean`，讓 Spring 管理生命週期，同時透過 `MongoClientSettingsBuilderCustomizer` 將其註冊到 MongoDB Driver。

## 命令過濾（Command Filtering）

MongoDB Driver 會觸發**所有**命令的事件，包含許多內部維護命令。如果不加過濾，監控資料會被大量噪音淹沒。

### 內部命令 vs 使用者命令

| 類別 | 命令範例 | 說明 |
|------|---------|------|
| **使用者命令** | `find`、`insert`、`update`、`delete`、`aggregate` | 業務邏輯觸發的資料操作 |
| **使用者命令** | `count`、`distinct`、`findAndModify`、`getMore`、`createIndexes` | 進階查詢與管理操作 |
| **內部命令** | `hello`、`isMaster` | Driver 心跳檢測、拓撲探測 |
| **內部命令** | `endSessions`、`abortTransaction` | 連線池管理、Session 清理 |
| **內部命令** | `saslStart`、`saslContinue` | 認證協議交握 |

### 過濾策略

在 `SlowQueryDetector` 中使用白名單（allowlist）過濾：

```java
private static final Set<String> TRACKED_COMMANDS = Set.of(
        "find", "insert", "update", "delete", "aggregate",
        "count", "distinct", "findAndModify", "getMore", "createIndexes"
);

private void captureIfSlow(String commandName, long durationMs, String databaseName) {
    if (!TRACKED_COMMANDS.contains(commandName)) {
        return; // 跳過內部命令
    }
    if (durationMs >= thresholdMs) {
        capturedQueries.add(new SlowQueryEntry(commandName, durationMs, databaseName, Instant.now()));
    }
}
```

> **為何用白名單而非黑名單？** MongoDB 版本更新可能新增內部命令，黑名單需要持續維護。白名單只追蹤我們關心的命令，更加穩定。

## SlowQueryDetector 設計

`SlowQueryDetector` 實作 `CommandListener` 介面，負責偵測並記錄超過閾值的慢查詢。

### 執行緒安全設計

```java
public class SlowQueryDetector implements CommandListener {

    private volatile long thresholdMs;
    private final CopyOnWriteArrayList<SlowQueryEntry> capturedQueries = new CopyOnWriteArrayList<>();
    // ...
}
```

兩個關鍵的執行緒安全考量：

1. **`volatile long thresholdMs`**：閾值可能在運行時動態調整（例如從 100ms 改為 50ms），`volatile` 保證所有執行緒立即可見最新值
2. **`CopyOnWriteArrayList`**：CommandListener 的回呼方法在 MongoDB Driver 的 I/O 執行緒中執行，與應用程式主執行緒並行。`CopyOnWriteArrayList` 在寫入時複製底層陣列，讀取時無需加鎖

### 為什麼使用記憶體內儲存？

`SlowQueryDetector` 將慢查詢記錄在記憶體中（`CopyOnWriteArrayList`），而非寫入 MongoDB：

- **避免遞迴**：如果慢查詢偵測器將結果寫入 MongoDB，這個寫入操作本身也會觸發 `CommandListener`，可能造成無限遞迴
- **低延遲**：記憶體寫入不會影響 Driver I/O 執行緒的效能
- **簡單可靠**：不依賴外部儲存的可用性

生產環境中，可以定期將記憶體中的慢查詢記錄批次匯出到日誌系統或監控平台。

### SlowQueryEntry Record

```java
public record SlowQueryEntry(
        String commandName,
        long durationMs,
        String databaseName,
        Instant capturedAt
) {}
```

使用 Java Record 作為不可變的資料載體，自動提供 `equals()`、`hashCode()`、`toString()`。

## 慢查詢偵測策略比較

| 特性 | MongoDB Profiler | CommandListener | APM 工具 |
|------|-----------------|-----------------|----------|
| **層級** | Server-side | Application-side | 端到端 |
| **設定方式** | `db.setProfilingLevel(1, {slowms: 100})` | Java 程式碼實作 | Agent 或 SDK |
| **資料存放** | `system.profile` 集合 | 應用程式記憶體 | 外部平台 |
| **對效能影響** | 中（寫入 capped collection） | 低（記憶體操作） | 視實作而定 |
| **可自訂性** | 有限（閾值+過濾） | 完全可自訂 | 視工具而定 |
| **跨語言支援** | 是（Server 層級） | 僅 Java Driver | 視工具而定 |
| **適用場景** | 資料庫管理員調優 | 應用開發者監控 | 維運團隊全局可觀測性 |

**MongoDB Profiler** 適合一次性的效能調查，但長期啟用會影響效能。**CommandListener** 輕量且完全在應用端控制，適合嵌入微服務。**APM 工具**（如 Datadog、New Relic）提供端到端的視覺化面板，適合生產環境的全局監控。

## Micrometer 指標類型與 MongoDB 對應

Spring Boot 透過 Micrometer 自動為 MongoDB Driver 註冊指標。理解指標類型有助於正確解讀監控資料。

### Timer

記錄事件的次數與持續時間，適合追蹤命令執行效能：

```java
// Spring Boot 自動註冊的 MongoDB 命令 Timer
var timer = meterRegistry.find("mongodb.driver.commands")
        .tag("command", "find")
        .timer();

// Timer 提供的資訊
timer.count();       // 執行次數
timer.totalTime(TimeUnit.MILLISECONDS);  // 總耗時
timer.mean(TimeUnit.MILLISECONDS);       // 平均耗時
timer.max(TimeUnit.MILLISECONDS);        // 最大耗時
```

`mongodb.driver.commands` 是最重要的 MongoDB 指標，以 `command` 標籤區分不同命令類型（find、insert、update 等）。

### Gauge

表示一個可升可降的瞬時值，適合追蹤連線池狀態：

```java
// 連線池大小（Gauge）
var poolSize = meterRegistry.find("mongodb.driver.pool.size").gauge();

// 常見的 MongoDB 連線池 Gauge 指標
// mongodb.driver.pool.size          - 連線池中的連線總數
// mongodb.driver.pool.checkedout    - 目前被借出的連線數
// mongodb.driver.pool.waitqueuesize - 等待借用連線的執行緒數
```

> **Gauge vs Counter**：Gauge 可增可減（連線數會隨借用/歸還變化），Counter 只能單調遞增。

### Counter

單調遞增的計數器，適合追蹤累計事件數：

```java
// 範例：自訂的慢查詢計數器
Counter slowQueryCounter = Counter.builder("mongodb.slow.queries")
        .tag("database", databaseName)
        .register(meterRegistry);
slowQueryCounter.increment();
```

### 指標對應總覽

| Micrometer 類型 | MongoDB 指標名稱 | 說明 |
|----------------|-----------------|------|
| Timer | `mongodb.driver.commands` | 命令執行耗時與次數 |
| Gauge | `mongodb.driver.pool.size` | 連線池總連線數 |
| Gauge | `mongodb.driver.pool.checkedout` | 已借出連線數 |
| Gauge | `mongodb.driver.pool.waitqueuesize` | 等待佇列長度 |

## 驗證指標是否正確註冊

在整合測試中，可以透過注入 `MeterRegistry` 驗證指標存在：

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
        transactionService.create("ACC-METRIC-001", 1000, "DEPOSIT");
        transactionService.findAll();

        var timers = meterRegistry.find("mongodb.driver.commands").timers();
        assertThat(timers).isNotEmpty();
    }

    @Test
    void connectionPoolMetrics_gaugeExists() {
        var gauge = meterRegistry.find("mongodb.driver.pool.size").gauge();
        assertThat(gauge).isNotNull();
    }
}
```

> **注意**：必須先執行至少一次 MongoDB 操作，Timer 才會被建立。這是因為 Micrometer 的 Timer 採用「懶初始化」策略，只有在第一次記錄時才會註冊到 Registry。

## Spring Boot Actuator Health 整合

除了指標與慢查詢偵測，自訂 `HealthIndicator` 可以將 MongoDB 的健康狀態整合到 Spring Boot Actuator 的 `/actuator/health` 端點：

```java
@Component
public class MongoDetailedHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;

    @Override
    public Health health() {
        try {
            mongoTemplate.getDb().runCommand(new Document("ping", 1));

            var buildInfo = mongoTemplate.getDb().runCommand(new Document("buildInfo", 1));
            var version = buildInfo.getString("version");
            var databaseName = mongoTemplate.getDb().getName();

            return Health.up()
                    .withDetail("version", version)
                    .withDetail("databaseName", databaseName)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
```

## 小結

| 主題 | 關鍵要點 |
|------|---------|
| 三支柱 | Metrics（量化）、Logging（事件）、Tracing（路徑），三者互補 |
| Server-Side 診斷 | `serverStatus`、`dbStats`、`$collStats`（取代已棄用的 `collStats`） |
| CommandListener | Driver 層級的命令攔截，支援 `commandSucceeded` / `commandFailed` |
| 命令過濾 | 白名單追蹤使用者命令，忽略 `hello`/`isMaster`/`endSessions` 等內部命令 |
| SlowQueryDetector | `CopyOnWriteArrayList` + `volatile` 確保執行緒安全，記憶體儲存避免遞迴 |
| Micrometer 對應 | Timer 對應命令耗時、Gauge 對應連線池狀態、Counter 對應累計計數 |
| 策略比較 | Profiler 適合調優、CommandListener 適合應用監控、APM 適合全局可觀測性 |
