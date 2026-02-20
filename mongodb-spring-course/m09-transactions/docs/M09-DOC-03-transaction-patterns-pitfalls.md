# M09-DOC-03：Transaction 實戰模式與陷阱

## 1. 銀行轉帳模式（Transfer Pattern）

### 設計要點

```
交易開始
  ├── 驗證來源帳戶存在且狀態正常
  ├── 驗證目標帳戶存在且狀態正常
  ├── 驗證來源帳戶餘額充足
  ├── 扣款（$inc balance -amount）
  ├── 入帳（$inc balance +amount）
  └── 建立轉帳記錄
交易提交
```

### 關鍵決策

1. **使用 `$inc` 而非 `$set`**：`$inc` 是原子操作，避免 read-modify-write 競態條件
2. **先驗證再操作**：在交易內先讀取並驗證，確保業務規則通過
3. **TransferRecord 作為審計軌跡**：在同一交易中建立，確保一致性

### 程式碼實作

```java
@Transactional
public TransferRecord transfer(String from, String to, BigDecimal amount) {
    // 1. 驗證
    BankAccount fromAccount = findByNumber(from);
    BankAccount toAccount = findByNumber(to);
    if (fromAccount.getBalance().compareTo(amount) < 0) {
        throw new InsufficientBalanceException("餘額不足");
    }

    // 2. 扣款
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("accountNumber").is(from)),
        new Update().inc("balance", amount.negate()),
        BankAccount.class
    );

    // 3. 入帳
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("accountNumber").is(to)),
        new Update().inc("balance", amount),
        BankAccount.class
    );

    // 4. 記錄
    return mongoTemplate.insert(new TransferRecord(from, to, amount, SUCCESS));
}
```

## 2. 保險核保模式（Multi-Collection Commit）

### 設計要點

```
交易開始
  ├── 查詢客戶（驗證存在且非停權）
  ├── 新增保單文件
  ├── 新增收費排程文件
  └── 更新客戶（狀態 → ACTIVE, policyCount++）
交易提交
```

### 跨集合一致性

此模式涉及 3 個集合：
- `insurance_customers`（更新）
- `insurance_policies`（新增）
- `billing_schedules`（新增）

如果沒有交易，可能出現：
- 保單已建立但收費排程未建立 → 無法收費
- 收費排程已建立但客戶狀態未更新 → 客戶資料不一致
- 保單和收費排程都建立了但客戶的 policyCount 沒更新 → 統計錯誤

## 3. 併發衝突（WriteConflict）

### 什麼時候發生？

當兩個交易同時修改**同一份文件**時，後提交的交易會收到 `WriteConflict`。

```
Transaction A                    Transaction B
    |                                |
    |  updateOne(doc1)              |
    |                               |  updateOne(doc1)  ← 衝突！
    |  commitTransaction()          |
    |                               |  ← WriteConflict
```

### 如何處理？

#### 方法一：應用層重試

```java
public void transferWithRetry(String from, String to, BigDecimal amount) {
    int maxRetries = 3;
    for (int i = 0; i < maxRetries; i++) {
        try {
            transferService.transfer(from, to, amount);
            return;  // 成功
        } catch (Exception e) {
            if (isWriteConflict(e) && i < maxRetries - 1) {
                // 短暫等待後重試
                Thread.sleep(100 * (i + 1));
                continue;
            }
            throw e;
        }
    }
}
```

#### 方法二：MongoDB 內建重試

MongoDB Driver 4.2+ 支援 `withTransaction()` 自動重試：

```java
session.withTransaction(() -> {
    collection.updateOne(session, filter1, update1);
    collection.updateOne(session, filter2, update2);
    return null;
});  // 自動重試 WriteConflict
```

### 降低衝突的設計策略

| 策略 | 說明 |
|------|------|
| 減小交易範圍 | 只包含必要的操作，減少持有鎖的時間 |
| 使用 `$inc` | 避免 read-modify-write 模式 |
| 文件設計 | 將經常一起修改的資料放在同一文件 |
| 避免熱點文件 | 分散寫入到不同文件 |

## 4. 常見陷阱與解決方案

### 陷阱一：交易超時（60 秒）

```java
@Transactional
public void slowOperation() {
    for (var doc : largeCollection) {
        // 處理每一份文件...
        mongoTemplate.save(doc);
    }
    // ❌ 超過 60 秒 → TransactionExceededLifetimeLimitSeconds
}
```

**解決**：分批處理

```java
public void processInBatches(List<Document> docs) {
    int batchSize = 100;
    for (int i = 0; i < docs.size(); i += batchSize) {
        var batch = docs.subList(i, Math.min(i + batchSize, docs.size()));
        processBatch(batch);  // 每批一個交易
    }
}

@Transactional
public void processBatch(List<Document> batch) {
    batch.forEach(doc -> mongoTemplate.save(doc));
}
```

### 陷阱二：16MB oplog 限制

單次交易產生的 oplog entry 不能超過 16MB。插入大量文件或修改大型文件時可能觸發。

**解決**：減小交易中的操作數量，或拆分為多個交易。

### 陷阱三：交易內建立集合

```java
@Transactional
public void createAndInsert() {
    // 如果 newCollection 不存在：
    mongoTemplate.insert(doc, "newCollection");
    // MongoDB 4.2: ❌ 失敗
    // MongoDB 4.4+: ✅ 允許（隱式建立集合）
}
```

**解決**：在交易開始前確保集合已存在。

### 陷阱四：忘記非交易讀取的影響

```java
@Transactional
public void process() {
    mongoTemplate.insert(doc);
    // 在同一個交易中可以讀到剛插入的 doc

    // ⚠️ 另一個非交易的讀取看不到這份 doc（直到 commit）
}
```

## 5. 效能影響

### 交易開銷

| 操作 | 非交易 | 交易模式 |
|------|--------|---------|
| 單次寫入延遲 | ~1ms | ~2-5ms |
| 吞吐量 | 高 | 降低 10-30% |
| 記憶體 | 正常 | 快照需要額外記憶體 |
| 鎖競爭 | 低 | 可能增加 |

### 最佳實踐

1. **保持交易簡短**：盡量在 1 秒內完成
2. **避免交易中的外部呼叫**：HTTP、MQ 等不應在交易內
3. **先處理業務邏輯，再寫入**：減少交易持有時間
4. **監控交易指標**：
   ```javascript
   db.serverStatus().transactions
   // totalStarted, totalCommitted, totalAborted
   ```

## 6. 什麼時候不使用交易

| 情境 | 建議 |
|------|------|
| 單一文件更新 | 天然原子性，不需要交易 |
| 嵌入式模型 | 文件設計已解決一致性 |
| 寫入密集場景 | Change Stream + 補償事務 |
| 最終一致性可接受 | Saga 模式 |
| 跨服務操作 | Saga/Outbox 模式 |

## 7. 小結

- 銀行轉帳模式：驗證 → $inc 扣款 → $inc 入帳 → 建立記錄
- 核保模式：跨 3 個集合的原子操作
- WriteConflict 透過重試或設計優化解決
- 注意超時、oplog 限制、集合建立等陷阱
- 交易有效能開銷，文件設計優先
