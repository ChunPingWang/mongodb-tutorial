# M13-DOC-01：CQRS 模式與 MongoDB 實作

## 什麼是 CQRS？

CQRS（Command Query Responsibility Segregation，命令查詢職責分離）是一種將**寫入操作**（Command）與**讀取操作**（Query）分離到不同模型的架構模式。

### 傳統單一模型 vs CQRS

在傳統架構中，同一個資料模型同時負責寫入與讀取：

```
[Client] → [Service] → [Single Model] → [Database]
```

CQRS 將兩者分離：

```
[Client] → [Command Service] → [Write Model (Event Store)] → [Database]
                                        ↓ (Projection)
[Client] → [Query Service]  → [Read Model (Denormalized)] → [Database]
```

### 核心概念

| 概念 | 說明 |
|------|------|
| **Command Side** | 處理業務邏輯、產生領域事件、確保資料一致性 |
| **Event Store** | 儲存所有領域事件，作為系統的 Source of Truth |
| **Projector** | 訂閱事件並更新讀取模型 |
| **Read Model** | 針對查詢最佳化的反正規化資料結構 |
| **Query Side** | 直接查詢讀取模型，不需任何聚合或 JOIN |

## 為什麼 MongoDB 適合 CQRS？

### 1. 靈活的文件結構

MongoDB 的 document model 天然適合反正規化的讀取模型。不同的查詢需求可以有不同的文件結構，無需遵守正規化約束。

### 2. 原子更新運算子

MongoDB 提供強大的原子更新運算子，讓 Projector 能高效地進行增量更新：

```java
// $inc：原子遞增，無需先讀後寫
var update = new Update()
    .inc("currentBalance", new Decimal128(amount))
    .inc("depositCount", 1)
    .set("lastActivityAt", occurredAt);
mongoTemplate.updateFirst(query, update, COLLECTION);
```

```java
// $push：原子新增陣列元素
var update = new Update()
    .push("timeline", new TimelineEntry("ClaimApproved", now, "核准理賠"))
    .inc("eventCount", 1);
```

```java
// upsert：不存在則建立，存在則更新
mongoTemplate.upsert(query, update, COLLECTION);
```

### 3. 多樣化的查詢能力

讀取模型一旦建立，查詢就變得極為簡單：

```java
// 單一文件讀取（O(1)）
mongoTemplate.findById(accountId, AccountSummaryDocument.class);

// 範圍查詢
Query.query(Criteria.where("currentBalance").gte(new Decimal128(threshold)));

// 排序 + 分頁
new Query().with(Sort.by(DESC, "currentBalance")).limit(10);

// 聚合計算
Aggregation.newAggregation(group().sum("currentBalance").as("total"));
```

## 同步 Projection vs 非同步 Projection

### 同步 Projection（本模組採用）

```java
public BankAccount openAccount(String accountId, String holder, BigDecimal balance, String currency) {
    var account = BankAccount.open(accountId, holder, balance, currency);
    var events = persistEvents(account);       // 1. 寫入事件
    events.forEach(this::projectEvent);         // 2. 同步投影
    return account;
}
```

**優點**：
- 讀取模型即時一致（Strong Consistency）
- 實作簡單，無需額外基礎設施
- 容易除錯與測試

**缺點**：
- 命令延遲包含投影時間
- 投影失敗會影響命令成功

### 非同步 Projection

```
[Command] → [Event Store] → [Change Stream / Message Queue] → [Projector] → [Read Model]
```

**優點**：
- 命令與投影解耦
- 可獨立擴展讀寫端

**缺點**：
- 最終一致性（Eventual Consistency）
- 需要額外基礎設施（Change Streams、Message Queue）
- 複雜的錯誤處理

> 本模組使用同步 Projection 以降低複雜度。M16 將介紹使用 MongoDB Change Streams 的非同步方案。

## 讀取模型是可丟棄的

CQRS 最重要的設計原則之一：**讀取模型可以隨時重建**。

```java
public void rebuildBankingProjections() {
    var events = eventStore.loadAllEvents(AccountEvent.class, ACCOUNT_EVENTS);
    accountSummaryProjector.rebuildAll(events);        // 清除 + 重播
    transactionHistoryProjector.rebuildAll(events);
}
```

這意味著：
1. 讀取模型的 schema 可以自由變更
2. 新增查詢需求時，可以新增讀取模型並從事件重建
3. 資料損壞時，可以從 Event Store 完全恢復

## 本模組架構總覽

```
┌─────────────────── Write Side ───────────────────┐
│                                                   │
│  BankAccount ──→ AccountEvent ──→ EventStore     │
│  ClaimProcess ──→ ClaimEvent  ──→ EventStore     │
│                                                   │
└───────────────────────┬───────────────────────────┘
                        │ (synchronous projection)
┌───────────────────────▼───────────────────────────┐
│                                                   │
│  AccountSummaryProjector    → m13_account_summaries│
│  TransactionHistoryProjector→ m13_transaction_history│
│  ClaimDashboardProjector    → m13_claim_dashboards│
│  ClaimStatisticsProjector   → m13_claim_statistics│
│                                                   │
└───────────────────────┬───────────────────────────┘
                        │
┌───────────────────────▼───────────────────────────┐
│                   Read Side                       │
│                                                   │
│  BankAccountQueryService  ← 直接查詢讀取模型      │
│  ClaimQueryService        ← 直接查詢讀取模型      │
│                                                   │
└───────────────────────────────────────────────────┘
```

## MongoDB Collections

| Collection | 類型 | 用途 |
|-----------|------|------|
| `m13_account_events` | Event Store | 銀行帳戶事件流 |
| `m13_claim_events` | Event Store | 保險理賠事件流 |
| `m13_account_summaries` | Read Model | 帳戶摘要（餘額、交易統計） |
| `m13_transaction_history` | Read Model | 交易歷史（逐筆記錄） |
| `m13_claim_dashboards` | Read Model | 理賠儀表板（狀態、時間線） |
| `m13_claim_statistics` | Read Model | 理賠統計（依類別彙總） |
