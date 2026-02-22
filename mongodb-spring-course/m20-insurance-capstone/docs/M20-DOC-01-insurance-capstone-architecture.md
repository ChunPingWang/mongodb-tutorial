# M20 — Insurance Capstone 架構概覽

## 模組定位

M20 是三個 Capstone 模組中的第二個（M19=Banking, M20=Insurance, M21=E-commerce），整合了先前學到的所有 MongoDB 進階模式：

| 模式 | 來源模組 | M20 應用 |
|------|---------|---------|
| Event Sourcing | M12 | ClaimProcess 理賠聚合體 |
| CQRS | M13 | ClaimDashboard + ClaimStatistics 讀取模型 |
| Saga | M14 | 4 步驟理賠結算流程 |
| DDD | M10 | UnderwritingApplication 核保聚合體 |
| Polymorphism | M11 | Policy sealed interface (Auto/Health/Life) |
| Schema Validation | M08 | m20_policies $jsonSchema 驗證 |
| Indexing | M15 | Compound index on dashboard |
| Change Streams | M16 | 監聽 CQRS 讀取模型狀態變更 |
| Observability | M17 | SlowQueryDetector + CommandListener |
| Transactions | M09 | MongoTransactionManager 配置 |

## 核心教學重點

M19 展示了 Event Sourcing 如何餵養 CQRS。M20 在此基礎上加入了 **多型（Polymorphism）**：

1. **多型 + 事件溯源**：Settlement Saga 的 `UpdatePolicyStep` 使用 switch pattern matching 對 sealed Policy 類型進行型別特定更新
2. **CQRS 讀取模型餵養 Saga 驗證**：`FraudCheckStep` 查詢 `ClaimStatistics`（CQRS 讀取模型）進行詐欺模式偵測
3. **Change Streams 監聽 CQRS 讀取模型**：監聽 `m20_claim_dashboard`（而非事件儲存庫）的狀態轉換
4. **DDD Specification 查詢 CQRS 讀取模型**：`ClaimHistoryRiskSpec` 讀取理賠統計來評估核保風險

---

## 七個 Collection 設計

```
m20_claim_events       ← Event Store（ClaimProcess 聚合體事件）
m20_snapshots          ← Snapshot Store（快照加速重播）
m20_policies           ← 多型 Policy 文件 + $jsonSchema 驗證
m20_claim_dashboard    ← CQRS 讀取模型（每筆理賠詳情 + 時間軸）
m20_claim_statistics   ← CQRS 讀取模型（按類別聚合統計）
m20_settlement_saga_logs ← Saga 編排追蹤紀錄
m20_claim_notifications  ← Change Stream 捕獲的通知
```

## 資料流架構

```
                     ┌─────────────────────┐
                     │  ClaimCommandService │
                     │  (file/investigate/  │
                     │   assess/approve/pay)│
                     └─────────┬───────────┘
                               │ events
                    ┌──────────▼──────────┐
                    │  m20_claim_events    │ ← Event Store
                    │  (聚合體ID+版本唯一) │
                    └──────────┬──────────┘
                               │ project
                    ┌──────────┴──────────┐
              ┌─────▼─────┐         ┌─────▼──────┐
              │ Dashboard │         │ Statistics │
              │ Projector │         │ Projector  │
              └─────┬─────┘         └─────┬──────┘
                    │                     │
           ┌───────▼───────┐    ┌────────▼────────┐
           │m20_claim_      │    │m20_claim_        │
           │dashboard       │    │statistics        │
           │(per-claim)     │    │(per-category)    │
           └───────┬───────┘    └────────┬────────┘
                   │                     │
                   │ Change Stream       │ FraudCheckStep
                   ▼                     ▼
           ┌───────────────┐    ┌────────────────┐
           │Notification   │    │Settlement Saga │
           │Listener       │    │(4 steps)       │
           └───────────────┘    └────────────────┘
```

## Settlement Saga 四步驟

| 步驟 | 名稱 | 動作 | 補償 |
|------|------|------|------|
| 1 | FRAUD_CHECK | 查詢統計+儀表板判斷詐欺風險 | 無（唯讀） |
| 2 | APPROVE_CLAIM | 透過 ES 核准理賠 | reject 理賠 |
| 3 | UPDATE_POLICY | 多型 switch 更新保單計數器 | 反向 $inc |
| 4 | NOTIFY_SETTLEMENT | 支付理賠 + 寫入通知 | 無 |

## 索引策略

- `m20_claim_events`: `{aggregateId: 1, version: 1}` 唯一複合索引（ES 樂觀並發）
- `m20_claim_dashboard`: `{category: 1, status: 1}` 複合索引 + `{lastUpdatedAt: -1}` 排序索引

## 測試規劃

- **11 個整合測試**：ClaimCommandService(3) + ClaimSettlementSaga(3) + ClaimProjection(2) + Underwriting(3)
- **13 個 BDD 場景**：claim-lifecycle(4) + claim-settlement(3) + insurance-dashboard(3) + underwriting(3)
- **總計 24 個測試**
