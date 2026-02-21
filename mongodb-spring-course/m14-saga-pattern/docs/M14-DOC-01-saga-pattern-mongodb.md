# M14-DOC-01: Saga 模式與 MongoDB 實作

## 分散式交易的挑戰

在微服務或 DDD 架構中，一個業務操作經常需要跨越多個 Aggregate（聚合根）。由於每個 Aggregate 擁有獨立的交易邊界，我們無法使用單一資料庫交易來保證一致性。

### 傳統方案：Two-Phase Commit (2PC)

| 特性 | 2PC | Saga |
|------|-----|------|
| 一致性 | 強一致性 | 最終一致性 |
| 效能 | 低（鎖定資源） | 高（無全域鎖） |
| 可用性 | 低（協調者是單點故障） | 高（各步驟獨立執行） |
| 複雜度 | 協議層面複雜 | 業務層面補償邏輯 |
| 適用場景 | 同質資料庫 | 異質系統、微服務 |

2PC 在分散式系統中的問題：
- **阻塞問題**：參與者在等待協調者回應期間持有鎖
- **單點故障**：協調者當機導致所有參與者阻塞
- **效能瓶頸**：跨服務網路延遲放大鎖定時間

### Saga 模式

Saga 將一個長交易拆成一系列**本地交易**（Local Transaction），每個步驟都有對應的**補償操作**（Compensating Action）。當某個步驟失敗時，反向執行已完成步驟的補償操作，將系統恢復到一致狀態。

```
正向執行：T1 → T2 → T3 → T4 ✓ 完成
失敗補償：T1 → T2 → T3✗ → C2 → C1（反向補償）
```

## Orchestration vs Choreography

### Orchestration（編排）

一個中央**編排器**（Orchestrator）控制整個 Saga 的執行流程：

```
Orchestrator → Step1.execute()
             → Step2.execute()
             → Step3.execute() ✗ 失敗
             → Step2.compensate()
             → Step1.compensate()
```

**優點**：
- 流程集中管理，易於理解和除錯
- 步驟間的依賴關係清晰
- 容易追蹤 Saga 執行狀態

**缺點**：
- 編排器可能成為瓶頸
- 步驟與編排器耦合

### Choreography（協同）

每個步驟透過事件通知下一個步驟：

```
Step1 --事件--> Step2 --事件--> Step3
  ↑               ↑
  └──補償事件──────┘
```

**優點**：
- 無中央協調者，更加解耦
- 各服務自主決定處理方式

**缺點**：
- 流程分散在各服務中，難以追蹤
- 循環依賴風險
- 除錯困難

### 本課程選擇：Orchestration

M14 採用 Orchestration 模式，原因：
1. 教學目的：集中式邏輯更容易理解
2. 單一 MongoDB 實例：無需跨服務事件匯流排
3. 可觀測性：SagaLog 完整記錄每個步驟的執行狀態

## MongoDB 作為 Saga Log 儲存

MongoDB 非常適合儲存 Saga 執行日誌：

1. **文件模型**：Saga 的步驟記錄自然適合嵌入式陣列
2. **彈性結構**：不同 Saga 類型的 context 可包含不同欄位
3. **原子更新**：`$set` 搭配陣列索引可精準更新單一步驟

### SagaLog 文件結構

```json
{
  "_id": "550e8400-e29b-41d4-a716-446655440000",
  "sagaType": "ORDER_SAGA",
  "status": "COMPLETED",
  "currentStepIndex": 3,
  "steps": [
    {
      "stepName": "PLACE_ORDER",
      "status": "SUCCEEDED",
      "executedAt": "2024-01-15T10:00:00Z",
      "compensatedAt": null,
      "errorMessage": null
    },
    {
      "stepName": "RESERVE_INVENTORY",
      "status": "SUCCEEDED",
      "executedAt": "2024-01-15T10:00:01Z",
      "compensatedAt": null,
      "errorMessage": null
    },
    {
      "stepName": "PROCESS_PAYMENT",
      "status": "SUCCEEDED",
      "executedAt": "2024-01-15T10:00:02Z",
      "compensatedAt": null,
      "errorMessage": null
    },
    {
      "stepName": "CONFIRM_ORDER",
      "status": "SUCCEEDED",
      "executedAt": "2024-01-15T10:00:03Z",
      "compensatedAt": null,
      "errorMessage": null
    }
  ],
  "context": {
    "customerId": "C001",
    "productId": "LAPTOP-001",
    "orderId": "...",
    "paymentId": "...",
    "totalAmount": 40000
  },
  "startedAt": "2024-01-15T10:00:00Z",
  "completedAt": "2024-01-15T10:00:03Z",
  "failureReason": null
}
```

### 步驟更新策略

使用 `$set` 搭配陣列索引精準更新個別步驟：

```java
// 更新第 i 個步驟的狀態
var update = new Update()
    .set("steps." + index, stepLog)
    .set("currentStepIndex", index);
mongoTemplate.updateFirst(query, update, SagaLog.class);
```

這比替換整個 `steps` 陣列更有效率且更安全。

## 與其他模組的關係

| 模組 | 關係 |
|------|------|
| M09 (Transactions) | Saga 解決的是**跨 Aggregate** 的一致性，單一 Aggregate 內仍可用 Transaction |
| M10 (DDD Aggregates) | Saga 協調多個 Aggregate 的狀態變更 |
| M12 (Event Sourcing) | Event Store 可作為 Saga 步驟的事件來源 |
| M13 (CQRS) | Saga 完成後可觸發 Read Model 更新 |

## 金額處理

本模組使用 `long` 型別表示金額（以最小貨幣單位計），簡化 Saga 模式的教學重點。在實際專案中，建議搭配 M05 介紹的 `BigDecimal` + `@Field(targetType = DECIMAL128)` 方案處理精確計算。

## 重點整理

1. **Saga 模式**解決跨 Aggregate 的分散式交易問題
2. **Orchestration** 提供集中式的流程控制，易於追蹤和除錯
3. MongoDB 文件模型天然適合儲存 **SagaLog**（嵌入式步驟陣列）
4. 每個步驟必須實作 `execute()` 和 `compensate()` 兩個方法
5. 補償操作按照**反向順序**執行，確保一致性
6. SagaLog 提供完整的執行軌跡，支援監控和故障恢復
