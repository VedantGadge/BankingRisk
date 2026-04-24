# Banking System Architecture (Modular Monolith)

## Overview

This project implements a **core banking transaction system** using a **modular monolith architecture** that mimics microservices.

Each module is logically isolated and designed to be extracted into independent microservices in the future.

---

## Core Principles

- Single Spring Boot application
- Strict module boundaries
- No shared database access across modules
- Event-driven architecture using Kafka
- Stateless authentication using JWT
- Idempotent transaction processing
- Clean layered architecture

---

## Module Structure

```
com.vedant.banking
├── gateway/
├── user/
├── transaction/
├── account/
├── fraud/
├── audit/
├── notification/
├── common/
```

---

## Layered Architecture (MANDATORY)

All modules MUST follow:

```
Controller → Service → Repository
```

### Rules:

- Controllers:
    - Handle HTTP requests only
    - No business logic
    - No database access

- Services:
    - Contain all business logic
    - Coordinate between modules
    - Call other services (NOT repositories of other modules)

- Repositories:
    - Only used within their own module
    - No cross-module usage

---

## Module Responsibilities

### Gateway Module

- Validates JWT tokens
- Extracts user identity
- Secures endpoints
- Routes requests internally

---

### User Module (Auth)

- Handles registration and login
- Stores users in database
- Generates JWT tokens

---

### Transaction Module

- Creates transactions (INITIATED state)
- Ensures idempotency using Redis
- Publishes Kafka events
- Does NOT update account balance directly

---

### Account Module

- Manages balances
- Handles debit/credit operations
- Only module allowed to modify balances

---

### Fraud Module

- Consumes transaction events
- Applies fraud detection rules
- Publishes fraud decision events

---

### Audit Module

- Logs all system events
- Stores immutable history

---

### Notification Module

- Sends notifications (mock email/SMS)
- Triggered by events

---

## Critical Rules (DO NOT BREAK)

### 1. No Cross-Module DB Access

❌ WRONG:
```
transaction → accountRepository
```

✅ CORRECT:
```
transaction → accountService
```

---

### 2. Transaction != Balance Update

- Transaction module only creates transaction records
- Account module performs actual debit/credit

---

### 3. Idempotency is Mandatory

- All transaction requests must be idempotent
- Use Redis key:

```
idempotency:{userId}:{requestId}
```

---

### 4. Event-Driven Communication

Use Kafka for:

- transaction.created
- fraud.clean
- fraud.suspected
- transaction.completed

---

### 5. No Business Logic in Controllers

Controllers must:

- Validate input
- Call service layer
- Return response

---

### 6. JWT-Based Security

- All secured endpoints require JWT
- Gateway validates token
- User identity passed internally

---

## Transaction Flow

1. Client sends request with JWT
2. Gateway validates token
3. Transaction module creates transaction (INITIATED)
4. Kafka event published
5. Fraud module evaluates
6. If clean:
    - Account module debits/credits
7. Transaction marked COMPLETED
8. Audit + Notification triggered

---

## Communication Strategy

| Type | Method |
|------|--------|
| Module → Module | Direct method call |
| Async events | Kafka |
| External client | REST |

---

## Coding Guidelines

- Use constructor injection
- Use Lombok for boilerplate
- Use DTOs (no entity exposure)
- Handle exceptions globally
- Use meaningful method names

---

## Naming Conventions

- Service: `XxxService`
- Implementation: `XxxServiceImpl`
- Controller: `XxxController`
- Repository: `XxxRepository`
- DTO: `XxxRequest`, `XxxResponse`

---

## Future Migration to Microservices

This architecture supports easy migration by:

- Extracting modules into separate services
- Replacing method calls with REST/gRPC
- Moving databases per service

---

## Golden Rule

> Follow architecture strictly.  
> If unsure, prioritize modular boundaries over shortcuts.
