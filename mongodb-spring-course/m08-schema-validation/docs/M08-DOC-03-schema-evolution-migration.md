# M08-DOC-03: Schema 演進與文件遷移策略

## 目錄

- [簡介](#簡介)
- [Schema 演進的挑戰](#schema-演進的挑戰)
  - [MongoDB vs RDB 的演進差異](#mongodb-vs-rdb-的演進差異)
  - [為什麼需要遷移策略](#為什麼需要遷移策略)
- [schemaVersion 欄位模式](#schemaversion-欄位模式)
  - [版本化文件設計](#版本化文件設計)
  - [ProductV1 → V2 → V3 範例](#productv1--v2--v3-範例)
- [DocumentMigrator 介面](#documentmigrator-介面)
  - [介面定義](#介面定義)
  - [V1 → V2 遷移器](#v1--v2-遷移器)
  - [V2 → V3 遷移器](#v2--v3-遷移器)
- [MigrationService 鏈式遷移](#migrationservice-鏈式遷移)
  - [遷移編排](#遷移編排)
  - [完整遷移鏈](#完整遷移鏈)
- [SchemaEvolutionService 實作](#schemaevolutionservice-實作)
  - [版本化查詢與遷移](#版本化查詢與遷移)
  - [冪等遷移](#冪等遷移)
  - [moderate 混合版本共存](#moderate-混合版本共存)
- [遷移策略比較](#遷移策略比較)
  - [Eager Migration (即時遷移)](#eager-migration-即時遷移)
  - [Lazy Migration (惰性遷移)](#lazy-migration-惰性遷移)
  - [Background Migration (背景遷移)](#background-migration-背景遷移)
- [與 RDB ALTER TABLE 比較](#與-rdb-alter-table-比較)
- [小結](#小結)

---

## 簡介

MongoDB 的 Schema-flexible 特性讓新增欄位不需要 `ALTER TABLE`,但這也帶來新的挑戰:同一集合中可能混合不同版本的文件。本文件介紹如何透過 `schemaVersion` 欄位、`DocumentMigrator` 鏈式遷移、以及 `moderate` 驗證模式,安全地演進 Schema 並遷移既有文件。

本文件涵蓋:
- `schemaVersion` 欄位版本化模式
- `DocumentMigrator` 介面與遷移器實作
- `MigrationService` 鏈式遷移編排
- 冪等遷移保證
- Eager/Lazy/Background 三種遷移策略
- `moderate` 驗證模式支援混合版本共存

---

## Schema 演進的挑戰

### MongoDB vs RDB 的演進差異

```
RDB Schema 演進:
┌────────────────────────────────────────────┐
│ ALTER TABLE products ADD COLUMN category   │ ← 鎖表、所有 Row 立即更新
│ ALTER TABLE products ADD COLUMN tags       │ ← 可能需要停機
│ 所有資料保證一致結構                         │
└────────────────────────────────────────────┘

MongoDB Schema 演進:
┌────────────────────────────────────────────┐
│ 新文件自然包含新欄位                         │ ← 不需要 ALTER TABLE
│ 舊文件不受影響,但缺少新欄位                  │ ← 混合版本共存
│ 需要遷移策略處理舊文件                       │
└────────────────────────────────────────────┘
```

### 為什麼需要遷移策略

當 Product 從 V1 演進到 V3:

| 版本 | 欄位 | 新增 |
|------|------|------|
| V1 | name, price, inStock | 初始版本 |
| V2 | + category, tags | 分類與標籤功能 |
| V3 | + rating, description, specifications | 評分與規格功能 |

沒有遷移策略的問題:
1. 查詢 `category` 欄位時,V1 文件回傳 `null`
2. 聚合統計 `rating` 時,V1/V2 文件被跳過或產生錯誤
3. 前端收到不一致的資料結構

---

## schemaVersion 欄位模式

### 版本化文件設計

每個文件包含 `schemaVersion` 欄位,標識該文件遵循的 Schema 版本:

```json
// V1 文件
{
  "name": "Laptop",
  "price": NumberDecimal("999"),
  "inStock": true,
  "schemaVersion": 1
}

// V2 文件
{
  "name": "Laptop",
  "price": NumberDecimal("999"),
  "inStock": true,
  "category": "Electronics",
  "tags": ["tech", "computer"],
  "schemaVersion": 2
}

// V3 文件
{
  "name": "Laptop",
  "price": NumberDecimal("999"),
  "inStock": true,
  "category": "Electronics",
  "tags": ["tech", "computer"],
  "rating": 4.5,
  "description": "High performance laptop",
  "specifications": {"cpu": "i7", "ram": "16GB"},
  "schemaVersion": 3
}
```

### ProductV1 → V2 → V3 範例

```java
// V1: 最小欄位
@Document("m08_product_versions")
public class ProductV1 {
    @Id private String id;
    private String name;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;
    private boolean inStock;
    private int schemaVersion = 1;
}

// V2: 加入分類與標籤
@Document("m08_product_versions")
public class ProductV2 {
    // ... V1 欄位 +
    private String category;
    private List<String> tags = new ArrayList<>();
    private int schemaVersion = 2;
}

// V3: 加入評分、描述、規格
@Document("m08_product_versions")
public class ProductV3 {
    // ... V2 欄位 +
    private double rating;
    private String description;
    private Map<String, String> specifications = new HashMap<>();
    private int schemaVersion = 3;
}
```

---

## DocumentMigrator 介面

### 介面定義

```java
public interface DocumentMigrator {
    int fromVersion();    // 來源版本
    int toVersion();      // 目標版本
    Document migrate(Document document);  // 遷移邏輯
}
```

每個遷移器負責**一個版本跳躍** (V1→V2 或 V2→V3),不做跨版本跳躍。

### V1 → V2 遷移器

```java
@Component
public class ProductV1ToV2Migrator implements DocumentMigrator {

    @Override
    public int fromVersion() { return 1; }

    @Override
    public int toVersion() { return 2; }

    @Override
    public Document migrate(Document document) {
        document.put("category", "Uncategorized");  // 預設分類
        document.put("tags", List.of());             // 空標籤列表
        document.put("schemaVersion", 2);
        return document;
    }
}
```

遷移策略:
- `category`: 設為 `"Uncategorized"` (安全預設值)
- `tags`: 設為空列表 (不是 null)
- `schemaVersion`: 更新為 2

### V2 → V3 遷移器

```java
@Component
public class ProductV2ToV3Migrator implements DocumentMigrator {

    @Override
    public int fromVersion() { return 2; }

    @Override
    public int toVersion() { return 3; }

    @Override
    public Document migrate(Document document) {
        document.put("rating", 0.0);
        document.put("description", "");
        document.put("specifications", new Document());
        document.put("schemaVersion", 3);
        return document;
    }
}
```

---

## MigrationService 鏈式遷移

### 遷移編排

```java
@Service
public class MigrationService {

    private final List<DocumentMigrator> migrators;

    public MigrationService(List<DocumentMigrator> migrators) {
        this.migrators = migrators.stream()
                .sorted(Comparator.comparingInt(DocumentMigrator::fromVersion))
                .toList();
    }

    public Document migrateToVersion(Document document, int targetVersion) {
        int currentVersion = document.getInteger("schemaVersion", 1);
        for (DocumentMigrator migrator : migrators) {
            if (migrator.fromVersion() == currentVersion
                    && currentVersion < targetVersion) {
                document = migrator.migrate(document);
                currentVersion = migrator.toVersion();
            }
        }
        return document;
    }
}
```

關鍵設計:
1. **排序**: 依 `fromVersion` 排序,確保遷移順序正確
2. **鏈式**: V1→V2→V3,每次只跳一個版本
3. **安全**: `currentVersion < targetVersion` 防止無窮迴圈

### 完整遷移鏈

```
V1 文件 ──→ V1ToV2Migrator ──→ V2ToV3Migrator ──→ V3 文件
   │              │                   │
   │  category="Uncategorized"   rating=0.0
   │  tags=[]                    description=""
   │  schemaVersion=2            specifications={}
   │                             schemaVersion=3
```

---

## SchemaEvolutionService 實作

### 版本化查詢與遷移

```java
@Service
public class SchemaEvolutionService {

    private static final String COLLECTION = "m08_product_versions";
    private final MongoTemplate mongoTemplate;
    private final MigrationService migrationService;

    public int migrateToVersion(int targetVersion) {
        // 查找所有版本低於目標的文件
        Query query = Query.query(Criteria.where("schemaVersion").lt(targetVersion));
        List<Document> docs = mongoTemplate.find(query, Document.class, COLLECTION);

        int count = 0;
        for (Document doc : docs) {
            Document migrated = migrationService.migrateToVersion(doc, targetVersion);
            mongoTemplate.getCollection(COLLECTION).replaceOne(
                    new Document("_id", doc.get("_id")),
                    migrated
            );
            count++;
        }
        return count;
    }
}
```

### 冪等遷移

遷移操作是冪等的——重複執行不會產生副作用:

```java
// 第一次遷移: V1 → V3,回傳遷移數量
int firstRun = schemaEvolutionService.migrateToVersion(3);
assertThat(firstRun).isEqualTo(3);  // 3 筆 V1 文件被遷移

// 第二次遷移: 所有文件已是 V3,回傳 0
int secondRun = schemaEvolutionService.migrateToVersion(3);
assertThat(secondRun).isEqualTo(0);  // 無需遷移
```

冪等性保證來自:
1. 遷移後 `schemaVersion` 更新為目標版本
2. 查詢條件 `schemaVersion < targetVersion` 自動排除已遷移文件

### moderate 混合版本共存

在遷移期間,集合中可能同時存在 V1、V2、V3 文件。使用 `moderate` 驗證模式:

```java
public void applyVersionedSchemaModerate() {
    MongoJsonSchema schema = MongoJsonSchema.builder()
            .properties(
                    required(string("name")),
                    required(number("price")),
                    required(int32("schemaVersion"))
            )
            .build();

    Document command = new Document("collMod", COLLECTION)
            .append("validator", schema.toDocument())
            .append("validationLevel", "moderate")
            .append("validationAction", "error");
    mongoTemplate.getDb().runCommand(command);
}
```

```
moderate 驗證 + 混合版本:
┌──────────────────────────────────────────────┐
│ V1 文件 (缺少 category, tags, rating...)     │ ← 既有文件,moderate 允許更新
│ V2 文件 (缺少 rating, description...)        │ ← 既有文件,moderate 允許更新
│ V3 文件 (完整欄位)                            │ ← 符合 Schema
│ 新插入文件                                    │ ← 必須符合 Schema (至少有 name, price, schemaVersion)
└──────────────────────────────────────────────┘
```

---

## 遷移策略比較

### Eager Migration (即時遷移)

一次性遷移所有文件到最新版本:

```java
schemaEvolutionService.migrateToVersion(3);
```

| 優點 | 缺點 |
|------|------|
| 遷移後所有文件一致 | 大量文件時耗時長 |
| 查詢不需處理多版本 | 遷移期間可能影響效能 |
| 程式碼簡單 | 需要維護視窗 |

**適合**: 文件數量可控 (< 100 萬筆)、可接受短暫效能影響。

### Lazy Migration (惰性遷移)

讀取時遷移,只有被存取的文件才會升級:

```java
public Product findAndMigrate(String id) {
    Document doc = mongoTemplate.findById(id, Document.class, COLLECTION);
    int version = doc.getInteger("schemaVersion", 1);
    if (version < LATEST_VERSION) {
        doc = migrationService.migrateToLatest(doc);
        mongoTemplate.getCollection(COLLECTION).replaceOne(
                new Document("_id", doc.get("_id")), doc);
    }
    return convertToProduct(doc);
}
```

| 優點 | 缺點 |
|------|------|
| 不需要維護視窗 | 讀取延遲增加 |
| 只遷移活躍資料 | 冷資料永遠不會遷移 |
| 分散遷移負載 | 查詢需處理多版本 |

**適合**: 大量文件但活躍比例低、無法停機。

### Background Migration (背景遷移)

使用排程任務在背景逐批遷移:

```java
@Scheduled(fixedRate = 60000)
public void backgroundMigration() {
    Query query = Query.query(Criteria.where("schemaVersion").lt(LATEST_VERSION))
            .limit(1000);  // 每批 1000 筆
    // ... 遷移邏輯
}
```

| 優點 | 缺點 |
|------|------|
| 不影響前台效能 | 實作較複雜 |
| 可控制遷移速率 | 完全遷移需要時間 |
| 支援暫停/恢復 | 需要監控遷移進度 |

**適合**: 大規模生產環境、需要零停機。

---

## 與 RDB ALTER TABLE 比較

| 特性 | MongoDB Schema 演進 | RDB ALTER TABLE |
|------|---------------------|-----------------|
| 新增欄位 | 新文件自動包含,舊文件不變 | 所有 Row 立即更新 |
| 停機需求 | 不需要 | 大表可能需要 |
| 資料一致性 | 需要遷移策略 | ALTER 後保證一致 |
| 回滾難度 | 低 (保留舊版本欄位) | 高 (需要反向 ALTER) |
| 預設值 | 遷移時設定 | ALTER TABLE SET DEFAULT |
| 混合版本 | 自然支援 (moderate) | 不存在此概念 |
| 適合的規模 | 任意規模 | 小中型表較容易 |
| 複雜度 | 遷移器鏈 + 版本管理 | DDL 語法 |

---

## 小結

| 概念 | 說明 |
|------|------|
| `schemaVersion` | 文件版本標記,識別 Schema 版本 |
| `DocumentMigrator` | 遷移器介面,負責單一版本跳躍 |
| `MigrationService` | 遷移編排,鏈式串接多個遷移器 |
| 鏈式遷移 | V1→V2→V3,依序經過每個遷移器 |
| 冪等遷移 | 重複執行不產生副作用 |
| `moderate` | 允許混合版本文件共存 |
| Eager | 一次性遷移所有文件 |
| Lazy | 讀取時遷移,只升級活躍資料 |
| Background | 背景排程逐批遷移 |
| `replaceOne` | 用遷移後的文件取代原始文件 |

M08 模組完成了 Schema Validation 的完整教學:從 `$jsonSchema` 資料庫驗證、Jakarta Bean Validation 應用層驗證、雙層驗證策略,到 Schema 演進與文件遷移。這些技巧是 MongoDB 生產環境中維護資料品質的關鍵實踐。
