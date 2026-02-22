# M10-DOC-01：Aggregate Root 與 MongoDB Collection 映射

## 1. 三個邊界的統一

DDD（Domain-Driven Design）的 Aggregate 模式與 MongoDB 的文件模型有著天然的契合度。在本模組中，我們遵循一個核心原則：

> **Aggregate 邊界 = Document 邊界 = Transaction 邊界**

這三個邊界的統一意味著：

| 邊界 | 意義 | 實務對應 |
|------|------|----------|
| **Aggregate 邊界** | 一組必須保持一致性的領域物件 | `LoanApplication`、`Claim`、`Order` |
| **Document 邊界** | MongoDB 中一份完整的 BSON 文件 | 一個 Collection 中的一筆資料 |
| **Transaction 邊界** | 原子性寫入的範圍 | 單一文件寫入，無需 `@Transactional` |

### 為什麼這很重要？

MongoDB 對**單一文件**的寫入本身就是原子性的。當我們把 Aggregate 的所有內部狀態都嵌入在同一份文件中，每次 `save()` 操作就是一次原子寫入——不需要開啟多文件交易，不需要 `@Transactional`，也不需要 M09 中學到的 `TransactionTemplate` 或 `ClientSession`。

```
┌─────────────────────────────────────────────┐
│         MongoDB Single Document Write       │
│                (Atomic)                     │
│  ┌───────────────────────────────────────┐  │
│  │        Aggregate Root (Order)         │  │
│  │  ┌─────────┐  ┌─────────────────────┐│  │
│  │  │OrderLine│  │  ShippingAddress     ││  │
│  │  │OrderLine│  │  (Value Object)      ││  │
│  │  └─────────┘  └─────────────────────┘│  │
│  │  ┌─────────────────────┐             │  │
│  │  │  PaymentInfo        │  status     │  │
│  │  │  (Value Object)     │  totalAmount│  │
│  │  └─────────────────────┘             │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

## 2. 內部一致性 vs 最終一致性

### Aggregate 內部：強一致性

Aggregate 內部的所有變更在同一次操作中完成。以 `Order` 為例，新增一筆 `OrderLine` 時，`totalAmount` 會在同一個方法中同步更新：

```java
// Order.java — Aggregate Root 內部保證一致性
public void addLine(OrderLine line) {
    var spec = new OrderModificationAllowedSpec();
    if (!spec.isSatisfiedBy(status)) {
        throw new IllegalStateException(
                "Cannot add lines to order in status " + status);
    }
    lines.add(line);
    totalAmount = totalAmount.add(line.lineTotal());  // 同步更新
    updatedAt = Instant.now();
}
```

這裡的關鍵是：`lines` 與 `totalAmount` 的一致性不依賴任何外部交易機制，而是由 Aggregate Root 自身的業務方法來保證。存入 MongoDB 時，整份文件（包含更新後的 `lines` 與 `totalAmount`）以原子方式寫入。

### Aggregate 之間：最終一致性

不同 Aggregate 之間的協調透過**領域事件（Domain Events）**實現最終一致性。例如，`Order` 完成付款後產生 `OrderPaid` 事件，由其他 Bounded Context 的監聽器非同步處理：

```java
// Order.java — 產生領域事件供其他 Aggregate 消費
public void pay(PaymentInfo paymentInfo) {
    transitionTo(OrderStatus.PAID);
    this.paymentInfo = paymentInfo;
    registerEvent(new OrderPaid(id, paymentInfo.transactionId(), Instant.now()));
}
```

```java
// DomainEvent.java — 共用的領域事件介面
public interface DomainEvent {
    Instant occurredAt();
    String aggregateId();
}
```

事件本身被暫存在 Aggregate 內部的 `domainEvents` 清單中，在 Application Service 呼叫 `save()` 之後由基礎設施層發布。這種模式確保了 Aggregate 之間不會產生跨文件的交易依賴。

## 3. Bounded Context 到 Collection 的映射

本模組包含三個 Bounded Context，每個都有一個核心 Aggregate，對應到一個 MongoDB Collection：

| Bounded Context | Aggregate Root | Collection 名稱 | 內嵌 Value Objects |
|-----------------|---------------|-----------------|-------------------|
| Banking | `LoanApplication` | `m10_loan_applications` | `Applicant`、`Money`、`LoanTerm` |
| Insurance | `Claim` | `m10_claims` | `ClaimItem`、`Assessment`、`PolicyReference`、`ClaimantReference` |
| E-commerce | `Order` | `m10_orders` | `OrderLine`、`ShippingAddress`、`PaymentInfo` |

### 映射原則

1. **一個 Aggregate Root 對應一個 Collection**——不拆表、不做正規化
2. **Value Object 嵌入為子文件**——不另開 Collection 儲存
3. **跨 Aggregate 的引用使用 ID**——`PolicyReference(policyId)` 而非直接嵌入整個 Policy 物件
4. **Domain Model 與 Persistence Model 分離**——Domain 層的 `LoanApplication` 不帶任何 `@Document` 註解

## 4. 三個 Aggregate 的 Document 結構

### 4.1 Banking：LoanApplication

Domain Model（純領域物件，零 Spring 依賴）：

```java
public class LoanApplication {                   // Aggregate Root
    private String id;
    private Applicant applicant;                  // Value Object（嵌入）
    private Money requestedAmount;                // Value Object（嵌入）
    private LoanTerm term;                        // Value Object（嵌入）
    private LoanStatus status;
    private String reviewResult;
    private Instant createdAt;
    private Instant updatedAt;
    private final List<DomainEvent> domainEvents; // 暫存事件
}

public record Applicant(String name, String nationalId,
                        Money annualIncome, String employer) {}
public record Money(BigDecimal amount, String currency) {}
public record LoanTerm(int years, BigDecimal annualInterestRate) {}
```

對應的 MongoDB Document（存入 `m10_loan_applications`）：

```json
{
  "_id": ObjectId("..."),
  "applicantName": "王小明",
  "applicantNationalId": "A123456789",
  "applicantAnnualIncome": NumberDecimal("1200000"),
  "applicantAnnualIncomeCurrency": "TWD",
  "applicantEmployer": "台積電",
  "requestedAmount": NumberDecimal("5000000"),
  "requestedAmountCurrency": "TWD",
  "termYears": 20,
  "annualInterestRate": NumberDecimal("2.5"),
  "status": "SUBMITTED",
  "reviewResult": null,
  "createdAt": ISODate("2026-02-20T08:00:00Z"),
  "updatedAt": ISODate("2026-02-20T08:00:00Z"),
  "domainEvents": [
    { "type": "LoanApplicationSubmitted", "applicantName": "王小明", ... }
  ]
}
```

### 4.2 Insurance：Claim

```java
public class Claim {                              // Aggregate Root
    private String id;
    private PolicyReference policyRef;            // Value Object（ID 引用）
    private ClaimantReference claimantRef;         // Value Object（ID 引用）
    private List<ClaimItem> items;                // Value Object 集合（嵌入）
    private Assessment assessment;                // Value Object（嵌入，可為 null）
    private List<ClaimDocument> documents;        // Value Object 集合（嵌入）
    private ClaimStatus status;
    private Money totalClaimedAmount;
    private Money deductible;
    private Money policyCoverage;
    private Instant filedAt;
}

public record ClaimItem(String description, Money amount, String category) {}
public record Assessment(String assessorName, Money approvedAmount, String notes) {}
public record PolicyReference(String policyId) {}
```

對應的 MongoDB Document（存入 `m10_claims`）：

```json
{
  "_id": ObjectId("..."),
  "policyId": "POL-2026-001",
  "claimantId": "CLM-001",
  "items": [
    { "description": "住院費用", "amount": NumberDecimal("50000"), "currency": "TWD", "category": "醫療" },
    { "description": "手術費用", "amount": NumberDecimal("120000"), "currency": "TWD", "category": "醫療" }
  ],
  "assessment": null,
  "documents": [],
  "status": "FILED",
  "totalClaimedAmount": NumberDecimal("170000"),
  "totalClaimedAmountCurrency": "TWD",
  "deductible": NumberDecimal("10000"),
  "deductibleCurrency": "TWD",
  "policyCoverage": NumberDecimal("500000"),
  "policyCoverageCurrency": "TWD",
  "filedAt": ISODate("2026-02-20T08:30:00Z")
}
```

注意 `PolicyReference` 與 `ClaimantReference` 在 MongoDB 中僅存為 ID 字串——它們是**跨 Aggregate 的引用**，不嵌入完整物件。

### 4.3 E-commerce：Order

```java
public class Order {                              // Aggregate Root
    private String id;
    private String orderNumber;
    private String customerId;                    // 跨 Aggregate 引用
    private List<OrderLine> lines;               // Value Object 集合（嵌入）
    private ShippingAddress shippingAddress;      // Value Object（嵌入）
    private PaymentInfo paymentInfo;              // Value Object（嵌入，可為 null）
    private String trackingNumber;
    private OrderStatus status;
    private Money totalAmount;
}

public record OrderLine(String productId, String productName,
                        int quantity, Money unitPrice) {}
public record ShippingAddress(String recipientName, String street,
                              String city, String zipCode) {}
public record PaymentInfo(String paymentMethod, String transactionId,
                          Instant paidAt) {}
```

對應的 MongoDB Document（存入 `m10_orders`）：

```json
{
  "_id": ObjectId("..."),
  "orderNumber": "ORD-2026-0001",
  "customerId": "CUST-001",
  "lines": [
    { "productId": "P001", "productName": "MacBook Pro", "quantity": 1,
      "unitPrice": NumberDecimal("72900"), "unitPriceCurrency": "TWD" },
    { "productId": "P002", "productName": "Magic Mouse", "quantity": 2,
      "unitPrice": NumberDecimal("2990"), "unitPriceCurrency": "TWD" }
  ],
  "shippingAddress": {
    "recipientName": "王小明",
    "street": "信義路五段7號",
    "city": "台北市",
    "zipCode": "110"
  },
  "paymentInfo": null,
  "trackingNumber": null,
  "status": "CREATED",
  "totalAmount": NumberDecimal("78880"),
  "totalAmountCurrency": "TWD",
  "createdAt": ISODate("2026-02-20T09:00:00Z"),
  "updatedAt": ISODate("2026-02-20T09:00:00Z")
}
```

## 5. Value Object 嵌入 vs Entity 引用

在 DDD 中，Value Object 沒有自己的身份（identity），而 Entity 有。這個區別直接影響 MongoDB 的儲存策略：

| 類型 | 特徵 | MongoDB 儲存方式 | 範例 |
|------|------|-----------------|------|
| **Value Object（嵌入）** | 無自身 ID、隨 Aggregate 生滅 | 嵌入為子文件或陣列 | `Applicant`、`Money`、`ShippingAddress`、`ClaimItem` |
| **Entity 引用（ID）** | 有自身 ID、獨立生命週期 | 僅存放 ID 字串 | `PolicyReference`、`ClaimantReference`、`customerId` |

### 嵌入的判斷準則

- **是否需要獨立查詢？** 否 -> 嵌入。例如不會單獨查詢某筆 `OrderLine`
- **是否隨 Aggregate Root 一起建立與刪除？** 是 -> 嵌入。`ShippingAddress` 隨 `Order` 消亡
- **是否跨多個 Aggregate 共享？** 是 -> 使用 ID 引用。`Policy` 被多個 `Claim` 引用
- **資料量是否可控？** 是 -> 嵌入。一張訂單通常不會有上千筆明細

## 6. 為什麼不需要 @Transactional

回顧 M09，我們學到 `@Transactional` 用於跨多個文件或集合的操作。但在 DDD Aggregate 模式下：

```
單一 Aggregate = 單一 Document = 單一原子寫入
```

因此，Application Service 的程式碼非常簡潔：

```java
// 不需要 @Transactional — 單一文件原子寫入
public void performPreliminaryReview(String applicationId) {
    LoanApplication app = repository.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));
    app.performPreliminaryReview(new IncomeToPaymentRatioSpec());
    repository.save(app);                          // 原子寫入整份文件
    publishEvents(app.getDomainEvents());          // 事後發布事件
    app.clearDomainEvents();
}
```

只有當業務操作涉及**多個 Aggregate**（例如從帳戶 A 轉帳到帳戶 B）時，才需要使用 M09 的交易機制。良好的 Aggregate 設計會盡可能減少這種跨 Aggregate 交易的需求。

## 7. 小結

| 設計決策 | 原因 | MongoDB 實踐 |
|----------|------|-------------|
| Aggregate 內部強一致性 | 業務不變量（invariant）必須始終成立 | 單一文件原子寫入 |
| Aggregate 之間最終一致性 | 降低耦合、提高可擴展性 | 領域事件 + 非同步處理 |
| Value Object 嵌入 | 無獨立身份、隨 Root 生滅 | 子文件 / 陣列 |
| 跨 Aggregate 引用用 ID | 各 Aggregate 獨立演進 | 僅存 ID 字串 |
| Domain Model 與 Persistence Model 分離 | 領域層不依賴框架 | Mapper 層負責轉換 |
| 不使用 `@Transactional` | 單一文件寫入已是原子性 | `repository.save()` 即可 |

這三個邊界的統一（Aggregate = Document = Transaction）正是 MongoDB 文件模型與 DDD 天然契合的核心原因。在下一篇文件中，我們將深入探討 Domain Model 與 Persistence Model 之間的 Mapper 層設計。
