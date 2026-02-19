# M05-DOC-01: MongoRepository CRUD 基礎

## 學習目標

- 理解 Spring Data MongoDB 的 Repository 介面階層
- 掌握 `MongoRepository` 的 CRUD 操作方法
- 學會使用 Derived Query 自動產生查詢
- 認識 `@Query` 註解的 JSON 查詢語法
- 了解 `save()` 全替換語意的陷阱

---

## 1. Repository 介面階層

Spring Data MongoDB 提供了階層式的 Repository 介面：

```
Repository<T, ID>
  └── CrudRepository<T, ID>           // 基本 CRUD
        └── ListCrudRepository<T, ID>  // 回傳 List 而非 Iterable
              └── PagingAndSortingRepository<T, ID>  // 分頁 + 排序
                    └── MongoRepository<T, ID>        // MongoDB 特有方法
```

`MongoRepository` 繼承了所有上層介面，提供最完整的功能。在實務中，直接繼承 `MongoRepository` 即可。

---

## 2. CRUD 方法對照表

| 操作 | Repository 方法 | MongoDB 操作 |
|------|----------------|-------------|
| 新增 | `save(entity)` | `insertOne()` (無 id) / `replaceOne()` (有 id) |
| 批次新增 | `saveAll(entities)` | 批次 `insertOne()`/`replaceOne()` |
| 依 ID 查詢 | `findById(id)` | `find({ _id: id })` |
| 查詢全部 | `findAll()` | `find({})` |
| 數量統計 | `count()` | `countDocuments({})` |
| 是否存在 | `existsById(id)` | `countDocuments({ _id: id }) > 0` |
| 依 ID 刪除 | `deleteById(id)` | `deleteOne({ _id: id })` |
| 全部刪除 | `deleteAll()` | `deleteMany({})` |

### 範例：Banking 領域

```java
@Document("bank_accounts")
public class BankAccount {
    @Id
    private String id;
    private String accountNumber;
    private String holderName;
    private AccountType type;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal balance;

    private AccountStatus status;
    private Instant openedAt;
    // ...
}
```

```java
public interface BankAccountRepository extends MongoRepository<BankAccount, String> {
    // 繼承了 save, findById, findAll, count, existsById, deleteById, deleteAll...
}
```

---

## 3. Derived Query（衍生查詢）

Spring Data 會根據方法名稱自動產生 MongoDB 查詢。命名規則為 `findBy` + 屬性名稱 + 條件關鍵字。

### 常用關鍵字

| 關鍵字 | 範例方法 | 產生的查詢 |
|--------|---------|-----------|
| (無) | `findByHolderName(name)` | `{ holderName: name }` |
| And | `findByTypeAndStatus(type, status)` | `{ type: type, status: status }` |
| GreaterThan | `findByBalanceGreaterThan(min)` | `{ balance: { $gt: min } }` |
| Between | `findByBalanceBetween(min, max)` | `{ balance: { $gt: min, $lt: max } }` |
| Containing | `findByTagsContaining(tag)` | `{ tags: tag }` (陣列元素匹配) |
| ContainingIgnoreCase | `findByNameContainingIgnoreCase(s)` | 正規表示式，不區分大小寫 |
| True/False | `findByInStockTrue()` | `{ inStock: true }` |
| OrderBy | `findByCategoryOrderByPriceAsc(cat)` | 排序結果 |

### Derived 的 count 和 exists

```java
long countByStatus(AccountStatus status);     // 回傳數量
boolean existsByAccountNumber(String number); // 回傳布林值
```

---

## 4. @Query 註解

當 Derived Query 的命名太長或需要更複雜的操作時，可以使用 `@Query` 直接撰寫 MongoDB JSON 查詢。

### 基本用法

```java
@Query("{ 'policyType': { $in: ?0 }, 'status': 'ACTIVE' }")
List<InsurancePolicyDocument> findActivePoliciesByTypes(List<PolicyType> types);
```

- `?0` — 對應第一個參數
- `?1` — 對應第二個參數
- 支援所有 MongoDB 查詢運算子：`$in`, `$all`, `$regex`, `$gt` 等

### 欄位投影（Field Projection）

```java
@Query(value = "{ 'holderName': ?0 }",
       fields = "{ 'accountNumber': 1, 'holderName': 1, 'balance': 1 }")
List<BankAccount> findAccountSummaryByHolder(String holderName);
```

`fields` 屬性指定只回傳特定欄位，減少網路傳輸量。`1` 表示包含，`0` 表示排除。

### 陣列查詢：$all 運算子

```java
@Query("{ 'tags': { $all: ?0 } }")
List<Product> findByAllTags(List<String> tags);
```

`$all` 要求陣列必須**同時包含**所有指定元素（AND 語意），與 `$in`（OR 語意）不同。

---

## 5. save() 的全替換陷阱

`save()` 方法的行為取決於是否有 `id`：

- **無 id** → `insertOne()`：新增文件
- **有 id** → `replaceOne()`：**整份文件替換**

### 陷阱示範

```java
// 從資料庫讀取
BankAccount account = repository.findById(id).orElseThrow();

// 只修改了 balance
account.setBalance(new BigDecimal("5000.00"));

// save() 會用整個 account 物件替換原本的文件
// 所有欄位（包括未修改的）都會被寫回
repository.save(account);
```

這在多數情況下沒問題，但如果：
1. 在讀取和寫回之間，其他執行緒修改了同一份文件
2. 物件中某些欄位為 `null`

就可能導致資料遺失。要做**部分更新**（Partial Update），應使用 `MongoTemplate`（見 DOC-02）。

---

## 6. 本模組的三大領域

| 領域 | Collection | 主要 CRUD 特色 |
|------|-----------|---------------|
| Banking | `bank_accounts` | 基本 CRUD + Derived Query + MongoTemplate |
| Insurance | `insurance_policies` | Derived Query + 分頁 + @Query |
| E-commerce | `products` | 陣列/Map 操作 + Upsert + MongoTemplate |

---

## 小結

- `MongoRepository` 提供完整的 CRUD 方法，無需實作
- Derived Query 透過方法命名自動產生查詢，適合簡單條件
- `@Query` 適合複雜查詢，支援 MongoDB 原生 JSON 語法
- `save()` 是全替換操作，部分更新應使用 `MongoTemplate`

> **下一篇**：[M05-DOC-02 MongoTemplate 操作詳解](M05-DOC-02-mongotemplate-operations.md) — 深入 `$set`, `$inc`, `$push`, `$pull` 等 Update 運算子
