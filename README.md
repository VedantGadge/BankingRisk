# 🏦 Hybrid Banking Risk Intelligence & Alert Triage System

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Cache-red.svg)](https://redis.io/)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-M6-purple.svg)](https://spring.io/projects/spring-ai)

> **A bank-internal operational intelligence platform — not a chatbot.**
> Built for fraud, risk, and compliance analysts to triage suspicious activity in seconds, not minutes.

---

## The Problem This Solves

Legacy fraud systems hand analysts an error code: `ERR-798`.

The analyst then spends **30 minutes** opening five different systems — transaction logs, account history, velocity reports, beneficiary records — to piece together what actually happened. At scale, this is unsustainable. Analyst teams are drowning in alerts, and the important ones get missed.

This system changes that. Every flagged transaction arrives with a **complete, human-readable fraud case summary** already written.

---

## Spring Boot Application Architecture

The system is built as a **modular monolith** in Spring Boot — a deliberate choice for banking systems where operational simplicity, transactional consistency, and deployment predictability matter more than microservice flexibility.

```mermaid
graph TB
    subgraph Client["🌐 Client Layer"]
        API["REST API<br/>HTTP Request"]
    end

    subgraph SpringBoot["☕ Spring Boot Application"]
        direction TB

        subgraph TransactionModule["📦 Transaction Module"]
            TC["TransactionController<br/>@RestController"]
            TS["TransactionService<br/>@Service"]
            TRepo["TransactionRepository<br/>@Repository · JPA"]
        end

        subgraph RiskModule["📦 Risk Module  ← NEW"]
            direction TB
            RAS["RiskAnalysisService<br/>@Service · @Async Orchestrator"]
            RRE["RiskRuleEngine<br/>@Component · Deterministic Math"]
            CTX["RiskContext Builder<br/>Evidence Aggregator"]
            AIGateway["Spring AI Gateway<br/>@Service · LLM Integration"]
            RRepo["RiskAlertRepository<br/>@Repository · JPA"]
        end

        subgraph Infrastructure["⚙️ Spring Infrastructure"]
            SEC["Spring Security<br/>Auth & Authorization"]
            ASYNC["@Async Thread Pool<br/>TaskExecutor"]
            TX["@Transactional<br/>Optimistic Locking"]
        end
    end

    subgraph DataLayer["🗄️ Data Layer"]
        PG[("PostgreSQL<br/>Core Ledger")]
        RD[("Redis<br/>Idempotency · Rate Limiting")]
    end

    subgraph AILayer["🤖 AI Layer"]
        LLM["LLM Provider<br/>OpenAI / Ollama"]
    end

    subgraph DevOps["🐳 DevOps"]
        Docker["Docker Compose"]
        CI["GitHub Actions CI/CD"]
        Trivy["Trivy Security Scanner"]
    end

    API --> SEC
    SEC --> TC
    TC --> TS
    TS --> TRepo
    TS -->|"@Async trigger"| RAS
    TRepo --> PG
    TS --> RD

    RAS --> CTX
    CTX --> TRepo
    CTX --> RRepo
    RAS --> RRE
    RRE -->|"Score ≥ 40"| AIGateway
    AIGateway --> LLM
    AIGateway --> RRepo
    RRepo --> PG
    RAS --> ASYNC
    TS --> TX

    Docker -.->|"containerizes"| SpringBoot
    Docker -.->|"containerizes"| DataLayer
    CI -.->|"builds & scans"| Trivy

    classDef springComponent fill:#6db33f,color:#fff,stroke:#5a9c32,stroke-width:2px
    classDef newModule fill:#1a365d,color:#fff,stroke:#2a4a7f,stroke-width:2px
    classDef dataStore fill:#336791,color:#fff,stroke:#2a5580,stroke-width:2px
    classDef redis fill:#dc382d,color:#fff,stroke:#b52e24,stroke-width:2px
    classDef ai fill:#7c3aed,color:#fff,stroke:#6d28d9,stroke-width:2px
    classDef devops fill:#374151,color:#fff,stroke:#1f2937,stroke-width:2px
    classDef client fill:#0f766e,color:#fff,stroke:#0d6460,stroke-width:2px

    class TC,TS,TRepo,SEC,ASYNC,TX springComponent
    class RAS,RRE,CTX,AIGateway,RRepo newModule
    class PG dataStore
    class RD redis
    class LLM ai
    class Docker,CI,Trivy devops
    class API client
```

---

## End-to-End Data Flow

How a single transaction moves through the entire system — from API call to analyst alert.

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 User
    participant API as REST API<br/>@RestController
    participant TS as TransactionService<br/>@Service
    participant Redis as Redis<br/>Idempotency Cache
    participant PG as PostgreSQL<br/>Ledger
    participant RAS as RiskAnalysisService<br/>@Async
    participant CTX as RiskContext Builder
    participant RRE as RiskRuleEngine
    participant AI as Spring AI Gateway
    participant LLM as LLM Provider
    participant Alert as RiskAlert<br/>Repository

    User->>API: POST /transactions
    API->>Redis: Check idempotency key
    Redis-->>API: Key not seen — proceed

    API->>TS: createTransaction()
    TS->>PG: BEGIN @Transactional
    PG-->>TS: Optimistic lock acquired
    TS->>PG: Debit sender · Credit receiver
    PG-->>TS: COMMIT — ledger updated
    TS-->>API: TransactionDTO (200 OK)
    API-->>User: ✅ Transaction confirmed

    Note over TS,RAS: @Async — non-blocking, separate thread pool
    TS-)RAS: analyzeTransaction(txnId) [fire and forget]

    RAS->>CTX: buildRiskContext(txnId)
    CTX->>PG: Fetch account age, velocity,<br/>prior alerts, beneficiary history
    PG-->>CTX: Structured evidence
    CTX-->>RAS: RiskContext object

    RAS->>RRE: evaluateRules(RiskContext)
    Note over RRE: Executes in less than 5ms — pure deterministic math
    RRE->>RRE: Score velocity breach +30<br/>Score new beneficiary +25<br/>Score account age +20<br/>Score login anomaly +16
    RRE-->>RAS: Score 91 · Level HIGH<br/>Rules: [VELOCITY_BREACH, NEW_BENEFICIARY, LOGIN_ANOMALY]

    alt Score less than 40 — LOW RISK
        RAS->>RAS: Fast-path exit. No AI call. No alert stored.
    else Score 40 or above — MEDIUM or HIGH RISK
        RAS->>AI: generateExplanation(RiskContext, triggeredRules)
        AI->>LLM: Structured prompt with evidence
        LLM-->>AI: Human-readable fraud pattern narrative
        AI->>Alert: saveRiskAlert(score, level, rules, explanation)
        Alert->>PG: INSERT risk_alerts
        PG-->>Alert: Alert persisted ✅
    end

    Note over Alert,PG: Analyst opens dashboard. Reads one paragraph. Triage complete in 30 seconds.
```

---

## Risk Module — Internal Component Flow

A closer look at what happens inside the `risk/` module between context building and alert generation.

```mermaid
flowchart TD
    START(["⚡ @Async Trigger\nfrom TransactionService"])

    subgraph CTXLayer["Context Builder Layer"]
        CB["RiskContext.build()"]
        CB_A["→ Account age (days)"]
        CB_B["→ Hourly transfer velocity ($)"]
        CB_C["→ Beneficiary seen before?"]
        CB_D["→ Login anomaly flag"]
        CB_E["→ Prior alert history"]
    end

    subgraph RuleLayer["Deterministic Rule Engine — @Component"]
        RE["RiskRuleEngine.evaluate()"]
        R1{"Account age\nunder 7 days?"}
        R2{"Velocity\nover $5,000/hr?"}
        R3{"New\nBeneficiary?"}
        R4{"Login\nAnomaly?"}
        R5{"Amount\nover $10,000?"}
        SCORE["Aggregate Score\n0 – 100"]
        RULES["Triggered Rules List"]
    end

    subgraph GatewayLayer["AI Explainability Layer — Spring AI"]
        GATE{"Risk Level?"}
        LOW["🟢 LOW  Score under 40\nFast-path exit\nNo AI cost"]
        MED["🟡 MEDIUM  Score 40 to 79\nAsync AI call"]
        HIGH["🔴 HIGH  Score 80 to 100\nAsync AI call\nPriority alert"]
        PROMPT["Build structured prompt\nRiskContext + Rules → LLM"]
        EXPLAIN["LLM returns\nfraud pattern narrative"]
    end

    subgraph AlertLayer["Risk Alert Layer — @Repository · PostgreSQL"]
        SAVE["RiskAlert.save()"]
        DB[("PostgreSQL\nrisk_alerts table")]
        ANALYST["👨‍💼 Analyst Dashboard\n30-second triage"]
    end

    START --> CB
    CB --> CB_A & CB_B & CB_C & CB_D & CB_E
    CB_A & CB_B & CB_C & CB_D & CB_E --> RE

    RE --> R1 & R2 & R3 & R4 & R5
    R1 -->|"+20 pts"| SCORE
    R2 -->|"+30 pts"| SCORE
    R3 -->|"+25 pts"| SCORE
    R4 -->|"+16 pts"| SCORE
    R5 -->|"+10 pts"| SCORE
    R1 & R2 & R3 & R4 & R5 --> RULES
    SCORE & RULES --> GATE

    GATE --> LOW
    GATE --> MED
    GATE --> HIGH
    MED & HIGH --> PROMPT
    PROMPT --> EXPLAIN
    EXPLAIN --> SAVE
    SAVE --> DB
    DB --> ANALYST

    classDef springGreen fill:#6db33f,color:#fff,stroke:#5a9c32,stroke-width:2px
    classDef riskBlue fill:#1a365d,color:#fff,stroke:#2a4a7f,stroke-width:2px
    classDef lowRisk fill:#166534,color:#fff,stroke:#14532d,stroke-width:2px
    classDef medRisk fill:#92400e,color:#fff,stroke:#78350f,stroke-width:2px
    classDef highRisk fill:#991b1b,color:#fff,stroke:#7f1d1d,stroke-width:2px
    classDef aiPurple fill:#7c3aed,color:#fff,stroke:#6d28d9,stroke-width:2px
    classDef db fill:#336791,color:#fff,stroke:#2a5580,stroke-width:2px
    classDef start fill:#0f766e,color:#fff,stroke:#0d6460,stroke-width:2px

    class CB,CB_A,CB_B,CB_C,CB_D,CB_E springGreen
    class RE,R1,R2,R3,R4,R5,SCORE,RULES riskBlue
    class LOW lowRisk
    class MED medRisk
    class HIGH highRisk
    class PROMPT,EXPLAIN aiPurple
    class SAVE,DB db
    class START,ANALYST start
```

---

## Module Structure

```
risk/                              ← New module built on top of existing Spring Boot app
├── dto/
│   ├── RiskAlertDTO.java          # API response shape
│   └── RiskContextDTO.java        # Structured evidence passed between layers
├── entity/
│   └── RiskAlert.java             # @Entity — persisted alert record
├── enums/
│   └── RiskLevel.java             # LOW · MEDIUM · HIGH
├── repository/
│   └── RiskAlertRepository.java   # @Repository — JpaRepository<RiskAlert, UUID>
└── service/
    ├── RiskAnalysisService.java    # @Service @Async — orchestrates both paths
    └── RiskRuleEngine.java         # @Component — pure deterministic scoring
```

---

## Tech Stack

| Component             | Technology         | Why                                                                    |
| --------------------- | ------------------ | ---------------------------------------------------------------------- |
| Backend Framework     | **Spring Boot**    | Production-grade, industry standard in banking systems                 |
| AI Integration        | **Spring AI**      | Structured prompt management, provider-agnostic LLM abstraction        |
| Database              | **PostgreSQL**     | ACID-compliant, optimistic locking for ledger integrity                |
| Cache / Rate Limiting | **Redis**          | Idempotency guarantees, sub-millisecond key lookups                    |
| Async Processing      | **Spring @Async**  | Non-blocking AI calls — zero added latency on transaction confirmation |
| Containerization      | **Docker**         | Environment parity across dev/staging/prod                             |
| CI/CD                 | **GitHub Actions** | Automated build, test, and deploy pipeline                             |
| Security Scanning     | **Trivy**          | Container vulnerability scanning on every build                        |

---

## Example Alert Output

```json
{
  "riskLevel": "HIGH",
  "riskScore": 91,
  "triggeredRules": [
    "VELOCITY_BREACH",
    "NEW_BENEFICIARY",
    "LOGIN_ANOMALY",
    "ACCOUNT_AGE_UNDER_7_DAYS"
  ],
  "explanation": "This transaction matches a classic Account Takeover pattern.
  A large transfer was attempted to a first-time beneficiary immediately
  following multiple failed login attempts on a previously dormant account.
  The combination of access anomaly + new payee + high value is consistent
  with credential-stuffing attacks observed in retail banking fraud."
}
```

An analyst reads this in **30 seconds**. Previously: 30 minutes.

---

## Why Not Just Use AI for Everything?

This is the most important design decision in the system.

**The "Black Box AI" problem in banking is real and legal.** Regulators require that risk decisions be explainable, reproducible, and auditable (SR 11-7, PCI-DSS). An LLM confidence score is none of these things — it can hallucinate, it varies between runs, and it cannot be cross-examined in a compliance review.

By splitting the architecture:

- The **Rule Engine** provides the auditable reason (`Velocity exceeded $5,000/hr`)
- The **LLM** provides the human translation (`This matches a Bust-Out fraud pattern...`)

The analyst gets speed. The bank gets legal defensibility.

---

## Risk Scoring Logic

| Score Range | Risk Level | Action                                                         |
| ----------- | ---------- | -------------------------------------------------------------- |
| 0 – 39      | 🟢 LOW     | Fast-path exit. No AI call. No alert raised.                   |
| 40 – 79     | 🟡 MEDIUM  | AI explanation generated async. Alert raised for review.       |
| 80 – 100    | 🔴 HIGH    | AI explanation generated async. Alert prioritized immediately. |

---

## Known Limitations (By Design)

- **No autonomous approve/reject** — Human analysts remain in control. The system informs, not decides.
- **No behavioral biometrics** — Device fingerprinting requires client-side instrumentation (ThreatMetrix, Sardine) outside this scope.
- **No cross-institution signals** — Synthetic identity detection requires consortium data; undetectable inside a single bank's DB.
- **Static thresholds** — Rule engine parameters are illustrative. Production calibration requires labelled historical fraud datasets.

These are not oversights — they are the reason this system is correctly scoped as a **triage intelligence layer**, not a complete fraud prevention platform.

---

## Roadmap

- [ ] Replace rule engine with XGBoost model trained on labelled fraud data
- [ ] Kafka-based async queue for AI gateway at scale
- [ ] Vector DB for semantic similarity search against historical fraud cases
- [ ] Analyst dashboard with case management workflow
- [ ] SR 11-7 model risk documentation

---

## 🚀 Getting Started

### Prerequisites

*   **Java 17** (or higher)
*   **Maven** 3.8+
*   **Docker** & **Docker Compose** (for running Redis/Postgres locally)
*   An **OpenRouter** or OpenAI API Key (for the AI Explainability module)

### 1. Environment Configuration

The application uses an `.env.properties` file for configuration. Create this file in the root directory and add the following keys (update the values with your credentials):

```properties
# .env.properties
# Database Configuration (e.g., NeonDB or Local)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=banking_db
DB_USERNAME=postgres
DB_PASSWORD=yourpassword

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# AI Gateway (Using OpenRouter in this example)
OPENROUTER_API_KEY=sk-or-v1-...

# Security
JWT_SECRET=your-super-secret-key-for-jwt-signing
```

### 2. Start Supporting Infrastructure

Use Docker Compose to spin up the required Redis (and optionally PostgreSQL) instances:

```bash
docker-compose up -d
```

### 3. Run the Application

You can start the Spring Boot application using the Maven wrapper:

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`. Flyway will automatically run database migrations on startup.

---

## 📖 API Documentation & Usage

Once the application is running, the interactive **Swagger/OpenAPI UI** is available at:

👉 `http://localhost:8080/swagger-ui.html`

### Testing the End-to-End Flow

To see the Risk Engine and AI Explainability in action, you can trigger a high-risk transaction. Assuming you have registered a user and obtained a JWT token, use the following `cURL` command:

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: txn-12345" \
  -d '{
    "amount": 15000.00,
    "receiverAccountId": "acc-98765",
    "description": "Urgent transfer"
  }'
```

*Since this is a high-value transfer, the `RiskAnalysisService` will intercept it, run the deterministic rules, score it asynchronously, and trigger the AI agent to generate a fraud case explanation.*

---

## 🧪 Testing

The project includes unit and integration tests (including Spring Security tests). You can execute the test suite using:

```bash
./mvnw test
```

For more details on the testing strategy, refer to `HOW_TO_RUN_TESTS.md`.

---

## What I Learned Building This

The hardest part wasn't the code. It was understanding _why_ the architecture had to be split.

Most AI fraud systems fail in banking not because the models are bad, but because the integration ignores regulatory reality. A model that is 94% accurate still has a 6% error rate — and every one of those errors is a customer wrongly blocked, a compliance incident, or a legal liability. The math layer exists to make those errors traceable and defensible.

Building this taught me that in regulated industries, system design is as much a legal problem as an engineering one.

---

_Built as a personal project to explore real-world banking systems architecture._

**Stack:** Spring Boot · Spring AI · PostgreSQL · Redis · Docker · GitHub Actions · Trivy
