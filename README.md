# MongoDB for Java Spring Developers

> A hands-on, test-first educational course covering MongoDB with Java Spring Boot.

```
   _____ ____  ____  _____ _   _  ____    ____   ____   ____  _______
  / ____|  _ \|  _ \|_   _| \ | |/ ___|  |  _ \ / __ \ / __ \|__   __|
 | (___ | |_) | |_) | | | |  \| | |  __  | |_) | |  | | |  | | | |
  \___ \|  __/|  _ <  | | | . ` | | |_ | |  _ <| |  | | |  | | | |
  ____) | |   | |_) |_| |_| |\  | |__| | | |_) | |__| | |__| | | |
 |_____/|_|   |____/|_____|_| \_|\____/  |____/ \____/ \____/  |_|
                         +  MongoDB
```

---

## Overview

This is a **Gradle multi-module project** designed as a progressive learning path for Java Spring developers transitioning from relational databases to MongoDB. Each module is a self-contained lesson with:

- **Documentation** (Markdown, Traditional Chinese)
- **Production code** (Java 23 + Spring Boot 3.4)
- **TDD tests** (JUnit 5 + AssertJ + Testcontainers)
- **BDD scenarios** (Cucumber + Gherkin)

No external MongoDB installation required — all tests use **Testcontainers** to spin up real MongoDB 8.0 instances in Docker.

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| **Java** | 23 | Language runtime (toolchain managed) |
| **Spring Boot** | 3.4.1 | Application framework |
| **Spring Data MongoDB** | 4.4.x (managed by Boot) | MongoDB data access |
| **Spring Data JPA** | 3.4.x (managed by Boot) | RDB data access (M01 comparison) |
| **Gradle** | 8.12 (wrapper) | Build tool |
| **Testcontainers** | 1.20.x (managed by Boot) | Docker-based integration tests |
| **Cucumber-JVM** | 7.20.1 | BDD test framework |
| **AssertJ** | 3.27.x (managed by Boot) | Fluent test assertions |
| **MongoDB** | 8.0 (container) | Document database |
| **PostgreSQL** | 16 (container, M01 only) | Relational database for comparison |

---

## Prerequisites

Before you start, make sure you have:

1. **Java 23** — Install via [SDKMAN](https://sdkman.io/):
   ```bash
   sdk install java 23.0.2-amzn
   ```

2. **Docker** — Required for Testcontainers:
   ```bash
   docker --version   # Docker 20.10+ recommended
   ```

3. **No MongoDB installation needed** — Testcontainers handles it automatically.

---

## Quick Start

### 1. Clone and Build

```bash
git clone https://github.com/ChunPingWang/mongodb-tutorial.git
cd mongodb-tutorial/mongodb-spring-course
./gradlew build
```

This will:
- Compile all 21 modules
- Download Docker images (first run only: `mongo:8.0`, `postgres:16-alpine`)
- Run all tests (573 tests across M01–M21)

### 2. Run Specific Module Tests

```bash
# M01: RDB vs MongoDB comparison
./gradlew :m01-rdb-vs-nosql:test

# M07: Aggregation Pipeline
./gradlew :m07-aggregation-pipeline:test

# M10: DDD Aggregate Modeling
./gradlew :m10-ddd-aggregate-modeling:test

# M17: Observability
./gradlew :m17-observability:test
```

### 3. Read the Documentation

Each module has 2–3 documentation files (Traditional Chinese). Start from M01:

```bash
ls mongodb-spring-course/m01-rdb-vs-nosql/docs/
ls mongodb-spring-course/m10-ddd-aggregate-modeling/docs/
ls mongodb-spring-course/m17-observability/docs/
```

---

## Project Structure

```
mongodb-tutorial/
├── README.md                          # This file
├── mongodb-spring-course-curriculum.md # Full curriculum overview
│
└── mongodb-spring-course/             # Gradle multi-module project
    ├── build.gradle.kts               # Root build
    ├── settings.gradle.kts            # Includes 21 submodules
    ├── gradle.properties              # Parallel builds, caching
    ├── gradle/libs.versions.toml      # Centralized version catalog
    ├── buildSrc/                      # Convention plugins
    │   └── src/main/kotlin/
    │       ├── course.java-common.gradle.kts    # Java 23 toolchain
    │       └── course.spring-module.gradle.kts  # Spring Boot + test deps
    │
    ├── m01-rdb-vs-nosql/         ✅ Phase 1: RDB vs NoSQL (14 tests)
    ├── m02-nosql-landscape/      ✅ Phase 1: NoSQL Landscape (12 tests)
    ├── m03-environment-setup/    ✅ Phase 1: Environment Setup (5 tests)
    ├── m04-document-thinking/    ✅ Phase 1: Document Modeling (17 tests)
    │
    ├── m05-spring-data-crud/     ✅ Phase 2: CRUD Operations (42 tests)
    ├── m06-query-dsl/            ✅ Phase 2: Query DSL (48 tests)
    ├── m07-aggregation-pipeline/ ✅ Phase 2: Aggregation (46 tests)
    ├── m08-schema-validation/    ✅ Phase 2: Schema Validation (38 tests)
    ├── m09-transactions/         ✅ Phase 2: Transactions (38 tests)
    │
    ├── m10-ddd-aggregate-modeling/ ✅ Phase 3: DDD Aggregates (41 tests)
    ├── m11-polymorphism-inheritance ✅ Phase 3: Polymorphism (32 tests)
    ├── m12-event-sourcing/       ✅ Phase 3: Event Sourcing (32 tests)
    ├── m13-cqrs-read-model/      ✅ Phase 3: CQRS (33 tests)
    ├── m14-saga-pattern/         ✅ Phase 3: SAGA Pattern (28 tests)
    │
    ├── m15-indexing-performance/ ✅ Phase 4: Indexing (28 tests)
    ├── m16-change-streams/       ✅ Phase 4: Change Streams (24 tests)
    ├── m17-observability/        ✅ Phase 4: Observability (24 tests)
    ├── m18-migration-versioning/ ✅ Phase 4: Schema Migration (24 tests)
    │
    ├── m19-banking-capstone/    ✅ Phase 5: Banking Capstone (24 tests)
    ├── m20-insurance-capstone/  ✅ Phase 5: Insurance Capstone (24 tests)
    └── m21-ecommerce-capstone/  ✅ Phase 5: E-commerce Capstone (24 tests)

✅ = Implemented with tests, BDD scenarios, and documentation
```

---

## Learning Path

### Phase 1: Foundation & Mindset Shift (M01–M04) ★☆☆

> Goal: Understand why MongoDB exists and how to think in documents.

| Module | Topic | Key Concepts | Tests |
|--------|-------|--------------|-------|
| M01 | RDB vs NoSQL | PostgreSQL + MongoDB 並行比較, Schema Evolution | 14 |
| M02 | NoSQL Landscape | MongoDB + Redis + Cassandra 比較, Cache-Aside | 12 |
| M03 | Environment Setup | Testcontainers, Cucumber BDD, Gradle Multi-Module | 5 |
| M04 | Document Thinking | Embedding vs Referencing, BSON Types, Java Records | 17 |

### Phase 2: Spring Data MongoDB Core (M05–M09) ★★☆

> Goal: Master CRUD, queries, aggregation, validation, and transactions.

| Module | Topic | Key Concepts | Tests |
|--------|-------|--------------|-------|
| M05 | Spring Data CRUD | MongoRepository, MongoTemplate, Derived Queries, @Query, Pagination | 42 |
| M06 | Query DSL | Criteria API, TextSearch, Geospatial, Aggregation intro | 48 |
| M07 | Aggregation Pipeline | $match/$group/$unwind/$lookup/$bucket/$facet, Typed DTOs | 46 |
| M08 | Schema Validation | $jsonSchema + Jakarta Bean Validation, Schema Evolution, DocumentMigrator | 38 |
| M09 | Transactions | @Transactional, TransactionTemplate, ClientSession, WriteConflict | 38 |

### Phase 3: DDD & Advanced Modeling (M10–M14) ★★★

> Goal: Apply Domain-Driven Design patterns with MongoDB.

| Module | Topic | Key Concepts | Tests |
|--------|-------|--------------|-------|
| M10 | DDD Aggregates | Hexagonal Architecture, Port/Adapter, Domain Events, Specification Pattern | 41 |
| M11 | Polymorphism | Sealed Interface, @TypeAlias, Custom Converter, Guarded Patterns | 32 |
| M12 | Event Sourcing | Event Store, Snapshot, Event Replay, Optimistic Concurrency | 32 |
| M13 | CQRS | Command/Query Separation, Synchronous Projection, ProjectionRebuildService | 33 |
| M14 | SAGA Pattern | Orchestration Saga, SagaOrchestrator, SagaLog, Reverse Compensation | 28 |

### Phase 4: Operations & Performance (M15–M18) ★★★

> Goal: Production-ready MongoDB with indexing, monitoring, and migration.

| Module | Topic | Key Concepts | Tests |
|--------|-------|--------------|-------|
| M15 | Indexing | ESR Rule, Compound/TTL/Partial/Sparse Index, Covered Query, explain() | 28 |
| M16 | Change Streams | MessageListenerContainer, Native Driver watch(), Resume Token, CDC | 24 |
| M17 | Observability | Actuator + Micrometer, CommandListener, SlowQueryDetector, HealthIndicator | 24 |
| M18 | Schema Migration | Mongock Eager + Converter Lazy Migration, Rollback, Zero-Downtime Coexistence | 24 |

### Phase 5: Capstone Projects (M19–M21) ★★★

> Goal: Build complete systems integrating all learned concepts from M01–M18.

| Module | Topic | Integration Highlights | Tests |
|--------|-------|----------------------|-------|
| M19 | Banking Capstone | ES + CQRS + Saga + DDD + Schema Validation + Indexing + Change Streams + Observability | 24 |
| M20 | Insurance Capstone | Polymorphism + ES + CQRS + Saga + DDD Specification reading CQRS | 24 |
| M21 | E-commerce Capstone | **Aggregation Pipeline as Business Logic** in Saga + DDD Specification | 24 |

Each capstone has a **novel integration** not seen in prior modules:

| Capstone | Novel Integration |
|----------|-------------------|
| M19 Banking | ES feeds CQRS synchronously in saga; Change Stream on event store |
| M20 Insurance | Polymorphism + ES; CQRS feeds Saga (simple queries); DDD creates polymorphic entities |
| M21 E-commerce | Aggregation Pipeline on CQRS read models in Saga + DDD Spec |

---

## Implemented Modules

### Module Documentation Index

All documentation is written in **Traditional Chinese (zh-TW)**.

#### M01 — RDB vs NoSQL

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m01-rdb-vs-nosql/docs/M01-DOC-01-data-model-philosophy.md) | 資料模型哲學比較 |
| [DOC-02](mongodb-spring-course/m01-rdb-vs-nosql/docs/M01-DOC-02-cap-consistency.md) | CAP 定理與一致性模型 |
| [DOC-03](mongodb-spring-course/m01-rdb-vs-nosql/docs/M01-DOC-03-selection-framework.md) | 資料庫選型決策框架 |

#### M02 — NoSQL Landscape

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m02-nosql-landscape/docs/M02-DOC-01-nosql-categories.md) | NoSQL 資料庫分類與比較 |
| [DOC-02](mongodb-spring-course/m02-nosql-landscape/docs/M02-DOC-02-mongodb-positioning.md) | MongoDB 在 NoSQL 生態的定位 |
| [DOC-03](mongodb-spring-course/m02-nosql-landscape/docs/M02-DOC-03-polyglot-persistence.md) | Polyglot Persistence 策略 |

#### M03 — Environment Setup

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m03-environment-setup/docs/M03-DOC-01-gradle-multi-module.md) | Gradle Multi-Module 架構 |
| [DOC-02](mongodb-spring-course/m03-environment-setup/docs/M03-DOC-02-testcontainers-strategy.md) | Testcontainers 測試策略 |
| [DOC-03](mongodb-spring-course/m03-environment-setup/docs/M03-DOC-03-bdd-tdd-workflow.md) | BDD + TDD 雙軌工作流 |

#### M04 — Document Thinking

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m04-document-thinking/docs/M04-DOC-01-document-modeling.md) | Document Modeling 設計哲學 |
| [DOC-02](mongodb-spring-course/m04-document-thinking/docs/M04-DOC-02-bson-type-mapping.md) | BSON 型別與 Java 型別映射 |
| [DOC-03](mongodb-spring-course/m04-document-thinking/docs/M04-DOC-03-java-records-sealed.md) | Java Records + Sealed Interface |

#### M05 — Spring Data CRUD

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m05-spring-data-crud/docs/M05-DOC-01-repository-vs-template.md) | MongoRepository vs MongoTemplate |
| [DOC-02](mongodb-spring-course/m05-spring-data-crud/docs/M05-DOC-02-query-methods.md) | 查詢方法: Derived Queries + @Query |
| [DOC-03](mongodb-spring-course/m05-spring-data-crud/docs/M05-DOC-03-update-operations.md) | 更新操作: $set/$inc/$push/$pull |

#### M06 — Query DSL

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m06-query-dsl/docs/M06-DOC-01-criteria-api.md) | Criteria API 程式化查詢 |
| [DOC-02](mongodb-spring-course/m06-query-dsl/docs/M06-DOC-02-text-geospatial.md) | 全文檢索與地理空間查詢 |
| [DOC-03](mongodb-spring-course/m06-query-dsl/docs/M06-DOC-03-projection-aggregation.md) | 投影、Distinct 與 Aggregation 初探 |

#### M07 — Aggregation Pipeline

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m07-aggregation-pipeline/docs/M07-DOC-01-aggregation-pipeline-stages.md) | Aggregation Pipeline 核心階段 |
| [DOC-02](mongodb-spring-course/m07-aggregation-pipeline/docs/M07-DOC-02-lookup-unwind-facet.md) | $lookup、$unwind 與 $facet |
| [DOC-03](mongodb-spring-course/m07-aggregation-pipeline/docs/M07-DOC-03-patterns-and-performance.md) | $bucket 分桶分析與效能最佳化 |

#### M08 — Schema Validation

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m08-schema-validation/docs/M08-DOC-01-mongodb-json-schema.md) | MongoDB $jsonSchema 驗證 |
| [DOC-02](mongodb-spring-course/m08-schema-validation/docs/M08-DOC-02-bean-validation-dual-strategy.md) | Bean Validation 雙軌策略 |
| [DOC-03](mongodb-spring-course/m08-schema-validation/docs/M08-DOC-03-schema-evolution-migration.md) | Schema Evolution 與遷移 |

#### M09 — Transactions

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m09-transactions/docs/M09-DOC-01-mongodb-transaction-deep-dive.md) | MongoDB Transaction 深入解析 |
| [DOC-02](mongodb-spring-course/m09-transactions/docs/M09-DOC-02-spring-transactional-mongodb.md) | Spring @Transactional + MongoDB |
| [DOC-03](mongodb-spring-course/m09-transactions/docs/M09-DOC-03-transaction-patterns-pitfalls.md) | Transaction 模式與陷阱 |

#### M10 — DDD Aggregate Modeling

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m10-ddd-aggregate-modeling/docs/M10-DOC-01-aggregate-root-collection-mapping.md) | Aggregate Root 與 Collection 映射 |
| [DOC-02](mongodb-spring-course/m10-ddd-aggregate-modeling/docs/M10-DOC-02-hexagonal-architecture-mongodb.md) | Hexagonal Architecture + MongoDB |
| [DOC-03](mongodb-spring-course/m10-ddd-aggregate-modeling/docs/M10-DOC-03-rich-domain-model-mongodb.md) | Rich Domain Model + MongoDB |

#### M11 — Polymorphism & Inheritance

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m11-polymorphism-inheritance/docs/M11-DOC-01-mongodb-polymorphism-strategies.md) | MongoDB 多型策略 |
| [DOC-02](mongodb-spring-course/m11-polymorphism-inheritance/docs/M11-DOC-02-sealed-interface-mongodb.md) | Sealed Interface + MongoDB |

#### M12 — Event Sourcing

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m12-event-sourcing/docs/M12-DOC-01-event-sourcing-mongodb.md) | Event Sourcing + MongoDB |
| [DOC-02](mongodb-spring-course/m12-event-sourcing/docs/M12-DOC-02-domain-event-design.md) | Domain Event 設計 |

#### M13 — CQRS Read Model

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m13-cqrs-read-model/docs/M13-DOC-01-cqrs-pattern-mongodb.md) | CQRS Pattern + MongoDB |
| [DOC-02](mongodb-spring-course/m13-cqrs-read-model/docs/M13-DOC-02-projection-design.md) | Projection 設計 |

#### M14 — Saga Pattern

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m14-saga-pattern/docs/M14-DOC-01-saga-pattern-mongodb.md) | Saga Pattern + MongoDB |
| [DOC-02](mongodb-spring-course/m14-saga-pattern/docs/M14-DOC-02-saga-orchestration-design.md) | Saga Orchestration 設計 |

#### M15 — Indexing & Performance

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m15-indexing-performance/docs/M15-DOC-01-index-types.md) | MongoDB 索引類型 |
| [DOC-02](mongodb-spring-course/m15-indexing-performance/docs/M15-DOC-02-esr-rule-index-design.md) | ESR Rule 與索引設計 |
| [DOC-03](mongodb-spring-course/m15-indexing-performance/docs/M15-DOC-03-performance-benchmarking.md) | 效能基準測試 |

#### M16 — Change Streams

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m16-change-streams/docs/M16-DOC-01-change-streams-fundamentals.md) | Change Streams 基礎 |
| [DOC-02](mongodb-spring-course/m16-change-streams/docs/M16-DOC-02-spring-data-message-listener.md) | Spring Data MessageListener |

#### M17 — Observability

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m17-observability/docs/M17-DOC-01-mongodb-observability-pillars.md) | MongoDB 可觀測性三支柱 |
| [DOC-02](mongodb-spring-course/m17-observability/docs/M17-DOC-02-actuator-micrometer-mongodb.md) | Spring Boot Actuator + Micrometer + MongoDB |

#### M18 — Schema Migration & Versioning

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m18-migration-versioning/docs/M18-DOC-01-mongock-eager-migration.md) | Mongock Eager 批次遷移 |
| [DOC-02](mongodb-spring-course/m18-migration-versioning/docs/M18-DOC-02-converter-lazy-migration.md) | Converter Lazy 惰性遷移 |

#### M19 — Banking Capstone

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m19-banking-capstone/docs/M19-DOC-01-banking-capstone-architecture.md) | Banking Capstone 架構概覽 |
| [DOC-02](mongodb-spring-course/m19-banking-capstone/docs/M19-DOC-02-es-cqrs-saga-integration.md) | ES + CQRS + Saga 整合 |

#### M20 — Insurance Capstone

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m20-insurance-capstone/docs/M20-DOC-01-insurance-capstone-architecture.md) | Insurance Capstone 架構概覽 |
| [DOC-02](mongodb-spring-course/m20-insurance-capstone/docs/M20-DOC-02-polymorphism-event-sourcing-integration.md) | 多型 + Event Sourcing 整合 |

#### M21 — E-commerce Capstone

| Document | Title |
|----------|-------|
| [DOC-01](mongodb-spring-course/m21-ecommerce-capstone/docs/M21-DOC-01-ecommerce-capstone-architecture.md) | E-commerce Capstone 架構概覽 |
| [DOC-02](mongodb-spring-course/m21-ecommerce-capstone/docs/M21-DOC-02-aggregation-driven-business-logic.md) | 聚合管線驅動業務邏輯 |

---

## Business Domains

The course uses three real-world domains throughout all modules:

| Domain | Key Entities | Modules |
|--------|-------------|---------|
| **Banking** | BankAccount, Transaction, LoanApplication, FinancialProduct | M01, M05–M07, M09, M10–M13, M15–M17 |
| **Insurance** | InsurancePolicy, Claim, ClaimProcess, ClaimSettlement | M01, M05–M07, M09–M14, M16 |
| **E-commerce** | Product, Order, OrderItem, OrderSaga | M05–M07, M10, M14–M17 |

---

## Common Commands

```bash
# Build everything (compile + test)
cd mongodb-spring-course
./gradlew build

# Build without tests
./gradlew build -x test

# Run a specific module's tests
./gradlew :m07-aggregation-pipeline:test

# Run a single test class
./gradlew :m03-environment-setup:test --tests "*.MongoDbSmokeTest"

# Run only BDD scenarios
./gradlew :m07-aggregation-pipeline:test --tests "*.RunCucumberTest"

# Check dependency tree
./gradlew :m07-aggregation-pipeline:dependencies --configuration testRuntimeClasspath

# Clean everything
./gradlew clean
```

---

## Troubleshooting

| Problem | Solution |
|---|---|
| `Cannot connect to the Docker daemon` | Start Docker Desktop or the Docker daemon |
| `Timeout waiting for container` | Increase Docker memory (4GB+ recommended) |
| `Port already in use` | Testcontainers uses random ports — check for zombie containers: `docker ps` |
| `Java version mismatch` | Ensure Java 23 is installed: `java -version` |
| `MultipleBagFetchException` (M01) | JPA entities use `Set` not `List` for `@OneToMany` with multi-level `JOIN FETCH` |

---

## Learning Resources

### MongoDB Official

| Resource | Description | Link |
|----------|-------------|------|
| **MongoDB Manual** | Official documentation covering all features | [mongodb.com/docs/manual](https://www.mongodb.com/docs/manual/) |
| **MongoDB University** | Free online courses with certificates | [learn.mongodb.com](https://learn.mongodb.com/) |
| **MongoDB Aggregation** | Interactive aggregation pipeline builder | [mongodb.com/docs/manual/aggregation](https://www.mongodb.com/docs/manual/aggregation/) |
| **MongoDB Playground** | Online sandbox to test queries | [mongoplayground.net](https://mongoplayground.net/) |
| **MongoDB Shell (mongosh)** | Modern MongoDB shell documentation | [mongodb.com/docs/mongodb-shell](https://www.mongodb.com/docs/mongodb-shell/) |

### Spring Data MongoDB

| Resource | Description | Link |
|----------|-------------|------|
| **Spring Data MongoDB Reference** | Official Spring Data MongoDB docs | [docs.spring.io/spring-data/mongodb](https://docs.spring.io/spring-data/mongodb/reference/) |
| **Spring Boot + MongoDB Guide** | Getting started guide | [spring.io/guides/gs/accessing-data-mongodb](https://spring.io/guides/gs/accessing-data-mongodb/) |
| **Baeldung Spring Data MongoDB** | Practical tutorials and examples | [baeldung.com/spring-data-mongodb-tutorial](https://www.baeldung.com/spring-data-mongodb-tutorial) |

### Java & Spring Boot

| Resource | Description | Link |
|----------|-------------|------|
| **Spring Boot Reference** | Official Spring Boot documentation | [docs.spring.io/spring-boot](https://docs.spring.io/spring-boot/reference/) |
| **Testcontainers Guide** | Docker-based integration testing | [testcontainers.com/guides](https://testcontainers.com/guides/) |
| **Cucumber-JVM Docs** | BDD framework for Java | [cucumber.io/docs/cucumber](https://cucumber.io/docs/cucumber/) |
| **AssertJ Documentation** | Fluent assertion library | [assertj.github.io/doc](https://assertj.github.io/doc/) |

### Recommended Books

| Book | Author | Topic |
|------|--------|-------|
| *MongoDB: The Definitive Guide* (3rd Ed.) | Shannon Bradshaw et al. | MongoDB fundamentals |
| *MongoDB in Action* (2nd Ed.) | Kyle Banker et al. | Practical MongoDB usage |
| *Spring in Action* (6th Ed.) | Craig Walls | Spring Boot essentials |
| *Domain-Driven Design* | Eric Evans | DDD patterns (Phase 3 background) |

### Video Tutorials

| Resource | Description | Link |
|----------|-------------|------|
| **MongoDB University Courses** | Free, structured learning paths | [learn.mongodb.com](https://learn.mongodb.com/) |
| **Spring Academy** | Official Spring courses | [spring.academy](https://spring.academy/) |

### Community

| Resource | Description | Link |
|----------|-------------|------|
| **MongoDB Community Forums** | Ask questions, share knowledge | [mongodb.com/community/forums](https://www.mongodb.com/community/forums/) |
| **Stack Overflow [mongodb]** | Q&A for specific issues | [stackoverflow.com/questions/tagged/mongodb](https://stackoverflow.com/questions/tagged/mongodb) |
| **Stack Overflow [spring-data-mongodb]** | Spring Data MongoDB Q&A | [stackoverflow.com/questions/tagged/spring-data-mongodb](https://stackoverflow.com/questions/tagged/spring-data-mongodb) |

### Tools

| Tool | Description | Link |
|------|-------------|------|
| **MongoDB Compass** | Official GUI for MongoDB | [mongodb.com/products/compass](https://www.mongodb.com/products/compass) |
| **MongoDB Atlas** | Free cloud-hosted MongoDB (512MB) | [mongodb.com/atlas](https://www.mongodb.com/atlas) |
| **Studio 3T** | Advanced MongoDB IDE | [studio3t.com](https://studio3t.com/) |
| **SDKMAN** | Java version manager | [sdkman.io](https://sdkman.io/) |
| **Docker Desktop** | Container runtime for Testcontainers | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) |

---

## Application Migration Reference

> 從傳統 RDB 遷移到 DDD + MongoDB 架構的實務指南

完成本課程後，讀者可運用以下策略將既有應用改造為 MongoDB 架構。

### 核心觀念：Domain Model vs Data Model

傳統 RDB 開發中，業務規則散落在三處：資料表約束（`NOT NULL`、`CHECK`）、SQL / Stored Procedure、Service 層 if-else。**知識被打碎了**。

Domain Model 把業務規則收攏到物件自身（充血模型），狀態變更的唯一途徑是呼叫物件方法，業務規則無法被繞過。

### MongoDB 的天然優勢

**三個邊界天然對齊**：

```
DDD Aggregate 邊界 ≈ Document 邊界 ≈ Transaction 邊界
```

| 面向 | RDB | MongoDB |
|------|-----|---------|
| Aggregate 重建 | 4~5 次 SQL + ORM 映射 | `findOne()` 直接取得完整 Aggregate |
| Schema 演進 | `ALTER TABLE` + 修改映射 | Schema-on-Read，Document 隨時擴展 |
| N+1 問題 | 需 JOIN Fetch 優化 | 天然消失（一次讀取完整 Document） |
| Event Sourcing | Write Side + Read Side 互相妥協 | MongoDB 同時適合 Event Store 和 Read Model |

### 四階段漸進遷移路線

**核心原則：舊表不動、Adapter 層轉譯、Domain Entity 維持充血模型。**

```
階段一：Anti-Corruption Layer
  └→ 在舊 Service 和舊表之間插入 Domain Entity + Adapter，兩套並行

階段二：收攏寫入路徑
  └→ 業務操作改走 Domain Entity 方法，逐步消除直接 SQL 更新

階段三：引入新 Storage (MongoDB)
  └→ 在 Adapter 層加入 MongoAdapter，做雙寫，讀取逐步切換

階段四：切換並退役
  └→ 驗證資料完整性後，停用 JPA Adapter，退役舊表
```

**每個階段都有穩定的中間態，不存在「一刀切」的風險點。**

### 架構圖

```
Domain Layer (新)               Infrastructure Layer (Adapter)          Storage (舊/新)
──────────────────              ────────────────────────────────        ───────────────
InsurancePolicy                 ├─ JpaInsurancePolicyAdapter ────────→ policy 表
  .endorse()                    │    ↕ PolicyJpaEntity                  endorsement 表
  .cancel()                     │    ↕ EndorsementJpaEntity             coverage 表
  .calculatePremium()           │
                                │
InsurancePolicyRepository       ├─ MongoInsurancePolicyAdapter ──────→ policies Collection
  (Port / Interface)            │    ↕ PolicyDocument                   (未來目標)
```

### 三個實務摩擦點

| 摩擦 | 處理方式 |
|------|---------|
| **約束重複**：DB constraint 與 Domain 規則重複 | Domain 做業務邏輯驗證，DB 做最後防線，確保 Domain 規則是 DB constraint 的超集 |
| **繞過 Domain 的外部寫入** | 短期用 CDC 偵測，長期遷移其他模組走 Domain API |
| **Adapter 層 N+1 問題** | Adapter 層做 JOIN Fetch 優化，但不汙染 Domain 層；這也是遷移到 MongoDB 的動機之一 |

### 課程對應模組

| 遷移階段 | 對應課程模組 |
|---------|------------|
| Aggregate 設計 | M04 Document Thinking、M10 DDD Aggregate |
| 充血模型 + Port/Adapter | M10 Hexagonal Architecture |
| Event Sourcing + CQRS | M12 + M13 |
| Schema 遷移 + 雙寫 | M08 Schema Validation、M18 Migration |
| 完整整合範例 | M19 Banking、M20 Insurance、M21 E-commerce Capstone |

---

## License

Internal educational use only.

---

> **Built with**: Java 23 + Spring Boot 3.4 + MongoDB 8.0 + Testcontainers + Cucumber
>
> **Teaching approach**: Test-First (BDD/TDD), Scenario-Driven (Banking, Insurance, E-commerce)
>
> **Architecture**: OOP, DDD, Hexagonal Architecture, SOLID Principles
