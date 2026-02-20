# M13-DOC-02：Projection 設計原則

## 依查詢設計讀取模型

CQRS 讀取模型的設計起點不是「我有什麼資料」，而是「我需要回答什麼問題」。

### 範例：銀行帳戶

**查詢需求**：
1. 查看帳戶摘要（餘額、交易統計）
2. 查看交易歷史（分頁、按類型篩選）
3. 帳戶餘額排行榜

**設計結果**：兩個獨立的讀取模型

| 讀取模型 | 對應查詢 | @Id 策略 |
|---------|---------|----------|
| `AccountSummaryDocument` | 需求 1, 3 | accountId（一帳戶一文件）|
| `TransactionHistoryDocument` | 需求 2 | eventId（一交易一文件）|

這兩個讀取模型從**相同的事件流**投影而來，但結構完全不同。

### 範例：保險理賠

**查詢需求**：
1. 理賠儀表板（完整狀態 + 時間線）
2. 按狀態/類別查詢理賠
3. 理賠統計（按類別彙總）

**設計結果**：

| 讀取模型 | 對應查詢 | @Id 策略 |
|---------|---------|----------|
| `ClaimDashboardDocument` | 需求 1, 2 | claimId |
| `ClaimStatisticsDocument` | 需求 3 | category（一類別一文件）|

## 反正規化策略

### 預計算聚合值

不要在查詢時計算，而是在投影時預計算：

```java
// ❌ 查詢時計算
var deposits = mongoTemplate.find(query, TransactionHistoryDocument.class);
var totalDeposited = deposits.stream().map(d -> d.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);

// ✅ 投影時預計算（使用 $inc）
case FundsDeposited e -> {
    var update = new Update()
        .inc("totalDeposited", new Decimal128(e.amount()))
        .inc("depositCount", 1);
    mongoTemplate.updateFirst(query, update, COLLECTION);
}
```

### 內嵌時間線

將事件歷史以嵌入陣列形式存入讀取模型，避免跨 collection 查詢：

```java
case ClaimInvestigated e -> {
    var entry = new TimelineEntry("ClaimInvestigated", e.occurredAt(),
            "Investigated by " + e.investigatorName());
    var update = new Update()
        .push("timeline", entry)    // $push 新增至嵌入陣列
        .inc("eventCount", 1);
}
```

## Projector 實作模式

### 模式一：Insert + UpdateFirst

適用於有明確「建立」事件的 Projector：

```java
public void project(AccountEvent event) {
    switch (event) {
        case AccountOpened e -> mongoTemplate.insert(initialDoc, COLLECTION);    // 建立
        case FundsDeposited e -> mongoTemplate.updateFirst(query, update, COLLECTION); // 更新
        // ...
    }
}
```

### 模式二：Upsert

適用於聚合統計，首次出現時自動建立：

```java
case ClaimFiled e -> {
    var update = new Update()
        .inc("totalClaims", 1)
        .inc("filedCount", 1)
        .inc("totalClaimedAmount", new Decimal128(e.claimedAmount()))
        .setOnInsert("approvedCount", 0)     // 僅首次建立時設定
        .setOnInsert("totalPaidAmount", new Decimal128(BigDecimal.ZERO));
    mongoTemplate.upsert(query, update, COLLECTION);   // upsert!
}
```

### 模式三：Always Insert

適用於每個事件都產生一筆新文件的情境：

```java
// TransactionHistoryProjector：每筆交易 = 一個文件
public void project(AccountEvent event) {
    switch (event) {
        case AccountOpened e -> mongoTemplate.insert(openingEntry, COLLECTION);
        case FundsDeposited e -> mongoTemplate.insert(depositEntry, COLLECTION);
        // ...
    }
}
```

## Projector 排序依賴

當 Projector 之間有資料依賴時，投影順序很重要。

本模組中，`ClaimStatisticsProjector` 需要查詢 `ClaimDashboardDocument` 來取得 `category`：

```java
// ClaimStatisticsProjector
case ClaimApproved e -> {
    var category = lookupCategory(e.aggregateId());  // 從 Dashboard 查詢
    // ...
}

private String lookupCategory(String claimId) {
    var dashboard = mongoTemplate.findOne(query, ClaimDashboardDocument.class, DASHBOARD_COLLECTION);
    return dashboard.category();
}
```

因此在 `ClaimCommandService` 中，Dashboard 必須先於 Statistics 投影：

```java
private void projectEvent(ClaimEvent event) {
    claimDashboardProjector.project(event);    // 先投影 Dashboard
    claimStatisticsProjector.project(event);   // 再投影 Statistics
}
```

## 重建機制

每個 Projector 都實作 `rebuildAll` 方法：

```java
public void rebuildAll(List<AccountEvent> events) {
    mongoTemplate.remove(new Query(), COLLECTION);  // 1. 清除所有讀取模型
    events.forEach(this::project);                  // 2. 重播所有事件
}
```

`ProjectionRebuildService` 協調所有 Projector 的重建：

```java
public void rebuildBankingProjections() {
    var events = eventStore.loadAllEvents(AccountEvent.class, ACCOUNT_EVENTS);
    accountSummaryProjector.rebuildAll(events);
    transactionHistoryProjector.rebuildAll(events);
}
```

### 重建的使用時機

1. **Schema 變更**：讀取模型結構改變時
2. **新增讀取模型**：新增查詢需求時
3. **資料修復**：讀取模型與事件不一致時
4. **效能優化**：需要重新組織索引或結構時

## projectedVersion 欄位

每個讀取模型都追蹤已投影的最新事件版本號：

```java
case FundsDeposited e -> {
    var update = new Update()
        .inc("currentBalance", new Decimal128(e.amount()))
        .set("projectedVersion", e.version());    // 記錄版本
}
```

用途：
- **冪等性判斷**：可用於檢查事件是否已被投影
- **一致性檢查**：比對 Event Store 的最新版本與讀取模型的版本
- **診斷**：快速判斷讀取模型是否落後

## 常見陷阱

### 1. Decimal128 與 `$inc`

使用 `Update.inc()` 操作 DECIMAL128 欄位時，必須傳入 `Decimal128` 型別：

```java
// ✅ 正確
update.inc("currentBalance", new Decimal128(amount));

// ❌ 錯誤（BigDecimal 會被轉為 String）
update.inc("currentBalance", amount);
```

### 2. Record 的不可變性

Java Record 是不可變的，但 `mongoTemplate.updateFirst()` 直接操作 MongoDB 文件，不經過 Java 物件。Record 只在讀取時使用。

### 3. `balanceAfter` 的計算

`TransactionHistoryProjector` 需要查詢自己的 collection 來取得最新餘額：

```java
private BigDecimal getLatestBalance(String accountId) {
    var query = Query.query(Criteria.where("accountId").is(accountId))
            .with(Sort.by(Sort.Direction.DESC, "occurredAt"))
            .limit(1);
    var latest = mongoTemplate.findOne(query, TransactionHistoryDocument.class, COLLECTION);
    return latest != null ? latest.balanceAfter() : BigDecimal.ZERO;
}
```

這是跨 Projector 依賴的替代方案——每個 Projector 只依賴自己的讀取模型。

### 4. setOnInsert 與 upsert 搭配

使用 `upsert` 時，`setOnInsert` 只在**文件不存在而新建**時生效：

```java
var update = new Update()
    .inc("totalClaims", 1)                              // 每次都執行
    .setOnInsert("approvedCount", 0)                    // 只在首次建立時設定
    .setOnInsert("totalPaidAmount", new Decimal128(BigDecimal.ZERO));
mongoTemplate.upsert(query, update, COLLECTION);
```
