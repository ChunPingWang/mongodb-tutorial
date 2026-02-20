# M09-DOC-01：MongoDB Transaction 深度解析

## 1. 為什麼需要多文件交易？

在 MongoDB 4.0 之前，MongoDB 僅支援**單一文件**的原子性操作。這對於嵌入式資料模型（Embedded Document）已經足夠——因為一次 `updateOne` 就能原子性地修改整份文件。

然而，當業務需求涉及**跨多個文件**或**跨多個集合**的操作時（例如銀行轉帳：同時扣款與入帳），就需要多文件交易（Multi-Document Transaction）來保證 ACID 特性。

### 典型場景

| 場景 | 涉及文件 | 為何需要交易 |
|------|----------|-------------|
| 銀行轉帳 | 2 個帳戶文件 + 1 筆轉帳記錄 | 扣款/入帳/記錄必須全部成功或全部失敗 |
| 保險核保 | 保單 + 收費排程 + 客戶更新 | 3 個集合必須一致 |
| 電商下單 | 訂單 + 庫存扣減 + 支付記錄 | 避免超賣 |
| 社群發文 | 貼文 + 動態牆 + 通知 | 避免幽靈通知 |

## 2. MongoDB ACID 保證

MongoDB 4.0+ 在 Replica Set 上支援多文件交易，4.2+ 擴展到 Sharded Cluster。

| 特性 | MongoDB 實作 |
|------|-------------|
| **Atomicity** | 交易中的所有操作全部提交或全部回滾 |
| **Consistency** | 交易前後資料滿足所有約束（Schema Validation、唯一索引） |
| **Isolation** | Snapshot Isolation——交易看到的是開始時的快照 |
| **Durability** | 搭配 `writeConcern: "majority"` 確保寫入多數節點 |

### WiredTiger MVCC

MongoDB 的 WiredTiger 儲存引擎使用 **MVCC（Multi-Version Concurrency Control）**：

- 每次寫入產生新版本，不覆蓋舊版本
- 讀取操作使用交易開始時的快照
- 提交時檢查是否有衝突（Optimistic Concurrency）
- 衝突時拋出 `WriteConflict`，由應用端重試

## 3. Replica Set 前提條件

MongoDB 交易**必須**在 Replica Set 上運行。單節點 Standalone 模式不支援交易。

```
// 這也是為什麼 Testcontainers 的 MongoDBContainer 預設啟動為單節點 Replica Set
// --replSet rs0 --bind_ip_all
```

Testcontainers 1.20.4 的 `MongoDBContainer("mongo:8.0")` 預設以 `--replSet` 方式啟動，因此在測試環境中可以直接使用交易功能。

## 4. 交易生命週期

```
ClientSession session = client.startSession();
session.startTransaction(txOptions);

try {
    // 執行多個操作（insert、update、delete...）
    collection1.insertOne(session, doc1);
    collection2.updateOne(session, filter, update);

    session.commitTransaction();    // 全部提交
} catch (Exception e) {
    session.abortTransaction();     // 全部回滾
} finally {
    session.close();
}
```

### 關鍵參數

| 參數 | 說明 | 預設值 |
|------|------|--------|
| `readConcern` | 讀取一致性級別 | `snapshot`（交易內） |
| `writeConcern` | 寫入確認級別 | `majority` |
| `readPreference` | 讀取偏好（Primary/Secondary） | `primary` |
| `maxCommitTimeMS` | 最大提交等待時間 | 無限制 |

## 5. 交易限制

| 限制 | 說明 |
|------|------|
| **60 秒超時** | 預設交易最多持續 60 秒（可調整 `transactionLifetimeLimitSeconds`） |
| **16MB oplog 限制** | 單次交易的所有操作產生的 oplog 不能超過 16MB |
| **不支援 DDL** | 交易內不能建立/刪除集合或索引 |
| **集合必須已存在** | 交易內的 `insert` 不能隱式建立集合（MongoDB 4.4+ 放寬此限制） |
| **WriteConflict** | 兩個交易寫同一文件 → 後者收到 WriteConflict |

## 6. 與關聯式資料庫的比較

| 面向 | MongoDB | PostgreSQL/MySQL |
|------|---------|-----------------|
| 隔離級別 | Snapshot Isolation（固定） | 可選（RC/RR/Serializable） |
| 鎖策略 | Optimistic（MVCC + 衝突偵測） | Pessimistic（行鎖/表鎖） |
| 超時行為 | 自動 abort | 等待鎖釋放或 deadlock 偵測 |
| DDL 交易 | 不支援 | PostgreSQL 支援 |
| 分散式交易 | 4.2+ 原生支援 | 需要 2PC/XA |

## 7. 何時使用（與不使用）交易

### 適合使用交易的場景

- 金融操作（轉帳、支付）
- 跨集合的原子更新
- 需要「全有或全無」語義的批次操作

### 不需要使用交易的場景

- **單一文件更新**：MongoDB 天然保證單文件原子性
- **嵌入式設計已解決**：如果透過文件設計（Embedding）就能避免跨文件更新
- **最終一致性可接受**：如日誌記錄、統計計數等
- **高頻寫入**：交易帶來額外開銷，大量併發交易可能導致 WriteConflict 頻繁

### 設計原則

> **文件模型優先，交易作為安全網。**
>
> 先思考是否能透過文件設計（嵌入）避免跨文件操作，只在必要時使用交易。

## 8. 小結

- MongoDB 4.0+ 支援 Replica Set 上的多文件交易
- 使用 WiredTiger MVCC，提供 Snapshot Isolation
- 交易有 60 秒超時和 16MB oplog 限制
- 併發寫入同一文件會觸發 WriteConflict
- 文件設計優先，交易作為補充
