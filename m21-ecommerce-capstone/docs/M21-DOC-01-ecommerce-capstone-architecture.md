# M21 — E-commerce Capstone 架構概覽

## 模組定位

M21 是 MongoDB Spring 課程的第三個也是最後一個 Capstone 模組，整合 M01–M18 所有進階模式，建構一個完整的**電商訂單履約系統（E-commerce Order Fulfillment System）**。

| Capstone | 領域 | 獨特整合 |
|----------|------|---------|
| M19 | 個人銀行 | ES 同步餵給 CQRS；Change Stream 監聽 Event Store |
| M20 | 保險理賠 | 多型 + ES；CQRS 餵給 Saga（簡單查詢）；DDD 建立多型實體 |
| **M21** | **電商履約** | **聚合管線（Aggregation Pipeline）驅動 Saga + DDD Spec 業務邏輯** |

## 系統架構

```
┌─────────────────────────────────────────────────────┐
│                  E-commerce System                   │
├──────────┬──────────┬───────────┬───────────────────┤
│  Order   │ Product  │  Listing  │   Fulfillment     │
│   (ES)   │ (Poly)   │  (DDD)    │    (Saga)         │
├──────────┴──────────┴───────────┴───────────────────┤
│              CQRS Projection Layer                   │
│   OrderDashboardProjector  │  SalesStatisticsProjector│
├──────────────────────────────────────────────────────┤
│     Infrastructure: EventStore, SagaOrchestrator     │
├──────────────────────────────────────────────────────┤
│  Cross-cutting: Schema Validation, Indexing,         │
│  Change Streams, Observability                       │
└──────────────────────────────────────────────────────┘
```

## 7 個 Collection

| Collection | 用途 | 來源模式 |
|---|---|---|
| `m21_order_events` | Order 聚合的事件儲存 | M12 Event Sourcing |
| `m21_snapshots` | 快照儲存 | M12 |
| `m21_products` | 多型商品（Electronics/Clothing/Food）+ $jsonSchema 驗證 | M11 多型 + M08 Schema Validation |
| `m21_order_dashboard` | CQRS 讀模型（訂單明細 + 時間軸） | M13 CQRS |
| `m21_sales_statistics` | CQRS 讀模型（類別銷售統計） | M13 CQRS |
| `m21_fulfillment_saga_logs` | Saga 流程追蹤 | M14 Saga |
| `m21_order_notifications` | Change Stream 捕獲的通知 | M16 Change Streams |

## 資料流

### 1. 訂單下單流程

```
Customer → OrderCommandService.placeOrder()
  → Order.place() 建立 OrderPlaced 事件
  → EventStore.appendAll() 寫入 m21_order_events
  → OrderDashboardProjector.project() 寫入 m21_order_dashboard
  → SalesStatisticsProjector.project() upsert m21_sales_statistics
```

### 2. 訂單履約 Saga

```
OrderFulfillmentSagaService.executeFulfillment()
  → Step 1: ValidateStockStep
      - 檢查商品庫存
      - 【獨特】Aggregation Pipeline 查詢 m21_order_dashboard 偵測大量採購
  → Step 2: ReserveInventoryStep
      - $inc 減少 m21_products 庫存
      - Order.reserveInventory() → ES event → 同步 Projection
  → Step 3: ProcessPaymentStep
      - 檢查付款上限 1,000,000
      - Order.processPayment() → ES event → 同步 Projection
  → Step 4: ConfirmOrderStep
      - Order.confirm() → ES event → 同步 Projection
```

### 3. 商品上架 DDD 流程

```
ProductListingService.submit() → ProductListing（純領域物件）
ProductListingService.review()
  → 【獨特】OrderQueryService.computeCategoryMetrics()
      使用 Aggregation Pipeline 計算 m21_order_dashboard 上的類別指標
  → CategoryProfitabilitySpec.isSatisfiedBy(metrics)
  → MinimumStockSpec.isSatisfiedBy(stock)
  → 若 APPROVED → 建立多型 Product 實體
```

## 模式整合對照表

| 模式 | M21 對應元件 | 原始模組 |
|------|-------------|---------|
| Event Sourcing | Order aggregate + OrderEvent sealed interface | M12 |
| CQRS | OrderDashboardProjector + SalesStatisticsProjector | M13 |
| Saga | OrderFulfillmentSagaService (4 步驟) | M14 |
| DDD | ProductListing + Specification Pattern | M10 |
| Polymorphism | Product sealed interface + 3 final classes | M11 |
| Schema Validation | $jsonSchema on m21_products | M08 |
| Indexing | Compound indexes on events + dashboard | M15 |
| Change Streams | OrderNotificationListener | M16 |
| Observability | SlowQueryDetector + CommandListener | M17 |
| Transactions | MongoTransactionManager | M09 |

## 與 M19/M20 的差異

### Event Sourcing
- M19: 7 事件類型（BankAccount）
- M20: 5 事件類型（ClaimProcess）+ 多型保單
- **M21: 6 事件類型（Order）+ 嵌套 OrderLine 記錄**

### CQRS
- M19/M20: 簡單的 findById() 查詢讀模型
- **M21: Aggregation Pipeline 查詢讀模型（$match + $group + $sum + $avg）**

### Saga
- M19: 3 步驟轉帳 Saga
- M20: 4 步驟理賠結算 Saga
- **M21: 4 步驟履約 Saga + 聚合管線偵測大量採購**

### DDD
- M19: LoanApplication + IncomeToPaymentRatioSpec
- M20: UnderwritingApplication + AgeEligibilitySpec + ClaimHistoryRiskSpec
- **M21: ProductListing + CategoryProfitabilitySpec（聚合管線驅動）+ MinimumStockSpec**
