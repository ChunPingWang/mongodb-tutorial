# M14-DOC-02: Saga 編排器設計與補償策略

## SagaOrchestrator 架構

### 核心元件

```
┌─────────────────────────────────────────────┐
│              SagaOrchestrator               │
│                                             │
│  execute(sagaType, steps, context)          │
│    ├── 建立 SagaLog                         │
│    ├── 依序執行 steps                       │
│    ├── 成功 → COMPLETED                     │
│    └── 失敗 → 反向補償 → COMPENSATED/FAILED │
└─────────────────────────────────────────────┘
         │                    │
    ┌────┴────┐         ┌────┴────┐
    │SagaStep │         │SagaLog  │
    │ execute │         │Repository│
    │compensate│        │  save   │
    └─────────┘         │  update │
                        └─────────┘
```

### SagaStep 介面

```java
public interface SagaStep {
    String name();                    // 步驟名稱（用於日誌記錄）
    void execute(SagaContext context); // 正向執行
    void compensate(SagaContext context); // 補償操作
}
```

設計原則：
- **冪等性**：同一步驟重複執行應產生相同結果
- **獨立性**：每個步驟只負責自己的業務邏輯
- **可補償性**：每個正向操作都有對應的反向操作

### SagaContext 共享上下文

```java
public final class SagaContext {
    private final Map<String, Object> data;

    public void put(String key, Object value);
    public <T> T get(String key, Class<T> type);
    public Map<String, Object> toMap(); // 不可變快照
}
```

步驟間透過 `SagaContext` 傳遞資料：
- `PlaceOrderStep` 產生 `orderId`，後續步驟讀取
- `ProcessPaymentStep` 產生 `paymentId`，補償時使用
- `toMap()` 回傳不可變快照，用於持久化到 SagaLog

## 狀態機

```
STARTED ──→ RUNNING ──→ COMPLETED
                │
                ↓ (步驟失敗)
           COMPENSATING
                │
         ┌──────┴──────┐
         ↓             ↓
    COMPENSATED     FAILED
   (全部補償成功)  (補償也失敗)
```

| 狀態 | 說明 |
|------|------|
| STARTED | SagaLog 建立完成 |
| RUNNING | 正在執行步驟 |
| COMPLETED | 所有步驟成功完成 |
| COMPENSATING | 正在執行補償操作 |
| COMPENSATED | 所有補償操作成功 |
| FAILED | 補償操作也失敗（需要人工介入） |

## 正向執行流程

```
1. 產生 sagaId (UUID)
2. 建立 SagaLog (STARTED)
3. 更新狀態為 RUNNING
4. For each step[i]:
   a. 更新 currentStepIndex = i
   b. 執行 step.execute(context)
   c. 成功 → 更新 StepLog 為 SUCCEEDED，快照 context
   d. 失敗 → 更新 StepLog 為 FAILED，進入補償流程
5. 全部成功 → 更新狀態為 COMPLETED
```

## 反向補償策略

補償操作的關鍵原則：**反向順序**。

```
執行順序：Step1 → Step2 → Step3(失敗)
補償順序：          Step2.compensate() → Step1.compensate()
```

### 為什麼要反向？

因為後面的步驟可能依賴前面步驟的結果。例如：
1. PlaceOrder（建立訂單）
2. ReserveInventory（預留庫存，依賴訂單）
3. ProcessPayment（付款，依賴庫存預留）

如果 Step3 失敗，先補償 Step2（釋放庫存），再補償 Step1（取消訂單）。

### 補償失敗處理

```java
for (int i = lastCompletedIndex; i >= 0; i--) {
    try {
        step.compensate(context);
        // 更新為 COMPENSATED
    } catch (Exception e) {
        // 記錄錯誤但繼續補償其他步驟
        compensationFailed = true;
    }
}

if (compensationFailed) {
    updateStatus(FAILED);  // 需要人工介入
} else {
    updateStatus(COMPENSATED);
}
```

重要：即使某個補償操作失敗，仍會繼續嘗試補償其他步驟。這避免了「補償連鎖失敗」的問題。

## 電商訂單 Saga（4 步驟）

```
┌──────────────┐    ┌──────────────────┐    ┌─────────────────┐    ┌──────────────┐
│ PlaceOrder   │ →  │ ReserveInventory │ →  │ ProcessPayment  │ →  │ ConfirmOrder │
│              │    │                  │    │                 │    │              │
│ 建立訂單     │    │ $inc qty:-N      │    │ 建立付款記錄    │    │ 更新為已確認 │
│ status=      │    │ $inc reserved:+N │    │ status=         │    │ $inc         │
│ PENDING      │    │ order→RESERVED   │    │ COMPLETED       │    │ reserved:-N  │
└──────────────┘    └──────────────────┘    └─────────────────┘    └──────────────┘
│ compensate   │    │ compensate       │    │ compensate      │    │ compensate   │
│ order→       │    │ $inc qty:+N      │    │ payment→        │    │ order→       │
│ CANCELLED    │    │ $inc reserved:-N │    │ REFUNDED        │    │ PAYMENT_     │
└──────────────┘    └──────────────────┘    └─────────────────┘    │ PROCESSED    │
                                                                   └──────────────┘
```

### 失敗模擬

| 步驟 | 失敗條件 | 類型 |
|------|---------|------|
| ReserveInventory | `inventory.quantity < requested` | 自然業務規則 |
| ProcessPayment | `totalAmount >= 100,000` | 確定性閾值 |

## 保險理賠結算 Saga（4 步驟）

```
┌──────────────┐    ┌──────────────────┐    ┌──────────────┐    ┌──────────────────┐
│ ApproveClaim │ →  │ CreateSettlement  │ →  │ UpdatePolicy │ →  │ NotifyCustomer  │
│              │    │ Payment           │    │              │    │                 │
│ claim→       │    │ 建立結算付款      │    │ $inc paid:   │    │ 建立通知記錄    │
│ APPROVED     │    │ type=CLAIM_       │    │ +amount      │    │ status=SENT     │
│              │    │ SETTLEMENT        │    │ claim→       │    │ claim→NOTIFIED  │
│              │    │                   │    │ POLICY_      │    │                 │
│              │    │                   │    │ UPDATED      │    │                 │
└──────────────┘    └──────────────────┘    └──────────────┘    └──────────────────┘
│ compensate   │    │ compensate        │    │ compensate   │    │ compensate      │
│ claim→       │    │ payment→          │    │ $inc paid:   │    │ notification→   │
│ PENDING      │    │ REFUNDED          │    │ -amount      │    │ FAILED          │
└──────────────┘    └──────────────────┘    │ claim→       │    └──────────────────┘
                                            │ APPROVED     │
                                            └──────────────┘
```

### 失敗模擬

| 步驟 | 失敗條件 | 類型 |
|------|---------|------|
| UpdatePolicy | `paidClaimsTotal + amount > coverageAmount` | 自然業務規則 |
| NotifyCustomer | `claimantName.startsWith("FAIL_")` | 測試鉤子 |

## Step 設計原則

### 1. 單一職責

每個 Step 只負責一個操作和其對應的補償：

```java
public final class ReserveInventoryStep implements SagaStep {
    // execute: 扣減庫存
    // compensate: 恢復庫存
}
```

### 2. 使用 MongoDB 原子操作

善用 `$inc`、`$set` 等原子操作，避免讀取-修改-寫入的競爭條件：

```java
mongoTemplate.updateFirst(
    Query.query(Criteria.where("_id").is(productId)),
    new Update().inc("quantity", -quantity).inc("reservedQuantity", quantity),
    InventoryItem.class
);
```

### 3. 補償的空值防護

補償操作必須處理步驟部分執行的情況：

```java
@Override
public void compensate(SagaContext context) {
    String paymentId = context.get("paymentId", String.class);
    if (paymentId != null) {  // 付款記錄可能尚未建立
        // 執行退款
    }
}
```

### 4. 確定性失敗模擬

使用業務規則或固定閾值模擬失敗，避免隨機性：

```java
static final long PAYMENT_LIMIT = 100_000;

if (totalAmount >= PAYMENT_LIMIT) {
    throw new IllegalStateException("Payment exceeds limit");
}
```

## 測試策略

### 純單元測試

`SagaContextTest`：測試上下文的讀寫、型別轉換、不可變快照。

### 編排器整合測試

使用匿名 `SagaStep` 實作測試編排器邏輯，不依賴特定業務：

```java
private SagaStep failStep(String name) {
    return new SagaStep() {
        @Override public String name() { return name; }
        @Override public void execute(SagaContext ctx) {
            throw new RuntimeException(name + " failed");
        }
        @Override public void compensate(SagaContext ctx) { }
    };
}
```

### 業務 Saga 整合測試

驗證完整的業務流程：
- 正常路徑：所有步驟成功完成
- 失敗路徑：觸發補償並驗證資料回滾
- 日誌驗證：SagaLog 記錄正確的步驟狀態

### BDD 驗收測試

使用 Cucumber 撰寫業務場景，以繁體中文描述業務流程：

```gherkin
Scenario: 付款金額超過限額觸發補償
  Given 商品 "PHONE-001" 名稱 "手機" 庫存為 5
  When 客戶 "C002" 下單購買 "PHONE-001" 數量 2 單價 60000 元
  Then Saga 狀態為 "COMPENSATED"
  And 訂單狀態為 "CANCELLED"
  And 商品 "PHONE-001" 庫存為 5
```

## 重點整理

1. **SagaOrchestrator** 是通用的執行引擎，不包含業務邏輯
2. **SagaStep** 介面簡潔（name/execute/compensate），各業務步驟實作
3. **SagaContext** 作為步驟間的資料傳遞媒介，快照到 SagaLog
4. 補償策略：**反向順序**、**繼續執行**（即使某步驟補償失敗）
5. 狀態機明確：STARTED → RUNNING → COMPLETED | COMPENSATING → COMPENSATED | FAILED
6. MongoDB `$set` + 陣列索引精準更新個別步驟記錄
7. 確定性失敗模擬讓測試可預測、可重現
