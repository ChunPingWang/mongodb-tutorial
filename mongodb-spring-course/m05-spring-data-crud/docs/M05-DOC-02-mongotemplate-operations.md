# M05-DOC-02: MongoTemplate 操作詳解

## 學習目標

- 理解 `MongoTemplate` 的定位與使用場景
- 掌握 `insert()` vs `save()` 的差異
- 學會使用 Update 運算子：`$set`, `$inc`, `$push`, `$pull`
- 認識 `updateFirst` vs `updateMulti` 的差別
- 掌握 `findAndModify` 原子操作與 `upsert` 模式

---

## 1. MongoTemplate vs MongoRepository

| 面向 | MongoRepository | MongoTemplate |
|------|----------------|---------------|
| 抽象層級 | 高（宣告式介面） | 低（命令式 API） |
| CRUD | 自動生成 | 手動撰寫 Query + Update |
| 部分更新 | 不支援（全替換） | 支援（`$set`, `$inc` 等） |
| 陣列操作 | 不支援 | `$push`, `$pull`, `$addToSet` |
| 批次更新 | 不支援 | `updateMulti` |
| 原子操作 | 不支援 | `findAndModify` |
| Upsert | 不支援 | 支援 |

**經驗法則**：簡單 CRUD 用 Repository，需要部分更新或進階操作時用 MongoTemplate。兩者可以在同一個 Service 中混用。

---

## 2. insert() vs save()

```java
// insert: 永遠新增，id 重複時拋出 DuplicateKeyException
mongoTemplate.insert(product);

// save: 無 id 時新增，有 id 時替換整份文件
mongoTemplate.save(product);
```

| 方法 | 無 id | id 不存在 | id 已存在 |
|------|-------|----------|----------|
| `insert()` | 新增 | 新增 | DuplicateKeyException |
| `save()` | 新增 | 新增 | 全文件替換 |

---

## 3. Update 運算子

### $set — 設定欄位值

最基本的部分更新運算子，只修改指定欄位，不影響其他欄位。

```java
Query query = Query.query(Criteria.where("id").is(accountId));
Update update = new Update().set("status", AccountStatus.FROZEN);
mongoTemplate.updateFirst(query, update, BankAccount.class);
```

**vs save() 全替換**：`$set` 只修改 `status` 欄位，其他欄位（balance, holderName 等）完全不受影響。

### $inc — 遞增 / 遞減

原子性地增加或減少數值欄位，適合金額、計數器等場景。

```java
// 存款：餘額 +500
Update deposit = new Update().inc("balance", new BigDecimal("500.00"));

// 提款：餘額 -300（傳入負數）
Update withdraw = new Update().inc("balance", new BigDecimal("-300.00"));
```

`$inc` 的優勢是**原子操作**——即使多個請求同時執行，每次 inc 都不會遺失。相較之下，read-modify-save 模式在並發時可能覆蓋彼此的變更。

### $push — 新增陣列元素

```java
Update update = new Update().push("tags", "portable");
mongoTemplate.updateFirst(query, update, Product.class);
// tags: ["computer"] → ["computer", "portable"]
```

### $pull — 移除陣列元素

```java
Update update = new Update().pull("tags", "wireless");
mongoTemplate.updateFirst(query, update, Product.class);
// tags: ["accessory", "wireless", "ergonomic"] → ["accessory", "ergonomic"]
```

### $set 在 Map 上的應用

MongoDB 的嵌入式文件（對應 Java 的 `Map`）可以用 `.` 語法設定個別鍵值：

```java
// 新增或更新 specifications 中的一個項目
Update update = new Update().set("specifications.resolution", "4K");
mongoTemplate.updateFirst(query, update, Product.class);
```

---

## 4. updateFirst vs updateMulti

| 方法 | 影響範圍 | 回傳值 |
|------|---------|--------|
| `updateFirst` | 第一筆符合的文件 | `UpdateResult` |
| `updateMulti` | **所有**符合的文件 | `UpdateResult` |

```java
// 為所有 SAVINGS 帳戶加上利息
Query query = Query.query(Criteria.where("type").is(AccountType.SAVINGS));
Update update = new Update().inc("balance", new BigDecimal("100.00"));
UpdateResult result = mongoTemplate.updateMulti(query, update, BankAccount.class);

// result.getModifiedCount() → 被修改的文件數量
```

---

## 5. findAndModify — 原子性讀取+修改

`findAndModify` 在一次原子操作中查詢、修改並回傳文件。可選擇回傳修改前或修改後的狀態。

```java
Query query = Query.query(Criteria.where("id").is(accountId));
Update update = new Update()
        .set("status", AccountStatus.CLOSED)
        .set("closedAt", Instant.now());

// returnNew(false) → 回傳修改前的文件
BankAccount oldState = mongoTemplate.findAndModify(
        query, update,
        FindAndModifyOptions.options().returnNew(false),
        BankAccount.class);

// oldState.getStatus() == ACTIVE（修改前的狀態）
```

**典型使用場景**：
- 產生序號（讀取並遞增）
- 分配任務（查詢待處理並標記為處理中）
- 關閉帳戶（需要記錄關閉前的狀態）

---

## 6. Upsert 模式

Upsert = Update + Insert。如果查詢條件找到文件就更新，找不到就新增。

```java
Query query = Query.query(Criteria.where("sku").is(sku));
Update update = new Update()
        .set("name", name)
        .set("category", category)
        .set("price", price)
        .set("inStock", true)
        .set("updatedAt", Instant.now())
        .setOnInsert("sku", sku)              // 只在新增時設定
        .setOnInsert("createdAt", Instant.now()); // 只在新增時設定

UpdateResult result = mongoTemplate.upsert(query, update, Product.class);

// result.getUpsertedId() != null → 執行了 insert
// result.getModifiedCount() > 0  → 執行了 update
```

### setOnInsert

`setOnInsert` 指定的欄位**只有在新增時**才會被設定。這常用於：
- `createdAt` 時間戳記（更新時不應覆蓋）
- 業務識別碼（如 `sku`、`accountNumber`）

---

## 7. Banking Service：Repository + MongoTemplate 混用

```java
@Service
public class BankAccountService {
    private final BankAccountRepository repository;
    private final MongoTemplate mongoTemplate;

    // Repository 方法：適合簡單 CRUD
    public BankAccount createAccount(BankAccount account) {
        return repository.save(account);
    }

    // MongoTemplate 方法：適合部分更新
    public UpdateResult deposit(String id, BigDecimal amount) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update().inc("balance", amount);
        return mongoTemplate.updateFirst(query, update, BankAccount.class);
    }
}
```

---

## 小結

| 運算子 | 用途 | 範例場景 |
|--------|------|---------|
| `$set` | 設定欄位值 | 凍結帳戶、更新價格 |
| `$inc` | 遞增/遞減 | 存款、提款、計數器 |
| `$push` | 新增陣列元素 | 加入標籤 |
| `$pull` | 移除陣列元素 | 移除標籤 |
| `$mul` | 乘法 | 批次調整（注意 Decimal128 相容性） |

> **注意**：`Update.multiply()` 在 Spring Data MongoDB 中會將值轉為 `double`，與 `@Field(targetType = DECIMAL128)` 的欄位不相容。如需對 Decimal128 欄位做批次數值調整，建議使用 `$inc` 加固定金額或 Aggregation Pipeline Update。

> **下一篇**：[M05-DOC-03 分頁、排序與實戰模式](M05-DOC-03-pagination-sorting-patterns.md) — Pageable, Sort, 以及 Repository vs Template 的決策矩陣
