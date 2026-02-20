# Rich Domain Model 在 MongoDB 的實踐

## 前言

傳統 Java 企業應用常落入 **Anemic Domain Model**（貧血領域模型）的陷阱：實體僅有 getter/setter，所有業務邏輯散落在 Service 層。Rich Domain Model 則將行為封裝在 Aggregate 內部，讓領域物件自身負責狀態變遷與業務規則驗證。本文透過 M10 模組的三個領域（Banking、Insurance、E-commerce）說明如何在 MongoDB + Spring Boot 架構中實踐 Rich Domain Model。

## 1. Rich Domain Model vs Anemic Domain Model

| 比較項目 | Anemic Model | Rich Domain Model |
|---------|-------------|-------------------|
| 實體角色 | 純資料載體（getter/setter） | 封裝行為與狀態的完整物件 |
| 業務邏輯位置 | Service 層 | Aggregate 內部 |
| 狀態變遷 | Service 直接修改欄位 | 透過行為方法（transition guard） |
| 不變式保護 | 仰賴外部檢查 | 建構時與操作時自我驗證 |
| 可測試性 | 必須啟動 Spring Context | 純 Java 單元測試即可 |

M10 模組的 Aggregate Root 類別標註了 **"Pure domain class with ZERO Spring/MongoDB dependencies"**，這是 Rich Domain Model 的關鍵特徵。

## 2. Aggregate 上的行為方法

Aggregate 不暴露 setter 給外部修改狀態，而是提供語意明確的行為方法。

### LoanApplication：submit() 與 performPreliminaryReview()

```java
// 靜態工廠 — 建立並進入 SUBMITTED 狀態
public static LoanApplication submit(Applicant applicant, Money requestedAmount, LoanTerm term) {
    var app = new LoanApplication();
    app.applicant = applicant;
    app.requestedAmount = requestedAmount;
    app.term = term;
    app.status = LoanStatus.SUBMITTED;
    app.createdAt = Instant.now();
    app.registerEvent(new LoanApplicationSubmitted(null, applicant.name(), requestedAmount, Instant.now()));
    return app;
}

// 行為方法 — 狀態前置檢查 + Specification 驗證 + 狀態變遷
public void performPreliminaryReview(IncomeToPaymentRatioSpec spec) {
    if (status != LoanStatus.SUBMITTED) {
        throw new IllegalStateException("Can only perform preliminary review on SUBMITTED applications");
    }
    Money annualPayment = term.computeAnnualPayment(requestedAmount);
    if (spec.isSatisfiedBy(applicant.annualIncome(), annualPayment)) {
        status = LoanStatus.PRELIMINARY_PASSED;
        registerEvent(new LoanPreliminaryReviewPassed(id, Instant.now()));
    } else {
        status = LoanStatus.PRELIMINARY_REJECTED;
        registerEvent(new LoanPreliminaryReviewRejected(id, reviewResult, Instant.now()));
    }
}
```

### Claim：file() 與 assess()

```java
public static Claim file(PolicyReference policyRef, ClaimantReference claimantRef,
                         List<ClaimItem> items, Money policyCoverage, Money deductible) {
    Money totalClaimed = items.stream().map(ClaimItem::amount).reduce(Money.twd(0), Money::add);
    if (totalClaimed.isGreaterThan(policyCoverage)) {
        throw new IllegalArgumentException("Total claimed amount exceeds policy coverage");
    }
    // ... 初始化 claim，狀態設為 FILED
}

public void assess(String assessorName, Money approvedAmount, String notes) {
    if (status != ClaimStatus.FILED && status != ClaimStatus.UNDER_REVIEW) {
        throw new IllegalStateException("Can only assess claims in FILED or UNDER_REVIEW status");
    }
    var spec = new ClaimAmountWithinCoverageSpec();
    if (!spec.isSatisfiedBy(approvedAmount, totalClaimedAmount, deductible)) {
        throw new IllegalArgumentException("Approved amount exceeds claimed amount minus deductible");
    }
    this.assessment = new Assessment(assessorName, approvedAmount, notes);
    this.status = ClaimStatus.APPROVED;
    registerEvent(new ClaimApproved(id, approvedAmount, Instant.now()));
}
```

### Order：create()、pay()、ship()

```java
public static Order create(String orderNumber, String customerId,
                           List<OrderLine> lines, ShippingAddress address) {
    if (lines == null || lines.isEmpty()) {
        throw new IllegalArgumentException("Order must have at least one line");
    }
    var order = new Order();
    order.totalAmount = lines.stream().map(OrderLine::lineTotal).reduce(Money.twd(0), Money::add);
    order.status = OrderStatus.CREATED;
    order.registerEvent(new OrderCreated(null, orderNumber, order.totalAmount, Instant.now()));
    return order;
}

public void pay(PaymentInfo paymentInfo) {
    transitionTo(OrderStatus.PAID);  // 狀態守衛
    this.paymentInfo = paymentInfo;
    registerEvent(new OrderPaid(id, paymentInfo.transactionId(), Instant.now()));
}

public void ship(String trackingNumber) {
    transitionTo(OrderStatus.SHIPPED);
    this.trackingNumber = trackingNumber;
    registerEvent(new OrderShipped(id, trackingNumber, Instant.now()));
}
```

## 3. Domain Events

### DomainEvent 介面

M10 定義了共用的事件標記介面，位於 `shared` 套件：

```java
public interface DomainEvent {
    Instant occurredAt();
    String aggregateId();
}
```

### Event Records

每個事件以 Java record 實作，攜帶最少且足夠的資訊：

```java
public record LoanApplicationSubmitted(
    String aggregateId, String applicantName, Money requestedAmount, Instant occurredAt
) implements DomainEvent {}

public record ClaimFiled(
    String aggregateId, String policyId, Money totalClaimedAmount, Instant occurredAt
) implements DomainEvent {}

public record OrderCreated(
    String aggregateId, String orderNumber, Money totalAmount, Instant occurredAt
) implements DomainEvent {}
```

### 事件收集與持久化

Aggregate 內部維護一個 `List<DomainEvent>`，行為方法執行時透過 `registerEvent()` 收集事件。Infrastructure 層（Mapper）在存檔時將事件序列化為 MongoDB 內嵌陣列：

```java
// Aggregate 內部
private final List<DomainEvent> domainEvents = new ArrayList<>();

private void registerEvent(DomainEvent event) {
    domainEvents.add(event);
}

public List<DomainEvent> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
}

public void clearDomainEvents() {
    domainEvents.clear();
}
```

持久化文件中，事件以 `List<Map<String, Object>>` 形式儲存為內嵌陣列，利用 MongoDB 文件模型的彈性，不需額外的事件表。

## 4. Specification Pattern

將複雜的業務規則抽取為獨立的 Specification 類別，使規則可重用、可測試。

### IncomeToPaymentRatioSpec（銀行）

```java
public class IncomeToPaymentRatioSpec {
    private final int ratio;

    public boolean isSatisfiedBy(Money annualIncome, Money annualPayment) {
        Money threshold = annualPayment.multiply(ratio);
        return annualIncome.isGreaterThanOrEqual(threshold);
    }
}
```

### ClaimAmountWithinCoverageSpec（保險）

```java
public class ClaimAmountWithinCoverageSpec {
    public boolean isSatisfiedBy(Money approvedAmount, Money totalClaimed, Money deductible) {
        BigDecimal maxApprovable = totalClaimed.amount().subtract(deductible.amount());
        return approvedAmount.amount().compareTo(maxApprovable) <= 0;
    }
}
```

### OrderModificationAllowedSpec（電商）

```java
public class OrderModificationAllowedSpec {
    public boolean isSatisfiedBy(OrderStatus status) {
        return status == OrderStatus.CREATED;
    }
}
```

Specification 的 `isSatisfiedBy()` 方法回傳布林值，讓 Aggregate 決定後續行為。這比在 Service 層寫一堆 if-else 更具表達力。

## 5. Factory Pattern

### Static Factory Methods（業務建立）

M10 的三個 Aggregate 皆使用靜態工廠方法取代公開建構子。建構子設為 `private`，強制呼叫端使用語意化的工廠方法：

- `LoanApplication.submit(applicant, amount, term)` -- 建立並進入 SUBMITTED 狀態
- `Claim.file(policyRef, claimantRef, items, coverage, deductible)` -- 建立並進入 FILED 狀態
- `Order.create(orderNumber, customerId, lines, address)` -- 建立並進入 CREATED 狀態

工廠方法執行建立時的不變式檢查並產生對應的 Domain Event。

### reconstitute() 方法（持久化重建）

從 MongoDB 讀取文件後，需要重建 Aggregate 但不應觸發驗證或事件。`reconstitute()` 是專為此目的設計的工廠方法：

```java
public static LoanApplication reconstitute(String id, Applicant applicant,
                                           Money requestedAmount, LoanTerm term,
                                           LoanStatus status, String reviewResult,
                                           Instant createdAt, Instant updatedAt) {
    var app = new LoanApplication();
    app.id = id;
    // ... 直接賦值，不觸發 registerEvent()
    return app;
}
```

這個分離確保了：業務建立走 `submit()/file()/create()`，持久化重建走 `reconstitute()`。

## 6. 狀態機：OrderStatus.canTransitionTo()

Order Aggregate 實作了完整的狀態機，合法的轉換路徑定義在 enum 內部：

```java
public enum OrderStatus {
    CREATED, PAID, SHIPPED, DELIVERED, COMPLETED, RETURNED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case CREATED   -> target == PAID;
            case PAID      -> target == SHIPPED;
            case SHIPPED   -> target == DELIVERED;
            case DELIVERED -> target == COMPLETED || target == RETURNED;
            case COMPLETED -> target == RETURNED;
            case RETURNED  -> false;
        };
    }
}
```

Aggregate 內部的 `transitionTo()` 方法作為統一的狀態守衛：

```java
private void transitionTo(OrderStatus target) {
    if (!status.canTransitionTo(target)) {
        throw new IllegalStateException("Cannot transition from " + status + " to " + target);
    }
    this.status = target;
    this.updatedAt = Instant.now();
}
```

這個設計將狀態轉換規則集中在一處，所有行為方法（`pay()`、`ship()`、`deliver()` 等）都透過 `transitionTo()` 執行轉換，確保不會出現非法的狀態跳躍。

## 7. Value Objects（Java Records）

M10 大量使用 Java record 實作 Value Object，具備不可變性與建構時自我驗證。

### Money -- 金額

```java
public record Money(BigDecimal amount, String currency) {
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    public Money add(Money other) { assertSameCurrency(other); return new Money(amount.add(other.amount), currency); }
    public Money subtract(Money other) { ... }
    public boolean isGreaterThan(Money other) { ... }
}
```

### LoanTerm -- 貸款期限

```java
public record LoanTerm(int years, BigDecimal annualInterestRate) {
    public LoanTerm {
        if (years <= 0) throw new IllegalArgumentException("Years must be positive");
    }
    public Money computeAnnualPayment(Money principal) { /* 年金公式計算 */ }
}
```

### OrderLine -- 訂單明細

```java
public record OrderLine(String productId, String productName, int quantity, Money unitPrice) {
    public OrderLine {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
    }
    public Money lineTotal() { return unitPrice.multiply(quantity); }
}
```

### ShippingAddress -- 運送地址

```java
public record ShippingAddress(String recipientName, String street, String city, String zipCode) {
    public ShippingAddress {
        if (recipientName == null || recipientName.isBlank())
            throw new IllegalArgumentException("Recipient name cannot be blank");
    }
}
```

Value Object 的三大特性：(1) 不可變 -- record 天然支援；(2) 以值比較相等性 -- record 自動生成 `equals()/hashCode()`；(3) 自我驗證 -- compact constructor 中檢查不變式。

## 8. Application Service（薄編排層）

Application Service 是 Rich Domain Model 的搭配角色。它不包含業務邏輯，僅負責「載入 Aggregate、呼叫行為方法、儲存結果」的三步驟編排：

```java
@Service
public class LoanApplicationService {
    private static final int INCOME_RATIO = 5;
    private final LoanApplicationRepository repository;

    public LoanApplication performPreliminaryReview(String applicationId) {
        LoanApplication app = repository.findById(applicationId)    // 1. 載入
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        app.performPreliminaryReview(new IncomeToPaymentRatioSpec(INCOME_RATIO));  // 2. 呼叫行為
        return repository.save(app);                                // 3. 儲存
    }
}
```

三個 Application Service 遵循完全相同的模式：

| Service | 方法 | 對應 Aggregate 行為 |
|---------|------|-------------------|
| `LoanApplicationService` | `submitApplication()` | `LoanApplication.submit()` |
| `LoanApplicationService` | `performPreliminaryReview()` | `app.performPreliminaryReview(spec)` |
| `ClaimService` | `fileClaim()` | `Claim.file()` |
| `ClaimService` | `assessClaim()` | `claim.assess()` |
| `OrderService` | `createOrder()` | `Order.create()` |
| `OrderService` | `pay()` / `ship()` / `deliver()` | `order.pay()` / `order.ship()` / `order.deliver()` |

Application Service 的職責界線非常清晰：它知道「做什麼」（流程編排），但不知道「怎麼做」（業務規則）。業務規則完全封裝在 Aggregate 與 Specification 中。

## 總結

Rich Domain Model 在 MongoDB 架構中的實踐要點：

1. **Aggregate 封裝行為** -- 狀態變遷透過語意明確的方法（`submit()`、`assess()`、`pay()`），而非直接 set 欄位
2. **Domain Events 內嵌儲存** -- MongoDB 文件模型天然適合將事件陣列與 Aggregate 一起儲存，無需額外的事件表
3. **Specification 抽離規則** -- 複雜商業規則獨立為可測試的類別，Aggregate 透過組合使用
4. **Factory 分離建立語意** -- `submit()/file()/create()` 用於業務建立，`reconstitute()` 用於持久化重建
5. **狀態機集中管理** -- `canTransitionTo()` 將合法轉換定義在 enum，`transitionTo()` 作為統一守衛
6. **Value Object = Java Record** -- 不可變、自我驗證、以值比較，完美對應 DDD 概念
7. **Application Service 保持薄** -- 僅做載入、呼叫、儲存，不參與業務決策
