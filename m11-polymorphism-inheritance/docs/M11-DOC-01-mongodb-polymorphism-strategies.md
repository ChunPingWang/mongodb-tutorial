# M11-DOC-01：MongoDB 多型文件策略

## 概述

在 MongoDB 中實現 OOP 繼承/多型，核心問題是：**如何將類型階層（type hierarchy）映射到 Document 結構？**

與 RDB 的繼承映射策略（JPA `@Inheritance`）類似，MongoDB 也有多種策略，各有優缺點。

---

## 三種主要策略

### 1. Single Collection（單一 Collection）— 推薦

所有子類型存放在**同一個 Collection**，透過 `_class` 欄位（discriminator）區分類型。

```json
// m11_financial_products collection
{ "_id": "...", "_class": "deposit", "name": "一年定存", "value": 100000, "annualRate": 1.5 }
{ "_id": "...", "_class": "fund",    "name": "全球股票", "value": 50000,  "nav": 15.32 }
{ "_id": "...", "_class": "insurance_product", "name": "年金險", "value": 200000, "coverage": 1000000 }
```

**優點：**
- 跨類型查詢簡單（單一 Collection 查詢即可）
- 索引管理集中
- 原子操作在同一 Collection 內
- Spring Data 原生支援（`@TypeAlias` + `_class`）

**缺點：**
- 不同子類型的欄位會有 null（sparse fields）
- Schema Validation 較複雜（需 Union Schema）
- 資料量大時可能影響效能

**適用場景：**
- 子類型數量有限（< 10 種）
- 頻繁需要跨類型查詢
- 子類型共享大部分欄位

### 2. Multiple Collections（多 Collection）

每個子類型存放在**獨立的 Collection**。

```
m11_deposits    → { "_id": "...", "name": "一年定存", "annualRate": 1.5 }
m11_funds       → { "_id": "...", "name": "全球股票", "nav": 15.32 }
m11_insurance   → { "_id": "...", "name": "年金險", "coverage": 1000000 }
```

**優點：**
- Schema 乾淨，無 null 欄位
- 獨立索引最佳化
- Schema Validation 簡單
- 各 Collection 獨立擴展

**缺點：**
- 跨類型查詢需多次查詢或 `$unionWith`
- 無法保證跨 Collection 原子性
- Collection 數量可能膨脹

**適用場景：**
- 子類型差異很大
- 很少需要跨類型查詢
- 各類型資料量差異大

### 3. Schema-per-Type（嵌入式 Schema 標記）

類似 Single Collection，但使用自訂 `type` 欄位而非 `_class`，並在應用層處理反序列化。

```json
{ "_id": "...", "type": "DEPOSIT", "data": { "annualRate": 1.5, "termMonths": 12 } }
{ "_id": "...", "type": "FUND",    "data": { "nav": 15.32, "riskProfile": "5:AGGRESSIVE" } }
```

**優點：**
- 不依賴框架特定欄位（`_class`）
- 跨語言/框架相容
- 可用 MongoDB 的 `$jsonSchema` + `oneOf` 驗證

**缺點：**
- 需自行處理序列化/反序列化
- 失去 Spring Data 自動多型解析
- 程式碼量較多

**適用場景：**
- 多語言存取同一 Collection
- 需要自訂 discriminator 邏輯

---

## 與 JPA 繼承策略對比

| JPA 策略 | MongoDB 對應 | 說明 |
|----------|-------------|------|
| `SINGLE_TABLE` | Single Collection | 最接近，都用 discriminator 欄位區分子類型 |
| `TABLE_PER_CLASS` | Multiple Collections | 每個具體類型一個儲存區 |
| `JOINED` | 無直接對應 | MongoDB 不適合 JOIN-heavy 設計，可用 `$lookup` 但不推薦 |

---

## Spring Data MongoDB 的 `_class` 機制

Spring Data MongoDB 預設在每個 Document 儲存 `_class` 欄位，值為 Java 類別的 FQCN（Fully Qualified Class Name）：

```json
{ "_class": "com.mongodb.course.m11.banking.model.Deposit" }
```

使用 `@TypeAlias` 可以控制 `_class` 的值：

```java
@TypeAlias("deposit")
public record Deposit(...) implements FinancialProduct { }
```

結果：
```json
{ "_class": "deposit" }
```

**優點：**
- 縮短儲存空間
- 更具可讀性
- 重構（重命名 class）不影響既有資料

---

## 選擇決策矩陣

| 考量因素 | Single Collection | Multiple Collections | Schema-per-Type |
|---------|:-:|:-:|:-:|
| 跨類型查詢頻率高 | ✅ 推薦 | ❌ 不適合 | ⚠️ 需自行處理 |
| 子類型差異大 | ⚠️ 可能浪費空間 | ✅ 推薦 | ⚠️ 可以 |
| Schema Validation 需求 | ⚠️ Union Schema | ✅ 各自獨立 | ✅ 用 oneOf |
| Spring Data 整合 | ✅ 原生支援 | ✅ 支援 | ❌ 需自行處理 |
| 跨語言存取 | ⚠️ 依賴 _class | ✅ 無特殊欄位 | ✅ 自訂格式 |
| 效能（大資料量） | ⚠️ 需注意索引 | ✅ 各自最佳化 | ⚠️ 需注意索引 |

**一般建議：** 預設選擇 **Single Collection**，除非有明確理由使用其他策略。

---

## M11 實作範例

本模組使用 **Single Collection** 策略：

- `m11_financial_products`：Deposit / Fund / InsuranceProduct
- `m11_policies`：AutoPolicy / LifePolicy / HealthPolicy

搭配 `@TypeAlias` 使用短名稱作為 discriminator：
- `"deposit"`, `"fund"`, `"insurance_product"`
- `"auto"`, `"life"`, `"health"`

詳細 Java 程式碼請參考 DOC-02。
