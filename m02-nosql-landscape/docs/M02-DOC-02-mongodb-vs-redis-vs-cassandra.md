# M02-DOC-02: MongoDB vs Redis vs Cassandra 深度比較

> **模組**: M02 - NoSQL 資料庫全景
> **對象**: 具備 RDB 經驗的 Java Spring 開發者
> **目標**: 深入比較三大 NoSQL 資料庫的架構、查詢模式與一致性模型，建立實務選型能力

---

## 目錄

1. [架構與設計哲學](#1-架構與設計哲學)
2. [資料模型差異](#2-資料模型差異)
3. [查詢模式差異](#3-查詢模式差異)
4. [M02 實驗程式碼解析](#4-m02-實驗程式碼解析)
5. [一致性模型比較](#5-一致性模型比較)
6. [效能特性比較](#6-效能特性比較)
7. [選型決策矩陣](#7-選型決策矩陣)

---

## 1. 架構與設計哲學

### 1.1 三者的核心設計理念

| 資料庫 | 核心理念 | 設計目標 |
|--------|---------|---------|
| **MongoDB** | 「資料即文件」 | 用最接近應用程式的資料結構儲存，最大化查詢靈活度 |
| **Redis** | 「資料即記憶體」 | 用記憶體換取極致速度，簡單但極快 |
| **Cassandra** | 「資料即分區」 | 為大規模分散式寫入而生，犧牲查詢靈活度換取線性擴展 |

### 1.2 架構圖比較

```mermaid
graph TB
    subgraph "MongoDB 架構"
        direction TB
        MC["Client (Spring Data)"]
        MC --> MP["Primary<br/>接受讀寫"]
        MP --> MS1["Secondary 1<br/>同步複製"]
        MP --> MS2["Secondary 2<br/>同步複製"]
        MP -.->|"選舉機制"| MS1
        MP -.->|"自動容錯"| MS2
    end

    subgraph "Redis 架構"
        direction TB
        RC["Client (StringRedisTemplate)"]
        RC --> RM["Master<br/>接受讀寫"]
        RM -->|"非同步複製"| RS1["Replica 1<br/>只讀"]
        RM -->|"非同步複製"| RS2["Replica 2<br/>只讀"]
    end

    subgraph "Cassandra 架構"
        direction TB
        CC["Client (CqlSession)"]
        CC --> CN1["Node 1<br/>Partition A,B"]
        CC --> CN2["Node 2<br/>Partition C,D"]
        CC --> CN3["Node 3<br/>Partition E,F"]
        CN1 -.->|"Gossip"| CN2
        CN2 -.->|"Gossip"| CN3
        CN3 -.->|"Gossip"| CN1
    end

    style MP fill:#c8e6c9
    style RM fill:#ffcdd2
    style CN1 fill:#bbdefb
    style CN2 fill:#bbdefb
    style CN3 fill:#bbdefb
```

### 1.3 架構差異分析

| 維度 | MongoDB | Redis | Cassandra |
|------|---------|-------|-----------|
| **拓撲** | Replica Set (主從) | Master-Replica | Peer-to-Peer (無主) |
| **寫入節點** | 僅 Primary | 僅 Master | 任何節點 |
| **讀取節點** | Primary 或 Secondary | Master 或 Replica | 任何節點 |
| **容錯機制** | 自動選舉新 Primary | 哨兵 (Sentinel) 切換 | 無單點故障 |
| **資料分片** | Sharding (手動配置) | Cluster (Hash Slot) | 一致性雜湊 (自動) |
| **CAP 定位** | CP (預設) | AP | AP (可調為 CP) |

---

## 2. 資料模型差異

### 2.1 同一筆資料在三種資料庫中的表示

**場景**：儲存一個金融商品資訊

#### MongoDB (Document)

```json
{
  "_id": "fund-001",
  "name": "富邦台灣科技基金",
  "category": "equity-fund",
  "price": NumberDecimal("25.67"),
  "currency": "TWD",
  "specifications": {
    "riskLevel": "RR4",
    "managementFee": "1.5%",
    "custodianBank": "台灣銀行",
    "benchmarkIndex": "台灣加權指數"
  },
  "nav_history": [
    { "date": "2024-01-15", "nav": 25.67 },
    { "date": "2024-01-14", "nav": 25.42 },
    { "date": "2024-01-13", "nav": 25.55 }
  ]
}
```

#### Redis (Key-Value)

```
KEY: "product:fund-001"
VALUE: "{\"name\":\"富邦台灣科技基金\",\"category\":\"equity-fund\",\"price\":25.67}"

KEY: "nav:fund-001:2024-01-15"
VALUE: "25.67"

KEY: "nav:fund-001:2024-01-14"
VALUE: "25.42"

KEY: "category:equity-fund"
VALUE: (Redis Set) {"fund-001", "fund-002", "fund-003"}
```

#### Cassandra (Wide-Column)

```cql
CREATE TABLE financial_products (
    category TEXT,            -- Partition Key
    id TEXT,                  -- Clustering Key
    name TEXT,
    price DECIMAL,
    currency TEXT,
    risk_level TEXT,
    management_fee TEXT,
    PRIMARY KEY (category, id)
);

CREATE TABLE nav_history (
    product_id TEXT,          -- Partition Key
    date DATE,                -- Clustering Key (DESC)
    nav DECIMAL,
    PRIMARY KEY (product_id, date)
) WITH CLUSTERING ORDER BY (date DESC);
```

### 2.2 資料模型特性比較

```mermaid
graph TB
    subgraph "資料模型比較"
        direction TB
        M["MongoDB<br/>Document Model"]
        R["Redis<br/>Key-Value Model"]
        C["Cassandra<br/>Wide-Column Model"]
    end

    M --> M1["巢狀文件<br/>陣列<br/>任意欄位"]
    M --> M2["同一 Collection 中<br/>文件結構可以不同"]
    M --> M3["16MB 文件限制<br/>但通常綽綽有餘"]

    R --> R1["String / List / Set<br/>Hash / Sorted Set<br/>Stream"]
    R --> R2["Value 是不透明的<br/>Redis 不理解 Value 的內容"]
    R --> R3["512MB Value 限制<br/>但建議 < 1MB"]

    C --> C1["固定 Column Schema<br/>需 ALTER TABLE 加欄位"]
    C --> C2["Partition Key 決定<br/>資料分布位置"]
    C --> C3["2GB Partition 限制<br/>需謹慎設計 Partition Key"]

    style M fill:#c8e6c9
    style R fill:#ffcdd2
    style C fill:#bbdefb
```

---

## 3. 查詢模式差異

### 3.1 查詢能力分級

| 查詢類型 | MongoDB | Redis | Cassandra |
|---------|---------|-------|-----------|
| **主鍵查找** | findById() | GET key | WHERE pk = ? AND ck = ? |
| **單欄位查詢** | findByCategory() | 不支援 | 僅 Partition Key |
| **範圍查詢** | findByPriceBetween() | ZRANGEBYSCORE (限 Sorted Set) | WHERE ck > ? AND ck < ? |
| **模糊搜尋** | findByNameContaining() | 不支援 | 不支援 |
| **多條件查詢** | findByCategoryAndPriceGreaterThan() | 不支援 | 僅 PK + CK 組合 |
| **聚合分析** | aggregate([{$group}, {$sort}]) | 不支援 | COUNT/SUM (有限) |
| **全文搜尋** | $text / Atlas Search | 不支援 | 需搭配 Elasticsearch |
| **關聯查詢** | $lookup (LEFT JOIN) | 不支援 | 不支援 |

### 3.2 查詢流程比較

```mermaid
sequenceDiagram
    participant App as Spring Boot
    participant M as MongoDB
    participant R as Redis
    participant C as Cassandra

    Note over App,M: MongoDB: 靈活的 Ad-hoc 查詢
    App->>M: findByCategory("electronics")
    Note over M: 掃描索引 (如有)<br/>或 Collection Scan
    M-->>App: List<ProductDocument>
    App->>M: findByNameContaining("Laptop")
    Note over M: 文字索引或正則匹配
    M-->>App: List<ProductDocument>

    Note over App,R: Redis: 僅精確 Key 查找
    App->>R: GET "product:laptop-001"
    Note over R: O(1) 雜湊查找<br/>直接從記憶體回傳
    R-->>App: String (JSON)
    App->>R: GET "product:unknown"
    R-->>App: null (key 不存在)

    Note over App,C: Cassandra: Partition Key 驅動
    App->>C: SELECT * FROM products<br/>WHERE category = 'electronics'
    Note over C: 定位 Partition<br/>讀取該 Partition 所有列
    C-->>App: List<Row>
    App->>C: SELECT * FROM products<br/>WHERE name = 'Laptop'
    Note over C: 拒絕！<br/>name 不是 Partition Key
    C-->>App: InvalidQueryException
```

### 3.3 查詢模式總結

```mermaid
graph LR
    subgraph "查詢模式光譜"
        direction LR
        R["Redis<br/>Key-only<br/>「告訴我鑰匙<br/>我找給你」"]
        C["Cassandra<br/>Partition Key<br/>「告訴我分區<br/>我列出所有」"]
        M["MongoDB<br/>Ad-hoc<br/>「告訴我條件<br/>我幫你搜」"]
    end

    style R fill:#ffcdd2
    style C fill:#ffe0b2
    style M fill:#c8e6c9
```

> **給 Spring 開發者的直覺**：
> - **MongoDB** 就像你的 JPA Repository，`findByXxx()` 的方法命名查詢幾乎都能用
> - **Redis** 就像一個 `HashMap`，只有 `get(key)` 和 `put(key, value)`
> - **Cassandra** 就像一個「只能用 Partition Key 當 WHERE 條件」的 SQL

---

## 4. M02 實驗程式碼解析

### 4.1 實驗設計：同一份產品資料，三種存取模式

M02 實驗的核心設計是將同一份產品資料分別存入 MongoDB、Redis、Cassandra，然後比較三者的存取差異。

```java
@Test
@DisplayName("Query pattern differences across three NoSQL types")
void queryPatternComparison() {
    // 同一筆產品資料存入三個資料庫
    var product = new ProductDocument("Laptop Pro", "electronics", new BigDecimal("35000"));
    ProductDocument saved = mongoRepository.save(product);
    redisService.save("product:" + saved.getId(), saved.getName());
    cassandraService.save(saved.getId(), saved.getName(), saved.getCategory(), saved.getPrice());

    // MongoDB: 可以用任何欄位查詢
    assertThat(mongoRepository.findByCategory("electronics")).isNotEmpty();
    assertThat(mongoRepository.findByNameContaining("Laptop")).isNotEmpty();

    // Redis: 只能用精確的 Key 查詢
    assertThat(redisService.findByKey("product:" + saved.getId())).isNotNull();
    // 無法: redisService.findByCategory("electronics")

    // Cassandra: 必須包含 Partition Key
    assertThat(cassandraService.findByCategory("electronics")).isNotEmpty();
    assertThat(cassandraService.findByCategoryAndId("electronics", saved.getId())).isNotNull();
}
```

### 4.2 Schema 彈性比較實驗

```java
@Test
@DisplayName("Schema flexibility comparison across three NoSQL types")
void schemaFlexibilityComparison() {
    // MongoDB: 同一 Collection 中，文件結構可以不同
    var basicProduct = new ProductDocument("Mouse", "electronics", new BigDecimal("500"));
    var richProduct = new ProductDocument("Laptop Pro", "electronics", new BigDecimal("35000"),
            Map.of("cpu", "M3 Pro", "ram", "18GB", "storage", "512GB SSD"));

    mongoRepository.saveAll(List.of(basicProduct, richProduct));

    ProductDocument foundBasic = mongoRepository.findById(basicProduct.getId()).orElseThrow();
    ProductDocument foundRich = mongoRepository.findById(richProduct.getId()).orElseThrow();

    // 不同文件可以有不同欄位 — Schema 彈性
    assertThat(foundBasic.getSpecifications()).isNull();
    assertThat(foundRich.getSpecifications()).containsKeys("cpu", "ram", "storage");

    // Redis: 任何字串都能存 — 完全無 Schema
    redisService.save("simple", "just a string");
    redisService.save("json", "{\"complex\":true,\"nested\":{\"key\":\"value\"}}");

    // Cassandra: 固定 Column Schema — 新增欄位需要 ALTER TABLE
    cassandraService.save("p1", "Mouse", "electronics", new BigDecimal("500"));
    Row row = cassandraService.findByCategoryAndId("electronics", "p1");
    assertThat(row.getString("name")).isEqualTo("Mouse");
}
```

### 4.3 三種 Repository 層程式碼比較

```mermaid
graph TB
    subgraph "Spring 整合比較"
        direction TB
        MS["MongoDB<br/>Spring Data MongoDB"]
        RS["Redis<br/>StringRedisTemplate"]
        CS["Cassandra<br/>CqlSession (Driver)"]
    end

    MS --> M1["interface ProductMongoRepository<br/>extends MongoRepository&lt;ProductDocument, String&gt;<br/><br/>findByCategory(String)<br/>findByNameContaining(String)"]
    RS --> R1["@Service RedisProductService<br/><br/>redisTemplate.opsForValue().set(key, value)<br/>redisTemplate.opsForValue().get(key)<br/>redisTemplate.hasKey(key)"]
    CS --> C1["@Service CassandraProductService<br/><br/>cqlSession.execute(<br/>  'SELECT * FROM products<br/>   WHERE category = ?', category)"]

    style MS fill:#c8e6c9
    style RS fill:#ffcdd2
    style CS fill:#bbdefb
```

**MongoDB** — 宣告式 Repository，自動生成查詢：

```java
public interface ProductMongoRepository extends MongoRepository<ProductDocument, String> {
    List<ProductDocument> findByCategory(String category);
    List<ProductDocument> findByNameContaining(String keyword);
}
```

**Redis** — 程式化操作，Key-Value 語義：

```java
@Service
public class RedisProductService {
    private final StringRedisTemplate redisTemplate;

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String findByKey(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
```

**Cassandra** — CQL 語句，類似 SQL 但受限更多：

```java
@Service
public class CassandraProductService {
    private final CqlSession cqlSession;

    public List<Row> findByCategory(String category) {
        ResultSet rs = cqlSession.execute(SimpleStatement.newInstance(
                "SELECT * FROM products WHERE category = ?",
                category));
        return rs.all();
    }
}
```

---

## 5. 一致性模型比較

### 5.1 三者的一致性保證

| 面向 | MongoDB | Redis | Cassandra |
|------|---------|-------|-----------|
| **預設一致性** | 強一致性 (Primary 讀寫) | 最終一致性 | 最終一致性 |
| **複製方式** | 同步 (majority) 或非同步 | 非同步複製 | 可調 Quorum |
| **調控機制** | Write Concern + Read Concern | 無 (固定非同步) | Consistency Level |
| **交易支援** | 多文件 ACID (4.0+) | MULTI/EXEC (限單節點) | 輕量交易 (LWT) |
| **衝突解決** | 單 Primary 無寫入衝突 | Last Write Wins | Last Write Wins / LWT |

### 5.2 MongoDB 的可調一致性

MongoDB 透過 **Write Concern** 和 **Read Concern** 組合，讓你精細控制一致性等級：

```java
// 金融交易場景：強一致性
@Service
public class FinancialProductService {

    private final MongoTemplate mongoTemplate;

    public void updateFundNav(String fundId, BigDecimal newNav) {
        // Write Concern: majority — 寫入多數節點才確認
        mongoTemplate.setWriteConcern(WriteConcern.MAJORITY);
        mongoTemplate.updateFirst(
            Query.query(Criteria.where("_id").is(fundId)),
            Update.update("price", newNav)
                   .push("nav_history", new NavEntry(LocalDate.now(), newNav)),
            ProductDocument.class
        );
    }

    public ProductDocument getFundInfo(String fundId) {
        // Read Concern: majority — 只讀取已被多數節點確認的資料
        Query query = Query.query(Criteria.where("_id").is(fundId))
            .withReadConcern(ReadConcern.MAJORITY);
        return mongoTemplate.findOne(query, ProductDocument.class);
    }
}
```

### 5.3 Redis 的非同步複製

Redis 採用 **非同步複製** — Master 寫入後立即回傳成功，再非同步同步到 Replica：

```mermaid
sequenceDiagram
    participant App as Spring Boot
    participant M as Redis Master
    participant R1 as Redis Replica 1
    participant R2 as Redis Replica 2

    App->>M: SET "rate:USD-TWD" "31.25"
    M-->>App: OK (立即回傳)
    Note over M: 寫入完成，回傳成功

    M->>R1: 非同步複製
    M->>R2: 非同步複製
    Note over R1,R2: 複製延遲: 通常 < 1ms<br/>但 Master 故障時可能丟資料

    rect rgb(255, 235, 238)
        Note over M: Master 故障！
        Note over R1: 可能尚未收到<br/>最新寫入
        R1-->>App: GET "rate:USD-TWD"<br/>可能回傳舊值或 null
    end
```

> **金融場景風險**：如果你用 Redis 儲存即時匯率，Master 故障時 Replica 可能回傳過時的匯率。這在大多數場景可接受（因為匯率本身就在變動），但不能用於結帳定價。

### 5.4 Cassandra 的可調一致性

Cassandra 使用 **Consistency Level** 控制每次讀寫需要多少節點參與：

```
Consistency Level    | 節點數 (3 節點叢集)    | 效果
─────────────────────┼──────────────────────┼────────────────
ONE                  | 1 個節點確認           | 最快，但可能讀到舊資料
QUORUM               | 2 個節點確認 (多數)    | 平衡速度與一致性
ALL                  | 3 個節點全部確認       | 最慢，強一致性
LOCAL_QUORUM         | 本地資料中心多數       | 跨資料中心場景
```

```java
// Cassandra: 調整一致性等級
SimpleStatement statement = SimpleStatement.newInstance(
    "SELECT * FROM products WHERE category = ?", "electronics")
    .setConsistencyLevel(ConsistencyLevel.QUORUM);  // 多數節點確認

cqlSession.execute(statement);
```

### 5.5 一致性模型視覺化

```mermaid
graph LR
    subgraph "一致性光譜"
        direction LR
        WEAK["最終一致性<br/>(Eventual)"] --> TUNABLE["可調一致性<br/>(Tunable)"] --> STRONG["強一致性<br/>(Strong)"]
    end

    WEAK --> R["Redis<br/>(固定非同步)"]
    TUNABLE --> C["Cassandra<br/>(Consistency Level)"]
    TUNABLE --> M1["MongoDB<br/>(Read/Write Concern)"]
    STRONG --> M2["MongoDB<br/>(majority + primary)"]

    style WEAK fill:#ffcdd2
    style TUNABLE fill:#fff9c4
    style STRONG fill:#c8e6c9
    style R fill:#ffcdd2
    style C fill:#bbdefb
    style M1 fill:#c8e6c9
    style M2 fill:#c8e6c9
```

---

## 6. 效能特性比較

### 6.1 延遲特性

| 操作 | MongoDB | Redis | Cassandra |
|------|---------|-------|-----------|
| **點查詢 (by ID/Key)** | 0.5 - 2 ms | 0.1 - 0.5 ms | 1 - 5 ms |
| **範圍查詢 (100 筆)** | 2 - 10 ms | N/A (不支援) | 5 - 20 ms |
| **單筆寫入** | 1 - 5 ms | 0.1 - 0.5 ms | 1 - 3 ms |
| **批量寫入 (1000 筆)** | 10 - 50 ms | 5 - 20 ms (Pipeline) | 10 - 30 ms |
| **聚合查詢** | 10 - 1000 ms (依複雜度) | N/A | 有限支援 |

> **註**: 以上數據為典型值，實際效能受硬體、網路、資料量、索引設計等因素影響。

### 6.2 吞吐量特性

```mermaid
graph TB
    subgraph "讀寫吞吐量特性"
        direction TB
        M["MongoDB<br/>讀: 高<br/>寫: 高"]
        R["Redis<br/>讀: 極高<br/>寫: 極高"]
        C["Cassandra<br/>讀: 中高<br/>寫: 極高"]
    end

    M --> M1["讀取吞吐:<br/>Secondary 讀取可線性擴展<br/>Sharding 分散讀取負載"]
    M --> M2["寫入吞吐:<br/>單 Primary 是寫入瓶頸<br/>Sharding 可分散寫入"]

    R --> R1["讀取吞吐:<br/>10萬+ ops/sec (單節點)<br/>記憶體操作，無磁碟 I/O"]
    R --> R2["寫入吞吐:<br/>10萬+ ops/sec (單節點)<br/>Pipeline 批量更快"]

    C --> C1["讀取吞吐:<br/>需命中 Partition Key<br/>否則全叢集掃描"]
    C --> C2["寫入吞吐:<br/>線性擴展，加節點即加速<br/>天然適合寫入密集"]

    style M fill:#c8e6c9
    style R fill:#ffcdd2
    style C fill:#bbdefb
```

### 6.3 讀寫模式適配

| 工作負載類型 | 最佳選擇 | 原因 |
|-------------|---------|------|
| **讀多寫少** (95/5) | MongoDB | 靈活查詢 + Secondary 讀取分散負載 |
| **寫多讀少** (20/80) | Cassandra | 線性寫入擴展，追加寫入最佳化 |
| **讀寫均衡** (50/50) | MongoDB / Cassandra | 依查詢複雜度決定 |
| **極低延遲** (< 1ms) | Redis | 記憶體操作，亞毫秒回應 |
| **大量即時寫入** | Cassandra | 無鎖寫入，線性擴展 |
| **複雜查詢 + 聚合** | MongoDB | 聚合管線，ad-hoc 查詢 |

### 6.4 金融場景延遲分析

```mermaid
sequenceDiagram
    participant Client as 客戶端 (App/Web)
    participant API as Spring Boot API
    participant Redis as Redis Cache
    participant Mongo as MongoDB
    participant Cass as Cassandra

    Note over Client,Cass: 場景：查詢帳戶資訊 + 最近交易紀錄

    Client->>API: GET /api/account/ACCT-001

    API->>Redis: GET "account:ACCT-001"
    Note over Redis: ~0.2ms
    Redis-->>API: Cache Hit! (帳戶摘要)

    API->>Mongo: findById("ACCT-001")
    Note over Mongo: ~1ms (完整帳戶文件)
    Mongo-->>API: AccountDocument (含嵌入資訊)

    API->>Cass: SELECT * FROM transactions<br/>WHERE account_id = 'ACCT-001'<br/>LIMIT 20
    Note over Cass: ~3ms (Partition Key 查詢)
    Cass-->>API: List<Row> (最近 20 筆交易)

    API-->>Client: 帳戶資訊 + 交易紀錄<br/>總延遲: ~5ms (平行查詢可更低)
```

---

## 7. 選型決策矩陣

### 7.1 場景對應表

| 你的需求是... | 選 MongoDB | 選 Redis | 選 Cassandra |
|-------------|-----------|---------|-------------|
| 靈活查詢，不確定未來查詢模式 | **首選** | 不適合 | 不適合 |
| 極低延遲 (< 1ms) | 不夠快 | **首選** | 不夠快 |
| 海量寫入 (> 10 萬/秒) | 可以 | 可以 (記憶體限制) | **首選** |
| 複雜聚合分析 | **首選** | 不支援 | 有限 |
| 快取 / Session | 可以但非最佳 | **首選** | 不適合 |
| 時序資料 (IoT / 日誌) | 可以 | 不適合 | **首選** |
| 文件結構多變 | **首選** | 不在意結構 | 不適合 |
| 多資料中心部署 | 支援 | 支援 (Enterprise) | **首選** |

### 7.2 組合使用建議

在實際金融系統中，三者經常組合使用：

```mermaid
graph TB
    subgraph "金融系統 — 三資料庫組合架構"
        APP["Spring Boot 金融應用"]

        APP -->|"客戶檔案查詢<br/>商品資訊查詢<br/>靈活搜尋"| MONGO["MongoDB<br/>主要資料庫<br/>客戶、商品、保單"]

        APP -->|"匯率快取<br/>Session 管理<br/>即時風控規則"| REDIS["Redis<br/>快取層 / 即時層<br/>TTL 自動過期"]

        APP -->|"交易流水帳<br/>稽核日誌<br/>事件紀錄"| CASS["Cassandra<br/>時序資料庫<br/>追加式寫入"]
    end

    MONGO -.->|"快取預熱"| REDIS
    MONGO -.->|"事件驅動同步"| CASS

    style MONGO fill:#c8e6c9
    style REDIS fill:#ffcdd2
    style CASS fill:#bbdefb
```

### 7.3 決策流程圖

```mermaid
flowchart TD
    Start(["資料要存哪裡？"]) --> Q1{"需要靈活查詢？<br/>不確定查詢模式？"}

    Q1 -->|"是"| MONGO["MongoDB<br/>靈活文件模型"]
    Q1 -->|"否"| Q2{"需要亞毫秒延遲？<br/>或是暫存性資料？"}

    Q2 -->|"是"| REDIS["Redis<br/>記憶體快取"]
    Q2 -->|"否"| Q3{"資料是追加型的？<br/>寫入遠多於讀取？"}

    Q3 -->|"是"| CASS["Cassandra<br/>海量寫入"]
    Q3 -->|"否"| Q4{"資料需要嵌入<br/>巢狀結構？"}

    Q4 -->|"是"| MONGO
    Q4 -->|"否"| Q5{"主要是簡單<br/>Key-Value 存取？"}

    Q5 -->|"是"| REDIS
    Q5 -->|"否"| MONGO

    style Start fill:#e1bee7
    style MONGO fill:#c8e6c9
    style REDIS fill:#ffcdd2
    style CASS fill:#bbdefb
```

### 7.4 不要這樣選

| 錯誤選擇 | 原因 | 正確做法 |
|---------|------|---------|
| 用 Redis 當主資料庫 | 記憶體昂貴、重啟可能丟資料 | Redis 做快取，MongoDB/RDB 做持久化 |
| 用 Cassandra 做即席查詢 | 沒有 Partition Key 就沒有好效能 | 用 MongoDB 做靈活查詢 |
| 用 MongoDB 做高頻率計數器 | 頻繁 inc 操作在高並行下有鎖競爭 | 用 Redis INCR 或 Cassandra Counter |
| 用 Redis 做大量資料儲存 | 10GB 資料需要 10GB 記憶體 | 用 MongoDB 或 Cassandra |
| 用 Cassandra 存小量關聯資料 | 殺雞用牛刀，運維複雜度高 | 用 PostgreSQL 或 MongoDB |

### 7.5 給 Spring 開發者的心法

> **MongoDB 是你的「萬能工具箱」** — 大多數場景都能勝任，查詢靈活度最接近 RDB。
>
> **Redis 是你的「加速器」** — 放在任何資料庫前面都能提速，但不要讓它獨自承擔持久化責任。
>
> **Cassandra 是你的「寫入引擎」** — 當你的資料像河流一樣不斷湧入時，Cassandra 是最佳選擇。
>
> **三者不是互相取代的關係，而是互補的關係。** 在真實的金融系統中，你很可能會同時使用至少兩種。

---

## 延伸閱讀

- [MongoDB vs Redis vs Cassandra: A Comparison](https://www.mongodb.com/compare)
- [Redis Persistence Explained](https://redis.io/docs/management/persistence/)
- [Cassandra Data Modeling Best Practices](https://cassandra.apache.org/doc/latest/cassandra/data_modeling/)
- [Spring Data - Multiple Database Support](https://docs.spring.io/spring-data/)

---

> **上一篇**: [M02-DOC-01: NoSQL 四大類型全景圖](./M02-DOC-01-nosql-four-types.md)
> **下一篇**: [M02-DOC-03: 金融場景 Polyglot Persistence 架構](./M02-DOC-03-polyglot-persistence.md)
