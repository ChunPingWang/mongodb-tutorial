# M05-DOC-03: 分頁、排序與實戰模式

## 學習目標

- 掌握 Spring Data 的 `Pageable` / `Page` 分頁機制
- 學會 `Sort` 排序的使用方式
- 建立 Repository vs MongoTemplate 的決策矩陣
- 回顧三大領域的 CRUD 模式
- 了解 M06 Query DSL 的銜接方向

---

## 1. 分頁（Pagination）

### Pageable 與 Page

Spring Data 的分頁以 `Pageable` 作為輸入，`Page<T>` 作為輸出。

```java
// Repository 方法宣告
Page<InsurancePolicyDocument> findByPolicyType(PolicyType type, Pageable pageable);
Page<Product> findByCategory(String category, Pageable pageable);
```

### 使用方式

```java
// 第 0 頁（從 0 開始），每頁 2 筆
Pageable pageable = PageRequest.of(0, 2);
Page<InsurancePolicyDocument> page = repository.findByPolicyType(PolicyType.TERM_LIFE, pageable);

// Page 提供的資訊
page.getContent()         // List<T> — 當頁資料
page.getTotalElements()   // long — 總筆數
page.getTotalPages()      // int — 總頁數
page.getNumber()          // int — 當前頁碼（0-based）
page.getSize()            // int — 每頁大小
page.isFirst()            // boolean — 是否第一頁
page.isLast()             // boolean — 是否最後一頁
page.hasNext()            // boolean — 是否有下一頁
```

### 分頁 + 排序

`PageRequest.of()` 可以同時指定排序：

```java
// 第 0 頁，每頁 2 筆，依 price 升冪排序
Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "price"));
Page<Product> page = productRepository.findByCategory("Electronics", pageable);
```

---

## 2. 排序（Sorting）

### 在 Derived Query 中排序

```java
// 方法名稱中直接指定排序
List<Product> findByCategoryOrderByPriceAsc(String category);
List<Product> findByCategoryOrderByPriceDesc(String category);
```

### 使用 Sort 參數

```java
// 動態指定排序
List<Product> findByCategory(String category, Sort sort);

// 呼叫時
List<Product> products = repository.findByCategory("Electronics",
        Sort.by(Sort.Direction.ASC, "price"));
```

### 多欄位排序

```java
Sort sort = Sort.by(Sort.Direction.ASC, "category")
               .and(Sort.by(Sort.Direction.DESC, "price"));
```

---

## 3. Repository vs MongoTemplate 決策矩陣

| 需求 | 推薦方式 | 原因 |
|------|---------|------|
| 基本 CRUD（新增/查詢/刪除） | Repository | 零程式碼，自動生成 |
| 簡單條件查詢 | Repository (Derived) | 方法命名即查詢 |
| 複雜 JSON 查詢 | Repository (@Query) | 原生 MongoDB 查詢語法 |
| 分頁 + 排序 | Repository (Pageable) | 內建支援，回傳 Page |
| 部分更新（$set, $inc） | MongoTemplate | Repository 只能全替換 |
| 陣列操作（$push, $pull） | MongoTemplate | Repository 不支援 |
| Map 欄位更新 | MongoTemplate | 需要 `.` 語法 |
| 批次更新多筆文件 | MongoTemplate (updateMulti) | Repository 無此功能 |
| 原子性讀取+修改 | MongoTemplate (findAndModify) | 保證原子操作 |
| 新增或更新（Upsert） | MongoTemplate (upsert) | Repository 不支援 |
| insert vs save 行為控制 | MongoTemplate | Repository 只有 save |

### 混用模式

實務中，同一個 Service 經常同時注入 Repository 和 MongoTemplate：

```java
@Service
public class BankAccountService {
    private final BankAccountRepository repository;   // 簡單 CRUD
    private final MongoTemplate mongoTemplate;         // 進階操作

    // Repository: 建立帳戶、查詢、關閉帳戶
    // MongoTemplate: 存款（$inc）、提款、凍結（$set）、批次利息
}
```

---

## 4. 三大領域 CRUD 模式總結

### Banking（銀行帳戶）

| 操作 | 實作方式 | 關鍵技術 |
|------|---------|---------|
| 建立帳戶 | Repository `save()` | 自動生成 id |
| 查詢帳戶 | Repository `findById()` | Optional 回傳 |
| 依持有人查詢 | Derived `findByHolderName()` | 自動產生查詢 |
| 存款/提款 | MongoTemplate `$inc` | 原子操作，避免 race condition |
| 凍結帳戶 | MongoTemplate `$set` | 部分更新，不影響餘額 |
| 關閉帳戶（取得舊狀態） | MongoTemplate `findAndModify` | 原子性讀取+修改 |
| 批次加息 | MongoTemplate `updateMulti` + `$inc` | 一次修改多筆 |

### Insurance（保險保單）

| 操作 | 實作方式 | 關鍵技術 |
|------|---------|---------|
| 依類型查詢 | Derived `findByPolicyType()` | Enum 參數 |
| 高額保費查詢 | Derived `findByPremiumGreaterThan()` | 比較運算子 |
| 分頁查詢 | Derived + `Pageable` | `Page<T>` 回傳 |
| 複合條件查詢 | `@Query` + `$in` | JSON 原生語法 |

### E-commerce（電商產品）

| 操作 | 實作方式 | 關鍵技術 |
|------|---------|---------|
| 依分類查詢 | Derived + Sort + Pageable | 分頁排序 |
| 標籤搜尋 | Derived `findByTagsContaining()` | 陣列元素匹配 |
| 全標籤匹配 | `@Query` + `$all` | 陣列全匹配 |
| 模糊名稱搜尋 | Derived `IgnoreCase` | 正規表示式 |
| 新增標籤 | MongoTemplate `$push` | 陣列操作 |
| 移除標籤 | MongoTemplate `$pull` | 陣列操作 |
| 更新規格 | MongoTemplate `$set` + `.` 語法 | Map 欄位操作 |
| 新增或更新 | MongoTemplate `upsert` | 依 SKU 冪等操作 |

---

## 5. 常見陷阱與注意事項

### BigDecimal 與 Decimal128

- 使用 `@Field(targetType = FieldType.DECIMAL128)` 確保在 MongoDB 中以數值型態儲存
- 不加註解時，`BigDecimal` 預設會被序列化為 String，導致數值比較失敗
- 測試中比較 BigDecimal 使用 `isEqualByComparingTo()` 而非 `isEqualTo()`

### Enum 持久化

Spring Data MongoDB 預設以 **String** 儲存 Enum：

```java
AccountStatus.ACTIVE → "ACTIVE"
PolicyType.TERM_LIFE → "TERM_LIFE"
```

在 `@Query` 中使用 Enum 值時，需用 String 格式：`'status': 'ACTIVE'`。

### save() 與並發

```
Thread A: read account (balance=1000)
Thread B: read account (balance=1000)
Thread A: account.setBalance(1500); save() → balance=1500
Thread B: account.setBalance(1200); save() → balance=1200 ← Thread A 的修改被覆蓋！
```

使用 `MongoTemplate.updateFirst()` + `$inc` 可避免此問題。

---

## 6. 銜接 M06：Query DSL

M05 涵蓋了基本的 CRUD 操作。在 M06 中，我們將深入：

- **Criteria API**：程式化組合複雜查詢條件
- **Aggregation Framework**：`$group`, `$match`, `$project`, `$lookup` 等管線操作
- **Text Search**：全文檢索
- **Geospatial Query**：地理空間查詢

M05 的 Derived Query 和 `@Query` 適合 80% 的查詢場景；M06 的 Criteria API 和 Aggregation 則處理剩餘的 20% 複雜需求。

---

## 小結

- `Pageable` + `Page` 提供完整的分頁元資料
- `Sort` 可在方法名稱或參數中指定
- Repository 與 MongoTemplate 各有所長，實務中經常混用
- Banking 示範了讀取/寫入分離模式（Repository 讀、MongoTemplate 寫）
- Insurance 示範了 Derived Query + 分頁的最佳實踐
- E-commerce 示範了陣列/Map 操作和 Upsert 的典型用法
