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
- Run all tests (146 tests across M01–M07)

### 2. Run Specific Module Tests

```bash
# M01: RDB vs MongoDB comparison
./gradlew :m01-rdb-vs-nosql:test

# M05: Spring Data CRUD
./gradlew :m05-spring-data-crud:test

# M07: Aggregation Pipeline
./gradlew :m07-aggregation-pipeline:test
```

### 3. Read the Documentation

Each module has 3 documentation files (Traditional Chinese). Start from M01:

```bash
ls mongodb-spring-course/m01-rdb-vs-nosql/docs/
ls mongodb-spring-course/m07-aggregation-pipeline/docs/
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
    ├── m08-schema-validation/       Phase 2: Schema Validation
    ├── m09-transactions/            Phase 2: Transactions
    │
    ├── m10-ddd-aggregate-modeling/  Phase 3: DDD Aggregates
    ├── m11-polymorphism-inheritance Phase 3: Polymorphism
    ├── m12-event-sourcing/          Phase 3: Event Sourcing
    ├── m13-cqrs-read-model/         Phase 3: CQRS
    ├── m14-saga-pattern/            Phase 3: SAGA Pattern
    │
    ├── m15-indexing-performance/    Phase 4: Indexing
    ├── m16-change-streams/          Phase 4: Change Streams
    ├── m17-observability/           Phase 4: Observability
    ├── m18-migration-versioning/    Phase 4: Schema Migration
    │
    ├── m19-banking-capstone/        Phase 5: Banking Capstone
    ├── m20-insurance-capstone/      Phase 5: Insurance Capstone
    └── m21-ecommerce-capstone/      Phase 5: E-commerce Capstone

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
| M08 | Schema Validation | *(planned)* | — |
| M09 | Transactions | *(planned)* | — |

### Phase 3: DDD & Advanced Modeling (M10–M14) ★★★

> Goal: Apply Domain-Driven Design patterns with MongoDB.

```
M10 ── DDD Aggregates ──── Hexagonal Architecture + MongoDB
M11 ── Polymorphism ────── Sealed Interfaces, Discriminators
M12 ── Event Sourcing ──── Event Store, Replay, Snapshots
M13 ── CQRS ────────────── Read Models, Projections
M14 ── SAGA Pattern ────── Distributed Transactions, Compensation
```

### Phase 4: Operations & Performance (M15–M18) ★★★

> Goal: Production-ready MongoDB with indexing, monitoring, and migration.

```
M15 ── Indexing ──────── ESR Rule, Compound Indexes, explain()
M16 ── Change Streams ── Real-time Sync, CDC
M17 ── Observability ──── Metrics, Logging, Tracing
M18 ── Migration ──────── Schema Versioning, Mongock
```

### Phase 5: Capstone Projects (M19–M21) ★★★

> Goal: Build complete systems integrating all learned concepts.

```
M19 ── Banking System ──── Event Sourcing + CQRS + SAGA
M20 ── Insurance System ── Polymorphism + Aggregation + Validation
M21 ── E-commerce System ─ High Concurrency + Polyglot Persistence
```

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

---

## Business Domains

The course uses three real-world domains throughout all modules:

| Domain | Key Entities | Modules |
|--------|-------------|---------|
| **Banking** | BankAccount, AccountType, AccountStatus | M01, M05, M06, M07 |
| **Insurance** | InsurancePolicyDocument, PolicyType, PolicyStatus | M01, M05, M06, M07 |
| **E-commerce** | Product, Order, OrderItem, OrderStatus | M05, M06, M07 |

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

## License

Internal educational use only.

---

> **Built with**: Java 23 + Spring Boot 3.4 + MongoDB 8.0 + Testcontainers + Cucumber
>
> **Teaching approach**: Test-First (BDD/TDD), Scenario-Driven (Banking, Insurance, E-commerce)
>
> **Architecture**: OOP, DDD, Hexagonal Architecture, SOLID Principles
