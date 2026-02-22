# M18-DOC-02: 資料版本化最佳實踐

## schemaVersion 欄位設計

### 基本原則

每個文件都應包含 `schemaVersion` 整數欄位，用於識別該文件的結構版本：

```json
{
    "name": "Alice",
    "email": "alice@example.com",
    "address": { "street": "...", "city": "..." },
    "loyaltyTier": "BRONZE",
    "schemaVersion": 3
}
```

### 設計規範

- **欄位名稱**：統一使用 `schemaVersion`（不使用底線前綴）
- **型別**：整數（int），從 1 開始遞增
- **預設值**：不含此欄位的文件視為 V1（`source.getInteger("schemaVersion", 1)`）
- **單調遞增**：版本號只增不減，每次結構變更加 1

---

## Converter Chain 逐步轉換

### V1 → V2 → V3 遷移鏈

```java
@ReadingConverter
public class CustomerReadConverter implements Converter<Document, Customer> {
    public Customer convert(Document source) {
        int version = source.getInteger("schemaVersion", 1);
        if (version < 2) migrateV1toV2(source);  // 扁平地址 → 嵌入式
        if (version < 3) migrateV2toV3(source);  // 加入 loyaltyTier + registeredAt
        return mapToCustomer(source);
    }
}
```

### 鏈式遷移的好處

1. **每步簡單**：每個版本升級只處理一個變更
2. **可測試**：可以分別測試 V1→V2 和 V2→V3
3. **向後相容**：V1 文件會經過 V1→V2→V3 兩步升級
4. **新版本容易新增**：只需加入 `if (version < 4)` 區塊

### 注意事項

- 遷移邏輯在記憶體中執行，不修改原始文件
- 只有明確呼叫 `save()` 才會透過 `@WritingConverter` 持久化最新版本
- Converter 應為無狀態（stateless），不注入 Spring Bean

---

## 向後相容性檢查清單

### 新增欄位（低風險）

| 項目 | 說明 |
|------|------|
| 預設值 | 提供合理預設值（如 `loyaltyTier = "BRONZE"`） |
| Null 安全 | 舊版應用讀取新欄位會得到 null，確保不會 NPE |
| 索引 | 新欄位上的索引對不含該欄位的文件使用 Sparse Index |

### 結構重組（中風險）

| 項目 | 說明 |
|------|------|
| 雙寫期 | 遷移期間同時保留新舊結構 |
| 讀取相容 | `@ReadingConverter` 同時處理新舊結構 |
| 清理時機 | 確認所有文件已遷移後再移除舊欄位處理 |

**範例**：扁平地址（street, city, zipCode, country）→ 嵌入式地址（address: {street, city, zipCode, country}）

### 移除欄位（高風險）

| 項目 | 說明 |
|------|------|
| 延遲移除 | 先停止寫入，等所有讀取路徑不再依賴後才 `$unset` |
| 回滾準備 | 移除前備份欄位值（或確認可重建） |
| 查詢更新 | 確認沒有查詢條件依賴被移除的欄位 |

### 型別變更（最高風險）

| 項目 | 說明 |
|------|------|
| 新欄位名 | 建議使用新欄位名而非原地型別轉換 |
| 轉換邏輯 | 在 Converter 中處理舊型別到新型別的轉換 |
| 索引重建 | 型別變更通常需要重建相關索引 |

---

## 測試矩陣：版本交叉讀寫驗證

### 讀取驗證

| 文件版本 | 讀取結果 | 驗證項目 |
|----------|----------|----------|
| V1 | Customer（V3 記憶體物件） | address 嵌入式、loyaltyTier="BRONZE"、registeredAt=EPOCH |
| V2 | Customer（V3 記憶體物件） | loyaltyTier="BRONZE"、registeredAt=EPOCH |
| V3 | Customer（V3 記憶體物件） | 所有欄位正確映射 |

### 寫入驗證

| 原始版本 | 讀取後儲存 | 驗證項目 |
|----------|-----------|----------|
| V1 → save | V3 文件 | schemaVersion=3、扁平欄位移除、address 嵌入式 |
| V2 → save | V3 文件 | schemaVersion=3、loyaltyTier 存在 |
| V3 → save | V3 文件 | 無變更 |

### 資料完整性

| 驗證項目 | 說明 |
|----------|------|
| 欄位保留 | name, email, phone 遷移前後一致 |
| 地址資料 | street, city, zipCode, country 值未丟失 |
| 新欄位預設值 | 確認預設值符合業務規則 |

---

## Rollback 設計原則

### Mongock Rollback

```java
@RollbackExecution
public void rollback(MongoTemplate mongoTemplate) {
    collection.updateMany(
        Filters.exists("riskScore"),
        Updates.combine(
            Updates.unset("riskScore"),
            Updates.unset("region"),
            Updates.set("schemaVersion", 1)
        )
    );
}
```

### Rollback 最佳實踐

1. **每個 ChangeUnit 都必須有 @RollbackExecution**
2. **回滾必須是冪等的**：多次執行結果相同
3. **回滾只移除新增的欄位**：不要嘗試恢復被修改的值
4. **回滾要重設 schemaVersion**：確保版本號回到前一版
5. **測試回滾**：正向遷移和回滾都需要獨立的測試案例

### Lazy Migration 不需要回滾

`@ReadingConverter` 的惰性遷移天然支援回滾：
- 原始文件未被修改
- 移除 Converter 即可「回滾」
- 已透過 save 升級的文件需要另外處理

---

## 實務建議

### 遷移腳本原則

1. **冪等性**：遷移腳本重複執行不應產生副作用
2. **小步前進**：每次遷移只做一件事
3. **可驗證**：遷移後可透過 `countPerVersion()` 驗證結果
4. **有稽核**：Mongock 自動記錄遷移紀錄於 `mongockChangeLog`

### 版本共存期間

1. **應用程式必須能讀取所有版本**
2. **新寫入永遠使用最新版本**
3. **背景遷移在低流量時段執行**
4. **監控版本分佈直到全部升級**

### 效能考量

- Eager：大量 `updateMany` 會產生 oplog 壓力，考慮分批執行
- Lazy：Converter 轉換在記憶體中極快，通常不是瓶頸
- 混合策略：先用 Lazy 確保讀取相容，再用背景批次最終統一
