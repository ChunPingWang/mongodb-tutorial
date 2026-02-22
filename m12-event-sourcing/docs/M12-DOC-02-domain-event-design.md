# M12-DOC-02: Domain Event 設計原則

## 事件命名慣例

### 過去式（Past Tense）

事件描述的是「已經發生的事實」，因此命名必須使用過去式或過去分詞：

```java
// ✅ 正確：過去式
AccountOpened, FundsDeposited, FundsWithdrawn, FundsTransferred
ClaimFiled, ClaimInvestigated, ClaimAssessed, ClaimApproved, ClaimRejected, ClaimPaid

// ❌ 錯誤：命令式
OpenAccount, DepositFunds, WithdrawFunds
FileClaim, InvestigateClaim
```

### 命名結構

```
{聚合名稱}{動作過去式}
```

- `Account` + `Opened` → `AccountOpened`
- `Funds` + `Deposited` → `FundsDeposited`
- `Claim` + `Filed` → `ClaimFiled`

## 事件階層設計

本模組使用 Java Sealed Interface 建立事件階層：

```
DomainEvent (interface)
├── AccountEvent (sealed interface)
│   ├── AccountOpened (record)
│   ├── FundsDeposited (record)
│   ├── FundsWithdrawn (record)
│   └── FundsTransferred (record)
└── ClaimEvent (sealed interface)
    ├── ClaimFiled (record)
    ├── ClaimInvestigated (record)
    ├── ClaimAssessed (record)
    ├── ClaimApproved (record)
    ├── ClaimRejected (record)
    └── ClaimPaid (record)
```

### 為什麼用 Sealed Interface？

1. **編譯期完整性檢查**：`switch` 必須處理所有子類型
2. **型別安全**：不允許外部定義新的事件子型別
3. **Pattern Matching**：搭配 Java 21+ 的 switch pattern matching

```java
// Sealed Interface 保證 switch exhaustiveness
private void apply(AccountEvent event) {
    switch (event) {
        case AccountOpened e -> { /* ... */ }
        case FundsDeposited e -> { /* ... */ }
        case FundsWithdrawn e -> { /* ... */ }
        case FundsTransferred e -> { /* ... */ }
        // 若新增事件類型但未處理，編譯器會報錯
    }
}
```

### 為什麼用 Record？

事件是不可變的事實，Record 天然具備：
- **不可變性**：欄位自動 `final`
- **值語意**：自動產生 `equals()`、`hashCode()`、`toString()`
- **簡潔**：減少 Boilerplate 程式碼
- **MongoDB 相容**：Spring Data MongoDB 4.x 完整支援 Record

## Payload 設計原則

### 1. 攜帶足夠的業務資訊

事件應包含重建狀態所需的所有資訊：

```java
// ✅ 良好：包含完整業務資訊
public record AccountOpened(
    @Id String eventId,
    String aggregateId,
    long version,
    Instant occurredAt,
    String accountHolder,         // 誰開的戶
    BigDecimal initialBalance,    // 初始餘額
    String currency               // 幣別
) implements AccountEvent {}

// ❌ 不足：缺少重建狀態所需資訊
public record AccountOpened(
    String eventId,
    String aggregateId,
    long version
) implements AccountEvent {}
```

### 2. 共用欄位定義在介面

```java
public interface DomainEvent {
    String eventId();       // 事件唯一識別（UUID）
    String aggregateId();   // 聚合根識別
    long version();         // 單調遞增版本號
    Instant occurredAt();   // 事件發生時間
}
```

這四個欄位是所有事件的「信封」（Envelope），每個具體事件在此基礎上加入業務欄位。

### 3. 避免攜帶過多衍生資訊

```java
// ✅ 只攜帶「發生了什麼」
public record FundsDeposited(
    /* 信封欄位... */
    BigDecimal amount,     // 存了多少
    String description     // 描述
) implements AccountEvent {}

// ❌ 攜帶衍生狀態
public record FundsDeposited(
    /* 信封欄位... */
    BigDecimal amount,
    BigDecimal newBalance,   // 衍生：可從重播計算
    int totalTransactions    // 衍生：可從事件數計算
) implements AccountEvent {}
```

### 4. 金額使用 BigDecimal + DECIMAL128

```java
@Field(targetType = FieldType.DECIMAL128)
BigDecimal initialBalance
```

避免浮點精度問題，在 MongoDB 中以 `NumberDecimal` 儲存。

## 版本號與樂觀併發

### 版本號策略

每個聚合根維護自己的版本號，從 1 開始單調遞增：

```
ACC-001: v1(AccountOpened) → v2(FundsDeposited) → v3(FundsWithdrawn)
ACC-002: v1(AccountOpened) → v2(FundsDeposited)
```

### 樂觀併發控制

複合唯一索引 `(aggregateId, version)` 確保同一聚合根不會有重複版本：

```
User A: ACC-001 v3 → 嘗試寫入 v4 → ✅ 成功
User B: ACC-001 v3 → 嘗試寫入 v4 → ❌ DuplicateKeyException
```

User B 必須重新載入聚合、重新決策、重試寫入 v5。

## Event Versioning 與向後相容

隨著系統演進，事件的結構可能需要改變。以下是幾種常見策略：

### 1. 新增欄位（向後相容）

```java
// V1
public record AccountOpened(
    String eventId, String aggregateId, long version, Instant occurredAt,
    String accountHolder, BigDecimal initialBalance
) implements AccountEvent {}

// V2：新增 currency 欄位，舊事件該欄位為 null
public record AccountOpened(
    String eventId, String aggregateId, long version, Instant occurredAt,
    String accountHolder, BigDecimal initialBalance,
    String currency  // 新增，舊事件讀取時為 null
) implements AccountEvent {}
```

`apply()` 中處理 null：

```java
case AccountOpened e -> {
    this.currency = e.currency() != null ? e.currency() : "TWD"; // 預設值
}
```

### 2. Event Upcasting

當欄位重新命名或結構大幅改變時，使用 Upcaster 在讀取時轉換：

```java
// 概念性範例
public class AccountOpenedV1ToV2Upcaster {
    public AccountOpened upcast(Document rawEvent) {
        return new AccountOpened(
            rawEvent.getString("eventId"),
            rawEvent.getString("aggregateId"),
            rawEvent.getLong("version"),
            rawEvent.get("occurredAt", Instant.class),
            rawEvent.getString("holder"),      // V1 欄位名稱
            rawEvent.get("balance", BigDecimal.class),  // V1 欄位名稱
            "TWD"  // V1 沒有 currency，給預設值
        );
    }
}
```

### 3. 版本化事件類型

極端情況下，可以保留多個版本的事件類型：

```java
sealed interface AccountEvent extends DomainEvent
    permits AccountOpenedV1, AccountOpenedV2, FundsDeposited, /* ... */ {}
```

但這種方式會增加 `apply()` 的複雜度，通常優先使用 Upcasting。

## 從 M10 Domain Events 到 M12 Event Sourcing 的演進

### M10：事件作為通知

```java
// M10: 事件嵌入在聚合文件中
public class LoanApplicationDocument {
    @Id String id;
    String status;
    BigDecimal amount;
    List<Map<String, Object>> domainEvents;  // 事件是附屬品
}
```

- 聚合狀態直接持久化
- 事件用於通知、審計
- 狀態和事件存在同一份文件中

### M12：事件作為真相來源

```java
// M12: 事件獨立儲存，聚合狀態從事件推導
@Document("m12_account_events")
@TypeAlias("AccountOpened")
public record AccountOpened(
    @Id String eventId,
    String aggregateId,
    long version,
    Instant occurredAt,
    String accountHolder,
    BigDecimal initialBalance,
    String currency
) implements AccountEvent {}
```

- 只儲存事件，不直接儲存聚合狀態
- 聚合是純 Java 類別，透過重播事件建構
- 快照是效能優化，不是必要的

### 關鍵差異

| 面向 | M10 | M12 |
|------|-----|-----|
| 事件角色 | 副產品 | 真相來源 |
| 聚合持久化 | `*Document` + `*Mapper` | 不持久化（從事件重建）|
| Spring 註解 | Domain 零註解，Infrastructure 有 | 事件有 `@Document`，聚合零註解 |
| 歷史查詢 | 查聚合文件中的事件陣列 | 查事件 Collection |
| 併發控制 | MongoDB 文件級原子性 | 複合唯一索引樂觀鎖 |

## 實作檢查清單

- [ ] 事件使用過去式命名
- [ ] 事件繼承自 Sealed Interface
- [ ] 事件使用 Record（不可變）
- [ ] 事件包含四個信封欄位（eventId, aggregateId, version, occurredAt）
- [ ] 金額欄位使用 BigDecimal + DECIMAL128
- [ ] EventStore 使用 `insert()` 而非 `save()`
- [ ] 建立 `(aggregateId, version)` 複合唯一索引
- [ ] 聚合的 `apply()` switch 是 exhaustive
- [ ] 快照中 BigDecimal 用 `toPlainString()` 序列化
- [ ] 測試清理用 `remove()` 而非 `dropCollection()`（保留索引）
