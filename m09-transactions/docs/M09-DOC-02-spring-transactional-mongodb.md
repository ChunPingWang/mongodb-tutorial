# M09-DOC-02：Spring @Transactional 與 MongoDB

## 1. MongoTransactionManager 配置

Spring Boot **不會**自動配置 `MongoTransactionManager`。必須手動宣告：

```java
@Configuration
public class TransactionConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
```

> **重要**：如果沒有這個 Bean，`@Transactional` 會被靜默忽略——所有操作變成非交易模式，不會有任何錯誤訊息。

### 為什麼 Spring Boot 不自動配置？

因為 MongoDB 交易需要 Replica Set，而 Spring Boot 無法確定目標 MongoDB 是否為 Replica Set。自動配置可能在 Standalone 模式下導致錯誤。

## 2. @Transactional 宣告式交易

### 基本用法

```java
@Service
public class TransferService {

    private final MongoTemplate mongoTemplate;

    @Transactional
    public void transfer(String from, String to, BigDecimal amount) {
        // 扣款
        mongoTemplate.updateFirst(
            Query.query(Criteria.where("accountNumber").is(from)),
            new Update().inc("balance", amount.negate()),
            BankAccount.class
        );
        // 入帳
        mongoTemplate.updateFirst(
            Query.query(Criteria.where("accountNumber").is(to)),
            new Update().inc("balance", amount),
            BankAccount.class
        );
        // 記錄
        mongoTemplate.insert(new TransferRecord(from, to, amount));
    }
}
```

### Rollback 規則

| 例外類型 | 預設行為 | 自訂 |
|----------|---------|------|
| `RuntimeException` | **自動回滾** | — |
| `Error` | **自動回滾** | — |
| Checked Exception | **不回滾** | `@Transactional(rollbackFor = Exception.class)` |

```java
// 讓所有例外都觸發回滾
@Transactional(rollbackFor = Exception.class)
public void riskyOperation() throws IOException {
    // ...
}
```

### Propagation 傳播行為

| 傳播類型 | 說明 |
|---------|------|
| `REQUIRED`（預設） | 加入現有交易，沒有則建立新的 |
| `REQUIRES_NEW` | 總是建立新交易，暫停現有交易 |
| `NESTED` | MongoDB **不支援**巢狀交易 |
| `SUPPORTS` | 有交易就加入，沒有就非交易執行 |

## 3. 常見陷阱

### 陷阱一：Self-Invocation（同類別內部呼叫）

```java
@Service
public class MyService {

    @Transactional
    public void methodA() {
        // ...
    }

    public void methodB() {
        methodA();  // ❌ 不會有交易！因為繞過了代理
    }
}
```

**原因**：Spring `@Transactional` 透過 AOP 代理實作。同類別內部呼叫不經過代理，因此交易註解被忽略。

**解決方案**：
1. 將交易方法搬到另一個 `@Service` 類別
2. 注入自身的代理（不推薦）
3. 使用 programmatic transaction

### 陷阱二：忘記宣告 MongoTransactionManager

```java
@Transactional
public void transfer(...) {
    // 看起來有交易，實際上沒有！
    // 不會有錯誤訊息，只是靜默失敗
}
```

**驗證方式**：在測試中刻意拋出例外，檢查是否真的回滾了。

### 陷阱三：Checked Exception 不回滾

```java
@Transactional
public void process() throws BusinessException {  // checked exception
    mongoTemplate.insert(doc1);
    throw new BusinessException("fail");  // ❌ 不會回滾！
}
```

**解決**：`@Transactional(rollbackFor = BusinessException.class)` 或將例外改為 `RuntimeException`。

### 陷阱四：長時間交易

```java
@Transactional
public void batchProcess() {
    for (int i = 0; i < 100000; i++) {
        mongoTemplate.insert(doc);  // ❌ 可能超過 60 秒超時或 16MB 限制
    }
}
```

**解決**：分批處理，每批使用獨立交易。

## 4. TransactionTemplate（程式化交易）

當需要更細粒度的控制時，使用 `TransactionTemplate`：

```java
@Autowired
MongoTransactionManager transactionManager;

public void transferWithTemplate(String from, String to, BigDecimal amount) {
    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

    txTemplate.execute(new TransactionCallbackWithoutResult() {
        @Override
        protected void doInTransactionWithoutResult(TransactionStatus status) {
            // 扣款
            mongoTemplate.updateFirst(...);
            // 入帳
            mongoTemplate.updateFirst(...);
            // 記錄
            mongoTemplate.insert(record);
        }
    });
}
```

### 有返回值的版本

```java
TransferRecord result = txTemplate.execute(status -> {
    mongoTemplate.updateFirst(...);
    mongoTemplate.updateFirst(...);
    return mongoTemplate.insert(record);
});
```

### 手動回滾

```java
txTemplate.execute(status -> {
    mongoTemplate.updateFirst(...);
    if (someCondition) {
        status.setRollbackOnly();  // 標記回滾
    }
    return null;
});
```

## 5. ClientSession（最低層 API）

直接使用 MongoDB Driver 的 `ClientSession`：

```java
@Autowired
MongoClient mongoClient;

public void transferWithSession() {
    try (ClientSession session = mongoClient.startSession()) {
        session.startTransaction();
        try {
            var collection = mongoClient.getDatabase("mydb")
                .getCollection("accounts");

            collection.updateOne(session, filter1, update1);
            collection.updateOne(session, filter2, update2);

            session.commitTransaction();
        } catch (Exception e) {
            session.abortTransaction();
            throw e;
        }
    }
}
```

### 三種方式比較

| 方式 | 適用場景 | 優點 | 缺點 |
|------|---------|------|------|
| `@Transactional` | 大多數業務場景 | 宣告式、簡潔 | 受代理限制（self-invocation） |
| `TransactionTemplate` | 需要細粒度控制 | 可程式化控制回滾 | 較冗長 |
| `ClientSession` | 需要直接操作 Driver | 完全控制 | 最冗長、不使用 Spring 抽象 |

## 6. 小結

- Spring Boot 不會自動配置 `MongoTransactionManager`，必須手動宣告
- `@Transactional` 預設只在 `RuntimeException` 時回滾
- 注意 Self-Invocation 陷阱
- `TransactionTemplate` 提供程式化交易控制
- `ClientSession` 是最低層 API，提供完全控制
