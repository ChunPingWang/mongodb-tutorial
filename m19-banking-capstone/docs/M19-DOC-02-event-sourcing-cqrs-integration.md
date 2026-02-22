# M19 Event Sourcing 與 CQRS 整合實踐

## Event Store 設計

### 事件結構

所有帳戶事件實作 `AccountEvent` sealed interface，透過 `@TypeAlias` 標註多型類型：

```java
public sealed interface AccountEvent extends DomainEvent
        permits AccountOpened, FundsDeposited, FundsWithdrawn,
                FundsTransferredOut, FundsTransferredIn,
                InterestAccrued, AccountClosed {
}
```

每個事件以 Java record 實作，金額欄位使用 `@Field(targetType = FieldType.DECIMAL128)` 確保 MongoDB 中以 Decimal128 儲存。

### EventStore 服務

`EventStore` 提供泛型方法處理事件的附加（append）、載入（load）、快照（snapshot）：

- `append(event, collection)` — 插入單一事件
- `appendAll(events, collection)` — 批次插入
- `loadEvents(aggregateId, type, collection)` — 載入全部事件（按版本排序）
- `loadEventsAfterVersion(aggregateId, afterVersion, type, collection)` — 載入快照後的新事件
- `saveSnapshot/loadLatestSnapshot` — 快照管理

### 樂觀併發控制

透過 `{aggregateId: 1, version: 1}` 唯一複合索引，如果兩個並發操作試圖寫入相同版本號的事件，MongoDB 會拋出 `DuplicateKeyException`。

---

## BankAccount 聚合

### 事件溯源模式

BankAccount 完全由事件重建，不直接持久化狀態。`apply()` 方法使用 Java 23 switch 模式匹配處理所有 7 種事件：

```java
private void apply(AccountEvent event) {
    switch (event) {
        case AccountOpened e -> { /* 初始化帳戶 */ }
        case FundsDeposited e -> balance = balance.add(e.amount());
        case FundsWithdrawn e -> balance = balance.subtract(e.amount());
        case FundsTransferredOut e -> balance = balance.subtract(e.amount());
        case FundsTransferredIn e -> balance = balance.add(e.amount());
        case InterestAccrued e -> balance = balance.add(e.amount());
        case AccountClosed e -> closed = true;
    }
    this.version = event.version();
}
```

### 快照加速載入

每 10 個事件自動建立快照（`SNAPSHOT_INTERVAL = 10`）。載入帳戶時優先查找快照，再重播快照後的新事件：

```java
var snapshotOpt = eventStore.loadLatestSnapshot(accountId, AGGREGATE_TYPE);
if (snapshotOpt.isPresent()) {
    var account = BankAccount.fromSnapshot(snapshot.state());
    var newEvents = eventStore.loadEventsAfterVersion(accountId, snapshot.version(), ...);
    account.replayAfterSnapshot(newEvents);
    return account;
}
```

### BigDecimal 序列化注意事項

快照中的 BigDecimal 以 `toPlainString()` 存為字串，避免 MongoDB 在 Map 內無法正確處理 Decimal128 的問題。還原時以 `new BigDecimal(string)` 重建。

---

## Projector 同步投影

### AccountSummaryProjector

將每個帳戶事件同步投影到 `m19_account_summaries` 讀模型。關鍵技術：

- **Decimal128 包裝**：`$inc` 操作必須使用 `new Decimal128(bigDecimal)` 包裝，否則 BigDecimal 會被存為字串
- **原子更新**：使用 `Update.inc()` 和 `Update.set()` 組合，單次 updateFirst 完成

```java
case FundsDeposited e -> {
    var update = new Update()
            .inc("currentBalance", new Decimal128(e.amount()))
            .inc("totalTransactions", 1)
            .inc("depositCount", 1)
            .set("lastActivityAt", e.occurredAt())
            .set("projectedVersion", e.version());
    mongoTemplate.updateFirst(query, update, COLLECTION);
}
```

### TransactionLedgerProjector

每個事件產生一筆交易記錄。透過 `getLatestBalance()` 查詢最新餘額計算 `balanceAfter`：

```java
private BigDecimal getLatestBalance(String accountId) {
    var query = Query.query(Criteria.where("accountId").is(accountId))
            .with(Sort.by(Sort.Direction.DESC, "occurredAt"))
            .limit(1);
    var latest = mongoTemplate.findOne(query, TransactionLedgerDocument.class, COLLECTION);
    return latest != null ? latest.balanceAfter() : BigDecimal.ZERO;
}
```

---

## Transfer Saga 跨帳戶原子操作

### 三步驟設計

1. **DebitSourceAccountStep** — 從來源帳戶扣款（產生 FundsTransferredOut 事件）
2. **CreditTargetAccountStep** — 向目標帳戶入帳（產生 FundsTransferredIn 事件）
3. **RecordTransferStep** — 確認點（交易帳本已由前兩步的投影器自動建立）

### 補償機制

當任何步驟失敗（例如餘額不足），SagaOrchestrator 反向執行已完成步驟的補償：

- DebitSourceAccountStep 補償：存回扣除的金額（FundsDeposited）
- CreditTargetAccountStep 補償：提取已入帳的金額（FundsWithdrawn）

### Saga 日誌追蹤

SagaLog 記錄完整的執行軌跡：STARTED → RUNNING → COMPLETED/COMPENSATED。每個步驟有獨立的 StepLog，記錄成功、失敗或補償狀態。

---

## Read Model 查詢最佳化

### 餘額排名查詢

DashboardQueryService 利用 `{currentBalance: -1}` 索引高效執行 Top-N 查詢：

```java
public List<AccountSummaryDocument> topAccountsByBalance(int limit) {
    var query = new Query()
            .with(Sort.by(Sort.Direction.DESC, "currentBalance"))
            .limit(limit);
    return mongoTemplate.find(query, AccountSummaryDocument.class, SUMMARIES);
}
```

### CQRS 驅動貸款審核

LoanApplicationService 不直接查詢事件存儲，而是透過 DashboardQueryService 讀取 CQRS 讀模型取得帳戶餘額，再交由 DDD 規格模式評估：

- **MinimumBalanceSpec**：帳戶餘額 >= 貸款金額 × 10%
- **DebtToIncomeRatioSpec**：年收入 >= 年還款額 × 3（使用年金公式計算）

---

## Change Stream 事件通知

TransferNotificationListener 監聽 `m19_account_events` 集合的插入操作，過濾 `_class` 為 `FundsTransferredOut` 或 `FundsTransferredIn` 的事件，建立通知記錄。

這展示了 Change Stream 不只能監聽實體集合（如 M16），也能監聽事件存儲，實現事件驅動的通知系統。

---

## 測試策略

### 整合測試（11 個）

- AccountCommandServiceTest — 帳戶操作 + 投影驗證
- TransferSagaServiceTest — 轉帳成功、失敗補償、日誌記錄
- LoanApplicationServiceTest — 審核通過、拒絕、Schema 驗證
- AccountSummaryProjectorTest — 投影正確性、排名查詢

### BDD 測試（13 個場景）

四個 Feature 檔案覆蓋完整業務流程：
- 個人銀行帳戶生命週期（4 scenarios）
- 跨帳戶資金轉帳（3 scenarios）
- 貸款申請審核（3 scenarios）
- 銀行營運儀表板（3 scenarios）
