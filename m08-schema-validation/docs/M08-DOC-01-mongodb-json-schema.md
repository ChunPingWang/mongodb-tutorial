# M08-DOC-01: MongoDB $jsonSchema 驗證機制

## 目錄

- [簡介](#簡介)
- [為什麼需要 Schema 驗證](#為什麼需要-schema-驗證)
- [$jsonSchema 語法](#jsonschema-語法)
  - [基本結構](#基本結構)
  - [必要欄位 (required)](#必要欄位-required)
  - [型別約束 (bsonType)](#型別約束-bsontype)
  - [字串約束](#字串約束)
  - [列舉值 (enum)](#列舉值-enum)
- [Spring Data MongoDB 整合](#spring-data-mongodb-整合)
  - [MongoJsonSchema Builder API](#mongojsonschema-builder-api)
  - [JsonSchemaProperty 靜態方法](#jsonschemaproperty-靜態方法)
  - [CollectionOptions 建立集合](#collectionoptions-建立集合)
- [Validation Level 驗證層級](#validation-level-驗證層級)
  - [strict 模式](#strict-模式)
  - [moderate 模式](#moderate-模式)
  - [off 模式](#off-模式)
- [Validation Action 驗證動作](#validation-action-驗證動作)
  - [error 動作](#error-動作)
  - [warn 動作](#warn-動作)
- [對既有集合套用 Schema](#對既有集合套用-schema)
- [小結](#小結)

---

## 簡介

MongoDB 是 Schema-flexible 的文件資料庫,文件結構可隨時變化。但在實務環境中,完全無約束的靈活性可能帶來資料品質問題。MongoDB 3.6 起支援 `$jsonSchema` 驗證,讓資料庫層也能強制約束文件結構。

本文件涵蓋:
- `$jsonSchema` 驗證語法:required、型別、字串長度、列舉
- Spring Data MongoDB 的 `MongoJsonSchema.builder()` API
- Validation Level (strict/moderate) 與 Validation Action (error/warn)
- 對既有集合套用 Schema 的 `collMod` 指令

---

## 為什麼需要 Schema 驗證

```
Schema-on-Write (傳統 RDB)           Schema-on-Read (MongoDB 預設)
┌────────────────────────┐          ┌────────────────────────┐
│ 寫入前強制欄位與型別    │          │ 寫入時不檢查結構        │
│ ALTER TABLE 修改很痛苦  │          │ 靈活但可能存入髒資料    │
└────────────────────────┘          └────────────────────────┘

                    $jsonSchema 驗證
            ┌────────────────────────────┐
            │ 彈性 Schema + 可選驗證規則  │
            │ 兩全其美的中間路線          │
            └────────────────────────────┘
```

常見需求場景:
1. **必要欄位保證**: 確保每筆訂單都有 `orderNumber`、`customerName`
2. **型別一致性**: 避免 `price` 欄位被存入字串 `"abc"`
3. **列舉值控制**: `status` 只接受 `ACTIVE`、`CLOSED`、`FROZEN`
4. **字串格式約束**: 帳號長度 5-20 字元

---

## $jsonSchema 語法

### 基本結構

MongoDB 的 `$jsonSchema` 基於 JSON Schema Draft 4,使用 `bsonType` 擴充支援 BSON 型別:

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "required": ["accountNumber", "holderName", "type", "balance", "status"],
    "properties": {
      "accountNumber": {
        "bsonType": "string",
        "minLength": 5,
        "maxLength": 20
      },
      "holderName": {
        "bsonType": "string"
      },
      "type": {
        "bsonType": "string",
        "enum": ["SAVINGS", "CHECKING"]
      },
      "balance": {
        "bsonType": "number"
      },
      "status": {
        "bsonType": "string",
        "enum": ["ACTIVE", "CLOSED", "FROZEN"]
      }
    }
  }
}
```

### 必要欄位 (required)

`required` 陣列列出文件必須包含的欄位名稱。缺少任一欄位的文件會被拒絕:

```javascript
// 這筆文件缺少 holderName → 被拒絕
db.m08_bank_accounts.insertOne({
  accountNumber: "ACC-12345",
  type: "SAVINGS",
  balance: NumberDecimal("1000"),
  status: "ACTIVE"
})
// Error: Document failed validation
```

### 型別約束 (bsonType)

| bsonType | 對應 BSON 型別 | 說明 |
|----------|---------------|------|
| `"string"` | String | 字串 |
| `"number"` | int/long/double/decimal | 所有數值型別 (含 Decimal128) |
| `"int"` | 32-bit integer | 整數 |
| `"long"` | 64-bit integer | 長整數 |
| `"double"` | 64-bit floating point | 浮點數 |
| `"decimal"` | Decimal128 | 高精度十進位 |
| `"bool"` | Boolean | 布林值 |
| `"object"` | Object/Document | 內嵌文件 |
| `"array"` | Array | 陣列 |
| `"objectId"` | ObjectId | MongoDB ObjectId |
| `"date"` | Date | 日期 |

> **注意**: `"number"` 是 MongoDB 擴充的別名型別,同時涵蓋 int、long、double、decimal。適合用在不限定精確數值型別的欄位 (如 `balance`)。

### 字串約束

```json
{
  "accountNumber": {
    "bsonType": "string",
    "minLength": 5,
    "maxLength": 20
  }
}
```

- `minLength`: 最小字元數,`"AB"` (長度 2) 不符合 `minLength: 5`
- `maxLength`: 最大字元數

### 列舉值 (enum)

```json
{
  "type": {
    "bsonType": "string",
    "enum": ["SAVINGS", "CHECKING"]
  }
}
```

不在列表中的值 (如 `"INVALID"`) 會被拒絕。

---

## Spring Data MongoDB 整合

### MongoJsonSchema Builder API

Spring Data MongoDB 提供流暢的 Java API 建構 `$jsonSchema`:

```java
import static org.springframework.data.mongodb.core.schema.JsonSchemaProperty.*;

MongoJsonSchema schema = MongoJsonSchema.builder()
        .properties(
                required(string("accountNumber").minLength(5).maxLength(20)),
                required(string("holderName")),
                required(string("type").possibleValues("SAVINGS", "CHECKING")),
                required(number("balance")),
                required(string("status").possibleValues("ACTIVE", "CLOSED", "FROZEN"))
        )
        .build();
```

關鍵模式:
1. 使用 `JsonSchemaProperty` 的靜態方法 (`string()`, `number()`, `int32()` 等)
2. `required()` 包裝器標記欄位為必要
3. `possibleValues()` 設定列舉約束
4. `minLength()` / `maxLength()` 設定字串長度約束

### JsonSchemaProperty 靜態方法

| 方法 | bsonType | 用途 |
|------|----------|------|
| `string("name")` | string | 字串欄位 |
| `number("name")` | number | 所有數值型別 |
| `int32("name")` | int | 32 位元整數 |
| `int64("name")` | long | 64 位元整數 |
| `decimal128("name")` | decimal | Decimal128 |
| `bool("name")` | bool | 布林值 |
| `object("name")` | object | 內嵌文件 |
| `array("name")` | array | 陣列 |
| `required(property)` | - | 標記欄位為必要 |

### CollectionOptions 建立集合

```java
@Service
public class SchemaValidationService {

    private final MongoTemplate mongoTemplate;

    public void createCollectionStrict(String name, MongoJsonSchema schema) {
        CollectionOptions options = CollectionOptions.empty()
                .schema(schema)
                .strictValidation()        // Validation Level: strict
                .failOnValidationError();  // Validation Action: error
        mongoTemplate.createCollection(name, options);
    }
}
```

---

## Validation Level 驗證層級

### strict 模式

**預設模式**。所有 insert 和 update 操作都會驗證文件是否符合 Schema:

```java
CollectionOptions.empty()
        .schema(schema)
        .strictValidation()
        .failOnValidationError();
```

- 新插入的文件必須符合 Schema
- 更新後的文件也必須符合 Schema (例如 `$unset` 必要欄位會失敗)

```java
// 插入有效文件
mongoTemplate.getCollection("m08_bank_accounts").insertOne(validDoc);  // ✓ 成功

// 更新移除必要欄位 → 失敗
mongoTemplate.getCollection("m08_bank_accounts").updateOne(
    filter, new Document("$unset", new Document("holderName", ""))
);  // ✗ MongoWriteException
```

### moderate 模式

moderate 模式對**既有不符合 Schema 的文件**比較寬容:

```java
CollectionOptions.empty()
        .schema(schema)
        .moderateValidation()
        .failOnValidationError();
```

| 操作 | 文件原本符合 Schema? | 結果 |
|------|---------------------|------|
| INSERT | - | 必須符合 Schema |
| UPDATE | 是 | 必須符合 Schema |
| UPDATE | 否 | **不驗證** (允許更新) |

典型場景:**漸進式遷移**。先有一批舊文件不符合新 Schema,用 moderate 讓舊文件仍可更新,同時確保新插入的文件都符合新 Schema。

```java
// 1. 集合中已有不符合 Schema 的舊文件
// 2. 以 moderate 模式套用 Schema
schemaValidationService.applySchemaToExistingCollection(
    "m08_validation_level_test", schema, "moderate");
// 3. 舊文件更新 → 成功 (不驗證)
// 4. 新插入不符合 Schema → 失敗
```

### off 模式

完全關閉驗證。通常用於批次匯入或遷移期間:

```java
CollectionOptions.empty()
        .schema(schema)
        .disableValidation();
```

---

## Validation Action 驗證動作

### error 動作

預設動作。不符合 Schema 的操作會拋出 `MongoWriteException`:

```java
CollectionOptions.empty()
        .schema(schema)
        .strictValidation()
        .failOnValidationError();  // 預設
```

### warn 動作

操作仍會成功,但 MongoDB 會在日誌中記錄警告:

```java
CollectionOptions.empty()
        .schema(schema)
        .strictValidation()
        .warnOnValidationError();
```

```java
// warn 模式下,不符合 Schema 的文件仍會被存入
mongoTemplate.getCollection(collection).insertOne(invalidDoc);  // ✓ 不拋出例外
long count = mongoTemplate.getCollection(collection).countDocuments();
assertThat(count).isEqualTo(1);  // 文件確實存在
```

適用場景:
- **觀察期**: 上線新 Schema 前,先用 warn 監控有多少文件不符合
- **漸進切換**: 從 warn 觀察 → 確認無問題 → 切換為 error

---

## 對既有集合套用 Schema

`mongoTemplate.createCollection()` 只能用於**新建**集合。對已存在的集合,使用 `collMod` 指令:

```java
public void applySchemaToExistingCollection(String name, MongoJsonSchema schema,
                                             String level, String action) {
    Document command = new Document("collMod", name)
            .append("validator", schema.toDocument())
            .append("validationLevel", level)
            .append("validationAction", action);
    mongoTemplate.getDb().runCommand(command);
}
```

等效 MongoDB Shell 指令:

```javascript
db.runCommand({
  collMod: "m08_bank_accounts",
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["accountNumber", "holderName"],
      properties: { /* ... */ }
    }
  },
  validationLevel: "moderate",
  validationAction: "error"
})
```

> **重要**: `schema.toDocument()` 回傳 `{"$jsonSchema": {...}}` 格式,正好符合 `validator` 欄位的期望格式。

---

## 小結

| 概念 | 說明 |
|------|------|
| `$jsonSchema` | MongoDB 文件驗證規則,基於 JSON Schema + bsonType 擴充 |
| `required` | 必要欄位列表 |
| `bsonType: "number"` | 涵蓋 int/long/double/decimal 的別名型別 |
| `possibleValues` / `enum` | 列舉值約束 |
| `MongoJsonSchema.builder()` | Spring Data 流暢 API |
| `CollectionOptions` | 建立集合時設定 Schema + Level + Action |
| **strict** | 所有 insert/update 都驗證 (預設) |
| **moderate** | 新文件驗證,既有不符合文件可更新不驗證 |
| **error** | 違反 Schema 拋出例外 (預設) |
| **warn** | 違反 Schema 只記錄警告,操作成功 |
| `collMod` | 對既有集合套用或修改 Schema |

下一篇文件將介紹 Jakarta Bean Validation 應用層驗證,以及雙層驗證策略。
