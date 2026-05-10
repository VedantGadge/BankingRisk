PROJECT FEATURE:
AI-Powered Banking Risk Intelligence & Alert Triage System

GOAL:
Extend the existing Spring Boot banking backend into a bank-side operational intelligence platform that helps fraud/risk/compliance analysts triage suspicious banking activity using AI.

IMPORTANT:
This is NOT a customer chatbot.
This is a bank-internal risk operations system.

ARCHITECTURE STYLE:
- Modular monolith
- Spring Boot
- PostgreSQL
- Spring AI integration
- Dockerized + deployed
- CI/CD enabled

NEW MODULE:
risk/

PURPOSE OF RISK MODULE:
The Risk module acts as an AI-assisted operational intelligence layer.

It:
- monitors transaction activity
- creates risk alerts
- analyzes suspicious behavior
- generates AI summaries
- helps analysts prioritize alerts
- provides explainable risk reasoning

It DOES NOT:
- directly move money
- directly freeze accounts
- autonomously approve/reject transactions

Human analysts remain in control.

====================================================
OVERALL FLOW
====================================================

1. User initiates transaction
2. Transaction module creates transaction
3. Risk module analyzes transaction
4. Risk module retrieves historical context from DB
5. Risk context is built
6. Spring AI analyzes transaction + context
7. AI generates:
    - risk score
    - risk level
    - summary
    - reason codes
    - recommended action
8. Risk alert stored in DB
9. Analyst reviews alert if needed
10. Transaction proceeds or gets flagged

====================================================
IMPORTANT AI ARCHITECTURE CONCEPT
====================================================

This system should use:
STRUCTURED RETRIEVAL

NOT just plain prompting.

Meaning:
Before calling the LLM, retrieve contextual evidence from PostgreSQL.

Examples:
- recent transactions
- previous alerts
- account age
- transfer velocity
- beneficiary history
- failed logins
- unusual activity patterns

This retrieved context is then injected into the AI prompt.

====================================================
ARCHITECTURAL LAYERS
====================================================

1. Retrieval Layer
   Responsible for fetching structured evidence from DB.

Examples:
- transactionRepository
- riskAlertRepository
- accountRepository

2. Context Builder Layer
   Converts raw DB data into AI-readable structured context.

Example:
RiskContext:
- avg transaction amount
- login anomalies
- recent alerts
- beneficiary trust level

3. AI Reasoning Layer
   Spring AI + LLM reasons over retrieved evidence.

4. Risk Alert Layer
   Stores generated alerts and analyst workflow state.

====================================================
WHY THIS IS BETTER THAN NORMAL CHATBOT
====================================================

This solves a real banking operational problem:
Too many alerts and too much manual analyst work.

The system helps banks:
- reduce analyst workload
- prioritize risky activity
- improve fraud/compliance triage
- generate explainable case summaries
- speed up investigations

====================================================
MODULE STRUCTURE
====================================================

risk/
├── controller/
├── dto/
├── entity/
├── enums/
├── repository/
├── service/
├── ai/

====================================================
CORE ENTITIES
====================================================

RiskAlert
Fields:
- id
- transactionId
- userId
- riskLevel
- riskScore
- summary
- reasonCodes
- recommendedAction
- status
- createdAt

====================================================
RISK LEVELS
====================================================

LOW
MEDIUM
HIGH

====================================================
RISK ALERT STATUSES
====================================================

CREATED
TRIAGED
UNDER_REVIEW
APPROVED
REJECTED
RESOLVED

====================================================
EXAMPLE AI OUTPUT
====================================================

{
"riskLevel": "HIGH",
"riskScore": 91,
"summary": "Large transfer to first-time beneficiary after multiple failed logins.",
"reasonCodes": [
"NEW_BENEFICIARY",
"LOGIN_ANOMALY",
"HIGH_AMOUNT"
],
"recommendedAction": "MANUAL_REVIEW"
}

====================================================
SPRING AI ROLE
====================================================

Spring AI should:
- orchestrate prompts
- call LLMs
- generate explanations
- summarize alerts
- classify severity

It should NOT:
- directly make financial decisions
- autonomously move money

====================================================
IMPORTANT DESIGN PRINCIPLES
====================================================

1. Strict module boundaries
2. No cross-module repository access
3. Transaction module does NOT modify balances directly
4. AI assists humans, does not replace them
5. Risk analysis must be explainable
6. Structured retrieval before AI reasoning
7. Full auditability

====================================================
FUTURE IMPROVEMENTS
====================================================

- Kafka event-driven architecture
- Real ML risk scoring
- Vector DB for historical fraud similarity search
- Analyst dashboard
- Case escalation workflows
- Semantic retrieval of previous fraud cases
- Real-time streaming analysis
- Explainable AI dashboards
- Multi-model risk ensemble