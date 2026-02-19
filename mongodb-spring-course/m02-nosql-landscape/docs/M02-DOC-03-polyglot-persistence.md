# M02-DOC-03: 金融場景 Polyglot Persistence 架構

> **模組**: M02 - NoSQL 資料庫全景
> **對象**: 具備 RDB 經驗的 Java Spring 開發者
> **目標**: 理解 Polyglot Persistence 的設計原則，掌握 Cache-Aside 模式，並學會在金融場景中正確搭配多種資料庫

---

## 目錄

1. [Polyglot Persistence 的定義與原則](#1-polyglot-persistence-的定義與原則)
2. [金融業務場景拆解](#2-金融業務場景拆解)
3. [每個場景的資料庫選型](#3-每個場景的資料庫選型)
4. [Cache-Aside Pattern 深入解析](#4-cache-aside-pattern-深入解析)
5. [M02 CacheAsideService 實作解析](#5-m02-cacheasideservice-實作解析)
6. [金融 Polyglot 完整架構圖](#6-金融-polyglot-完整架構圖)
7. [實作考量](#7-實作考量)
8. [Anti-Patterns：過度使用 Polyglot 的風險](#8-anti-patterns過度使用-polyglot-的風險)

---

## 1. Polyglot Persistence 的定義與原則

### 1.1 什麼是 Polyglot Persistence？

**Polyglot Persistence（多語言持久化）** 是一種架構策略：**根據不同資料的特性和存取模式，選擇最適合的資料庫**，而非用單一資料庫處理所有場景。

這個概念由 Martin Fowler 在 2011 年提出，靈感來自「Polyglot Programming（多語言程式設計）」— 就像我們不會只用一種程式語言寫所有東西，我們也不應該只用一種資料庫儲存所有資料。

### 1.2 核心原則

```mermaid
graph TB
    subgraph "Polyglot Persistence 三大原則"
        P1["<b>原則一：適材適用</b><br/>每種資料庫都有甜蜜點<br/>讓對的工具做對的事"]
        P2["<b>原則二：業務驅動</b><br/>從業務需求出發選型<br/>不是從技術偏好出發"]
        P3["<b>原則三：務實節制</b><br/>複雜度是有成本的<br/>不要為了多樣而多樣"]
    end

    P1 --> E1["PostgreSQL 做交易<br/>MongoDB 做文件<br/>Redis 做快取"]
    P2 --> E2["先問：這個場景的<br/>一致性、延遲、擴展需求<br/>再選資料庫"]
    P3 --> E3["2-3 種資料庫就夠了<br/>不要搞成動物園"]

    style P1 fill:#c8e6c9
    style P2 fill:#bbdefb
    style P3 fill:#fff9c4
```

### 1.3 從單體到 Polyglot 的演進

```mermaid
graph LR
    subgraph "Phase 1: 單一資料庫"
        A1["所有資料<br/>→ PostgreSQL"]
    end

    subgraph "Phase 2: 加入快取"
        A2["業務資料 → PostgreSQL<br/>快取 → Redis"]
    end

    subgraph "Phase 3: Polyglot"
        A3["交易 → PostgreSQL<br/>文件 → MongoDB<br/>快取 → Redis<br/>日誌 → Cassandra"]
    end

    A1 -->|"效能瓶頸"| A2
    A2 -->|"業務多樣化"| A3

    style A1 fill:#bbdefb
    style A2 fill:#fff9c4
    style A3 fill:#c8e6c9
```

> **重要提醒**：大部分 Spring Boot 應用從 Phase 1 開始就好。只在有明確需求時才演進到 Phase 2 或 Phase 3。過早引入 Polyglot 是常見的反模式。

---

## 2. 金融業務場景拆解

### 2.1 典型金融機構的業務場景

```mermaid
graph TB
    subgraph "金融機構業務場景"
        direction TB
        CORE["核心帳務<br/>帳戶管理、轉帳、對帳"]
        MARKET["即時行情<br/>匯率、股價、利率"]
        CRM["客戶分析<br/>CRM、360 視圖、行銷"]
        TX["交易紀錄<br/>流水帳、稽核、歷史查詢"]
        RISK["風控引擎<br/>即時風險評估、規則引擎"]
        DOC["文件管理<br/>合約、保單、法規文件"]
    end

    CORE --> C1["特性: 強一致性、ACID<br/>頻率: 中等<br/>模式: 讀寫均衡"]
    MARKET --> C2["特性: 極低延遲、高頻更新<br/>頻率: 極高 (每秒數千次)<br/>模式: 寫多讀多"]
    CRM --> C3["特性: 彈性結構、巢狀資料<br/>頻率: 讀多寫少<br/>模式: 以客戶為中心查詢"]
    TX --> C4["特性: 海量追加寫入<br/>頻率: 極高<br/>模式: 寫多讀少、時序查詢"]
    RISK --> C5["特性: 亞毫秒回應<br/>頻率: 極高<br/>模式: 讀多寫少"]
    DOC --> C6["特性: 結構多變、全文搜尋<br/>頻率: 低<br/>模式: 讀多寫少"]

    style CORE fill:#bbdefb
    style MARKET fill:#ffcdd2
    style CRM fill:#c8e6c9
    style TX fill:#ffe0b2
    style RISK fill:#e1bee7
    style DOC fill:#fff9c4
```

### 2.2 各場景的資料特性分析

| 場景 | 資料結構 | 一致性需求 | 延遲要求 | 資料量 | 讀寫比 |
|------|---------|-----------|---------|--------|--------|
| **核心帳務** | 高度結構化 | 強一致性 | < 50ms | 中 (GB) | 50:50 |
| **即時行情** | 簡單 Key-Value | 最終一致性 | < 1ms | 小 (MB) | 20:80 |
| **客戶分析** | 半結構化文件 | 最終一致性 | < 100ms | 大 (TB) | 90:10 |
| **交易紀錄** | 時序、追加 | 最終一致性 | < 50ms | 極大 (PB) | 10:90 |
| **風控引擎** | 規則 + 狀態 | 混合 | < 5ms | 小 (MB) | 80:20 |
| **文件管理** | 非結構化 | 最終一致性 | < 500ms | 中 (GB) | 95:5 |

---

## 3. 每個場景的資料庫選型

### 3.1 核心帳務 -- PostgreSQL

**選型理由**: 強一致性 ACID 交易，複雜多表 JOIN，外鍵約束保護資料完整性。

```java
// PostgreSQL + Spring Data JPA
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}

@Service
@Transactional
public class CoreBankingService {

    public TransferResult transfer(String from, String to, BigDecimal amount) {
        Account debit = accountRepo.findByAccountNumberForUpdate(from);
        Account credit = accountRepo.findByAccountNumberForUpdate(to);

        if (debit.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }

        debit.setBalance(debit.getBalance().subtract(amount));
        credit.setBalance(credit.getBalance().add(amount));

        accountRepo.save(debit);
        accountRepo.save(credit);
        return TransferResult.success();
    }
}
```

### 3.2 客戶 CRM -- MongoDB

**選型理由**: 半結構化文件、嵌入式巢狀結構、Schema 靈活可快速迭代。

```java
// MongoDB + Spring Data MongoDB
@Document(collection = "customer_profiles")
public class CustomerProfile {
    @Id
    private String id;
    private String customerId;
    private String name;

    // 嵌入式地址 — 不需要另一個 collection
    private List<Address> addresses;

    // 彈性欄位 — 不同客群有不同標籤
    private Map<String, Object> tags;

    // 嵌入式互動紀錄 — 最近 50 筆
    private List<Interaction> recentInteractions;

    // 計算欄位 (反正規化)
    private BigDecimal estimatedLifetimeValue;
}

// 一次查詢取得客戶 360 度視圖 — 無需 JOIN
public CustomerProfile getCustomer360(String customerId) {
    return mongoTemplate.findOne(
        Query.query(Criteria.where("customerId").is(customerId)),
        CustomerProfile.class
    );
}
```

### 3.3 即時行情 / 快取 -- Redis

**選型理由**: 亞毫秒延遲、TTL 自動過期、Pub/Sub 即時推送。

```java
// Redis + StringRedisTemplate
@Service
public class MarketDataService {

    private final StringRedisTemplate redisTemplate;

    // 更新匯率 — 每秒數千次
    public void updateExchangeRate(String pair, BigDecimal rate) {
        String key = "rate:" + pair;
        redisTemplate.opsForValue().set(key, rate.toPlainString());
        redisTemplate.expire(key, Duration.ofSeconds(30)); // 30 秒後過期

        // Pub/Sub 通知所有訂閱者
        redisTemplate.convertAndSend("market-updates", pair + ":" + rate);
    }

    // 讀取匯率 — 亞毫秒回應
    public BigDecimal getExchangeRate(String pair) {
        String value = redisTemplate.opsForValue().get("rate:" + pair);
        return value != null ? new BigDecimal(value) : null;
    }

    // Session 管理
    public void createSession(String sessionId, String userData) {
        redisTemplate.opsForValue().set(
            "session:" + sessionId, userData,
            Duration.ofMinutes(30)  // 30 分鐘過期
        );
    }
}
```

### 3.4 交易紀錄 -- Cassandra

**選型理由**: 海量追加寫入、時序查詢、線性擴展、多資料中心複製。

```cql
-- Cassandra Schema: 以帳戶 ID 為 Partition Key，時間戳為 Clustering Key
CREATE TABLE transaction_log (
    account_id TEXT,
    timestamp TIMESTAMP,
    transaction_id UUID,
    type TEXT,
    amount DECIMAL,
    balance_after DECIMAL,
    description TEXT,
    PRIMARY KEY (account_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);

-- 查詢某帳戶最近 30 天交易
SELECT * FROM transaction_log
WHERE account_id = 'ACCT-001'
  AND timestamp > '2024-01-01'
LIMIT 100;
```

```java
@Service
public class TransactionLogService {

    private final CqlSession cqlSession;

    // 寫入交易紀錄 — 追加式寫入
    public void logTransaction(String accountId, String type,
                                BigDecimal amount, BigDecimal balanceAfter) {
        cqlSession.execute(SimpleStatement.newInstance(
            "INSERT INTO transaction_log " +
            "(account_id, timestamp, transaction_id, type, amount, balance_after) " +
            "VALUES (?, toTimestamp(now()), uuid(), ?, ?, ?)",
            accountId, type, amount, balanceAfter));
    }

    // 查詢交易紀錄 — Partition Key 查詢
    public List<Row> getRecentTransactions(String accountId, int limit) {
        return cqlSession.execute(SimpleStatement.newInstance(
            "SELECT * FROM transaction_log WHERE account_id = ? LIMIT ?",
            accountId, limit)).all();
    }
}
```

---

## 4. Cache-Aside Pattern 深入解析

### 4.1 什麼是 Cache-Aside？

**Cache-Aside（旁路快取）** 是最常見的快取策略，也叫 **Lazy Loading**。核心邏輯：

1. **讀取時**：先查快取，命中就直接回傳；未命中就查資料庫，回傳結果並寫入快取
2. **寫入時**：先寫資料庫，再失效快取（而非更新快取）

### 4.2 Cache Miss 流程

```mermaid
sequenceDiagram
    participant App as Spring Boot
    participant Redis as Redis (Cache)
    participant Mongo as MongoDB (Source)

    Note over App,Mongo: Cache Miss 流程 (首次讀取)

    App->>Redis: GET "product:laptop-001"
    Redis-->>App: null (Cache Miss)

    App->>Mongo: findById("laptop-001")
    Mongo-->>App: ProductDocument {name: "Laptop Pro", price: 35000}

    App->>Redis: SET "product:laptop-001" "{...json...}"
    Redis-->>App: OK

    App-->>App: 回傳 ProductDocument

    Note over App,Mongo: 下次讀取時就會 Cache Hit
```

### 4.3 Cache Hit 流程

```mermaid
sequenceDiagram
    participant App as Spring Boot
    participant Redis as Redis (Cache)
    participant Mongo as MongoDB (Source)

    Note over App,Mongo: Cache Hit 流程 (後續讀取)

    App->>Redis: GET "product:laptop-001"
    Redis-->>App: "{\"name\":\"Laptop Pro\",\"price\":35000}" (Cache Hit!)

    Note over Mongo: MongoDB 完全沒被查詢<br/>節省了磁碟 I/O 和網路延遲

    App-->>App: 反序列化 JSON → ProductDocument
    App-->>App: 回傳 ProductDocument

    Note over App,Redis: 回應時間: ~0.3ms (vs MongoDB ~2ms)
```

### 4.4 寫入失效流程

```mermaid
sequenceDiagram
    participant App as Spring Boot
    participant Redis as Redis (Cache)
    participant Mongo as MongoDB (Source)

    Note over App,Mongo: 寫入失效流程 (更新資料)

    App->>Mongo: save(product) [price: 35000 → 32000]
    Mongo-->>App: 儲存成功

    App->>Redis: DELETE "product:laptop-001"
    Redis-->>App: OK (快取已失效)

    Note over Redis: 快取被刪除<br/>下次讀取會 Cache Miss<br/>然後自動載入新資料

    Note over App,Mongo: 為什麼是「失效」而非「更新」？<br/>避免 MongoDB 和 Redis 寫入順序不一致<br/>導致快取中出現過時資料
```

### 4.5 為什麼是「失效」而非「更新」？

```mermaid
graph TB
    subgraph "反模式：寫入時更新快取"
        direction TB
        BAD1["Thread A: 更新價格為 32000"]
        BAD2["Thread B: 更新價格為 28000"]
        BAD3["可能的執行順序：<br/>1. A 寫 MongoDB (32000)<br/>2. B 寫 MongoDB (28000)<br/>3. B 更新 Redis (28000)<br/>4. A 更新 Redis (32000)<br/><br/>結果：MongoDB=28000, Redis=32000<br/>資料不一致！"]
    end

    subgraph "正確做法：寫入時失效快取"
        direction TB
        GOOD1["Thread A: 更新價格為 32000"]
        GOOD2["Thread B: 更新價格為 28000"]
        GOOD3["可能的執行順序：<br/>1. A 寫 MongoDB (32000)<br/>2. B 寫 MongoDB (28000)<br/>3. B 刪除 Redis<br/>4. A 刪除 Redis<br/><br/>結果：MongoDB=28000, Redis=空<br/>下次讀取會載入正確的 28000"]
    end

    style BAD3 fill:#ffcdd2
    style GOOD3 fill:#c8e6c9
```

---

## 5. M02 CacheAsideService 實作解析

### 5.1 完整原始碼

以下是 M02 實驗中的 `CacheAsideService`，實作了完整的 Cache-Aside 模式：

```java
@Service
public class CacheAsideService {

    private static final String CACHE_PREFIX = "product:";

    private final ProductMongoRepository mongoRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheAsideService(ProductMongoRepository mongoRepository,
                             StringRedisTemplate redisTemplate) {
        this.mongoRepository = mongoRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 讀取：Cache-Aside 核心邏輯
     * 1. 先查 Redis 快取
     * 2. Cache Miss → 查 MongoDB
     * 3. 回填快取
     */
    public ProductDocument findById(String id) {
        String cacheKey = CACHE_PREFIX + id;

        // Step 1: 查 Redis
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserialize(cached);  // Cache Hit — 直接回傳
        }

        // Step 2: Cache Miss — 查 MongoDB
        ProductDocument product = mongoRepository.findById(id).orElse(null);
        if (product != null) {
            // Step 3: 回填快取
            redisTemplate.opsForValue().set(cacheKey, serialize(product));
        }
        return product;
    }

    /**
     * 寫入：先寫 MongoDB，再失效 Redis 快取
     */
    public ProductDocument save(ProductDocument product) {
        // Step 1: 寫入 MongoDB (Source of Truth)
        ProductDocument saved = mongoRepository.save(product);
        // Step 2: 失效快取 (不是更新！)
        redisTemplate.delete(CACHE_PREFIX + saved.getId());
        return saved;
    }

    /**
     * 檢查是否已快取
     */
    public boolean isCached(String id) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CACHE_PREFIX + id));
    }

    private String serialize(ProductDocument product) {
        try {
            return objectMapper.writeValueAsString(product);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize product", e);
        }
    }

    private ProductDocument deserialize(String json) {
        try {
            return objectMapper.readValue(json, ProductDocument.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize product", e);
        }
    }
}
```

### 5.2 測試案例解析

M02 實驗包含三個測試案例，驗證 Cache-Aside 的完整行為：

**測試一：Cache Miss — 首次讀取**

```java
@Test
@DisplayName("Cache miss: fetch from MongoDB and populate Redis")
void cacheMissFetchesFromMongoAndCaches() {
    // Given: 產品只存在 MongoDB 中
    var product = mongoRepository.save(
            new ProductDocument("Laptop Pro", "electronics", new BigDecimal("35000")));
    assertThat(cacheAsideService.isCached(product.getId())).isFalse();

    // When: 透過 Cache-Aside 讀取
    ProductDocument result = cacheAsideService.findById(product.getId());

    // Then: 產品被回傳，且已被快取
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Laptop Pro");
    assertThat(cacheAsideService.isCached(product.getId())).isTrue();
}
```

**測試二：Cache Hit — 快取命中**

```java
@Test
@DisplayName("Cache hit: return from Redis without hitting MongoDB")
void cacheHitReturnsFromRedis() {
    // Given: 產品已被快取 (透過首次讀取)
    var product = mongoRepository.save(
            new ProductDocument("Laptop Pro", "electronics", new BigDecimal("35000")));
    cacheAsideService.findById(product.getId()); // 預熱快取

    // When: 從 MongoDB 刪除 (模擬 MongoDB 不可用)
    mongoRepository.deleteById(product.getId());

    // Then: 仍然從快取回傳
    ProductDocument cached = cacheAsideService.findById(product.getId());
    assertThat(cached).isNotNull();
    assertThat(cached.getName()).isEqualTo("Laptop Pro");
}
```

**測試三：寫入失效 — 更新後快取被清除**

```java
@Test
@DisplayName("Write invalidation: update MongoDB and invalidate Redis cache")
void writeInvalidatesCacheOnUpdate() {
    // Given: 產品已被快取
    var product = mongoRepository.save(
            new ProductDocument("Laptop Pro", "electronics", new BigDecimal("35000")));
    cacheAsideService.findById(product.getId()); // 預熱快取
    assertThat(cacheAsideService.isCached(product.getId())).isTrue();

    // When: 透過 Cache-Aside 更新
    product.setPrice(new BigDecimal("32000"));
    cacheAsideService.save(product);

    // Then: 快取被失效
    assertThat(cacheAsideService.isCached(product.getId())).isFalse();

    // And: 下次讀取會從 MongoDB 載入新資料
    ProductDocument updated = cacheAsideService.findById(product.getId());
    assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("32000"));
    assertThat(cacheAsideService.isCached(product.getId())).isTrue();
}
```

### 5.3 金融場景的 Cache-Aside 增強版

在生產環境中，Cache-Aside 需要更多防護機制：

```java
@Service
public class EnhancedCacheAsideService {

    private static final String CACHE_PREFIX = "product:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final String NULL_SENTINEL = "__NULL__";

    private final ProductMongoRepository mongoRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductDocument findById(String id) {
        String cacheKey = CACHE_PREFIX + id;

        String cached = redisTemplate.opsForValue().get(cacheKey);

        // 防止快取穿透 (Cache Penetration)
        // 如果之前查過且 MongoDB 中不存在，快取一個 null 標記
        if (NULL_SENTINEL.equals(cached)) {
            return null;
        }

        if (cached != null) {
            return deserialize(cached);
        }

        // Cache Miss — 加分散式鎖防止快取擊穿 (Cache Stampede)
        String lockKey = "lock:" + cacheKey;
        Boolean locked = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));

        if (Boolean.TRUE.equals(locked)) {
            try {
                // 雙重檢查：另一個執行緒可能已經填充了快取
                cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null && !NULL_SENTINEL.equals(cached)) {
                    return deserialize(cached);
                }

                ProductDocument product = mongoRepository.findById(id).orElse(null);
                if (product != null) {
                    // 加入 TTL 防止快取雪崩 (Cache Avalanche)
                    // 加入隨機偏移，避免大量快取同時過期
                    Duration ttl = CACHE_TTL.plusSeconds(
                        ThreadLocalRandom.current().nextInt(0, 300));
                    redisTemplate.opsForValue().set(cacheKey, serialize(product), ttl);
                } else {
                    // 快取 null 結果，防止反覆查詢不存在的 ID
                    redisTemplate.opsForValue().set(cacheKey, NULL_SENTINEL,
                        Duration.ofMinutes(5));
                }
                return product;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // 沒拿到鎖，短暫等待後重試
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            return findById(id); // 重試
        }
    }
}
```

---

## 6. 金融 Polyglot 完整架構圖

### 6.1 整體架構

```mermaid
graph TB
    subgraph "前端層"
        WEB["網頁銀行"]
        MOB["行動銀行 App"]
        ATM["ATM 終端"]
    end

    subgraph "API Gateway"
        GW["Spring Cloud Gateway<br/>路由 / 限流 / 認證"]
    end

    subgraph "微服務層"
        SVC1["帳務服務<br/>(Account Service)"]
        SVC2["客戶服務<br/>(Customer Service)"]
        SVC3["行情服務<br/>(Market Data Service)"]
        SVC4["風控服務<br/>(Risk Service)"]
        SVC5["稽核服務<br/>(Audit Service)"]
    end

    subgraph "資料層 — Polyglot Persistence"
        PG["PostgreSQL<br/>核心帳務<br/>帳戶、轉帳、對帳"]
        MONGO["MongoDB<br/>客戶檔案、商品<br/>保單、文件"]
        REDIS["Redis<br/>匯率快取、Session<br/>風控規則、分散式鎖"]
        CASS["Cassandra<br/>交易流水帳<br/>稽核日誌、事件紀錄"]
    end

    subgraph "事件匯流排"
        KAFKA["Apache Kafka<br/>跨服務事件同步"]
    end

    WEB --> GW
    MOB --> GW
    ATM --> GW

    GW --> SVC1
    GW --> SVC2
    GW --> SVC3
    GW --> SVC4

    SVC1 --> PG
    SVC1 --> REDIS
    SVC2 --> MONGO
    SVC2 --> REDIS
    SVC3 --> REDIS
    SVC4 --> REDIS
    SVC4 --> MONGO
    SVC5 --> CASS

    SVC1 -->|"TransferCompletedEvent"| KAFKA
    KAFKA -->|"同步交易紀錄"| SVC5
    KAFKA -->|"更新客戶活動"| SVC2
    KAFKA -->|"風控評估"| SVC4

    style PG fill:#bbdefb
    style MONGO fill:#c8e6c9
    style REDIS fill:#ffcdd2
    style CASS fill:#ffe0b2
    style KAFKA fill:#e1bee7
```

### 6.2 資料流動圖

```mermaid
sequenceDiagram
    participant Client as 行動銀行
    participant GW as API Gateway
    participant AcctSvc as 帳務服務
    participant CustSvc as 客戶服務
    participant PG as PostgreSQL
    participant Mongo as MongoDB
    participant Redis as Redis
    participant Kafka as Kafka
    participant AuditSvc as 稽核服務
    participant Cass as Cassandra

    Client->>GW: POST /api/transfer
    GW->>AcctSvc: 轉帳請求

    AcctSvc->>Redis: 取得分散式鎖
    Redis-->>AcctSvc: 鎖定成功

    AcctSvc->>PG: BEGIN TRANSACTION
    AcctSvc->>PG: 扣款 (FROM 帳戶)
    AcctSvc->>PG: 入帳 (TO 帳戶)
    AcctSvc->>PG: COMMIT
    PG-->>AcctSvc: 交易成功

    AcctSvc->>Redis: 釋放分散式鎖
    AcctSvc->>Redis: 失效帳戶快取

    AcctSvc->>Kafka: 發送 TransferCompletedEvent

    par 非同步處理
        Kafka->>AuditSvc: 消費事件
        AuditSvc->>Cass: 寫入交易流水帳
    and
        Kafka->>CustSvc: 消費事件
        CustSvc->>Mongo: 更新客戶最近活動
    end

    AcctSvc-->>GW: 轉帳成功
    GW-->>Client: 200 OK
```

### 6.3 各資料庫的職責邊界

| 資料庫 | 職責 | 不該做的事 |
|--------|------|-----------|
| **PostgreSQL** | 帳戶餘額、轉帳交易、對帳、法規報表 | 儲存非結構化文件、高頻率快取 |
| **MongoDB** | 客戶檔案、商品目錄、保單文件、CRM | 替代 PostgreSQL 做核心帳務 |
| **Redis** | 匯率快取、Session、分散式鎖、風控規則 | 作為主要持久化儲存 |
| **Cassandra** | 交易流水帳、稽核日誌、事件紀錄 | 即席查詢、複雜聚合分析 |

---

## 7. 實作考量

### 7.1 資料一致性

Polyglot 架構中最大的挑戰是跨資料庫的資料一致性。

```mermaid
graph TB
    subgraph "一致性策略"
        S1["<b>強一致性</b><br/>同一交易內<br/>PostgreSQL ACID"]
        S2["<b>事件驅動最終一致性</b><br/>Kafka 事件<br/>非同步同步到其他 DB"]
        S3["<b>Saga 模式</b><br/>跨服務的補償交易<br/>最終達到一致"]
    end

    S1 --> E1["適用：核心帳務<br/>轉帳 = 扣款 + 入帳<br/>必須在同一交易內"]
    S2 --> E2["適用：跨 DB 同步<br/>轉帳完成 → Kafka<br/>→ 更新 Cassandra 日誌<br/>→ 更新 MongoDB 客戶活動"]
    S3 --> E3["適用：跨服務交易<br/>訂單服務 → 庫存服務<br/>→ 支付服務<br/>任一失敗則補償"]

    style S1 fill:#c8e6c9
    style S2 fill:#fff9c4
    style S3 fill:#ffe0b2
```

**實務建議**：

```
Rule 1: 同一個微服務內，盡量只用一個主資料庫 + Redis 快取
Rule 2: 跨微服務的資料同步，用事件驅動 (Kafka)
Rule 3: 需要強一致性的操作，限制在同一個 PostgreSQL 交易中
Rule 4: 接受最終一致性的場景（稽核、分析），用非同步事件同步
```

### 7.2 運維複雜度

| 考量 | 單一 DB | 2 種 DB | 3 種 DB | 4+ 種 DB |
|------|--------|---------|---------|----------|
| **部署複雜度** | 低 | 中 | 高 | 很高 |
| **監控面板** | 1 個 | 2 個 | 3 個 | N 個 |
| **備份策略** | 統一 | 分別制定 | 分別制定 | 分別制定 |
| **故障排查** | 單點 | 需判斷哪個 DB | 交叉排查 | 極度困難 |
| **團隊技能** | 專精 1 種 | 至少 2 種 | 至少 3 種 | 稀有人才 |
| **DevOps 需求** | 低 | 中 | 高 | 極高 |

**Spring Boot 中的多資料庫配置**：

```yaml
# application.yml — Polyglot 配置
spring:
  datasource:
    url: jdbc:postgresql://pg-primary:5432/banking
    username: ${PG_USER}
    password: ${PG_PASSWORD}
  data:
    mongodb:
      uri: mongodb://mongo-rs1:27017,mongo-rs2:27017/banking?replicaSet=rs0
    redis:
      host: redis-primary
      port: 6379
      password: ${REDIS_PASSWORD}

# Cassandra 需要額外配置
cassandra:
  contact-points: cass-node1,cass-node2,cass-node3
  local-datacenter: dc1
  keyspace: banking_audit
```

### 7.3 團隊技能

```mermaid
graph TB
    subgraph "團隊技能需求"
        direction TB
        T1["後端開發者"]
        T2["DBA / 資料工程師"]
        T3["DevOps / SRE"]
    end

    T1 --> S1["Spring Data JPA<br/>Spring Data MongoDB<br/>Spring Data Redis<br/>Cassandra Driver"]
    T2 --> S2["PostgreSQL 調優<br/>MongoDB 索引設計<br/>Redis 記憶體管理<br/>Cassandra 資料建模"]
    T3 --> S3["多 DB 容器編排<br/>多 DB 監控告警<br/>多 DB 備份恢復<br/>災難復原演練"]

    style T1 fill:#c8e6c9
    style T2 fill:#bbdefb
    style T3 fill:#fff9c4
```

> **務實建議**：如果你的團隊只有 5 個人，Polyglot 的上限大概是 2 種資料庫 (PostgreSQL + MongoDB 或 PostgreSQL + Redis)。每多一種資料庫，就需要至少一個人深入了解它。

---

## 8. Anti-Patterns：過度使用 Polyglot 的風險

### 8.1 Anti-Pattern 清單

```mermaid
graph TB
    subgraph "Polyglot Anti-Patterns"
        direction TB
        AP1["<b>資料庫動物園</b><br/>用了 7 種資料庫<br/>但團隊只有 5 個人"]
        AP2["<b>簡歷驅動開發</b><br/>為了學新技術<br/>而非業務需求引入新 DB"]
        AP3["<b>忽略一致性</b><br/>跨 DB 資料不一致<br/>但沒有同步機制"]
        AP4["<b>重複儲存失控</b><br/>同一份資料存在 4 個 DB<br/>但沒人知道哪個是真的"]
        AP5["<b>過早最佳化</b><br/>資料量只有 100MB<br/>就搞了 Cassandra 叢集"]
    end

    AP1 --> FIX1["修正：控制在 2-3 種<br/>每種都有明確的團隊負責人"]
    AP2 --> FIX2["修正：用 POC 驗證需求<br/>不在生產環境做實驗"]
    AP3 --> FIX3["修正：事件驅動同步<br/>明確定義 Source of Truth"]
    AP4 --> FIX4["修正：每份資料只有<br/>一個 Source of Truth<br/>其餘都是衍生副本"]
    AP5 --> FIX5["修正：YAGNI 原則<br/>先證明需要再引入"]

    style AP1 fill:#ffcdd2
    style AP2 fill:#ffcdd2
    style AP3 fill:#ffcdd2
    style AP4 fill:#ffcdd2
    style AP5 fill:#ffcdd2
    style FIX1 fill:#c8e6c9
    style FIX2 fill:#c8e6c9
    style FIX3 fill:#c8e6c9
    style FIX4 fill:#c8e6c9
    style FIX5 fill:#c8e6c9
```

### 8.2 Anti-Pattern 1：資料庫動物園

```
錯誤場景：
  一個中小型金融科技公司 (FinTech Startup)
  團隊 8 人
  使用了：PostgreSQL, MongoDB, Redis, Cassandra, Neo4j,
          Elasticsearch, InfluxDB

結果：
  - 每個 DB 都沒有人真正精通
  - 故障排查耗時 3 倍
  - 運維成本失控
  - 資料一致性問題頻發

正確做法：
  PostgreSQL (核心業務)
  + Redis (快取)
  + MongoDB (文件/CRM)  ← 只在確實需要時才加
  = 3 種，足以覆蓋 90% 場景
```

### 8.3 Anti-Pattern 2：忽略 Source of Truth

```mermaid
graph TB
    subgraph "錯誤：多個 Source of Truth"
        direction TB
        BAD["客戶姓名"]
        BAD --> B1["PostgreSQL: 王大明"]
        BAD --> B2["MongoDB: 王大銘 (打錯字)"]
        BAD --> B3["Redis: 王大明"]
        BAD --> B4["Cassandra: Wang Daming"]
        Note1["哪個才是正確的？<br/>沒有人知道"]
    end

    subgraph "正確：單一 Source of Truth"
        direction TB
        GOOD["客戶姓名"]
        GOOD --> G1["MongoDB: 王大明<br/>(Source of Truth)"]
        G1 -.->|"快取副本"| G2["Redis: 王大明"]
        G1 -.->|"事件同步"| G3["Cassandra: 王大明"]
        G1 -.->|"ETL 同步"| G4["PostgreSQL: 王大明"]
        Note2["MongoDB 是唯一真相<br/>其餘都是衍生副本"]
    end

    style BAD fill:#ffcdd2
    style GOOD fill:#c8e6c9
    style Note1 fill:#ffcdd2
    style Note2 fill:#c8e6c9
```

### 8.4 健康的 Polyglot 架構檢查清單

```
評估你的 Polyglot 架構是否健康：

  [x] 每種資料庫都有明確的職責邊界
  [x] 每份資料都有唯一的 Source of Truth
  [x] 跨 DB 資料同步有明確的機制 (事件驅動 / ETL)
  [x] 團隊中每種 DB 至少有一個熟悉的人
  [x] 有統一的監控和告警機制
  [x] 有各 DB 的備份和災難復原計畫
  [x] 新增一種 DB 需要經過架構評審
  [x] 有文件記錄每種 DB 的選型理由

  如果你有 3 項以上沒打勾，
  先停下來整頓現有架構，不要再加新的 DB。
```

### 8.5 給 Spring 開發者的 Polyglot 起步建議

> **Step 1**: 用 PostgreSQL 跑起來
> - 所有業務都先用 PostgreSQL
> - 這是你的安全底線
>
> **Step 2**: 引入 Redis 做快取
> - 這幾乎沒有爭議，Redis 快取是標配
> - 用 Cache-Aside 模式（就像 M02 實驗那樣）
> - `spring-boot-starter-data-redis` 就搞定
>
> **Step 3**: 評估是否需要 MongoDB
> - 如果你有「文件型資料」或「彈性 Schema 需求」
> - 用 M02 的評估方法分析場景
> - `spring-boot-starter-data-mongodb` 加入
>
> **Step 4**: 評估是否需要 Cassandra
> - 只有在「海量時序寫入」場景才考慮
> - 大多數團隊不需要 Cassandra
> - 如果只是日誌，MongoDB TTL Collection 可能就夠了
>
> **記住：每多一種資料庫，運維複雜度不是線性增長，而是指數增長。**

---

## 延伸閱讀

- [Martin Fowler: Polyglot Persistence](https://martinfowler.com/bliki/PolyglotPersistence.html)
- [Cache-Aside Pattern - Microsoft](https://learn.microsoft.com/en-us/azure/architecture/patterns/cache-aside)
- [Designing Data-Intensive Applications (Martin Kleppmann)](https://dataintensive.net/)
- [Spring Data - Multi-Store Support](https://docs.spring.io/spring-data/)
- [Netflix: Polyglot Persistence at Scale](https://netflixtechblog.com/)

---

> **上一篇**: [M02-DOC-02: MongoDB vs Redis vs Cassandra 深度比較](./M02-DOC-02-mongodb-vs-redis-vs-cassandra.md)
> **下一篇**: M03 - MongoDB 環境建置與基礎操作
