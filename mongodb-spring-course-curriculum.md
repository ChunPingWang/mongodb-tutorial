# MongoDB for Java Spring Developers — 完整教學課程

> **技術棧**: Java 23, Spring Boot 4, Gradle, Testcontainers, MongoDB 7+
> **教學法**: Test-First (BDD/TDD), 情境驅動 (金融、保險、電商)
> **架構理念**: OOP, DDD, Hexagonal Architecture, SOLID Principles
> **目標讀者**: 具備 RDB 經驗的 Java Spring 開發人員
> **產出格式**: 每個 Module 為獨立 Gradle Sub-Module，可單獨建置與執行

---

## 課程總覽

| Phase | 主題 | 難度 | Sub-Modules |
|-------|------|------|-------------|
| Phase 1 | 基礎建設與思維轉換 | ★☆☆ | M01 ~ M04 |
| Phase 2 | Spring Data MongoDB 核心 | ★★☆ | M05 ~ M09 |
| Phase 3 | 領域驅動與進階建模 | ★★★ | M10 ~ M14 |
| Phase 4 | 效能、可觀測性與維運 | ★★★ | M15 ~ M18 |
| Phase 5 | 整合專案與架構決策 | ★★★ | M19 ~ M21 |

---

## Gradle 多模組專案結構

```
mongodb-spring-course/
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Include all sub-modules
├── buildSrc/
│   └── conventions.gradle.kts    # 共用 dependency 版本、test config
├── m01-rdb-vs-nosql/
├── m02-nosql-landscape/
├── m03-environment-setup/
├── m04-document-thinking/
├── m05-spring-data-crud/
├── m06-query-dsl/
├── m07-aggregation-pipeline/
├── m08-schema-validation/
├── m09-transactions/
├── m10-ddd-aggregate-modeling/
├── m11-polymorphism-inheritance/
├── m12-event-sourcing/
├── m13-cqrs-read-model/
├── m14-saga-pattern/
├── m15-indexing-performance/
├── m16-change-streams/
├── m17-observability/
├── m18-migration-versioning/
├── m19-banking-capstone/
├── m20-insurance-capstone/
└── m21-ecommerce-capstone/
```

---

## Phase 1: 基礎建設與思維轉換

### M01 — RDB vs NoSQL 思維轉換 (`m01-rdb-vs-nosql`)

**學習目標**: 理解關聯式與文件式資料庫的本質差異，建立正確的選型思維

#### 教材工作清單

- [ ] **M01-DOC-01**: 撰寫「Data Model 哲學比較」文件
  - RDB: 正規化、參照完整性、Schema-on-Write
  - MongoDB: 反正規化、嵌入 vs 引用、Schema-on-Read
  - 用「銀行帳戶 + 交易明細」對比兩種建模方式
  - 圖表：Entity-Relationship Diagram vs Document Structure Diagram

- [ ] **M01-DOC-02**: 撰寫「CAP 定理與一致性模型」文件
  - ACID (RDB) vs BASE (NoSQL) 比較表
  - 金融場景：轉帳一致性需求分析
  - 電商場景：庫存最終一致性可接受場景
  - 保險場景：保單狀態一致性需求

- [ ] **M01-DOC-03**: 撰寫「選型決策框架」文件
  - 決策樹：何時選 RDB、何時選 MongoDB
  - 混合架構：Polyglot Persistence 策略
  - 真實案例：銀行核心 vs 銀行 CRM 的選型差異

- [ ] **M01-LAB-01**: 建立比較用測試案例
  - 用 Testcontainers 同時啟動 PostgreSQL + MongoDB
  - BDD Scenario: 「Given 相同的客戶訂單資料 When 分別存入 RDB 和 MongoDB Then 比較查詢效能與彈性」
  - 實作 JPA Entity vs MongoDB Document 的 POJO 對照
  - 測試：巢狀查詢 (Nested Query) 在兩種 DB 的寫法差異

- [ ] **M01-LAB-02**: Schema Evolution 對比實驗
  - TDD: 新增欄位時 RDB 需要 ALTER TABLE，MongoDB 直接寫入
  - 測試案例：保險保單新增「附加條款」欄位的遷移成本比較

---

### M02 — NoSQL 版圖：MongoDB vs Redis vs Cassandra (`m02-nosql-landscape`)

**學習目標**: 掌握不同 NoSQL 資料庫的定位與適用場景

#### 教材工作清單

- [ ] **M02-DOC-01**: 撰寫「NoSQL 四大類型全景圖」文件
  - Document Store (MongoDB): 彈性結構、Ad-hoc Query
  - Key-Value Store (Redis): 極速存取、快取與 Session
  - Wide-Column Store (Cassandra): 高寫入吞吐、時序資料
  - Graph Database (Neo4j): 關係遍歷、社群網路（簡述）

- [ ] **M02-DOC-02**: 撰寫「MongoDB vs Redis vs Cassandra 深度比較」文件
  - 比較維度表格：

    | 維度 | MongoDB | Redis | Cassandra |
    |------|---------|-------|-----------|
    | 資料模型 | Document (BSON) | Key-Value / 多種資料結構 | Wide-Column (Partition Key + Clustering Key) |
    | 查詢能力 | 豐富 (Ad-hoc, Aggregation) | 簡單 (Key Lookup, 有限 Query) | CQL (類 SQL, 但限制多) |
    | 一致性 | 可調 (Write Concern / Read Concern) | 強一致 (單節點) / 最終一致 (Cluster) | 可調 (Consistency Level) |
    | 擴展方式 | Sharding (Auto-Balancing) | Cluster (Hash Slot) | Consistent Hashing (無主節點) |
    | 寫入效能 | 中高 | 極高 (in-memory) | 極高 (LSM-Tree, 寫入優化) |
    | 適用場景 | 通用文件、CMS、目錄 | 快取、Session、排行榜、Pub/Sub | IoT 時序、日誌、高寫入量 |
    | 交易支援 | 多文件 ACID Transaction (4.0+) | 單命令原子 / Lua Script | 輕量級 Batch (非 ACID) |

- [ ] **M02-DOC-03**: 撰寫「金融場景 Polyglot Persistence 架構」文件
  - 銀行場景：核心帳務 (RDB) + 客戶 360 (MongoDB) + 即時風控快取 (Redis) + 交易日誌 (Cassandra)
  - 保險場景：保單管理 (RDB) + 理賠文件 (MongoDB) + 報價快取 (Redis)
  - 電商場景：訂單 (RDB/MongoDB) + 商品目錄 (MongoDB) + 購物車 (Redis) + 使用者行為 (Cassandra)
  - 架構圖：各資料庫在系統中的位置與資料流向

- [ ] **M02-LAB-01**: 三種 NoSQL 併行測試
  - 使用 Testcontainers 啟動 MongoDB + Redis + Cassandra
  - BDD Scenario: 「Given 電商訂單建立事件 When 資料分別寫入三種 DB Then 驗證各自的存取模式」
  - 實作相同商品資訊在三種 DB 的存取 Pattern 比較
  - 測試：讀取延遲、寫入吞吐量的量化比較

- [ ] **M02-LAB-02**: Redis 快取 + MongoDB 持久化整合
  - TDD: 實作 Cache-Aside Pattern (先查 Redis → Miss → 查 MongoDB → 回寫 Redis)
  - 情境：保險商品報價快取，MongoDB 存完整資料，Redis 存熱門查詢結果
  - 測試：快取命中率、資料一致性、TTL 過期行為

---

### M03 — 開發環境與測試基礎設施 (`m03-environment-setup`)

**學習目標**: 建立標準化的 MongoDB 開發與測試環境

#### 教材工作清單

- [ ] **M03-DOC-01**: 撰寫「Gradle 多模組專案建置指南」文件
  - Root `build.gradle.kts` 設定 (Java 23, Spring Boot 4 Plugin)
  - `buildSrc` 共用 Convention Plugin
  - 依賴管理：Spring Data MongoDB, Testcontainers, Cucumber, AssertJ
  - 版本目錄 (`libs.versions.toml`) 配置

- [ ] **M03-DOC-02**: 撰寫「Testcontainers + MongoDB 測試策略」文件
  - Testcontainers MongoDB Module 設定
  - Singleton Container Pattern（共享 Container 加速測試）
  - `@DynamicPropertySource` 動態注入連線資訊
  - Reusable Containers 在本地開發的使用
  - 測試資料生命週期管理 (`@BeforeEach` 清理策略)

- [ ] **M03-DOC-03**: 撰寫「BDD + TDD 雙軌測試流程」文件
  - BDD: Cucumber + Gherkin → Step Definitions → Spring Integration Test
  - TDD: JUnit 5 + AssertJ → Unit Test → Integration Test
  - 測試金字塔：Unit (Repository) → Integration (Service + DB) → BDD (End-to-End Scenario)
  - 目錄結構慣例：`src/test/java` (TDD) + `src/test/resources/features` (BDD)

- [ ] **M03-LAB-01**: 專案骨架建立
  - 建立完整 Gradle 多模組專案結構
  - 每個 sub-module 的標準 `build.gradle.kts`
  - 驗證：`./gradlew build` 全模組通過
  - 實作第一個 Testcontainers 冒煙測試：啟動 MongoDB、寫入、讀取

- [ ] **M03-LAB-02**: BDD 基礎架構
  - Cucumber + Spring Boot 4 整合設定
  - 撰寫第一個 Feature File:
    ```gherkin
    Feature: MongoDB 連線驗證
      Scenario: 成功連線至 MongoDB
        Given MongoDB container 已啟動
        When 執行 ping 命令
        Then 回傳成功狀態
    ```
  - Step Definition 實作與驗證

---

### M04 — Document 思維與基礎建模 (`m04-document-thinking`)

**學習目標**: 從 RDB 正規化思維轉換至 Document 建模思維

#### 教材工作清單

- [ ] **M04-DOC-01**: 撰寫「Document 建模原則」文件
  - 核心原則：「一起讀取的資料，一起儲存」
  - Embedding vs Referencing 決策矩陣
  - 1:1, 1:N, N:N 關係在 Document DB 的處理方式
  - Anti-Pattern: 過度嵌入、無限增長的陣列

- [ ] **M04-DOC-02**: 撰寫「BSON 資料類型與 Java 映射」文件
  - BSON Types → Java Types 對應表
  - ObjectId 生成機制與使用策略
  - Date/Timestamp 處理 (Java 23 `java.time` 整合)
  - Decimal128 在金融計算中的重要性 (避免浮點數精度問題)

- [ ] **M04-DOC-03**: 撰寫「金融場景建模實戰」文件
  - 案例 1: 銀行客戶 Profile (嵌入地址、聯絡方式、KYC 資料)
  - 案例 2: 保險保單 (嵌入被保險人、保障項目；引用理賠記錄)
  - 案例 3: 電商商品目錄 (嵌入規格、圖片；引用評價)
  - 每個案例提供 Document 結構圖 + Java Record/Class 定義

- [ ] **M04-LAB-01**: Embedding vs Referencing 實驗
  - TDD: 銀行客戶 + 帳戶建模
    - 方案 A: 帳戶嵌入客戶文件
    - 方案 B: 帳戶獨立 Collection + 客戶引用
  - 測試比較：查詢效能、更新成本、資料一致性
  - 使用 AssertJ 驗證 Document 結構

- [ ] **M04-LAB-02**: Java 23 特性與 MongoDB 整合
  - 使用 Record Class 定義 Value Object
  - Pattern Matching 處理多型 Document
  - Sealed Interface 定義有限的文件類型
  - TDD: 驗證 Java 23 特性在 Spring Data MongoDB 的支援度

---

## Phase 2: Spring Data MongoDB 核心操作

### M05 — CRUD 操作與 Repository Pattern (`m05-spring-data-crud`)

**學習目標**: 掌握 Spring Data MongoDB 的基本 CRUD 操作

#### 教材工作清單

- [ ] **M05-DOC-01**: 撰寫「Spring Data MongoDB Repository 體系」文件
  - `MongoRepository` vs `ReactiveMongoRepository`
  - `MongoTemplate` 底層操作
  - 自訂 Repository 實作 (Fragment Interface Pattern)
  - `@Document`, `@Id`, `@Field`, `@Indexed` 等核心註解

- [ ] **M05-DOC-02**: 撰寫「OOP 與 DDD 映射策略」文件
  - Entity vs Value Object 在 MongoDB 的表現
  - Aggregate Root → Collection 的映射規則
  - Java Record 作為 Value Object 的最佳實踐
  - `@PersistenceCreator` 與不可變物件

- [ ] **M05-LAB-01**: 銀行帳戶 CRUD
  - BDD Feature:
    ```gherkin
    Feature: 銀行帳戶管理
      Scenario: 開立新帳戶
        Given 客戶 "張三" 已通過 KYC 驗證
        When 開立活期存款帳戶 初始餘額 10000 元
        Then 帳戶狀態為 ACTIVE
        And 帳戶餘額為 10000 元

      Scenario: 帳戶凍結
        Given 帳戶 "A001" 狀態為 ACTIVE
        When 因可疑交易執行凍結
        Then 帳戶狀態為 FROZEN
        And 無法執行提款操作
    ```
  - TDD: Repository 層 → Service 層 → Domain Model
  - 實作 `BankAccount` Aggregate Root + `Money` Value Object

- [ ] **M05-LAB-02**: 保險保單 CRUD
  - BDD Feature: 保單建立、批改、終止
  - TDD: 保單狀態機 (Pending → Active → Expired/Cancelled)
  - 實作 `InsurancePolicy` 含嵌入的 `Coverage` 列表
  - 測試：保單號碼唯一性、狀態轉換驗證

- [ ] **M05-LAB-03**: 電商商品 CRUD
  - BDD Feature: 商品上架、下架、庫存更新
  - TDD: `Product` + 嵌入 `Variant` (尺寸、顏色、價格)
  - 測試：SKU 唯一性、庫存不可為負數

---

### M06 — 查詢 DSL 與方法名稱推導 (`m06-query-dsl`)

**學習目標**: 精通 Spring Data MongoDB 的多種查詢方式

#### 教材工作清單

- [ ] **M06-DOC-01**: 撰寫「查詢方法四層體系」文件
  - Level 1: Method Name Derivation (`findByStatusAndAmountGreaterThan`)
  - Level 2: `@Query` 手寫 JSON Query
  - Level 3: `Criteria` API 動態組合
  - Level 4: `MongoTemplate` + `Query` 物件完全控制
  - 選擇指南：何時用哪一層

- [ ] **M06-DOC-02**: 撰寫「複雜查詢模式」文件
  - 巢狀文件查詢 (`address.city`)
  - 陣列查詢 (`$elemMatch`, `$all`, `$in`)
  - 正則表達式查詢
  - 地理空間查詢 (分行位置查詢)
  - 全文搜尋 (Text Index)

- [ ] **M06-LAB-01**: 銀行交易查詢
  - BDD Feature:
    ```gherkin
    Feature: 交易記錄查詢
      Scenario: 依日期區間查詢交易
        Given 帳戶 "A001" 有 100 筆交易記錄
        When 查詢 2024-01-01 至 2024-03-31 的交易
        Then 回傳該區間內的交易記錄
        And 按交易日期降冪排序

      Scenario: 依條件組合查詢
        Given 帳戶 "A001" 有多種類型交易
        When 查詢金額大於 50000 的轉帳交易
        Then 只回傳符合條件的交易記錄
    ```
  - TDD: 四種查詢層級實作同一需求的比較
  - 實作 `TransactionQueryService` 含動態條件組合

- [ ] **M06-LAB-02**: 保險理賠查詢
  - 複雜查詢：保單號 + 理賠狀態 + 金額區間 + 事故類型
  - 巢狀查詢：查詢含特定保障項目的保單
  - 分頁與排序：大量理賠記錄的分頁查詢

- [ ] **M06-LAB-03**: 電商商品搜尋
  - 全文搜尋：商品名稱 + 描述關鍵字搜尋
  - 多維篩選：分類 + 價格區間 + 評分 + 庫存狀態
  - 地理查詢：距離最近的實體店面有庫存的商品

---

### M07 — Aggregation Pipeline (`m07-aggregation-pipeline`)

**學習目標**: 掌握 MongoDB Aggregation Framework 進行複雜資料分析

#### 教材工作清單

- [ ] **M07-DOC-01**: 撰寫「Aggregation Pipeline 概念與階段」文件
  - Pipeline 思維：資料流經多個轉換階段
  - 核心階段：`$match`, `$group`, `$project`, `$sort`, `$limit`, `$skip`
  - 進階階段：`$lookup` (Join), `$unwind`, `$facet`, `$bucket`
  - 與 SQL GROUP BY / JOIN 的概念對應

- [ ] **M07-DOC-02**: 撰寫「Spring Data MongoDB Aggregation API」文件
  - `Aggregation.newAggregation()` 建構器
  - `TypedAggregation` 型別安全操作
  - `AggregationResults` 結果處理
  - 自訂 `AggregationOperation` 實作

- [ ] **M07-LAB-01**: 銀行報表 Aggregation
  - BDD Feature:
    ```gherkin
    Feature: 月度交易報表
      Scenario: 帳戶月度收支摘要
        Given 帳戶 "A001" 有 2024 年度的交易記錄
        When 產出月度收支摘要報表
        Then 每月顯示收入總額、支出總額、淨額
        And 包含交易筆數統計

      Scenario: 客戶資產配置分析
        Given 客戶 "C001" 持有多個帳戶
        When 執行資產配置分析
        Then 顯示各帳戶類型的餘額占比
    ```
  - TDD: `$group` + `$project` 實作月度彙總
  - `$lookup` 關聯客戶資料與帳戶資料

- [ ] **M07-LAB-02**: 保險理賠統計
  - `$facet` 多維度統計：依險種、依地區、依金額級距
  - `$bucket` 理賠金額分佈直方圖
  - `$unwind` 展開保障項目進行個別統計

- [ ] **M07-LAB-03**: 電商銷售分析
  - 漏斗分析：瀏覽 → 加入購物車 → 下單 → 付款
  - 商品排行榜：按銷量、營收、評分排名
  - 時間序列分析：每日/每週/每月銷售趨勢

---

### M08 — Schema Validation 與資料治理 (`m08-schema-validation`)

**學習目標**: 在 MongoDB 彈性 Schema 中建立適度的資料品質控制

#### 教材工作清單

- [ ] **M08-DOC-01**: 撰寫「MongoDB JSON Schema Validation」文件
  - `$jsonSchema` 驗證規則定義
  - Validation Level: `strict` vs `moderate`
  - Validation Action: `error` vs `warn`
  - 與 Java Bean Validation (`jakarta.validation`) 的搭配策略

- [ ] **M08-DOC-02**: 撰寫「Schema-on-Read vs Schema-on-Write 混合策略」文件
  - 金融場景：核心欄位 Schema-on-Write，擴展欄位 Schema-on-Read
  - 版本化 Schema：文件中的 `schemaVersion` 欄位
  - 漸進式 Migration 策略

- [ ] **M08-LAB-01**: 金融合規資料驗證
  - BDD Feature:
    ```gherkin
    Feature: 保單資料完整性驗證
      Scenario: 缺少必要欄位的保單被拒絕
        Given MongoDB Collection 已設定 Schema Validation
        When 嘗試建立缺少投保人資訊的保單
        Then 寫入被拒絕並回傳驗證錯誤

      Scenario: 保費金額必須為正數
        Given MongoDB Collection 已設定金額驗證規則
        When 嘗試建立保費為負數的保單
        Then 寫入被拒絕
    ```
  - TDD: 使用 `MongoTemplate` 建立 Collection 並設定 Validator
  - Java Bean Validation + MongoDB Schema Validation 雙層防護

- [ ] **M08-LAB-02**: Schema 版本演進
  - 情境：電商商品 Schema 從 V1 → V2 → V3 的演進
  - 實作 `DocumentMigrator` 處理不同版本的文件
  - 測試：混合版本文件的讀取與寫入

---

### M09 — Multi-Document Transactions (`m09-transactions`)

**學習目標**: 理解 MongoDB Transaction 的能力與限制

#### 教材工作清單

- [ ] **M09-DOC-01**: 撰寫「MongoDB Transaction 深度解析」文件
  - Transaction 在 Replica Set 與 Sharded Cluster 的差異
  - Write Concern / Read Concern / Read Preference 三軸配置
  - Transaction 的效能影響與使用原則
  - 與 RDB Transaction 的比較：能力與限制

- [ ] **M09-DOC-02**: 撰寫「Spring @Transactional 與 MongoDB」文件
  - `MongoTransactionManager` 配置
  - `@Transactional` 在 MongoDB 的行為
  - Reactive Transaction 支援
  - 常見陷阱：單節點不支援 Transaction

- [ ] **M09-LAB-01**: 銀行轉帳交易
  - BDD Feature:
    ```gherkin
    Feature: 帳戶間轉帳
      Scenario: 成功轉帳
        Given 帳戶 "A001" 餘額 50000 元
        And 帳戶 "A002" 餘額 10000 元
        When 從 "A001" 轉帳 20000 元至 "A002"
        Then "A001" 餘額為 30000 元
        And "A002" 餘額為 30000 元
        And 產生兩筆交易記錄

      Scenario: 餘額不足轉帳失敗
        Given 帳戶 "A001" 餘額 5000 元
        When 從 "A001" 轉帳 20000 元至 "A002"
        Then 轉帳失敗並回傳餘額不足錯誤
        And 兩帳戶餘額維持不變
    ```
  - TDD: `TransferService` 含 `@Transactional` 標註
  - Testcontainers: 使用 MongoDB Replica Set 容器
  - 測試：並發轉帳、失敗回滾、冪等性

- [ ] **M09-LAB-02**: 保險多文件一致性
  - 情境：核保通過 → 建立保單 + 建立收費排程 + 更新客戶狀態
  - 測試：任一步驟失敗時全部回滾

---

## Phase 3: 領域驅動設計與進階建模

### M10 — DDD Aggregate 建模 (`m10-ddd-aggregate-modeling`)

**學習目標**: 將 DDD Aggregate Pattern 落實於 MongoDB Document 設計

#### 教材工作清單

- [ ] **M10-DOC-01**: 撰寫「Aggregate Root 與 MongoDB Collection 映射」文件
  - Aggregate 邊界 = Document 邊界 = Transaction 邊界
  - Aggregate 內部一致性保證
  - Aggregate 間的最終一致性
  - 圖解：Bounded Context → Aggregate → Collection

- [ ] **M10-DOC-02**: 撰寫「Hexagonal Architecture + MongoDB」文件
  - Port: Repository Interface (Domain Layer)
  - Adapter: Spring Data MongoDB Repository (Infrastructure Layer)
  - 目錄結構範例與依賴方向
  - 測試策略：Domain Unit Test → Port Mock Test → Adapter Integration Test

- [ ] **M10-DOC-03**: 撰寫「Rich Domain Model 在 MongoDB 的實踐」文件
  - Domain Entity 行為方法 vs Anemic Model
  - Domain Event 的產生與儲存
  - Specification Pattern 用於複雜業務規則
  - Factory Pattern 建構複雜 Aggregate

- [ ] **M10-LAB-01**: 銀行貸款申請 Aggregate
  - BDD Feature:
    ```gherkin
    Feature: 貸款申請流程
      Scenario: 提交貸款申請
        Given 客戶 "C001" 信用評分 750
        When 提交房貸申請 金額 5000000 元 期限 20 年
        Then 貸款申請狀態為 SUBMITTED
        And 產生 LoanApplicationSubmitted 領域事件

      Scenario: 自動初審
        Given 貸款申請 "L001" 狀態為 SUBMITTED
        When 系統執行自動初審
        And 申請人年收入大於年還款額的 3 倍
        Then 初審結果為 PASSED
        And 進入人工複審階段
    ```
  - Hexagonal Architecture 實作:
    - Domain: `LoanApplication` Aggregate Root, `Applicant` Entity, `Money` VO
    - Port: `LoanApplicationRepository` Interface
    - Adapter: `MongoLoanApplicationRepository` Implementation
  - TDD: Domain Logic 獨立於 MongoDB 測試

- [ ] **M10-LAB-02**: 保險理賠 Aggregate
  - `Claim` Aggregate Root
  - 嵌入: `ClaimItem`, `Assessment`, `Document`
  - 引用: `PolicyId`, `ClaimantId`
  - 業務規則: 理賠金額不超過保額、自負額扣除

- [ ] **M10-LAB-03**: 電商訂單 Aggregate
  - `Order` Aggregate Root
  - 嵌入: `OrderLine`, `ShippingAddress`, `PaymentInfo`
  - 狀態機: Created → Paid → Shipped → Delivered → Completed/Returned
  - 業務規則: 庫存檢查、價格鎖定、訂單修改限制

---

### M11 — 多型與繼承建模 (`m11-polymorphism-inheritance`)

**學習目標**: 運用 OOP 多型概念在 MongoDB 實現靈活的文件結構

#### 教材工作清單

- [ ] **M11-DOC-01**: 撰寫「MongoDB 多型文件策略」文件
  - Single Collection Polymorphism (推薦，使用 discriminator field `_class`)
  - Multiple Collection Inheritance (每個子類型一個 Collection)
  - Schema-per-type 與 Union Schema
  - 與 JPA 繼承策略的比較 (SINGLE_TABLE, TABLE_PER_CLASS, JOINED)

- [ ] **M11-DOC-02**: 撰寫「Java Sealed Interface + MongoDB」文件
  - Java 23 Sealed Interface 定義有限的文件類型
  - Pattern Matching 處理不同文件類型
  - `@TypeAlias` 控制 `_class` 欄位值
  - Custom `Converter` 自訂序列化

- [ ] **M11-LAB-01**: 銀行金融商品多型
  - BDD Feature:
    ```gherkin
    Feature: 多型金融商品管理
      Scenario: 儲存不同類型的金融商品
        Given 以下金融商品:
          | 類型   | 名稱     | 特有屬性             |
          | 定存   | 一年定存 | 年利率: 1.5%         |
          | 基金   | 全球股票 | 淨值: 15.32, 風險等級: 5 |
          | 保險   | 年金險   | 繳費年期: 20, 保額: 1000000 |
        When 全部存入 products Collection
        Then 可用統一介面查詢所有商品
        And 可依類型篩選特定商品
    ```
  - Java Sealed Interface:
    ```java
    sealed interface FinancialProduct permits Deposit, Fund, InsuranceProduct { }
    ```
  - TDD: Repository 讀寫多型文件、型別安全查詢

- [ ] **M11-LAB-02**: 保險多險種保單
  - 車險、壽險、健康險共用 `Policy` 基礎結構
  - 各險種有獨特的承保內容和理賠規則
  - Pattern Matching 實作不同險種的保費計算

---

### M12 — Event Sourcing with MongoDB (`m12-event-sourcing`)

**學習目標**: 使用 MongoDB 實作 Event Sourcing Pattern

#### 教材工作清單

- [ ] **M12-DOC-01**: 撰寫「Event Sourcing 概念與 MongoDB 實作」文件
  - Event Store 設計：Events Collection 結構
  - Aggregate 重建：Event Replay 機制
  - Snapshot 優化策略
  - MongoDB 特性如何支援 Event Sourcing (Capped Collection, Change Streams)

- [ ] **M12-DOC-02**: 撰寫「Domain Event 設計原則」文件
  - Event 命名慣例 (Past Tense: `AccountOpened`, `FundsTransferred`)
  - Event Payload 設計：充足但不過度
  - Event Versioning 與向後相容
  - Event Upcasting 策略

- [ ] **M12-LAB-01**: 銀行帳戶 Event Sourcing
  - BDD Feature:
    ```gherkin
    Feature: 帳戶 Event Sourcing
      Scenario: 從事件重建帳戶狀態
        Given 帳戶 "A001" 的事件序列:
          | 序號 | 事件類型           | 資料                    |
          | 1    | AccountOpened      | balance: 0              |
          | 2    | FundsDeposited     | amount: 50000           |
          | 3    | FundsWithdrawn     | amount: 10000           |
          | 4    | FundsTransferred   | amount: 5000, to: A002  |
        When 重建帳戶狀態
        Then 帳戶餘額為 35000 元
        And 事件版本號為 4
    ```
  - TDD: `EventStore` Repository 實作
  - `BankAccount` Aggregate 支援 Event Replay
  - Snapshot 每 100 個事件產生一次

- [ ] **M12-LAB-02**: 保險理賠 Event Sourcing
  - 理賠生命週期事件：報案 → 查勘 → 定損 → 核賠 → 給付
  - 審計追蹤：完整記錄每次狀態變更的人、時、事

---

### M13 — CQRS Read Model (`m13-cqrs-read-model`)

**學習目標**: 實作 Command/Query 分離，優化讀取效能

#### 教材工作清單

- [ ] **M13-DOC-01**: 撰寫「CQRS 架構與 MongoDB」文件
  - Write Model (Aggregate/Event Store) vs Read Model (Query-Optimized View)
  - Projection: 從 Event 建構 Read Model
  - Eventually Consistent Read Model
  - 與 Materialized View 的類比

- [ ] **M13-DOC-02**: 撰寫「Read Model 設計策略」文件
  - Query-Driven Design: 先定義查詢需求，再設計 Read Model
  - Denormalization 策略：適度冗餘換取查詢效能
  - Multiple Read Model: 同一 Write Model 對應多個 Read Model
  - Read Model Rebuild 機制

- [ ] **M13-LAB-01**: 銀行客戶 360 度視圖
  - BDD Feature:
    ```gherkin
    Feature: 客戶 360 度視圖
      Scenario: 查詢客戶綜合資訊
        Given 客戶 "C001" 有帳戶、貸款、投資等多種業務
        When 查詢客戶 360 度視圖
        Then 回傳整合後的客戶資訊，包含:
          | 區塊     | 內容               |
          | 基本資料 | 姓名、等級、VIP 狀態 |
          | 資產摘要 | 存款總額、投資總額   |
          | 負債摘要 | 貸款餘額、信用卡額度 |
          | 近期活動 | 最近 5 筆交易       |
    ```
  - Write Model: 各業務 Aggregate 獨立
  - Read Model: `CustomerSummaryView` 聚合所有業務資料
  - Projection Handler: 監聽 Domain Event 更新 Read Model

- [ ] **M13-LAB-02**: 電商商品列表頁 Read Model
  - Write Model: `Product` Aggregate (完整商品資訊)
  - Read Model: `ProductListView` (列表頁所需的精簡資訊)
  - Read Model: `ProductSearchView` (搜尋引擎所需的索引資訊)

---

### M14 — SAGA Pattern (`m14-saga-pattern`)

**學習目標**: 實作跨 Aggregate 的分散式交易協調

#### 教材工作清單

- [ ] **M14-DOC-01**: 撰寫「SAGA Pattern 與 MongoDB」文件
  - Choreography SAGA vs Orchestration SAGA
  - Compensating Transaction 設計
  - SAGA State Machine
  - MongoDB 作為 SAGA Log Store

- [ ] **M14-DOC-02**: 撰寫「冪等性與重試機制」文件
  - Idempotency Key 設計
  - Outbox Pattern: 可靠的事件發送
  - Dead Letter Queue 處理
  - MongoDB Unique Index 保證冪等性

- [ ] **M14-LAB-01**: 電商下單 SAGA
  - BDD Feature:
    ```gherkin
    Feature: 訂單建立 SAGA
      Scenario: 正常下單流程
        Given 商品 "P001" 庫存 10 件
        And 客戶 "C001" 錢包餘額 5000 元
        When 客戶下單購買 2 件 商品單價 1000 元
        Then 庫存扣減至 8 件
        And 錢包餘額扣減至 3000 元
        And 訂單狀態為 CONFIRMED

      Scenario: 付款失敗補償
        Given 商品 "P001" 庫存 10 件
        And 客戶 "C001" 錢包餘額 500 元
        When 客戶下單購買 2 件 商品單價 1000 元
        Then 庫存恢復至 10 件 (補償)
        And 訂單狀態為 CANCELLED
    ```
  - Orchestration SAGA 實作
  - SAGA Log 存儲於 MongoDB
  - 測試：正常流程、各步驟失敗的補償、冪等性

- [ ] **M14-LAB-02**: 保險投保 SAGA
  - 步驟：核保 → 建保單 → 建收費排程 → 通知客戶
  - 補償：各步驟的反向操作

---

## Phase 4: 效能、可觀測性與維運

### M15 — 索引策略與效能調優 (`m15-indexing-performance`)

**學習目標**: 設計有效的索引策略，優化查詢效能

#### 教材工作清單

- [ ] **M15-DOC-01**: 撰寫「MongoDB 索引類型全解」文件
  - Single Field Index, Compound Index, Multikey Index
  - Text Index, Geospatial Index, Hashed Index
  - Partial Index, Sparse Index, TTL Index
  - Unique Index 與 Compound Unique Index
  - 索引與 RDB B-Tree Index 的異同

- [ ] **M15-DOC-02**: 撰寫「ESR 規則與索引設計」文件
  - ESR (Equality, Sort, Range) 原則
  - Covered Query 實作
  - `explain()` 輸出解讀
  - Index 選擇性分析

- [ ] **M15-DOC-03**: 撰寫「效能基準測試方法論」文件
  - 測試環境標準化 (Testcontainers 配置)
  - 大量資料產生策略 (Java Faker + Bulk Insert)
  - 效能指標：Query Time, Throughput, Index Size
  - 效能回歸測試自動化

- [ ] **M15-LAB-01**: 銀行交易查詢效能調優
  - BDD Feature:
    ```gherkin
    Feature: 交易查詢效能
      Scenario: 大量資料下的查詢效能
        Given 帳戶 "A001" 有 1000000 筆交易記錄
        When 查詢最近 30 天的交易記錄
        Then 查詢回應時間小於 100ms
        And 使用 IXSCAN 而非 COLLSCAN
    ```
  - TDD: 建立不同索引組合，比較 `explain()` 結果
  - Compound Index 最佳化：帳戶ID + 日期 + 類型
  - TTL Index: 自動清理過期的臨時交易記錄

- [ ] **M15-LAB-02**: 電商商品搜尋效能
  - Text Index + Compound Index 組合策略
  - Partial Index: 只索引上架中的商品
  - 測試：10 萬商品下的多維篩選效能

---

### M16 — Change Streams 與事件驅動 (`m16-change-streams`)

**學習目標**: 使用 Change Streams 實作即時資料同步與事件驅動

#### 教材工作清單

- [ ] **M16-DOC-01**: 撰寫「Change Streams 原理與應用」文件
  - Change Streams vs Polling 比較
  - Resume Token 與容錯機制
  - Pre-Image 和 Post-Image 配置
  - 與 Kafka Connect CDC 的比較

- [ ] **M16-DOC-02**: 撰寫「Spring Data MongoDB Change Streams API」文件
  - `ReactiveMongoTemplate.changeStream()`
  - `@Tailable` Cursor
  - `MessageListener` Container 模式
  - Error Handling 與 Backpressure

- [ ] **M16-LAB-01**: 即時帳戶餘額通知
  - BDD Feature:
    ```gherkin
    Feature: 即時餘額變動通知
      Scenario: 存款後即時通知
        Given 客戶 "C001" 訂閱帳戶 "A001" 的餘額變動
        When 帳戶收到一筆 10000 元存款
        Then 在 1 秒內收到餘額變動通知
        And 通知包含變動前後餘額
    ```
  - 使用 Change Streams 監聽帳戶 Collection 變更
  - 整合 Read Model 自動更新

- [ ] **M16-LAB-02**: CDC 驅動的 CQRS Projection
  - Change Streams 作為 Event Source
  - 自動更新 Read Model
  - Resume Token 持久化與容錯

---

### M17 — 可觀測性與監控 (`m17-observability`)

**學習目標**: 建立 MongoDB 應用的完整可觀測性

#### 教材工作清單

- [ ] **M17-DOC-01**: 撰寫「MongoDB 可觀測性三支柱」文件
  - Metrics: Connection Pool, Query Stats, Operation Counts
  - Logging: Query Profiler, Slow Query Log
  - Tracing: Distributed Tracing 與 MongoDB Driver

- [ ] **M17-DOC-02**: 撰寫「Spring Boot Actuator + Micrometer + MongoDB」文件
  - MongoDB Health Indicator
  - Custom Metrics: Query Duration, Collection Size
  - Micrometer + Prometheus 整合
  - Grafana Dashboard 範本

- [ ] **M17-LAB-01**: 全方位監控實作
  - Testcontainers: MongoDB + Prometheus + Grafana Stack
  - 實作自訂 MongoDB Metrics
  - Distributed Tracing: OpenTelemetry + MongoDB Command Listener
  - 測試：慢查詢告警、連線池耗盡告警

- [ ] **M17-LAB-02**: 效能問題診斷演練
  - 模擬場景：缺少索引的慢查詢、連線池耗盡、Write Concern 延遲
  - 使用監控工具定位問題根因
  - 修復驗證

---

### M18 — Schema Migration 與版本管理 (`m18-migration-versioning`)

**學習目標**: 管理 MongoDB Schema 的版本演進

#### 教材工作清單

- [ ] **M18-DOC-01**: 撰寫「MongoDB Schema Migration 策略」文件
  - Eager Migration vs Lazy Migration
  - Mongock (MongoDB Migration Tool) 介紹
  - Application-Level Migration Pattern
  - 零停機 Migration 策略

- [ ] **M18-DOC-02**: 撰寫「資料版本化最佳實踐」文件
  - Document Version Field (`_schemaVersion`)
  - Converter Chain: V1 → V2 → V3 漸進轉換
  - 向後相容設計原則
  - 測試矩陣：新舊版本並存的讀寫測試

- [ ] **M18-LAB-01**: Mongock Migration 實作
  - BDD Feature:
    ```gherkin
    Feature: Schema Migration
      Scenario: 新增欄位的無痛升級
        Given Collection 中有 V1 格式的保單文件 1000 筆
        When 執行 V1 → V2 Migration (新增 riskScore 欄位)
        Then 所有文件更新為 V2 格式
        And riskScore 欄位預設值為 "UNRATED"
        And 應用程式可正常讀寫 V2 文件

      Scenario: 結構重組 Migration
        Given Collection 中有扁平的地址欄位
        When 執行地址結構重組 Migration
        Then 地址欄位重組為嵌入式 Address 物件
    ```
  - Mongock ChangeUnit 實作
  - 回滾機制測試

---

## Phase 5: 整合專案 (Capstone Projects)

### M19 — 銀行核心系統 Capstone (`m19-banking-capstone`)

**學習目標**: 整合所有概念，建構完整的銀行帳戶管理系統

#### 教材工作清單

- [ ] **M19-DOC-01**: 撰寫「銀行帳戶管理系統架構設計」文件
  - Bounded Context Map: 帳戶管理、交易處理、客戶管理、報表
  - Aggregate 識別與邊界定義
  - Event Flow 設計
  - Polyglot Persistence: MongoDB (帳戶/交易) + Redis (Session/快取)

- [ ] **M19-LAB-01**: 完整系統實作
  - Hexagonal Architecture 全模組實作
  - Event Sourcing + CQRS 帳戶交易
  - SAGA: 跨帳戶轉帳
  - Change Streams: 即時餘額更新
  - 完整 BDD Test Suite (10+ Scenarios)
  - 效能測試: 10 萬帳戶、100 萬交易
  - 可觀測性: Metrics + Logging + Tracing

---

### M20 — 保險理賠系統 Capstone (`m20-insurance-capstone`)

**學習目標**: 建構保險理賠處理系統，強調多型建模與流程管理

#### 教材工作清單

- [ ] **M20-DOC-01**: 撰寫「保險理賠系統架構設計」文件
  - 多險種支援：車險、健康險、財產險
  - 理賠流程 State Machine
  - 文件管理策略 (GridFS 或外部 Object Storage)
  - 與核心保單系統的整合

- [ ] **M20-LAB-01**: 完整系統實作
  - Polymorphic Claim Documents (各險種)
  - Event Sourcing: 理賠生命週期完整記錄
  - Aggregation Pipeline: 理賠統計報表
  - Schema Validation: 合規性檢查
  - SAGA: 理賠 → 核賠 → 給付流程
  - 完整 BDD Test Suite

---

### M21 — 電商平台 Capstone (`m21-ecommerce-capstone`)

**學習目標**: 建構電商核心系統，強調高併發與最終一致性

#### 教材工作清單

- [ ] **M21-DOC-01**: 撰寫「電商平台架構設計」文件
  - 商品目錄 (MongoDB) + 庫存 (MongoDB + Redis) + 訂單 (MongoDB)
  - 購物車策略: Redis (即時) + MongoDB (持久化)
  - 搜尋優化: Text Index + Read Model
  - 高併發庫存扣減: Optimistic Lock vs Atomic Update

- [ ] **M21-LAB-01**: 完整系統實作
  - Product Catalog: 多型商品 + 多維搜尋
  - Shopping Cart: Redis + MongoDB 混合
  - Order SAGA: 庫存 → 付款 → 出貨
  - CQRS: 商品列表 Read Model
  - Change Streams: 庫存變動即時同步
  - 壓力測試: 模擬秒殺場景
  - 完整 BDD Test Suite

---

## 附錄

### A. 共用 Gradle 設定範本

```kotlin
// buildSrc/src/main/kotlin/course.conventions.gradle.kts
plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:mongodb")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-spring")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("org.assertj:assertj-core")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### B. Testcontainers 基礎配置範本

```java
@Testcontainers
@SpringBootTest
abstract class MongoIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = 
        new MongoDBContainer("mongo:7.0")
            .withReuse(true);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
}
```

### C. BDD Cucumber 基礎配置範本

```java
@CucumberContextConfiguration
@SpringBootTest
public class CucumberSpringConfig { }
```

```gherkin
# src/test/resources/features/example.feature
Feature: 範例功能
  Scenario: 範例場景
    Given 前置條件
    When 執行動作
    Then 預期結果
```

### D. 各 Module 估計時數

| Module | 文件 | 實作 | 測試 | 合計 |
|--------|------|------|------|------|
| M01 | 3h | 2h | 2h | 7h |
| M02 | 3h | 3h | 2h | 8h |
| M03 | 3h | 3h | 1h | 7h |
| M04 | 3h | 2h | 2h | 7h |
| M05 | 2h | 4h | 3h | 9h |
| M06 | 2h | 4h | 3h | 9h |
| M07 | 2h | 4h | 3h | 9h |
| M08 | 2h | 3h | 2h | 7h |
| M09 | 2h | 3h | 3h | 8h |
| M10 | 3h | 5h | 3h | 11h |
| M11 | 2h | 3h | 2h | 7h |
| M12 | 2h | 5h | 3h | 10h |
| M13 | 2h | 4h | 3h | 9h |
| M14 | 2h | 5h | 3h | 10h |
| M15 | 3h | 4h | 3h | 10h |
| M16 | 2h | 3h | 2h | 7h |
| M17 | 2h | 4h | 2h | 8h |
| M18 | 2h | 3h | 2h | 7h |
| M19 | 2h | 8h | 4h | 14h |
| M20 | 2h | 8h | 4h | 14h |
| M21 | 2h | 8h | 4h | 14h |
| **總計** | | | | **~195h** |

### E. AI Agent 教材產出指引

每個 Module 的教材產出應遵循以下格式與原則:

1. **文件 (DOC)**: Markdown 格式，含程式碼片段、圖表（Mermaid）、比較表格
2. **實作 (LAB)**: 完整可執行的 Gradle Sub-Module，含:
   - `build.gradle.kts` (依賴設定)
   - `src/main/java` (Production Code)
   - `src/test/java` (TDD Test Cases)
   - `src/test/resources/features` (BDD Feature Files)
   - `README.md` (Lab 指引)
3. **測試優先**: 每個 LAB 先寫測試 (Red) → 實作 (Green) → 重構 (Refactor)
4. **情境驅動**: 每個技術概念都綁定金融/保險/電商的具體業務場景
5. **漸進複雜度**: Phase 1 專注基礎, Phase 2-3 逐步加入 DDD/Event Sourcing, Phase 4-5 整合
6. **程式碼風格**: 遵循 Java 23 最新特性、SOLID 原則、Clean Code

---

> **版本**: v1.0
> **最後更新**: 2025-02
> **授權**: 內部教育訓練使用
