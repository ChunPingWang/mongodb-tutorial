# M16 DOC-01：Change Streams 基礎概念

## 什麼是 Change Streams？

Change Streams 是 MongoDB 3.6+ 提供的功能，允許應用程式即時監聽資料庫中的資料變更事件。它基於 MongoDB 的 **oplog**（操作日誌）機制，提供可靠的、可恢復的變更通知串流。

## 核心前提：Replica Set

Change Streams 依賴 oplog，因此**必須在 Replica Set 或 Sharded Cluster 上運行**。單機模式（standalone）不支援 Change Streams。

> **Testcontainers 提示**：`MongoDBContainer("mongo:8.0")` 預設啟動為單節點 Replica Set，因此 Change Streams 可直接使用。

## 與 Polling 的比較

| 特性 | Change Streams | Polling |
|------|---------------|---------|
| 延遲 | 近即時（毫秒級） | 取決於輪詢間隔 |
| 資源消耗 | 低（事件驅動） | 高（定期查詢） |
| 可靠性 | 有 Resume Token 保證 | 可能遺漏變更 |
| 複雜度 | 需管理連線生命週期 | 實作簡單 |
| 即時性 | 推送模式 | 拉取模式 |

## 事件結構

每個 Change Stream 事件包含以下關鍵欄位：

```json
{
  "_id": { "_data": "..." },          // Resume Token
  "operationType": "insert",          // 操作類型
  "fullDocument": { ... },            // 完整文件（需設定）
  "ns": {                             // 命名空間
    "db": "mydb",
    "coll": "mycoll"
  },
  "documentKey": { "_id": "..." },    // 文件識別鍵
  "updateDescription": {              // 僅 update 事件
    "updatedFields": { ... },
    "removedFields": [ ... ]
  },
  "clusterTime": Timestamp(...)       // 叢集時間戳
}
```

## 操作類型（Operation Types）

| 操作類型 | 說明 | fullDocument |
|---------|------|-------------|
| `insert` | 新增文件 | 完整文件 |
| `update` | 更新文件 | 需設定 `FullDocument.UPDATE_LOOKUP` |
| `replace` | 替換文件 | 完整文件 |
| `delete` | 刪除文件 | `null`（文件已不存在） |
| `drop` | 刪除集合 | N/A |
| `rename` | 重新命名集合 | N/A |
| `invalidate` | 串流失效 | N/A |

## FullDocument 選項

預設情況下，`update` 事件只包含 `updateDescription`（變更的差異），不含完整文件。透過設定 `FullDocument.UPDATE_LOOKUP`，MongoDB 會在事件發送時查詢最新的完整文件：

```java
// Spring Data
ChangeStreamOptions.builder()
    .fullDocumentLookup(FullDocument.UPDATE_LOOKUP)
    .build();

// Native Driver
collection.watch().fullDocument(FullDocument.UPDATE_LOOKUP);
```

> **注意**：`UPDATE_LOOKUP` 查詢的是「當下」的文件狀態，如果在事件產生和送達之間有其他更新，fullDocument 可能反映的是最新狀態而非事件當時的狀態。

## Resume Token（恢復令牌）

Resume Token 是 Change Streams 最重要的可靠性機制：

1. **每個事件**都包含一個唯一的 Resume Token（`_id` 欄位）
2. 應用程式可以儲存 Token，在中斷後從該位置**繼續接收事件**
3. Token 對應 oplog 中的位置，只要 oplog 未被覆蓋就可以恢復

### Resume Token 持久化

```java
// 儲存（BsonDocument → JSON 字串）
String json = resumeToken.toJson();

// 載入（JSON 字串 → BsonDocument）
BsonDocument token = BsonDocument.parse(json);

// 從 Token 恢復
collection.watch()
    .resumeAfter(token)
    .cursor();
```

### `resumeAfter` vs `startAfter`

| 方法 | 說明 | 使用時機 |
|------|------|---------|
| `resumeAfter` | 從 Token **之後**的事件開始 | 一般恢復 |
| `startAfter` | 與 `resumeAfter` 類似，但可在集合被刪除/重建後繼續 | 需要跨集合生命週期 |

## Pipeline 篩選

Change Streams 支援 Aggregation Pipeline 篩選，只接收符合條件的事件：

```java
// 僅監聽 update 事件中 status 欄位的變更
var pipeline = List.of(
    Aggregates.match(Filters.and(
        Filters.eq("operationType", "update"),
        Filters.exists("updateDescription.updatedFields.status")
    ))
);

collection.watch(pipeline);
```

支援的 Pipeline 階段：`$match`、`$project`、`$addFields`、`$replaceRoot`、`$redact`。

## 常見使用場景

| 場景 | 說明 |
|------|------|
| **稽核日誌** | 記錄所有資料變更歷史 |
| **快取失效** | 資料變更時即時更新或清除快取 |
| **即時通知** | 觸發推播或 WebSocket 訊息 |
| **CQRS 投影** | 非同步更新讀取模型 |
| **跨服務同步** | 微服務間的事件驅動資料同步 |
| **ETL 管線** | 即時資料處理與轉換 |

## 監聽層級

Change Streams 可在三個層級開啟：

1. **Collection 層級**：監聽單一集合的變更（最常用）
2. **Database 層級**：監聽整個資料庫的所有集合變更
3. **Deployment 層級**：監聽整個 MongoDB 部署的變更（需 admin 權限）

```java
// Collection 層級
collection.watch();

// Database 層級
database.watch();

// Deployment 層級（MongoClient）
mongoClient.watch();
```

## 注意事項

1. **oplog 大小有限**：如果應用程式離線太久，oplog 可能被覆蓋，Resume Token 將失效
2. **delete 事件無 fullDocument**：文件已被刪除，即使設定 `UPDATE_LOOKUP` 也無法取得
3. **`UPDATE_LOOKUP` 效能考量**：每個事件都會額外查詢一次資料庫
4. **執行緒管理**：Change Stream 遊標是阻塞式的，需要在背景執行緒中運行
5. **連線中斷**：應用程式應處理網路中斷和自動重連邏輯
6. **事務相容性**：在多文件事務中的變更，會在事務提交後作為單一事件批次發出
