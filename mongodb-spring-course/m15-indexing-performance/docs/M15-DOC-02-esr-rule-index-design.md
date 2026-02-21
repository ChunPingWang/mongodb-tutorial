# M15-DOC-02：ESR 規則與索引設計

## ESR 規則（Equality-Sort-Range）

ESR 是 MongoDB 官方推薦的**複合索引欄位排列原則**，決定哪些欄位應該放在索引的什麼位置：

```
索引欄位順序：[ Equality ] → [ Sort ] → [ Range ]
```

| 位置 | 說明 | 查詢範例 |
|------|------|---------|
| **E（Equality）** | 精確匹配的欄位，放最前面 | `accountId = "ACC-000001"`, `type = "DEPOSIT"` |
| **S（Sort）** | 排序欄位，放中間 | `sort({ transactionDate: -1 })` |
| **R（Range）** | 範圍查詢的欄位，放最後面 | `transactionDate >= from AND <= to` |

### 為什麼這個順序很重要？

1. **Equality 放前面**：精確匹配可以最大程度縮小索引掃描範圍
2. **Sort 放中間**：在等值過濾後的子集上，索引已排序，避免記憶體排序
3. **Range 放最後**：範圍查詢會「打斷」索引的有序性，所以放最後影響最小

---

## M15 實際範例

### Banking Transaction ESR 索引

```java
// 索引：{ accountId: 1, type: 1, transactionDate: 1 }
var keys = new LinkedHashMap<String, Sort.Direction>();
keys.put("accountId", Sort.Direction.ASC);   // E: 精確匹配帳戶
keys.put("type", Sort.Direction.ASC);         // E: 精確匹配類型
keys.put("transactionDate", Sort.Direction.ASC); // R: 日期範圍
```

**查詢**：
```java
// 完整 ESR 查詢 — 效率最高
Criteria.where("accountId").is("ACC-000001")      // E
        .and("type").is(TransactionType.DEPOSIT)    // E
        .and("transactionDate").gte(from).lte(to)   // R
```

**explain() 結果**：
```
stage: IXSCAN
indexName: accountId_1_type_1_transactionDate_1
keysExamined: 50    ← 掃描的索引鍵數
docsExamined: 50    ← 檢查的文件數（= keysExamined，完美匹配！）
nReturned: 50       ← 回傳的文件數
```

當 `docsExamined == nReturned` 時，代表每一筆被掃描的文件都是有效結果，沒有浪費。

### 前綴匹配原則

同一個索引 `{accountId, type, transactionDate}` 也能服務只用前綴的查詢：

```java
// 只用 accountId（前綴 1 個欄位）
Criteria.where("accountId").is("ACC-000001")
// → 仍使用 IXSCAN，但掃描範圍較大

// 用 accountId + type（前綴 2 個欄位）
Criteria.where("accountId").is("ACC-000001")
        .and("type").is(TransactionType.DEPOSIT)
// → IXSCAN，掃描範圍更精確
```

---

## Covered Query（覆蓋查詢）

當查詢的**所有欄位**（filter + projection）都在索引中時，MongoDB 不需要回表讀取原始文件，直接從索引返回結果。

### 條件

1. 查詢過濾欄位在索引中
2. 投影欄位在索引中
3. **投影必須排除 `_id`**（除非 `_id` 也在索引中）

### M15 覆蓋查詢範例

```java
// 索引：{ accountId: 1, amount: 1 }
var keys = new LinkedHashMap<String, Sort.Direction>();
keys.put("accountId", Sort.Direction.ASC);
keys.put("amount", Sort.Direction.ASC);
indexManagementService.createCompoundIndex("m15_transactions", keys);

// 覆蓋查詢：filter 和 projection 都在索引內
var filter = new Document("accountId", "ACC-000001");
var projection = new Document("accountId", 1)
        .append("amount", 1)
        .append("_id", 0);  // 必須排除 _id！

mongoTemplate.getCollection("m15_transactions")
    .find(filter)
    .projection(projection);
```

**explain() 結果**：
```
stage: IXSCAN
totalDocsExamined: 0   ← 沒有讀取任何原始文件！
nReturned: 200
isIndexOnly: true      ← 覆蓋查詢確認
```

`docsExamined = 0` 表示完全從索引回傳，效能最佳。

---

## explain() 輸出解讀

### 執行 explain

```java
// ExplainAnalyzer 使用 runCommand 執行 explain
var findCommand = new Document("find", collectionName)
        .append("filter", filter);
if (projection != null) {
    findCommand.append("projection", projection);
}
var explainCommand = new Document("explain", findCommand)
        .append("verbosity", "executionStats");
Document result = mongoTemplate.getDb().runCommand(explainCommand);
```

### 關鍵指標

| 指標 | 含義 | 理想值 |
|------|------|--------|
| `executionStats.nReturned` | 回傳文件數 | — |
| `executionStats.totalKeysExamined` | 掃描的索引鍵數 | 接近 nReturned |
| `executionStats.totalDocsExamined` | 檢查的文件數 | 等於 nReturned（或 0 for covered） |
| `winningPlan.stage` | 查詢階段 | IXSCAN（非 COLLSCAN） |

### 效率判斷公式

```
Index Efficiency Ratio = nReturned / totalKeysExamined
// 接近 1.0 = 高效
// 遠小於 1.0 = 索引掃描了很多不需要的鍵

Document Efficiency = nReturned / totalDocsExamined
// 1.0 = 完美（沒有多餘文件檢查）
// 0 docsExamined = 覆蓋查詢（最佳）
```

### MongoDB 8.0 雙格式處理

MongoDB 8.0 使用 SBE（Slot-Based Engine），explain 輸出格式與經典格式不同：

```java
// ExplainResult.from() 處理兩種格式
Document winningPlan = queryPlanner.get("winningPlan", Document.class);

// SBE 格式：計畫包在 queryPlan 裡面
Document plan = winningPlan.get("queryPlan", Document.class);
if (plan == null) {
    // Classic 格式：直接在 winningPlan 上
    plan = winningPlan;
}
```

遞迴搜尋掃描階段：
```java
private static Document findScanStage(Document plan) {
    String stage = plan.getString("stage");
    if ("IXSCAN".equals(stage) || "COLLSCAN".equals(stage)) {
        return plan;
    }
    Document inputStage = plan.get("inputStage", Document.class);
    return inputStage != null ? findScanStage(inputStage) : null;
}
```

---

## 索引選擇性分析

**選擇性（Selectivity）** 是衡量索引欄位區分度的指標：

```
Selectivity = 不同值的數量 / 總文件數
```

| 欄位 | 不同值 | 總文件 | 選擇性 | 建議 |
|------|--------|--------|--------|------|
| `accountId` | 10 | 2000 | 0.005 | 適合等值查詢 |
| `type` | 4 | 2000 | 0.002 | 低選擇性，單獨索引效果差 |
| `transactionDate` | ~365 | 2000 | 0.18 | 適合範圍查詢 |
| `description` | ~2000 | 2000 | ~1.0 | 高選擇性，但通常用全文搜尋 |

**原則**：高選擇性欄位放在複合索引前面，但要結合 ESR 規則綜合考量。

---

## 常見反模式

### 1. 錯誤的欄位順序

```
❌ { transactionDate: 1, accountId: 1 }  — 範圍欄位在前
✅ { accountId: 1, transactionDate: 1 }  — 等值在前，範圍在後
```

### 2. 冗餘索引

```
❌ 同時有 { accountId: 1 } 和 { accountId: 1, type: 1 }
✅ 只保留 { accountId: 1, type: 1 }（前綴已包含 accountId 查詢）
```

### 3. 忘記排除 _id 的覆蓋查詢

```
❌ projection: { accountId: 1, amount: 1 }  — 隱含 _id: 1，無法覆蓋
✅ projection: { accountId: 1, amount: 1, _id: 0 }  — 完整覆蓋
```

### 4. 低選擇性欄位的單獨索引

```
❌ db.products.createIndex({ inStock: 1 })  — 只有 true/false，效果差
✅ 使用 Partial Index 取代：只索引 inStock=true 的文件
```
