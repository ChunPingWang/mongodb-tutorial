# M15-DOC-01：MongoDB 索引類型全解

## 索引的本質

MongoDB 索引是獨立於集合資料之外的 B-Tree 資料結構（WiredTiger 引擎），儲存特定欄位值與對應文件位置的對應關係。沒有索引時，MongoDB 必須執行**全集合掃描（COLLSCAN）**，逐一檢查每一筆文件。有了索引，查詢引擎可以透過 **索引掃描（IXSCAN）** 快速定位目標文件。

### 與關聯式資料庫的比較

| 特性 | RDB（如 PostgreSQL） | MongoDB |
|------|---------------------|---------|
| 預設索引 | 主鍵自動建立 | `_id` 自動建立 |
| 索引結構 | B-Tree / Hash / GIN / GiST | B-Tree（WiredTiger） |
| 複合索引 | 支援 | 支援，最多 32 個欄位 |
| 全文檢索 | `tsvector` + GIN | Text Index |
| 陣列索引 | 需要 GIN | Multikey（自動偵測） |
| 部分索引 | Partial Index | Partial Index（filter expression） |
| TTL | 需要排程或 pg_cron | 原生 TTL Index |
| 地理空間 | PostGIS extension | 原生 2dsphere / 2d |

---

## 索引類型總覽

### 1. Single Field Index（單欄位索引）

最基本的索引類型，針對單一欄位建立。

```java
// Spring Data
indexManagementService.createSingleFieldIndex("m15_transactions", "accountId", Sort.Direction.ASC);

// 等同 MongoDB Shell
// db.m15_transactions.createIndex({ accountId: 1 })
```

**特點**：
- 方向（1 或 -1）對單欄位索引影響不大，MongoDB 可雙向走訪
- 適合等值查詢與簡單範圍查詢

### 2. Compound Index（複合索引）

多欄位組合索引，欄位順序**極為重要**（見 DOC-02 ESR 規則）。

```java
var keys = new LinkedHashMap<String, Sort.Direction>();
keys.put("accountId", Sort.Direction.ASC);
keys.put("type", Sort.Direction.ASC);
keys.put("transactionDate", Sort.Direction.ASC);
indexManagementService.createCompoundIndex("m15_transactions", keys);

// 等同：db.m15_transactions.createIndex({ accountId: 1, type: 1, transactionDate: 1 })
```

**特點**：
- 支援**前綴匹配**：`{accountId, type, transactionDate}` 的索引可服務 `{accountId}` 或 `{accountId, type}` 的查詢
- 欄位順序不可任意交換，必須遵循查詢模式設計

### 3. Multikey Index（多鍵索引）

當欄位為陣列時，MongoDB 自動建立 Multikey Index，為每個陣列元素建立索引條目。

```java
indexManagementService.createSingleFieldIndex("m15_products", "tags", Sort.Direction.ASC);

// 若文件 tags: ["portable", "wireless"]，會建立兩個索引條目
```

**特點**：
- 自動偵測，無需特殊語法
- 一個複合索引中最多只能有一個陣列欄位

### 4. Text Index（全文檢索索引）

支援文字搜尋，可設定欄位權重。**每個集合只能有一個 Text Index**。

```java
indexManagementService.createTextIndex("m15_products",
    Map.of("name", 3F, "description", 1F));

// name 欄位權重為 3，description 為 1
// 搜尋時 name 的匹配得分是 description 的 3 倍
```

**特點**：
- 每個集合限制一個 Text Index
- 支援多語言分詞（預設英文）
- 搜尋結果可依 `textScore` 排序

```java
// 搜尋使用
var criteria = TextCriteria.forDefaultLanguage().matching("wireless");
var query = TextQuery.queryText(criteria).sortByScore();
mongoTemplate.find(query, Product.class, "m15_products");
```

### 5. Hashed Index（雜湊索引）

對欄位值做雜湊後建立索引，僅支援等值查詢。

```java
indexManagementService.createHashedIndex("m15_test_indexes", "field4");
```

**特點**：
- 不支援範圍查詢
- 主要用於 Sharded Collection 的雜湊分片

### 6. TTL Index（存活時間索引）

自動刪除超過指定時間的文件，適合暫存資料、Session、Log。

```java
indexManagementService.createTtlIndex("m15_transactions", "createdAt", 2);

// 2 秒後自動刪除（測試用極短值）
// 生產環境通常設 86400（1天）或更長
```

**特點**：
- 只能建立在單一日期型欄位上
- MongoDB 背景執行緒（TTL Monitor）預設每 60 秒檢查一次
- 測試時可透過 `setParameter ttlMonitorSleepSecs=1` 加速

```java
// 加速 TTL Monitor（測試環境）
mongoClient.getDatabase("admin").runCommand(
    new Document("setParameter", 1).append("ttlMonitorSleepSecs", 1));
```

### 7. Unique Index（唯一索引）

確保索引欄位值不重複。

```java
var keys = new LinkedHashMap<String, Sort.Direction>();
keys.put("name", Sort.Direction.ASC);
indexManagementService.createUniqueIndex("m15_products", keys);
```

**特點**：
- 插入重複值會拋出 `DuplicateKeyException`
- `_id` 欄位的索引預設就是唯一索引
- 可與複合索引結合，確保多欄位組合唯一

### 8. Partial Index（部分索引）

只對符合過濾條件的文件建立索引，減少索引大小和維護成本。

```java
// 只索引有庫存的產品
indexManagementService.createPartialIndex("m15_products",
    Indexes.ascending("category"),
    new Document("inStock", true));
```

**特點**：
- Spring Data 的 `Index` 類別不支援 `partialFilterExpression`，需用 native driver
- 查詢時必須包含 partial filter 條件，否則 MongoDB 不會選用此索引
- 大幅減少索引儲存空間

### 9. Sparse Index（稀疏索引）

只為有該欄位的文件建立索引，忽略欄位不存在的文件。

```java
indexManagementService.createSparseIndex("m15_test_indexes", "field5");
```

**特點**：
- 與 Partial Index 功能重疊，Partial Index 更靈活
- Sparse + Unique 允許多筆文件缺少該欄位

---

## M15 索引策略總覽

### Banking Transaction 索引

| 索引 | 欄位 | 類型 | 用途 |
|------|------|------|------|
| ESR 複合 | `{accountId:1, type:1, transactionDate:1}` | Compound | ESR 規則示範 |
| 覆蓋查詢 | `{accountId:1, amount:1}` | Compound | 避免回表 |
| TTL | `{createdAt:1}` expireAfterSeconds=2 | TTL | 自動清除暫存 |

### E-commerce Product 索引

| 索引 | 欄位 | 類型 | 用途 |
|------|------|------|------|
| 全文 | `{name:"text", description:"text"}` 3:1 | Text | 關鍵字搜尋 |
| 部分 | `{category:1}` where `{inStock:true}` | Partial | 有庫存篩選 |
| 複合 | `{category:1, price:1}` | Compound | 分類+價格 |
| 多鍵 | `{tags:1}` | Multikey | 標籤搜尋 |
| 唯一 | `{name:1}` | Unique | 防止重複 |

---

## 索引管理最佳實務

1. **不要過度索引**：每個寫入操作都需更新所有相關索引
2. **監控索引使用率**：透過 `$indexStats` 聚合找出未使用的索引
3. **考慮索引大小**：使用 `db.collection.totalIndexSize()` 監控
4. **背景建立**：生產環境使用 `background: true`（MongoDB 4.2+ 預設）
5. **定期檢視**：隨查詢模式變化調整索引策略
