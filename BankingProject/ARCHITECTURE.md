# Banking System Architecture (Modular Monolith)

## Overview

This project implements a core banking transaction system using a modular monolith architecture that mimics microservices.

Each module is logically isolated and designed to be extracted into independent microservices in the future.

## Core Principles

- Single Spring Boot application
- Strict module boundaries
- No shared database access across modules
- Event-driven architecture using Kafka
- Stateless authentication using JWT
- Idempotent transaction processing
- Clean layered architecture

## Module Structure

    com.vedant.banking
    ├── gateway/
    ├── user/
    ├── transaction/
    ├── account/
    ├── fraud/
    ├── audit/
    ├── notification/
    └── common/

## Layered Architecture

All modules must follow:

    Controller → Service → Repository

### Rules

- Controllers
  - Handle HTTP requests only
  - No business logic
  - No database access

- Services
  - Contain all business logic
  - Coordinate between modules
  - Call other services, not repositories of other modules

- Repositories
  - Only used within their own module
  - No cross-module usage

## Module Responsibilities

### Gateway Module

- Validates JWT tokens
- Extracts user identity
- Secures endpoints
- Routes requests internally

### User Module (Auth)

- Handles registration and login
- Stores users in database
- Generates JWT tokens

### Transaction Module

- Creates transactions in INITIATED state
- Ensures idempotency using Redis
- Publishes Kafka events
- Does not update account balance directly

### Account Module

- Manages balances
- Handles debit and credit operations
- Only module allowed to modify balances

### Fraud Module

- Consumes transaction events
- Applies fraud detection rules
- Publishes fraud decision events

### Audit Module

- Logs all system events
- Stores immutable history

### Notification Module

- Sends notifications
- Triggered by events
- Email/SMS mock for now

## Transfer System

### Overview

Transfers move funds between accounts and must ensure:

- Atomicity
- Consistency
- Idempotency
- Fraud validation
- Event-driven execution

### Transfer Request

TransferRequest contains:

- fromAccountId
- toAccountId
- amount
- requestId for idempotency

### Transfer Flow

1. Client sends transfer request with JWT
2. Gateway validates token
3. Transaction module creates transaction in INITIATED state
4. Transaction module stores idempotency key
5. Transaction module publishes event: transaction.created
6. Fraud module evaluates transaction
7. Fraud module emits fraud.clean or fraud.suspected
8. If fraud.clean:
  - Account module debits fromAccount
  - Account module credits toAccount
  - Operation must be atomic
9. Transaction module updates status to COMPLETED
10. Audit module logs event
11. Notification module sends confirmation

### Transfer States

- INITIATED
- PENDING
- COMPLETED
- FAILED
- REJECTED

### Critical Transfer Rules

#### Atomicity

Debit and credit must happen together in one transaction.

#### Balance Validation

- No insufficient funds transfers
- No negative balance

#### Idempotency

Duplicate transfers must be prevented.

Redis key pattern:

    idempotency:{userId}:{requestId}

#### Module Isolation

- Wrong: transaction → accountRepository
- Correct: transaction → accountService

#### Concurrency Safety

Use one of:

- Database locking
- Optimistic locking with @Version

## Updated Responsibilities

### Account Module

- Debit account
- Credit account
- Validate balance
- Ensure transactional integrity
- Handle concurrency

### Transaction Module

- Create transaction
- Maintain state lifecycle
- Ensure idempotency
- Publish Kafka events

## Kafka Events

- transaction.created
- fraud.clean
- fraud.suspected
- transaction.completed
- transaction.failed

## Critical Rules

### 1. No Cross-Module DB Access

Wrong:

    transaction → accountRepository

Correct:

    transaction → accountService

### 2. Transaction Does Not Mean Balance Update

- Transaction module only creates transaction records
- Account module performs actual debit and credit

### 3. Idempotency is Mandatory

- All transaction requests must be idempotent

### 4. Event-Driven Communication

- Use Kafka for async flows

### 5. No Business Logic in Controllers

Controllers must:

- Validate input
- Call service layer
- Return response

### 6. JWT-Based Security

- All secured endpoints require JWT
- Gateway validates token
- User identity passed internally

## Communication Strategy

| Type | Method |
|------|--------|
| Module → Module | Direct method call |
| Async events | Kafka |
| External client | REST |

## Coding Guidelines

- Use constructor injection
- Use Lombok for boilerplate
- Use DTOs, not entities, in API responses
- Handle exceptions globally
- Use meaningful method names

## Naming Conventions

- Service: XxxService
- Implementation: XxxServiceImpl
- Controller: XxxController
- Repository: XxxRepository
- DTO: XxxRequest, XxxResponse

## Future Migration to Microservices

This architecture supports easy migration by:

- Extracting modules into separate services
- Replacing method calls with REST or gRPC
- Moving databases per service

## Golden Rule

Follow architecture strictly.

If unsure, prioritize modular boundaries over shortcuts.