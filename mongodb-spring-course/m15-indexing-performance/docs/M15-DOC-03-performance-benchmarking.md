# M15-DOC-03：效能基準測試方法論

## 測試基礎設施

### Testcontainers 配置

M15 使用與前幾個模組相同的 `SharedContainersConfig` 模式，確保所有測試類別共享同一個 MongoDB 容器：

```java
@TestConfiguration(proxyBeanMethods = false)
public class SharedContainersConfig {
    private static final MongoDBContainer mongodb = new MongoDBContainer("mongo:8.0");
    static {
        mongodb.start();
    }
    @Bean @ServiceConnection
    MongoDBContainer mongoDBContainer() { return mongodb; }
}
```

**關鍵設計**：
- 靜態單例容器，JVM 生命週期內只啟動一次
- `@ServiceConnection` 自動注入連線字串
- MongoDB 8.0 預設以 Single-Node Replica Set 啟動

### TTL 測試加速

生產環境中，TTL Monitor 每 60 秒執行一次。測試環境需要加速：

```java
mongoClient.getDatabase("admin").runCommand(
    new Document("setParameter", 1).append("ttlMonitorSleepSecs", 1));
```

搭配較短的過期時間（2 秒）和等待時間（5 秒），確保 TTL 清除在測試時間範圍內完成。

---

## 確定性資料產生策略

### 設計原則

M15 使用**確定性迴圈**產生測試資料，不依賴隨機函式庫（如 Java Faker）：

```java
for (int i = 0; i < totalCount; i++) {
    String accountId = String.format("ACC-%06d", (i % accountCount) + 1);
    var type = TYPES[i % TYPES.length];           // 循環 4 種類型
    long amount = ((i % 50) + 1) * 100L;          // 100~5000
    var date = baseDate.plus(i % 365, ChronoUnit.DAYS);  // 分散 365 天
    String category = CATEGORIES[i % CATEGORIES.length];  // 循環 5 種分類
}
```

### 資料分佈特性

| 參數 | Banking (2000 筆/10 帳戶) | E-commerce (500 筆/5 分類) |
|------|--------------------------|--------------------------|
| 每帳戶/分類筆數 | 200 | 100 |
| 類型分佈 | 均勻 4 種 | — |
| 金額範圍 | 100-5000 (步進 100) | 50-5000 (步進 50) |
| 時間分佈 | 365 天均勻分佈 | — |
| 庫存比例 | — | 70% 有庫存 |
| 標籤 | — | 每產品 2 個，7 種選項循環 |

### 確定性的好處

1. **可重複**：相同參數產生相同資料
2. **可預測**：測試斷言可以精確計算預期值（如 `countByAccount = 200`）
3. **無額外依賴**：不需要引入 Faker 或 Random 函式庫

---

## 效能指標定義

### explain() 自動化驗證

M15 的核心測試模式是透過 `ExplainAnalyzer` 驗證查詢計畫：

```java
// 1. 執行查詢（獲取實際結果）
var results = queryService.findByAccountTypeAndDateRange(...);
assertThat(results).isNotEmpty();

// 2. 分析查詢計畫（驗證索引使用）
var explain = explainAnalyzer.explain(COLLECTION, filter);
assertThat(explain.stage()).isEqualTo("IXSCAN");
assertThat(explain.docsExamined()).isEqualTo(explain.nReturned());
```

### ExplainResult 指標

```java
public record ExplainResult(
    String stage,         // "IXSCAN" 或 "COLLSCAN"
    String indexName,     // 使用的索引名稱（COLLSCAN 時為 null）
    long keysExamined,    // 掃描的索引鍵數
    long docsExamined,    // 檢查的原始文件數
    long nReturned,       // 回傳的文件數
    boolean isIndexOnly   // 是否為覆蓋查詢（IXSCAN + docsExamined=0）
)
```

### 效能等級判斷

| 等級 | 條件 | 說明 |
|------|------|------|
| 最佳 | `isIndexOnly = true` | 覆蓋查詢，完全不讀原始文件 |
| 優良 | `docsExamined == nReturned` | ESR 完美匹配 |
| 良好 | `keysExamined ≈ nReturned` | 索引掃描接近回傳數 |
| 待優化 | `stage == "COLLSCAN"` | 全集合掃描 |

---

## BDD 驗證策略

### 交易索引場景（5 個）

```gherkin
Feature: 銀行交易查詢索引優化

  Scenario: 帳戶交易查詢使用索引掃描
    # 驗證：有索引時使用 IXSCAN

  Scenario: ESR 完整條件查詢效率最高
    # 驗證：docsExamined == nReturned

  Scenario: 覆蓋查詢避免回表
    # 驗證：isIndexOnly = true, docsExamined = 0

  Scenario: TTL 索引自動清除過期資料
    # 驗證：等待後資料被刪除

  Scenario: 無索引欄位查詢使用全集合掃描
    # 驗證：COLLSCAN（對照組）
```

### 產品搜尋場景（5 個）

```gherkin
Feature: 電商產品搜尋索引優化

  Scenario: 全文檢索使用文字索引
  Scenario: 分類加價格範圍使用複合索引
  Scenario: 部分索引僅掃描有庫存產品
  Scenario: 多標籤篩選使用多鍵索引
  Scenario: 有庫存產品依價格排序
```

---

## 測試架構

### 測試分層

```
Unit Tests (3)
├── ExplainResultTest — 純 Document 解析，無 Spring
│   ├── classic IXSCAN 格式
│   ├── SBE queryPlan 格式
│   └── COLLSCAN 格式

Integration Tests (15)
├── ExplainAnalyzerTest (2) — 索引 vs 無索引
├── IndexManagementServiceTest (2) — 建立/刪除索引
├── TransactionDataGeneratorTest (1) — 資料產生
├── TransactionQueryServiceTest (5) — ESR/覆蓋查詢/排序
└── ProductSearchServiceTest (5) — 全文/複合/多鍵/部分

BDD Scenarios (10)
├── transaction-index.feature (5)
└── product-search.feature (5)

Total: 28 tests
```

### 測試隔離策略

```java
@BeforeEach
void setUp() {
    mongoTemplate.remove(new Query(), COLLECTION);      // 清除資料
    mongoTemplate.indexOps(COLLECTION).dropAllIndexes(); // 清除索引
    dataGenerator.generateTransactions(2000, 10);        // 重新產生
}
```

每個測試方法獨立建立所需索引，確保測試之間不互相影響。

---

## 與其他模組的關聯

| 模組 | 關聯 |
|------|------|
| M05 | 基礎 CRUD — M15 在此基礎上加入索引優化 |
| M06 | Criteria API 查詢 — M15 驗證這些查詢的索引使用 |
| M07 | Aggregation Pipeline — 聚合也受索引影響（`$match` 階段） |
| M08 | Schema Validation — 索引與驗證是兩個獨立的效能面向 |
| M16+ | Change Streams — 需要 oplog，與索引無直接關係 |

---

## 生產環境建議

1. **使用 `db.collection.explain("executionStats")` 定期分析慢查詢**
2. **監控 `db.collection.stats()` 的 `totalIndexSize`**
3. **設定 MongoDB Profiler 收集超過閾值的慢查詢**
4. **索引建立時使用 `background: true`**（4.2+ 預設）
5. **避免在高峰時段建立大型索引**
6. **定期使用 `$indexStats` 清理未使用的索引**
