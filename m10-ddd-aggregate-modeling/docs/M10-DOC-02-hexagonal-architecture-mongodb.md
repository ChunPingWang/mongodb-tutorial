# Hexagonal Architecture 與 MongoDB 的整合

## 前言

在 M10 模組中，我們採用 Hexagonal Architecture（六角形架構，又稱 Ports and Adapters 架構）來組織 DDD Aggregate 的程式碼結構。此架構的核心理念是：**領域邏輯不依賴任何技術框架**，所有對外部系統（資料庫、訊息佇列、HTTP）的存取都透過 Port/Adapter 模式隔離。

本文將以 Banking 領域的 `LoanApplication` Aggregate 為主要範例，說明 Hexagonal Architecture 如何與 Spring Data MongoDB 整合。

## Port/Adapter 模式概述

Hexagonal Architecture 將系統分為三個同心圈層：

| 圈層 | 職責 | 依賴方向 |
|------|------|---------|
| **Domain Layer** | 業務規則、Aggregate、Value Object、Domain Event | 不依賴任何外部框架 |
| **Application Layer** | 協調用例（Use Case），呼叫 Port 介面 | 僅依賴 Domain Layer |
| **Infrastructure Layer** | 實作 Port 介面，連接外部技術（MongoDB、Redis...） | 依賴 Domain + Application Layer |

關鍵原則：**依賴方向永遠由外向內**。Infrastructure 依賴 Domain，而 Domain 絕不依賴 Infrastructure。

### Port（埠）

Port 是定義在 Domain Layer 中的介面，描述領域需要什麼能力，但不規定如何實現。以 `LoanApplicationRepository` 為例：

```java
// domain/port/LoanApplicationRepository.java — 純 Java 介面，零 Spring 依賴
public interface LoanApplicationRepository {
    LoanApplication save(LoanApplication application);
    Optional<LoanApplication> findById(String id);
    List<LoanApplication> findByStatus(LoanStatus status);
    List<LoanApplication> findByApplicantName(String name);
}
```

注意此介面：
- 位於 `domain.port` 套件中
- 沒有繼承 `MongoRepository` 或任何 Spring Data 介面
- 方法簽章只使用領域物件（`LoanApplication`、`LoanStatus`）
- 回傳標準 Java 型別（`Optional`、`List`）

### Adapter（轉接器）

Adapter 是 Port 的具體實作，位於 Infrastructure Layer。`MongoLoanApplicationRepository` 透過 `MongoTemplate` 將領域物件持久化到 MongoDB：

```java
// infrastructure/persistence/MongoLoanApplicationRepository.java
@Repository
public class MongoLoanApplicationRepository implements LoanApplicationRepository {

    private final MongoTemplate mongoTemplate;
    private final LoanApplicationMapper mapper;

    public MongoLoanApplicationRepository(MongoTemplate mongoTemplate,
                                          LoanApplicationMapper mapper) {
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public LoanApplication save(LoanApplication application) {
        LoanApplicationDocument doc = mapper.toDocument(application);
        LoanApplicationDocument saved = mongoTemplate.save(doc);
        application.setId(saved.getId());
        application.clearDomainEvents();
        return application;
    }

    @Override
    public Optional<LoanApplication> findById(String id) {
        LoanApplicationDocument doc = mongoTemplate.findById(id, LoanApplicationDocument.class);
        return Optional.ofNullable(doc).map(mapper::toDomain);
    }
    // ...
}
```

Adapter 的職責：
1. 將 Domain 物件透過 Mapper 轉為 `@Document` 類別
2. 使用 `MongoTemplate` 執行實際的資料庫操作
3. 將查詢結果透過 Mapper 轉回 Domain 物件

## Domain Layer：純 Java，零 Spring 依賴

### 為什麼 Domain 類別不能有 @Document、@Id、@Field？

這是 Hexagonal Architecture 最重要的設計決策。觀察 `LoanApplication` Aggregate Root：

```java
// domain/model/LoanApplication.java — 無任何 Spring 註解
public class LoanApplication {
    private String id;
    private Applicant applicant;
    private Money requestedAmount;
    private LoanTerm term;
    private LoanStatus status;
    private String reviewResult;
    private Instant createdAt;
    private Instant updatedAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    // ...
}
```

沒有 `@Document`、`@Id`、`@Field(targetType = FieldType.DECIMAL128)` 等 Spring Data MongoDB 註解。這帶來三個重要好處：

1. **可測試性**：Domain 測試不需要啟動 Spring Context 或 MongoDB 容器，毫秒級完成
2. **技術可替換性**：若未來從 MongoDB 遷移至 PostgreSQL，Domain Layer 零修改
3. **領域純粹性**：類別只表達業務概念，不受持久化格式污染

### Business Factory vs reconstitute()

`LoanApplication` 提供兩種建立方式，各有不同語意：

```java
// Business Factory — 表達業務行為，觸發 Domain Event
public static LoanApplication submit(Applicant applicant, Money requestedAmount, LoanTerm term) {
    var app = new LoanApplication();
    app.status = LoanStatus.SUBMITTED;
    app.createdAt = Instant.now();
    app.registerEvent(new LoanApplicationSubmitted(...));
    return app;
}

// Persistence Reconstitution — 從資料庫還原，不觸發 Event，不做驗證
public static LoanApplication reconstitute(String id, Applicant applicant,
                                           Money requestedAmount, LoanTerm term,
                                           LoanStatus status, String reviewResult,
                                           Instant createdAt, Instant updatedAt) {
    var app = new LoanApplication();
    app.id = id;
    app.applicant = applicant;
    // ... 直接賦值，不觸發任何事件
    return app;
}
```

| 方法 | 用途 | 觸發 Event | 執行驗證 |
|------|------|-----------|---------|
| `submit()` | Application Service 中的業務操作 | 是 | 是 |
| `reconstitute()` | Mapper 從 DB 還原物件 | 否 | 否 |

`reconstitute()` 只在 Mapper 中被呼叫，它假設資料庫中的資料已經通過了當初寫入時的業務驗證。

## Infrastructure Layer：@Document 類別與 Mapper

### @Document 類別 — 持久化專用

`LoanApplicationDocument` 是純粹的持久化模型，只服務 MongoDB 存取：

```java
// infrastructure/persistence/LoanApplicationDocument.java
@Document("m10_loan_applications")
public class LoanApplicationDocument {
    @Id
    private String id;
    private String applicantName;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal applicantAnnualIncome;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal requestedAmount;
    private String status;        // 以 String 儲存列舉
    private Instant createdAt;
    // ... getters/setters
}
```

注意 Domain 與 Document 的結構差異：

| Domain Model | Document Model | 說明 |
|-------------|---------------|------|
| `Applicant` (Value Object) | 扁平化為多個欄位 | `applicantName`、`applicantNationalId`... |
| `Money` (Value Object) | 拆分為 amount + currency | `requestedAmount` + `requestedAmountCurrency` |
| `LoanTerm` (Value Object) | 拆分為基本型別 | `termYears` + `annualInterestRate` |
| `LoanStatus` (Enum) | `String` | 避免 MongoDB 列舉序列化問題 |

這種扁平化策略讓 MongoDB 文件結構更適合查詢與索引建立，而 Domain Model 保持富物件導向設計。

### Mapper — 雙向轉換橋樑

`LoanApplicationMapper` 負責 Domain 與 Document 之間的雙向轉換：

```java
@Component
public class LoanApplicationMapper {

    public LoanApplicationDocument toDocument(LoanApplication domain) {
        var doc = new LoanApplicationDocument();
        doc.setApplicantName(domain.getApplicant().name());
        doc.setApplicantAnnualIncome(domain.getApplicant().annualIncome().amount());
        doc.setRequestedAmount(domain.getRequestedAmount().amount());
        doc.setStatus(domain.getStatus().name());
        // ...
        return doc;
    }

    public LoanApplication toDomain(LoanApplicationDocument doc) {
        var applicant = new Applicant(
            doc.getApplicantName(), doc.getApplicantNationalId(),
            new Money(doc.getApplicantAnnualIncome(), doc.getApplicantAnnualIncomeCurrency()),
            doc.getApplicantEmployer()
        );
        return LoanApplication.reconstitute(
            doc.getId(), applicant, requestedAmount, term,
            status, doc.getReviewResult(),
            doc.getCreatedAt(), doc.getUpdatedAt()
        );
    }
}
```

Mapper 的關鍵設計：
- `toDocument()`：拆解 Value Object 為扁平欄位
- `toDomain()`：從扁平欄位重組 Value Object，並使用 `reconstitute()` 還原 Aggregate

## 目錄結構

M10 的完整套件結構如下，每個 Bounded Context（banking / insurance / ecommerce）都遵循相同分層：

```
banking/
  domain/
    model/          ← Aggregate Root, Value Object, Enum
      LoanApplication.java
      Applicant.java
      Money.java
      LoanTerm.java
      LoanStatus.java
    event/          ← Domain Event
      LoanApplicationSubmitted.java
      LoanPreliminaryReviewPassed.java
      LoanPreliminaryReviewRejected.java
    port/           ← Port 介面（Repository）
      LoanApplicationRepository.java
    specification/  ← 業務規格（Specification Pattern）
      IncomeToPaymentRatioSpec.java
  application/      ← Application Service（Use Case 協調）
    LoanApplicationService.java
  infrastructure/
    persistence/    ← MongoDB Adapter 實作
      LoanApplicationDocument.java
      LoanApplicationMapper.java
      MongoLoanApplicationRepository.java
```

`domain/` 下的所有類別只 import 標準 Java API（`java.time`、`java.math`、`java.util`）。`infrastructure/` 下的類別才引入 Spring 和 MongoDB 相關依賴。

## 測試策略：三層測試金字塔

Hexagonal Architecture 的分層自然形成三層測試策略：

### 第一層：純 Domain 單元測試

```java
// 無 @SpringBootTest，無 Testcontainers，無 Spring Context
class LoanApplicationDomainTest {

    @Test
    void submit_createsWithStatusSubmitted() {
        var applicant = new Applicant("Alice", "A123", Money.twd(1_200_000), "TechCorp");
        LoanApplication app = LoanApplication.submit(applicant, Money.twd(1_000_000), STANDARD_TERM);

        assertThat(app.getStatus()).isEqualTo(LoanStatus.SUBMITTED);
        assertThat(app.getDomainEvents()).hasSize(1);
        assertThat(app.getDomainEvents().getFirst()).isInstanceOf(LoanApplicationSubmitted.class);
    }

    @Test
    void preliminaryReview_failsWhenNotSubmitted() {
        // ... 純邏輯測試，驗證狀態機與業務規則
        assertThatThrownBy(() -> app.performPreliminaryReview(spec))
                .isInstanceOf(IllegalStateException.class);
    }
}
```

特點：純 JUnit 5 + AssertJ，執行速度極快（毫秒級），覆蓋所有業務規則。

### 第二層：Adapter 整合測試

```java
@SpringBootTest
@Import(SharedContainersConfig.class)
class MongoLoanApplicationRepositoryTest {

    @Autowired
    private LoanApplicationRepository repository;  // 注入的是 Port 介面

    @Test
    void save_persistsAndRetrievesApplication() {
        LoanApplication app = LoanApplication.submit(applicant, Money.twd(1_000_000), term);
        LoanApplication saved = repository.save(app);
        assertThat(saved.getId()).isNotNull();

        Optional<LoanApplication> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getApplicant().name()).isEqualTo("Alice");
    }
}
```

特點：啟動 Spring Context + Testcontainers MongoDB，驗證 Mapper 轉換與實際資料庫讀寫正確性。

### 第三層：BDD 整合測試

以 Cucumber Feature 驅動的端對端場景測試，涵蓋完整的 Application Service 用例流程。

### 測試數量分布原則

| 測試層級 | 執行速度 | 測試數量 | 覆蓋重點 |
|---------|---------|---------|---------|
| Domain 單元測試 | 毫秒 | 最多 | 業務邏輯、狀態轉換、Domain Event |
| Adapter 整合測試 | 秒 | 適中 | Mapper 正確性、查詢正確性 |
| BDD 整合測試 | 秒 | 最少 | 端對端用例場景 |

## 總結

Hexagonal Architecture 在 M10 中實現了：

1. **Domain 純粹性**：`LoanApplication`、`Claim`、`Order` 三個 Aggregate Root 完全不依賴 Spring，是純 Java 業務物件
2. **技術隔離**：所有 MongoDB 相關的 `@Document`、`@Field`、`MongoTemplate` 操作都封裝在 `infrastructure/persistence/` 中
3. **Mapper 橋接**：`LoanApplicationMapper`、`ClaimMapper`、`OrderMapper` 負責 Domain 與 Document 的雙向轉換
4. **Port 介面解耦**：Application Service 只依賴 `LoanApplicationRepository` 介面，不知道底層是 MongoDB 還是其他資料庫
5. **測試友善**：Domain 測試零框架依賴，快速且穩定；Adapter 測試僅在需要驗證持久化正確性時才啟動容器
