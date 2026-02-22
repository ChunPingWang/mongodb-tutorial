# M08-DOC-02: Bean Validation 與雙層驗證策略

## 目錄

- [簡介](#簡介)
- [Jakarta Bean Validation 基礎](#jakarta-bean-validation-基礎)
  - [常用驗證註解](#常用驗證註解)
  - [Domain Model 範例](#domain-model-範例)
- [ValidatingMongoEventListener](#validatingmongoeventlistener)
  - [註冊方式](#註冊方式)
  - [攔截時機](#攔截時機)
  - [例外處理](#例外處理)
- [雙層驗證架構](#雙層驗證架構)
  - [兩層各司其職](#兩層各司其職)
  - [流程圖](#流程圖)
  - [互補而非重複](#互補而非重複)
- [MongoJsonSchemaCreator 自動生成](#mongojsonschemacreator-自動生成)
  - [使用方式](#使用方式)
  - [產生的 Schema](#產生的-schema)
  - [限制與注意事項](#限制與注意事項)
- [Schema-on-Read vs Schema-on-Write](#schema-on-read-vs-schema-on-write)
- [錯誤處理模式](#錯誤處理模式)
- [小結](#小結)

---

## 簡介

DOC-01 介紹了 MongoDB `$jsonSchema` 資料庫層驗證。但資料庫驗證有其限制——錯誤訊息不友善、無法表達複雜商業邏輯。Jakarta Bean Validation 在 Java 層提供豐富的註解式驗證,搭配 Spring Data 的 `ValidatingMongoEventListener`,形成**雙層驗證策略**。

本文件涵蓋:
- Jakarta Bean Validation 註解與 Spring Data 整合
- `ValidatingMongoEventListener` 攔截機制
- 雙層驗證架構設計:Java 層 + DB 層
- `MongoJsonSchemaCreator` 自動生成 Schema
- 錯誤處理模式

---

## Jakarta Bean Validation 基礎

### 常用驗證註解

| 註解 | 用途 | 範例 |
|------|------|------|
| `@NotNull` | 不可為 null | 必要的物件欄位 |
| `@NotBlank` | 不可為 null、空字串、空白字串 | 名稱、帳號 |
| `@Size(min, max)` | 字串或集合大小範圍 | `@Size(min=5, max=20)` |
| `@DecimalMin` | 數值最小值 | `@DecimalMin("0")` |
| `@Positive` | 必須為正數 | 保費、金額 |
| `@Min` / `@Max` | 整數或小數範圍 | `@Min(0) @Max(5)` |
| `@Email` | 電子郵件格式 | 聯絡信箱 |
| `@Pattern` | 正則表達式 | 電話號碼格式 |

### Domain Model 範例

```java
@Document("m08_bank_accounts")
public class BankAccount {

    @Id
    private String id;

    @NotBlank
    @Size(min = 5, max = 20)
    private String accountNumber;

    @NotBlank
    private String holderName;

    @NotNull
    private AccountType type;

    @NotNull
    @DecimalMin("0")
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal balance;

    @NotNull
    private AccountStatus status;

    // ...
}
```

```java
@Document("m08_insurance_policies")
public class InsurancePolicyDocument {

    @NotBlank
    private String policyNumber;

    @NotBlank
    private String holderName;

    @NotNull
    @Positive
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal premium;

    @NotNull
    @Positive
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal coverageAmount;

    // ...
}
```

---

## ValidatingMongoEventListener

### 註冊方式

需要 `spring-boot-starter-validation` 依賴:

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
```

註冊為 Spring Bean:

```java
@Configuration
public class ValidationConfig {

    @Bean
    ValidatingMongoEventListener validatingMongoEventListener(Validator validator) {
        return new ValidatingMongoEventListener(validator);
    }
}
```

Spring Boot 自動配置 `LocalValidatorFactoryBean`,注入 `jakarta.validation.Validator`。

### 攔截時機

`ValidatingMongoEventListener` 繼承 `AbstractMongoEventListener`,在 `onBeforeSave` 事件觸發驗證:

```
mongoTemplate.save(entity)
    │
    ▼
onBeforeConvert  (型別轉換前)
    │
    ▼
onBeforeSave     ← ValidatingMongoEventListener 在此驗證
    │                ConstraintViolationException (如果驗證失敗)
    ▼
MongoDB Driver 執行寫入
    │                MongoWriteException (如果 $jsonSchema 驗證失敗)
    ▼
onAfterSave      (寫入後)
```

### 例外處理

驗證失敗拋出 `jakarta.validation.ConstraintViolationException`:

```java
BankAccount invalid = new BankAccount("", "Alice", AccountType.SAVINGS, new BigDecimal("5000"));
// accountNumber 為空字串 → @NotBlank 違規

assertThatThrownBy(() -> beanValidationService.saveBankAccount(invalid))
        .isInstanceOf(ConstraintViolationException.class);
```

`ConstraintViolationException` 包含詳細的違規資訊:

```java
try {
    beanValidationService.saveBankAccount(invalid);
} catch (ConstraintViolationException e) {
    for (var violation : e.getConstraintViolations()) {
        System.out.println(violation.getPropertyPath() + ": " + violation.getMessage());
        // accountNumber: must not be blank
    }
}
```

---

## 雙層驗證架構

### 兩層各司其職

```
┌─────────────────────────────────────────────────────────────┐
│                     Java 應用層                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Bean Validation (ValidatingMongoEventListener)      │    │
│  │  - @NotBlank, @NotNull, @Positive, @Size            │    │
│  │  - 商業邏輯驗證 (自訂 Validator)                      │    │
│  │  - 友善的錯誤訊息 (i18n 支援)                         │    │
│  │  - 在 Java 物件層級操作                               │    │
│  └─────────────────────────────────────────────────────┘    │
│                         │                                    │
│                    mongoTemplate.save()                       │
│                         │                                    │
└─────────────────────────┼────────────────────────────────────┘
                          │
┌─────────────────────────┼────────────────────────────────────┐
│                     MongoDB 資料庫層                          │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  $jsonSchema Validation                              │    │
│  │  - required 欄位、bsonType 型別                       │    │
│  │  - enum 列舉、minLength/maxLength                    │    │
│  │  - 攔截所有寫入管道 (Driver / Shell / Compass)        │    │
│  │  - 在 BSON 文件層級操作                               │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 流程圖

```
                正常流程 (mongoTemplate.save)
Client ──→ Bean Validation ──→ MongoDB $jsonSchema ──→ 寫入成功
                 │                    │
            驗證失敗              驗證失敗
                 │                    │
                 ▼                    ▼
       ConstraintViolation    MongoWriteException
         Exception (Java)       (BSON 文件層級)


                繞過 Java 的流程 (raw BSON insert)
Client ──→ MongoDB Driver 直接寫入 ──→ $jsonSchema ──→ 寫入成功
                                           │
                                      驗證失敗
                                           │
                                           ▼
                                   MongoWriteException
```

### 互補而非重複

| 特性 | Bean Validation | $jsonSchema |
|------|----------------|-------------|
| 攔截範圍 | 只有 Spring Data 的 save/insert | 所有寫入管道 |
| 錯誤訊息 | 友善、支援 i18n | 技術性 (WriteError code 121) |
| 複雜驗證 | 自訂 Validator、跨欄位驗證 | 有限 (基本結構約束) |
| 效能 | Java 層攔截,不消耗 DB 資源 | DB 每次寫入都檢查 |
| 繞過可能 | raw BSON 操作可繞過 | 無法繞過 (除非 off) |
| 適合的驗證 | 商業規則、格式驗證 | 結構完整性、最後防線 |

---

## MongoJsonSchemaCreator 自動生成

### 使用方式

Spring Data 提供 `MongoJsonSchemaCreator` 從 Java 類別自動生成 `$jsonSchema`:

```java
MongoJsonSchemaCreator creator = MongoJsonSchemaCreator.create(mongoTemplate.getConverter());
MongoJsonSchema schema = creator.createSchemaFor(BankAccount.class);
Document schemaDoc = schema.toDocument();
```

### 產生的 Schema

對 `BankAccount` 類別,自動生成的 Schema 包含:
- 每個欄位的 BSON 型別 (根據 Java 型別和 `@Field(targetType)` 映射)
- `@NotNull` 標記的欄位被加入 `required` (部分版本支援)
- Enum 型別自動列舉

### 限制與注意事項

`MongoJsonSchemaCreator` 的重要限制:

1. **不讀取 Jakarta 驗證註解**: `@NotBlank`、`@Size`、`@Positive` 等註解不會反映在生成的 Schema 中
2. **`_id` 型別問題**: `@Id String id` 在 Java 是 `String`,但 MongoDB 存為 `ObjectId`,導致型別不匹配。實務上需要移除生成 Schema 中的 `_id` 屬性:

```java
public void createCollectionWithAutoSchema(Class<?> clazz, String collectionName) {
    MongoJsonSchema schema = generateSchemaFor(clazz);

    // 移除 _id: MongoJsonSchemaCreator 映射為 type "object",
    // 但 MongoDB 實際存為 ObjectId — 型別不匹配
    Document schemaDoc = schema.toDocument().get("$jsonSchema", Document.class);
    Document properties = schemaDoc.get("properties", Document.class);
    if (properties != null) {
        properties.remove("_id");
    }
    MongoJsonSchema fixedSchema = MongoJsonSchema.of(schemaDoc);

    CollectionOptions options = CollectionOptions.empty()
            .schema(fixedSchema)
            .strictValidation()
            .failOnValidationError();
    mongoTemplate.createCollection(collectionName, options);
}
```

3. **建議用途**: 自動生成 Schema 適合用作**起點**,再手動調整加入業務約束。不建議直接用於生產環境。

> 這正是雙層驗證策略的核心理由:自動生成 Schema 無法完整表達 Java 層的驗證邏輯,因此需要兩層互補。

---

## Schema-on-Read vs Schema-on-Write

| 模式 | 說明 | 適用場景 |
|------|------|---------|
| Schema-on-Write | 寫入時強制驗證 (RDB、$jsonSchema) | 資料品質要求高 |
| Schema-on-Read | 讀取時解釋結構 (MongoDB 預設) | 快速迭代、探索性開發 |
| 混合模式 | Bean Validation + $jsonSchema | 生產環境最佳實踐 |

MongoDB 的優勢在於**可選擇性的 Schema 約束**:
- 開發初期: 無 Schema,快速迭代
- 功能穩定後: 加入 Bean Validation 確保 Java 層資料品質
- 正式上線: 加入 $jsonSchema 作為資料庫層最後防線

---

## 錯誤處理模式

```java
// 方法一: 統一攔截
try {
    beanValidationService.saveProduct(product);
} catch (ConstraintViolationException e) {
    // Java Bean Validation 失敗
    // 提取 violation 詳情,回傳友善錯誤訊息
} catch (DataIntegrityViolationException e) {
    // MongoDB $jsonSchema 驗證失敗
    // 通常表示繞過了 Java 驗證的非預期寫入
}
```

```java
// 方法二: 使用 @ExceptionHandler (Spring MVC)
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<Map<String, String>> handleValidation(ConstraintViolationException e) {
    Map<String, String> errors = e.getConstraintViolations().stream()
            .collect(Collectors.toMap(
                    v -> v.getPropertyPath().toString(),
                    ConstraintViolation::getMessage
            ));
    return ResponseEntity.badRequest().body(errors);
}
```

---

## 小結

| 概念 | 說明 |
|------|------|
| Jakarta Bean Validation | Java 層註解式驗證 (@NotBlank, @Positive, @Size) |
| `ValidatingMongoEventListener` | 在 `onBeforeSave` 攔截,觸發 Bean Validation |
| `ConstraintViolationException` | Bean Validation 失敗例外,含詳細違規資訊 |
| 雙層驗證 | Java 層 (商業邏輯) + DB 層 (結構完整性) |
| `MongoJsonSchemaCreator` | 從 Java 類別自動生成 Schema (有限制) |
| 互補策略 | Bean Validation 攔截商業錯誤,`$jsonSchema` 作為最後防線 |

下一篇文件將介紹 Schema 演進與文件遷移策略。
