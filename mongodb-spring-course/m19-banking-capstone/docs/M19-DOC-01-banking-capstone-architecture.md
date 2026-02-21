# M19 Banking Capstone 架構概覽

## 課程定位

M19 是三個 Capstone 模組（M19–M21）中的第一個，聚焦於**個人銀行系統**。本模組整合了課程中所有核心 MongoDB 模式，展示這些模式如何在真實應用中協同運作。

### 整合地圖

| 模組 | 概念 | M19 中的應用 |
|------|------|-------------|
| M07 Aggregation | 聚合管道 | DashboardQueryService 排名查詢 |
| M08 Schema Validation | $jsonSchema | 貸款申請集合的嚴格驗證 |
| M09 Transactions | MongoTransactionManager | TransactionConfig 配置 |
| M10 DDD | 聚合根、值物件、規格模式 | LoanApplication、Applicant、MinimumBalanceSpec |
| M12 Event Sourcing | 事件存儲、快照、重播 | BankAccount 聚合、EventStore |
| M13 CQRS | 讀模型、投影器 | AccountSummaryProjector、TransactionLedgerProjector |
| M14 Saga | 編排式 Saga | TransferSagaService 三步驟轉帳 |
| M15 Indexing | 複合索引、ESR 規則 | IndexConfig（交易帳本、餘額排名）|
| M16 Change Streams | MessageListenerContainer | TransferNotificationListener 監聽事件存儲 |
| M17 Observability | CommandListener | SlowQueryDetector 慢查詢偵測 |

---

## 系統架構

```
┌─────────────────────────────────────────────────────────┐
│                    Command Side                          │
│  AccountCommandService ──→ BankAccount (ES Aggregate)   │
│  TransferSagaService ──→ SagaOrchestrator               │
│  LoanApplicationService ──→ LoanApplication (DDD)       │
└───────────────┬─────────────────────────────────────────┘
                │ Events
                ▼
┌─────────────────────────────────────────────────────────┐
│                    Event Store                           │
│  m19_account_events  (sealed interface + @TypeAlias)     │
│  m19_snapshots       (快照加速載入)                       │
└───────────────┬─────────────────────────────────────────┘
                │ Sync Projection
                ▼
┌─────────────────────────────────────────────────────────┐
│                    Query Side (CQRS)                     │
│  AccountSummaryProjector ──→ m19_account_summaries      │
│  TransactionLedgerProjector ──→ m19_transaction_ledger  │
│  DashboardQueryService (排名、統計)                       │
└─────────────────────────────────────────────────────────┘
```

---

## 七個 Collection 設計

### 1. m19_account_events — 事件存儲

儲存 BankAccount 聚合的所有領域事件，採用 sealed interface + @TypeAlias 實現多型序列化。

**事件類型（7 種）**：
- `AccountOpened` — 開戶
- `FundsDeposited` — 存款
- `FundsWithdrawn` — 提款
- `FundsTransferredOut` — 轉出（扣款方）
- `FundsTransferredIn` — 轉入（收款方）
- `InterestAccrued` — 利息加計
- `AccountClosed` — 結清

**索引**：`{aggregateId: 1, version: 1}` unique（樂觀併發控制）

### 2. m19_snapshots — 快照存儲

每 10 個事件建立一次快照，加速聚合重建。快照中 BigDecimal 以 `toPlainString()` 序列化。

### 3. m19_loan_applications — 貸款申請

DDD 聚合，帶有 $jsonSchema 驗證。必填欄位：applicantName、requestedAmount、termMonths、status、annualIncome。使用 `strictValidation().failOnValidationError()`。

### 4. m19_account_summaries — 帳戶摘要（CQRS 讀模型）

由 AccountSummaryProjector 維護，包含即時餘額、交易統計、利息累計。支援餘額排名查詢。

**索引**：`{currentBalance: -1}`

### 5. m19_transaction_ledger — 交易帳本（CQRS 讀模型）

由 TransactionLedgerProjector 維護，每筆事件產生一筆交易記錄，包含 balanceAfter。

**索引**：`{accountId: 1, occurredAt: -1}`（ESR 規則）

### 6. m19_transfer_saga_logs — Saga 日誌

追蹤三步驟轉帳 Saga 的執行狀態、每步驟結果、補償紀錄。

### 7. m19_transfer_notifications — 轉帳通知

由 Change Stream 監聽器捕獲的轉帳事件通知。

---

## Schema Validation + Indexing 的營運層設計

### Schema Validation 策略

貸款申請集合採用 **嚴格驗證**（strict + failOnError），確保所有文件都符合 schema。透過 `@PostConstruct` 在應用啟動時建立集合與驗證規則。

### 索引策略

- **事件存儲**：複合唯一索引保證事件版本不重複（樂觀併發）
- **交易帳本**：遵循 ESR 規則，accountId 做等值匹配、occurredAt 做排序
- **帳戶摘要**：currentBalance 降序索引，支援 Top-N 排名查詢

---

## 跨模式互動亮點

### Event Sourcing 驅動 CQRS

每當 AccountCommandService 儲存事件後，同步呼叫 AccountSummaryProjector 和 TransactionLedgerProjector 更新讀模型。這確保讀模型始終與事件存儲保持一致。

### CQRS 讀模型驅動 DDD 規格

LoanApplicationService 審核貸款時，透過 DashboardQueryService 查詢 m19_account_summaries（CQRS 讀模型）取得帳戶餘額，再交由 MinimumBalanceSpec 和 DebtToIncomeRatioSpec 評估。

### Saga 編排跨帳戶轉帳

TransferSagaService 使用三步驟 Saga：DebitSourceAccountStep → CreditTargetAccountStep → RecordTransferStep。每步驟操作事件存儲並更新投影。失敗時反向補償。

### Change Stream 監聽事件存儲

TransferNotificationListener 監聽 m19_account_events 集合的變更，過濾轉帳事件並儲存通知。
