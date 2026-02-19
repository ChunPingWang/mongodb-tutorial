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

- **Documentation** (Markdown with Mermaid diagrams)
- **Production code** (Java 23 + Spring Boot 3.4)
- **TDD tests** (JUnit 5 + AssertJ + Testcontainers)
- **BDD scenarios** (Cucumber + Gherkin)

No external MongoDB installation required — all tests use **Testcontainers** to spin up real MongoDB 8.0 instances in Docker.

---

## Architecture

### Project Structure

```
mongodb-spring-course/
├── build.gradle.kts                 # Root build (minimal)
├── settings.gradle.kts              # Includes all 21 submodules
├── gradle.properties                # Parallel builds, caching, JVM args
├── gradle/
│   └── libs.versions.toml           # Centralized version catalog
├── buildSrc/
│   ├── build.gradle.kts             # Kotlin DSL + Spring Boot plugin
│   ├── settings.gradle.kts          # Links version catalog
│   └── src/main/kotlin/
│       ├── course.java-common.gradle.kts    # Java 23 toolchain, test config
│       └── course.spring-module.gradle.kts  # Spring Boot + MongoDB + test deps
│
├── m01-rdb-vs-nosql/          ★ Phase 1: RDB vs NoSQL mindset shift
├── m02-nosql-landscape/         Phase 1: NoSQL landscape
├── m03-environment-setup/     ★ Phase 1: Dev environment & test infra
├── m04-document-thinking/       Phase 1: Document modeling
│
├── m05-spring-data-crud/        Phase 2: CRUD operations
├── m06-query-dsl/               Phase 2: Query DSL
├── m07-aggregation-pipeline/    Phase 2: Aggregation
├── m08-schema-validation/       Phase 2: Schema validation
├── m09-transactions/            Phase 2: Multi-document transactions
│
├── m10-ddd-aggregate-modeling/  Phase 3: DDD aggregates
├── m11-polymorphism-inheritance Phase 3: Polymorphism
├── m12-event-sourcing/          Phase 3: Event sourcing
├── m13-cqrs-read-model/        Phase 3: CQRS
├── m14-saga-pattern/            Phase 3: SAGA pattern
│
├── m15-indexing-performance/    Phase 4: Indexing & performance
├── m16-change-streams/          Phase 4: Change streams
├── m17-observability/           Phase 4: Observability
├── m18-migration-versioning/    Phase 4: Schema migration
│
├── m19-banking-capstone/        Phase 5: Banking system capstone
├── m20-insurance-capstone/      Phase 5: Insurance system capstone
└── m21-ecommerce-capstone/      Phase 5: E-commerce capstone

★ = Fully implemented with tests and documentation
```

### Build System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    settings.gradle.kts                       │
│              (includes 21 submodules)                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                      buildSrc/                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  course.java-common.gradle.kts                      │    │
│  │  • Java 23 toolchain                                │    │
│  │  • JUnit Platform                                   │    │
│  │  • Maven Central repository                         │    │
│  └──────────────────────┬──────────────────────────────┘    │
│  ┌──────────────────────▼──────────────────────────────┐    │
│  │  course.spring-module.gradle.kts                    │    │
│  │  • Spring Boot 3.4.1 + Dependency Management        │    │
│  │  • Spring Data MongoDB                              │    │
│  │  • Testcontainers (MongoDB, JUnit Jupiter)          │    │
│  │  • Cucumber (Java, Spring, JUnit Platform Engine)   │    │
│  │  • AssertJ                                          │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
   ┌─────────┐   ┌─────────┐   ┌─────────┐
   │  m01    │   │  m02    │   │  ...    │    Each module applies
   │  build  │   │  build  │   │  build  │    course.spring-module
   │ .kts    │   │ .kts    │   │ .kts    │    convention plugin
   └─────────┘   └─────────┘   └─────────┘
```

### Test Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  ./gradlew test                          │
└────────────────────────┬────────────────────────────────┘
                         │
              ┌──────────▼──────────┐
              │   JUnit 5 Platform  │
              └──┬──────────────┬───┘
                 │              │
    ┌────────────▼───┐   ┌─────▼────────────┐
    │ Jupiter Engine │   │ Cucumber Engine  │
    │ (JUnit tests)  │   │ (BDD scenarios)  │
    └───────┬────────┘   └───────┬──────────┘
            │                    │
  ┌─────────▼─────────┐  ┌──────▼──────────────┐
  │ MongoDbSmokeTest   │  │ .feature files      │
  │ RdbVsMongoTest     │  │    ↓                │
  │ SchemaEvolution    │  │ Step Definitions    │
  │ Test               │  │    ↓                │
  │                    │  │ CucumberSpringConfig│
  └─────────┬──────────┘  └──────┬──────────────┘
            │                    │
            └────────┬───────────┘
                     │
          ┌──────────▼──────────┐
          │  Spring Boot Test   │
          │  ApplicationContext │
          └──────────┬──────────┘
                     │
          ┌──────────▼──────────┐
          │   Testcontainers    │
          │  ┌───────┐ ┌─────┐ │
          │  │MongoDB│ │Pg   │ │
          │  │ 8.0   │ │ 16  │ │
          │  └───────┘ └─────┘ │
          └─────────────────────┘
               Docker
```

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
cd mongodb-spring-course
./gradlew build
```

This will:
- Compile all 21 modules
- Download Docker images (first run only: `mongo:8.0`, `postgres:16-alpine`)
- Run all tests (14 tests across M01 and M03)

### 2. Run Specific Module Tests

```bash
# M03: Environment setup smoke test + BDD
./gradlew :m03-environment-setup:test

# M01: RDB vs MongoDB comparison tests
./gradlew :m01-rdb-vs-nosql:test
```

### 3. Read the Documentation

Start with the M03 docs to understand the build system, then M01 for the conceptual foundation:

| Document | Description |
|---|---|
| [`m03/docs/M03-DOC-01`](m03-environment-setup/docs/M03-DOC-01-gradle-multi-module.md) | Gradle multi-module build guide |
| [`m03/docs/M03-DOC-02`](m03-environment-setup/docs/M03-DOC-02-testcontainers-strategy.md) | Testcontainers testing strategy |
| [`m03/docs/M03-DOC-03`](m03-environment-setup/docs/M03-DOC-03-bdd-tdd-workflow.md) | BDD + TDD dual-track workflow |
| [`m01/docs/M01-DOC-01`](m01-rdb-vs-nosql/docs/M01-DOC-01-data-model-philosophy.md) | 資料模型哲學比較 (Data Model Philosophy) |
| [`m01/docs/M01-DOC-02`](m01-rdb-vs-nosql/docs/M01-DOC-02-cap-consistency.md) | CAP 定理與一致性模型 (CAP & Consistency) |
| [`m01/docs/M01-DOC-03`](m01-rdb-vs-nosql/docs/M01-DOC-03-selection-framework.md) | 資料庫選型決策框架 (Selection Framework) |

---

## Learning Path

### Phase 1: Foundation & Mindset Shift (M01–M04)

> Goal: Understand why MongoDB exists and how to think in documents.

```
M01 ─── RDB vs NoSQL ────────── "Why not just use PostgreSQL?"
 │      ├── LAB-01: Side-by-side PostgreSQL + MongoDB
 │      ├── LAB-02: Schema evolution comparison
 │      └── BDD: 2 scenarios
 │
M02 ─── NoSQL Landscape ─────── "Where does MongoDB fit?"
 │      (placeholder)
 │
M03 ─── Environment Setup ───── "How do I set up and test?"
 │      ├── LAB-01: Testcontainers smoke test
 │      ├── LAB-02: Cucumber BDD infrastructure
 │      └── BDD: 2 scenarios
 │
M04 ─── Document Thinking ───── "How do I model data?"
        (placeholder)
```

### Phase 2: Spring Data MongoDB Core (M05–M09)

> Goal: Master CRUD, queries, aggregation, validation, and transactions.

```
M05 ─── CRUD & Repository ───── Banking accounts, insurance policies
M06 ─── Query DSL ────────────── 4 query levels, complex filters
M07 ─── Aggregation Pipeline ── Reports, statistics, analytics
M08 ─── Schema Validation ────── JSON Schema, data governance
M09 ─── Transactions ──────────── Multi-document ACID, bank transfers
```

### Phase 3: DDD & Advanced Modeling (M10–M14)

> Goal: Apply Domain-Driven Design patterns with MongoDB.

```
M10 ─── DDD Aggregates ──────── Hexagonal architecture + MongoDB
M11 ─── Polymorphism ──────────── Sealed interfaces, discriminators
M12 ─── Event Sourcing ──────── Event store, replay, snapshots
M13 ─── CQRS ──────────────────── Read models, projections
M14 ─── SAGA Pattern ──────────── Distributed transactions, compensation
```

### Phase 4: Operations & Performance (M15–M18)

> Goal: Production-ready MongoDB with indexing, monitoring, and migration.

```
M15 ─── Indexing ──────────────── ESR rule, compound indexes, explain()
M16 ─── Change Streams ──────── Real-time sync, CDC
M17 ─── Observability ──────── Metrics, logging, tracing
M18 ─── Migration ────────────── Schema versioning, Mongock
```

### Phase 5: Capstone Projects (M19–M21)

> Goal: Build complete systems integrating all learned concepts.

```
M19 ─── Banking System ──────── Event Sourcing + CQRS + SAGA
M20 ─── Insurance System ────── Polymorphism + Aggregation + Validation
M21 ─── E-commerce System ───── High concurrency + Polyglot Persistence
```

---

## Module Anatomy

Every active module follows this standard structure:

```
m03-environment-setup/
├── build.gradle.kts                          # Dependencies (applies convention plugin)
├── docs/
│   ├── M03-DOC-01-gradle-multi-module.md     # Concept documentation
│   ├── M03-DOC-02-testcontainers-strategy.md
│   └── M03-DOC-03-bdd-tdd-workflow.md
└── src/
    ├── main/
    │   ├── java/com/mongodb/course/m03/
    │   │   └── M03Application.java           # Spring Boot entry point
    │   └── resources/
    │       └── application.properties
    └── test/
        ├── java/com/mongodb/course/m03/
        │   ├── MongoDbSmokeTest.java          # TDD: JUnit 5 integration tests
        │   └── bdd/
        │       ├── CucumberSpringConfig.java  # BDD: Spring + Testcontainers
        │       ├── MongoConnectionSteps.java  # BDD: Step definitions
        │       └── RunCucumberTest.java       # BDD: Suite runner
        └── resources/
            └── features/
                └── mongodb-connection.feature # BDD: Gherkin scenarios
```

---

## Implemented Modules Detail

### M03 — Environment Setup

The foundation module that establishes the entire test infrastructure.

**Tests (5 total):**

| Test | Type | What It Verifies |
|---|---|---|
| `shouldConnectToMongoDB` | TDD | MongoDB container starts and responds to ping |
| `shouldWriteAndReadDocument` | TDD | Insert and find a BSON document |
| `shouldPerformCrudOperations` | TDD | Full Create → Read → Update → Delete cycle |
| `Successfully connect to MongoDB` | BDD | Cucumber: ping command returns ok |
| `Write and read a document` | BDD | Cucumber: insert + query by name |

### M01 — RDB vs NoSQL

Side-by-side comparison running **both PostgreSQL and MongoDB** simultaneously.

**Domain Models:**

| RDB (JPA) | MongoDB (Spring Data) | Purpose |
|---|---|---|
| `CustomerEntity` + `OrderEntity` + `OrderItemEntity` | `CustomerDocument` (embedded orders + items) | Customer order comparison |
| `InsurancePolicyEntity` | `InsurancePolicyDocument` | Schema evolution comparison |

**Tests (9 total):**

| Test | Type | What It Demonstrates |
|---|---|---|
| `rdbNormalizedModel` | TDD | 3-table normalized model requires JOIN |
| `mongoDbDenormalizedModel` | TDD | Single document read, no JOINs |
| `nestedQueryComparison` | TDD | Multi-table JOIN vs nested document query |
| `mongoFlexibleSchema` | TDD | Different document structures coexist |
| `rdbSchemaEvolution` | TDD | ALTER TABLE required for new column |
| `mongoSchemaEvolution` | TDD | Add fields freely, no migration |
| `mongoBackwardCompatibility` | TDD | Old documents readable with new schema |
| `Store and retrieve...` | BDD | Cucumber: same data in both databases |
| `Nested query comparison` | BDD | Cucumber: JOIN vs embedded query |

---

## Common Commands

```bash
# Build everything (compile + test)
./gradlew build

# Build without tests
./gradlew build -x test

# Run a specific module's tests
./gradlew :m03-environment-setup:test
./gradlew :m01-rdb-vs-nosql:test

# Run a single test class
./gradlew :m03-environment-setup:test --tests "*.MongoDbSmokeTest"

# Run only BDD scenarios
./gradlew :m03-environment-setup:test --tests "*.RunCucumberTest"

# Check dependency tree
./gradlew :m03-environment-setup:dependencies --configuration testRuntimeClasspath

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

## Business Scenarios

The course uses three real-world domains throughout all modules:

| Domain | Example Use Cases | Modules |
|---|---|---|
| **Banking** | Account management, transfers, ledger, loan applications | M01, M05, M06, M07, M09, M10, M12, M13, M15, M16, M19 |
| **Insurance** | Policy CRUD, claims processing, multi-product, underwriting | M01, M05, M08, M10, M11, M14, M18, M20 |
| **E-commerce** | Product catalog, orders, shopping cart, inventory, search | M05, M06, M07, M13, M14, M15, M21 |

---

## License

Internal educational use only.

---

> **Built with**: Java 23 + Spring Boot 3.4 + MongoDB 8.0 + Testcontainers + Cucumber
>
> **Teaching approach**: Test-First (BDD/TDD), Scenario-Driven (Banking, Insurance, E-commerce)
>
> **Architecture**: OOP, DDD, Hexagonal Architecture, SOLID Principles
