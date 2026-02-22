# M16 DOC-02：Spring Data MessageListenerContainer 實戰

## 概述

Spring Data MongoDB 提供兩種使用 Change Streams 的方式：

1. **MessageListenerContainer**（宣告式/同步）— 本模組 Banking 領域使用
2. **Native Driver `watch()`**（命令式/手動管理）— 本模組 E-commerce 領域使用

兩種方式各有優缺點，適用於不同場景。

## 方式一：MessageListenerContainer

### 核心元件

| 元件 | 職責 |
|------|------|
| `DefaultMessageListenerContainer` | 管理背景執行緒，驅動 Change Stream 遊標 |
| `ChangeStreamRequest` | 定義要監聽的集合、篩選條件、回呼函式 |
| `MessageListener` | 接收變更事件的回呼介面 |
| `Subscription` | 代表一個訂閱，提供 `await()` 同步等待 |
| `ChangeStreamOptions` | 設定 `FullDocument`、Pipeline 篩選等選項 |

### 基本使用

```java
// 1. 建立容器
var container = new DefaultMessageListenerContainer(mongoTemplate);
container.start();

// 2. 設定選項
var options = ChangeStreamOptions.builder()
    .fullDocumentLookup(FullDocument.UPDATE_LOOKUP)
    .build();

// 3. 建立並註冊請求
var request = ChangeStreamRequest.builder()
    .collection("my_collection")
    .publishTo(message -> {
        var event = message.getRaw();
        // 處理事件...
    })
    .filter(options)
    .build();

// 4. 註冊並等待就緒
var subscription = container.register(request, Document.class);
subscription.await(Duration.ofSeconds(5));

// 5. 停止
container.stop();
```

### 執行緒模型

`DefaultMessageListenerContainer` 使用 `SimpleAsyncTaskExecutor`，每次 `register()` 呼叫都會建立一個新的執行緒。這意味著：

- 每個訂閱獨立運行在自己的執行緒中
- `container.stop()` 會中斷所有訂閱執行緒
- 容器本身是輕量級的，可以重複建立和銷毀

### `subscription.await()` 的重要性

```java
subscription.await(Duration.ofSeconds(5));
```

這行程式碼**至關重要**。它會阻塞直到 Change Stream 遊標建立完成。如果省略：

- 在 `register()` 和實際開始監聽之間存在時間差
- 測試中的操作可能在監聽開始前執行，導致事件遺漏
- 生產環境中可能錯過啟動期間的事件

### Pipeline 篩選

```java
// 使用 Spring Data Aggregation DSL
var options = ChangeStreamOptions.builder()
    .fullDocumentLookup(FullDocument.UPDATE_LOOKUP)
    .filter(match(where("operationType").is("insert")))
    .build();
```

Spring Data 的 `filter()` 方法接受 `Aggregation` 物件，可以使用熟悉的 Criteria API 來定義篩選條件。

### 事件處理

```java
.publishTo(message -> {
    var raw = message.getRaw();  // ChangeStreamDocument<Document>

    // 操作類型
    String opType = raw.getOperationType().getValue();

    // 文件識別鍵
    Document docKey = raw.getDocumentKey();
    ObjectId id = docKey.getObjectId("_id");

    // 完整文件（需設定 UPDATE_LOOKUP）
    Document fullDoc = raw.getFullDocument();

    // 更新描述（僅 update 事件）
    UpdateDescription updateDesc = raw.getUpdateDescription();
})
```

> **重要**：`onMessage()` 內的例外會逃逸到 uncaught exception handler，不會被容器的 `ErrorHandler` 捕獲。務必在回呼內處理所有可能的例外。

## 方式二：Native Driver `watch()`

### 基本使用

```java
var pipeline = List.of(
    Aggregates.match(Filters.and(
        Filters.eq("operationType", "update"),
        Filters.exists("updateDescription.updatedFields.status")
    ))
);

var cursor = collection.watch(pipeline)
    .fullDocument(FullDocument.UPDATE_LOOKUP)
    .cursor();

while (running.get()) {
    var event = cursor.tryNext();
    if (event != null) {
        processEvent(event);
    } else {
        Thread.sleep(50);
    }
}
cursor.close();
```

### 背景執行緒管理

```java
private final AtomicBoolean running = new AtomicBoolean(false);
private Thread watcherThread;

public void startWatching() {
    running.set(true);
    var latch = new CountDownLatch(1);

    watcherThread = new Thread(() -> {
        try (var cursor = collection.watch().cursor()) {
            latch.countDown();  // 通知主執行緒遊標已就緒
            while (running.get()) {
                var event = cursor.tryNext();
                if (event != null) processEvent(event);
                else Thread.sleep(50);
            }
        }
    });
    watcherThread.setDaemon(true);
    watcherThread.start();

    latch.await(5, TimeUnit.SECONDS);
}

public void stopWatching() {
    running.set(false);
    watcherThread.interrupt();
    watcherThread.join(5000);
}
```

### Resume Token 恢復

```java
var watchBuilder = collection.watch(pipeline)
    .fullDocument(FullDocument.UPDATE_LOOKUP);

if (resumeToken != null) {
    watchBuilder = watchBuilder.resumeAfter(resumeToken);
}

try (var cursor = watchBuilder.cursor()) {
    // 處理事件...
}
```

## 兩種方式比較

| 面向 | MessageListenerContainer | Native Driver `watch()` |
|------|------------------------|----------------------|
| 程式碼量 | 較少（Spring 管理執行緒） | 較多（手動管理執行緒） |
| 彈性 | 受限於 Spring API | 完全控制 |
| Pipeline | Spring Aggregation DSL | MongoDB Driver Aggregates |
| Resume Token | 需手動實作 | 需手動實作 |
| 錯誤處理 | 較不直觀 | 完全掌控 |
| 測試容易度 | `subscription.await()` | `CountDownLatch` |
| 適用場景 | 簡單監聽、Spring 生態系統 | 複雜管線、精細控制 |

## Awaitility 測試策略

Change Streams 是非同步的，事件處理有延遲。使用 Awaitility 等待斷言：

```java
// 等待非同步事件到達
await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
    var notifications = mongoTemplate.findAll(AccountNotification.class);
    assertThat(notifications).hasSize(1);
    assertThat(notifications.getFirst().operationType()).isEqualTo("insert");
});
```

### 「什麼都不應該發生」的測試

Awaitility 不適合驗證「事件沒有到達」。改用直接等待：

```java
// 停止監聽後，新操作不應產生通知
listener.stopListening();
accountService.create("Grace", 5000);
Thread.sleep(500);  // 給足夠的等待時間

var notifications = mongoTemplate.findAll(AccountNotification.class);
assertThat(notifications).hasSize(1);  // 仍然只有之前的 1 筆
```

## Resume Token 持久化設計

```java
@Document("m16_resume_tokens")
public record ResumeTokenDocument(
    @Id String listenerName,    // 自然鍵
    String tokenJson,           // BsonDocument.toJson()
    Instant savedAt
) {}
```

使用 `upsert` 確保每個 listener 只保留最新的 Token：

```java
public void saveToken(String listenerName, BsonDocument token) {
    var query = Query.query(Criteria.where("_id").is(listenerName));
    var update = new Update()
        .set("tokenJson", token.toJson())
        .set("savedAt", Instant.now());
    mongoTemplate.upsert(query, update, ResumeTokenDocument.class);
}
```

## 生產環境注意事項

1. **連線中斷重連**：實作指數退避重連策略
2. **oplog 過期**：監控 Resume Token 有效性，過期時考慮全量同步
3. **冪等處理**：Change Stream 可能重發事件（at-least-once），確保處理邏輯冪等
4. **監控指標**：追蹤事件處理延遲、失敗次數、Resume Token 更新頻率
5. **資源清理**：應用程式關閉時確保停止所有 Change Stream（`@PreDestroy`）
6. **多實例部署**：多個應用實例監聽同一集合時，每個實例都會收到所有事件
