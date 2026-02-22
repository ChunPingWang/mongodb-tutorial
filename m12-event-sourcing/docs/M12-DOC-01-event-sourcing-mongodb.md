# M12-DOC-01: Event Sourcing 概念與 MongoDB 實作

## 什麼是 Event Sourcing？

Event Sourcing（事件溯源）是一種資料儲存模式，不直接儲存實體的「當前狀態」，而是儲存所有導致狀態改變的「事件」。聚合根（Aggregate Root）的狀態透過重播（Replay）事件序列來重建。

### 傳統 CRUD vs Event Sourcing

| 面向 | 傳統 CRUD | Event Sourcing |
|------|-----------|----------------|
| 儲存內容 | 當前狀態 | 事件序列 |
| 歷史記錄 | 需額外設計審計表 | 天然具備完整歷史 |
| 資料修改 | 直接 UPDATE | 只有 INSERT（Append-Only）|
| 狀態重建 | 直接讀取 | 重播事件序列 |
| 除錯能力 | 只看得到最終狀態 | 可追溯每一步變化 |

### 核心概念

```
命令 (Command) → 產生事件 (Event) → 改變狀態 (State)
                                   ↓
                              持久化事件
                                   ↓
                         重播事件 → 重建狀態
```

1. **事件是不可變的（Immutable）**：一旦寫入就不能修改或刪除
2. **事件是過去式（Past Tense）**：`AccountOpened`、`FundsDeposited` 描述已發生的事實
3. **聚合狀態由事件推導**：沒有事件就沒有狀態
4. **Append-Only**：只有新增，沒有更新或刪除

## MongoDB 作為 Event Store

MongoDB 天然適合作為 Event Store，原因包括：

### 1. 彈性 Schema

不同類型的事件可以有不同欄位，存在同一個 Collection 中：

```json
// AccountOpened 事件
{
  "_id": "uuid-1",
  "_class": "AccountOpened",
  "aggregateId": "ACC-001",
  "version": 1,
  "accountHolder": "王小明",
  "initialBalance": NumberDecimal("10000")
}

// FundsDeposited 事件
{
  "_id": "uuid-2",
  "_class": "AccountOpened",
  "aggregateId": "ACC-001",
  "version": 2,
  "amount": NumberDecimal("5000"),
  "description": "薪資入帳"
}
```

### 2. `@TypeAlias` 多型反序列化

利用 M11 介紹的 `@TypeAlias`，Spring Data MongoDB 自動將 `_class` 欄位對應到正確的 Java 類型：

```java
@Document("m12_account_events")
@TypeAlias("AccountOpened")
public record AccountOpened(
    @Id String eventId,
    String aggregateId,
    long version,
    Instant occurredAt,
    String accountHolder,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal initialBalance,
    String currency
) implements AccountEvent {}
```

### 3. 複合唯一索引保證樂觀鎖

```java
// (aggregateId, version) 複合唯一索引
var indexDef = new CompoundIndexDefinition(
    new Document("aggregateId", 1).append("version", 1))
    .unique();
mongoTemplate.indexOps(collection).ensureIndex(indexDef);
```

若兩個並發操作嘗試寫入相同 `aggregateId` + `version`，第二個會收到 `DuplicateKeyException`，實現樂觀併發控制（Optimistic Concurrency Control）。

### 4. `insert()` 而非 `save()`

```java
// ✅ 正確：insert() 只會新增
mongoTemplate.insert(event, collection);

// ❌ 錯誤：save() 會 upsert（找到就更新）
mongoTemplate.save(event, collection);
```

Event Sourcing 是 Append-Only 模式，絕對不能更新已有事件。

## Event Replay 機制

### 從零重播

```java
public static BankAccount replayFrom(List<AccountEvent> events) {
    var account = new BankAccount();
    events.forEach(account::apply);
    return account;
}
```

`apply()` 使用 Java 21+ 的 Pattern Matching switch：

```java
private void apply(AccountEvent event) {
    switch (event) {
        case AccountOpened e -> {
            this.accountId = e.aggregateId();
            this.balance = e.initialBalance();
        }
        case FundsDeposited e -> this.balance = this.balance.add(e.amount());
        case FundsWithdrawn e -> this.balance = this.balance.subtract(e.amount());
        case FundsTransferred e -> this.balance = this.balance.subtract(e.amount());
    }
    this.version = event.version();
}
```

Sealed Interface 保證 switch 的完整性（exhaustiveness）：新增事件類型時，編譯器會強制處理。

### Snapshot 優化

隨著事件累積，每次都從頭重播效能會下降。Snapshot 機制定期保存聚合狀態快照：

```
事件 1 → 事件 2 → ... → 事件 5 → [快照 v5]
                                      ↓
              事件 6 → 事件 7    ← 從快照 v5 開始重播
```

**Snapshot 設計要點**：

1. **狀態序列化**：使用 `Map<String, Object>` 儲存，BigDecimal 用 `toPlainString()` 轉為字串
2. **閾值觸發**：每 N 個事件產生一次快照（本模組設定為 5）
3. **載入策略**：先查快照，再從快照版本之後重播增量事件

```java
public BankAccount loadAccount(String accountId) {
    var snapshot = eventStore.loadLatestSnapshot(accountId, AGGREGATE_TYPE);
    if (snapshot.isPresent()) {
        var account = BankAccount.fromSnapshot(snapshot.get().state());
        var incrementalEvents = eventStore.loadEventsAfterVersion(
            accountId, snapshot.get().version(), AccountEvent.class, COLLECTION);
        account.replayAfterSnapshot(incrementalEvents);
        return account;
    }
    // 無快照，從頭重播
    var events = eventStore.loadEvents(accountId, AccountEvent.class, COLLECTION);
    return BankAccount.replayFrom(events);
}
```

## EventStore 設計

本模組的 `EventStore` 是一個泛型服務，支援任何 Domain 的事件：

```java
@Service
public class EventStore {
    <T extends DomainEvent> T append(T event, String collection);
    <T extends DomainEvent> List<T> loadEvents(aggregateId, Class<T>, collection);
    <T extends DomainEvent> List<T> loadEventsAfterVersion(aggregateId, afterVersion, Class<T>, collection);
    long countEvents(aggregateId, collection);
    SnapshotDocument saveSnapshot(SnapshotDocument);
    Optional<SnapshotDocument> loadLatestSnapshot(aggregateId, aggregateType);
}
```

### Collection 策略

每個 Domain 使用獨立的 Collection：

- `m12_account_events`：銀行帳戶事件
- `m12_claim_events`：保險理賠事件
- `m12_snapshots`：所有 Domain 共用的快照 Collection（透過 `aggregateType` 區分）

## 與傳統 CRUD 的比較：何時使用 Event Sourcing？

### 適合的場景

- **需要完整審計軌跡**：金融交易、保險理賠、合規需求
- **需要時間旅行**：回到任意時間點查看狀態
- **複雜的業務邏輯**：狀態轉換規則多且嚴格
- **事件驅動架構**：與 CQRS、Event-Driven Microservices 搭配

### 不適合的場景

- **簡單的 CRUD 應用**：沒有複雜業務邏輯
- **頻繁的全量查詢**：每次讀取都需重播效能不佳（需搭配 CQRS）
- **資料量極大且事件很長**：快照策略變得關鍵

## 從 M10 到 M12 的演進

| M10 Domain Events | M12 Event Sourcing |
|-------------------|--------------------|
| 事件是副產品 | 事件是真相來源 |
| 聚合狀態直接持久化 | 聚合狀態從事件推導 |
| `*Document` + `*Mapper` 橋接 | 事件即文件，無需額外 Mapper |
| 事件嵌入在聚合文件中 | 事件獨立儲存在事件 Collection |
| `@Document` 在 Infrastructure 層 | `@Document` 在事件記錄上 |

M10 的 Domain Events 是「通知」用途，M12 的 Events 是「唯一真相來源」。
