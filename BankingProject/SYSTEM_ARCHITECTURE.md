# System Architecture - Banking Project

**Version:** 1.0  
**Last Updated:** May 10, 2026  
**Status:** Production Ready  
**Deployment:** Render + Neon PostgreSQL

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture Principles](#architecture-principles)
3. [System Architecture](#system-architecture)
4. [Module Architecture](#module-architecture)
5. [Data Flow](#data-flow)
6. [Database Design](#database-design)
7. [Security Architecture](#security-architecture)
8. [Deployment Architecture](#deployment-architecture)
9. [API Layer](#api-layer)
10. [Error Handling](#error-handling)
11. [Scalability & Future](#scalability--future)

---

## Overview

### What is This System?

A **modular monolith** banking system that provides core transaction processing capabilities. It's structured as separate logical modules that can be extracted into independent microservices in the future.

### Key Design Goals

✅ **Modularity** - Independent modules with clear boundaries  
✅ **Scalability** - Designed to scale horizontally  
✅ **Security** - JWT-based, role-based access control  
✅ **Reliability** - Transaction safety, data integrity  
✅ **Auditability** - Transaction history immutability  
✅ **Future Ready** - Easy migration to microservices  

---

## Architecture Principles

### 1. Modular Monolith Pattern

The application is organized into **logical modules** that mimic microservices:

```
BankingProject (Single Spring Boot App)
├── User Module (Authentication & User Management)
├── Account Module (Balance Management)
├── Transaction Module (Transaction Processing)
└── Common Module (Shared Infrastructure)
```

**Benefits:**
- Single deployment unit (easier to start)
- Shared database (simpler transactions)
- Clear module boundaries (easy to extract later)
- Faster inter-module communication

### 2. Layered Architecture (Per Module)

Each module follows the classic three-layer pattern:

```
┌─────────────────────────┐
│  Controller Layer (REST)│  ← HTTP endpoints, request validation
├─────────────────────────┤
│  Service Layer          │  ← Business logic, orchestration
├─────────────────────────┤
│  Repository Layer       │  ← Data access, queries
├─────────────────────────┤
│  Entity/Domain Layer    │  ← Business objects
└─────────────────────────┘
```

**Rules:**
- Controllers only handle HTTP
- Services contain business logic
- Repositories = database access
- NO direct repository access across modules

### 3. Separation of Concerns

- **User Module** - Only knows about users and authentication
- **Account Module** - Only knows about accounts and balances
- **Transaction Module** - Only knows about transactions
- NO module talks directly to another module's database

### 4. API-First Integration

Modules communicate through **service interfaces**, not direct database access:

```
WRONG: TransactionService → AccountRepository
RIGHT: TransactionService → AccountService
```

---

## System Architecture

### High-Level System Diagram

```
                        ┌─────────────────┐
                        │   Client Apps   │
                        │  (Web, Mobile)  │
                        └────────┬────────┘
                                 │
                         HTTP/HTTPS + JWT
                                 │
                ┌────────────────▼──────────────────┐
                │  Spring Boot Application          │
                │  (Render Dyno - Production)       │
                │                                    │
                ├────────────────────────────────────┤
                │                                    │
                │  ┌──────────────────────────────┐ │
                │  │   API Servlet Gateway        │ │
                │  │   ├─ JWT Validation         │ │
                │  │   ├─ CORS Configuration      │ │
                │  │   └─ Exception Handling      │ │
                │  └──────────────────────────────┘ │
                │                                    │
                │  ┌──────────────────────────────┐ │
                │  │   REST Controllers           │ │
                │  │   ├─ /api/auth/*            │ │
                │  │   ├─ /api/users/*           │ │
                │  │   ├─ /api/accounts/*        │ │
                │  │   └─ /api/transactions/*    │ │
                │  └──────────────────────────────┘ │
                │                                    │
                │  ┌──────────────────────────────┐ │
                │  │   Service Layer              │ │
                │  │   ├─ AuthService            │ │
                │  │   ├─ AccountService         │ │
                │  │   └─ TransactionService     │ │
                │  └──────────────────────────────┘ │
                │                                    │
                │  ┌──────────────────────────────┐ │
                │  │   Repository Layer           │ │
                │  │   ├─ UserRepository         │ │
                │  │   ├─ AccountRepository      │ │
                │  │   └─ TransactionRepository  │ │
                │  └──────────────────────────────┘ │
                │                                    │
                └────────────────┬───────────────────┘
                                 │
                        Hibernate/JPA
                                 │
                ┌────────────────▼──────────────────┐
                │  PostgreSQL 15 (Neon Cloud)      │
                │                                    │
                │  ├─ users (Authentication)        │
                │  ├─ accounts (Balances)           │
                │  ├─ transaction (History)         │
                │  └─ Indexes & Constraints         │
                └────────────────────────────────────┘

Legend:
  ┌─┐ = Synchronous call
  ├─┤ = Depends on
  ▼   = Data flow
```

### Component Interaction

```
Client Request
  │
  ▼
┌──────────────────────────────┐
│ Spring Servlet (Port 8080)   │
└──────────────────────────────┘
  │
  ▼
┌──────────────────────────────┐
│ Security Filter Chain        │
│ ├─ JWT Token Extraction      │
│ ├─ Role Validation           │
│ └─ User Context Setup        │
└──────────────────────────────┘
  │
  ▼
┌──────────────────────────────┐
│ Dispatcher Servlet           │
└──────────────────────────────┘
  │
  ▼
┌──────────────────────────────┐
│ Controller ({Module}Ctrls)   │
│ ├─ Input Validation          │
│ ├─ Request Parsing           │
│ └─ Call Service              │
└──────────────────────────────┘
  │
  ▼
┌──────────────────────────────┐
│ Service Layer                │
│ ├─ Business Logic            │
│ ├─ Cross-module Calls        │
│ ├─ Exception Handling        │
│ └─ Repository Calls          │
└──────────────────────────────┘
  │
  ▼
┌──────────────────────────────┐
│ Repository Layer             │
│ ├─ Query Construction        │
│ ├─ JPA/Hibernate             │
│ └─ SQL Execution             │
└──────────────────────────────┘
  │
  ▼
┌──────────────────────────────┐
│ Database (PostgreSQL)        │
│ ├─ Query Execution           │
│ ├─ Transaction Isolation      │
│ └─ Data Persistence          │
└──────────────────────────────┘
```

---

## Module Architecture

### 1. User Module (Authentication)

**Purpose:** Manage users and authentication

**Components:**

```
User Module
├── UserController
│   ├── POST /api/auth/register
│   └── POST /api/auth/login
│
├── AuthService / AuthServiceImpl
│   ├── register(email, password)
│   ├── login(email, password) → JWT Token
│   ├── validateToken(token)
│   └── getUserFromToken(token)
│
├── UserRepository (JPA)
│   ├── findByEmail(email)
│   └── save(user)
│
└── User Entity
    ├── id (PK)
    ├── email (UNIQUE)
    ├── passwordHash
    ├── role (ENUM: USER, ADMIN)
    └── createdAt
```

**Key Responsibilities:**
- User registration
- Password hashing & validation (BCrypt)
- JWT token generation & validation
- Role-based access control setup

**Security:**
- Passwords never transmitted in plain text
- JWT expires after X minutes
- Token validation on every protection request

---

### 2. Account Module (Balance Management)

**Purpose:** Manage account balances and account state

**Components:**

```
Account Module
├── AccountController
│   ├── GET /api/accounts/{id}
│   ├── POST /api/accounts
│   └── GET /api/accounts/user/{userId}
│
├── AccountService / AccountServiceImpl
│   ├── createAccount(userId)
│   ├── getAccount(accountId)
│   ├── getAccountByUserId(userId)
│   ├── debit(accountId, amount)
│   ├── credit(accountId, amount)
│   └── getBalance(accountId)
│
├── AccountRepository (JPA)
│   ├── findById(id)
│   ├── findByUserId(userId) - UNIQUE
│   ├── save(account)
│   └── Custom queries
│
└── Account Entity
    ├── id (PK)
    ├── userId (FK to User, UNIQUE)
    ├── balance (DECIMAL)
    ├── version (Optimistic Lock)
    └── createdAt
```

**Key Responsibilities:**
- Create account for new user
- Manage balances (debit/credit)
- Balance validation (no negatives)
- Optimistic locking for concurrency

**Constraints:**
- One account per user (UNIQUE constraint)
- Balance >= 0 (CHECK constraint)
- Balance is DECIMAL(19,2) for precision

**Concurrency Strategy:**
```
Version-based Optimistic Locking:

Account v1: balance = 1000
├─ Thread A reads: balance = 1000, version = 1
├─ Thread B reads: balance = 1000, version = 1
├─ Thread A updates: balance = 900, version = 2
├─ Thread B tries: balance = 950, version = 2 (expected 1)
└─ Thread B fails: OptimisticLockException
```

---

### 3. Transaction Module (Transaction Processing)

**Purpose:** Create and track financial transactions

**Components:**

```
Transaction Module
├── TransactionController
│   ├── POST /api/transactions/deposit
│   ├── POST /api/transactions/withdraw
│   ├── POST /api/transactions/transfer
│   ├── GET /api/transactions/{id}
│   └── GET /api/transactions/account/{accountId}
│
├── TransactionService / TransactionServiceImpl
│   ├── deposit(accountId, amount)
│   ├── withdraw(accountId, amount)
│   ├── transfer(fromId, toId, amount)
│   ├── getTransaction(id)
│   └── getAccountHistory(accountId)
│
├── TransactionRepository (JPA)
│   ├── findById(id)
│   ├── findByFromUserId(id)
│   ├── findByToUserId(id)
│   ├── save(transaction)
│   └── Custom queries with pagination
│
└── Transaction Entity
    ├── id (PK)
    ├── type (ENUM: DEPOSIT, WITHDRAW, TRANSFER)
    ├── status (ENUM: INITIATED, COMPLETED, FAILED)
    ├── amount (DECIMAL)
    ├── fromUserId (FK, nullable)
    ├── toUserId (FK, nullable)
    └── createdAt (IMMUTABLE)
```

**Key Responsibilities:**
- Create transactions (deposits, withdrawals, transfers)
- Maintain transaction state lifecycle
- Ensure atomicity (all-or-nothing)
- Track transaction history

**Transaction States:**

```
INITIATED ──success──> COMPLETED
│                      (Balance updated)
│
└──failure──> FAILED
             (No balance change)

State Flow Rules:
- Only INITIATED → COMPLETED or INITIATED → FAILED
- Never reverse: COMPLETED → FAILED (immutable once done)
```

**Critical Rules:**

1. **Atomicity Rule**
   - Debit and credit must happen together
   - If one fails, both rollback

2. **Balance Validation Rule**
   - Withdraw/Transfer checks sufficient funds INSIDE transaction
   - Prevents race conditions

3. **Immutability Rule**
   - Once COMPLETED or FAILED, cannot change
   - History is audit trail

---

### 4. Common Module (Shared Infrastructure)

**Purpose:** Cross-cutting concerns

**Components:**

```
Common Module
├── config/
│   ├── SecurityConfig (JWT, CORS, Auth)
│   ├── JpaConfig (DataSource, Transactions)
│   └── CorsConfig
│
├── security/
│   ├── JwtProvider (Generate, Validate tokens)
│   ├── JwtAuthenticationFilter
│   └── CustomUserDetails
│
├── exception/
│   ├── GlobalExceptionHandler
│   ├── CustomExceptions
│   │   ├─ ResourceNotFoundException
│   │   ├─ InsufficientFundsException
│   │   ├─ DuplicateEmailException
│   │   └─ TransactionFailedException
│   └─ ErrorResponse DTO
│
├── dto/
│   ├── LoginRequest
│   ├── RegisterRequest
│   ├── ErrorResponse
│   └─ API Response wrappers
│
└── util/
    ├── ValidationUtils
    └─ Constants
```

---

## Data Flow

### Scenario 1: User Registration

```
POST /api/auth/register
Body: { email: "user@bank.com", password: "secure123" }

┌─────────────────────────────────────────────────┐
│ 1. Controller receives request                   │
│    - Validate email format                      │
│    - Validate password strength                 │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│ 2. AuthService.register()                       │
│    - Check if email exists (query DB)           │
│    - Hash password with BCrypt                  │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│ 3. UserRepository.save(newUser)                 │
│    - INSERT into users table                    │
│    - DB constraint: UNIQUE(email)               │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│ 4. Auto-Create Account (within same txn)        │
│    - AccountService.createAccount(userId)       │
│    - INSERT into accounts with balance = 0      │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│ 5. Return success                               │
│    { status: "created", userId: 1 }             │
└─────────────────────────────────────────────────┘
```

### Scenario 2: Money Transfer

```
POST /api/transactions/transfer
Body: {
  fromAccountId: 1,
  toAccountId: 2,
  amount: 250.00
}

┌──────────────────────────────────────────────┐
│ 1. Controller validates input                 │
│    - Amount > 0                              │
│    - Both accounts exist                     │
└─────────────┬────────────────────────────────┘
              │
┌─────────────▼────────────────────────────────┐
│ 2. TransactionService.transfer()              │
│    START TRANSACTION                          │
│    Isolation Level: READ_COMMITTED            │
└─────────────┬────────────────────────────────┘
              │
┌─────────────▼────────────────────────────────┐
│ 3. Create Transaction record (INITIATED)     │
│    INSERT into transaction                    │
│    status = INITIATED                         │
└─────────────┬────────────────────────────────┘
              │
┌─────────────▼────────────────────────────────┐
│ 4. Debit from account                         │
│    AccountService.debit(fromId, 250)          │
│    ├─ Read account (acquires row lock)       │
│    ├─ Check: balance >= 250 ✓                │
│    ├─ Update: balance -= 250                 │
│    └─ version++ (optimistic lock)             │
└─────────────┬────────────────────────────────┘
              │
┌─────────────▼────────────────────────────────┐
│ 5. Credit to account                          │
│    AccountService.credit(toId, 250)           │
│    ├─ Read account                           │
│    ├─ Update: balance += 250                 │
│    └─ version++                               │
└─────────────┬────────────────────────────────┘
              │
┌─────────────▼────────────────────────────────┐
│ 6. Set transaction to COMPLETED               │
│    UPDATE transaction status                  │
│    COMMIT TRANSACTION                         │
└─────────────┬────────────────────────────────┘
              │
┌─────────────▼────────────────────────────────┐
│ 7. Return success                             │
│    { id: 123, status: "COMPLETED", ...}       │
└──────────────────────────────────────────────┘

ERROR SCENARIO:
If at any point (3-6) an exception occurs:
├─ OptimisticLockException (concurrent edit)
├─ InsufficientFundsException (not enough balance)
├─ SQLException (DB constraint violation)
│
└─> ROLLBACK (entire transaction)
    ├─ No balance changes
    ├─ Transaction stays INITIATED (or gets FAILED)
    └─ Return 400/500 error
```

### Scenario 3: Authentication Flow

```
POST /api/auth/login
Body: { email: "user@bank.com", password: "secure123" }

┌────────────────────────────────────────────┐
│ 1. AuthService.login()                      │
│    - Find user by email                     │
│    - Compare password with hash             │
└─────────────┬──────────────────────────────┘
              │
        ┌─────▼─────┐
        │ Match?    │
   NO  │           │ YES
──────┘            └──────
      │                    │
      ▼                    ▼
  Return Error      JwtProvider.generateToken()
  401 Unauthorized  ├─ Create claims (userId, role)
                    ├─ Sign with SECRET_KEY
                    ├─ Set expiration (1 hour)
                    └─ Return JWT string
                         │
                         ▼
                    Return to Client
                    { token: "eyJhbGc..." }
                         │
                         ▼ (Client stores in localStorage/session)
                         │
                    Subsequent Requests
                    Header: Authorization: Bearer eyJhbGc...
                         │
                         ▼
                    JwtAuthenticationFilter
                    ├─ Extract token from header
                    ├─ Validate signature
                    ├─ Check expiration
                    ├─ Extract userId
                    └─ Set SecurityContext
                         │
                         ▼
                    Request proceeds
                    (Protected endpoint)
```

---

## Database Design

### Schema Diagram

```
┌──────────────────────┐
│      users           │
├──────────────────────┤
│ id (PK)              │
│ email (UNIQUE)       │———┐
│ password_hash        │   │
│ role (ENUM)          │   │
│ created_at           │   │
└──────────────────────┘   │
                           │
                      ┌────┼────┐
                      │         │
                      │ 1:1      │
                      │         │
                ┌─────▼────────┬┴──────────────┐
                │  accounts    │               │
                ├──────────────┤               │
                │ id (PK)      │               │
                │ user_id (FK) │────┬──────────┘
                │  (UNIQUE)    │    │
                │ balance      │    │
                │ version      │    │
                │ created_at   │    │
                └──────────────┘    │
                                    │
        ┌───────────────────────────┘
        │
        │
┌───────▼─────────────────────────┐
│      transaction                 │
├──────────────────────────────────┤
│ id (PK)                          │
│ type (DEPOSIT, WITHDRAW, XFER)  │
│ status (INITIATED, COMPLETED)   │
│ amount (DECIMAL)                 │
│ from_user_id (FK, nullable)      │
│ to_user_id (FK, nullable)        │
│ created_at (IMMUTABLE)           │
│                                   │
│ Constraints:                      │
│ ├─ type IN (...)                 │
│ ├─ status IN (...)               │
│ └─ amount > 0                    │
└──────────────────────────────────┘

Relationships:
- users 1───────N accounts
- users 1───────N transaction (from)
- users 1───────N transaction (to)

Indexes:
- users(email) - UNIQUE
- accounts(user_id) - UNIQUE
- transaction(from_user_id)
- transaction(to_user_id)
- transaction(created_at)
```

### DDL Scripts

```sql
-- Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Accounts Table
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Transaction Table
CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(255) NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAW', 'TRANSFER')),
    status VARCHAR(255) NOT NULL CHECK (status IN ('INITIATED', 'COMPLETED', 'FAILED')),
    amount NUMERIC(38,2) NOT NULL CHECK (amount > 0),
    from_user_id BIGINT REFERENCES users(id),
    to_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for Performance
CREATE INDEX idx_transaction_from_user ON transaction(from_user_id);
CREATE INDEX idx_transaction_to_user ON transaction(to_user_id);
CREATE INDEX idx_transaction_created ON transaction(created_at);
CREATE INDEX idx_transaction_status ON transaction(status);
```

### Query Patterns

**High-Frequency Queries:**

```sql
-- Get user account
SELECT * FROM accounts WHERE user_id = ? FOR UPDATE;

-- Get transaction history
SELECT * FROM transaction 
WHERE from_user_id = ? OR to_user_id = ? 
ORDER BY created_at DESC 
LIMIT 50;

-- Check balance
SELECT balance FROM accounts WHERE id = ?;

-- Verify unique email
SELECT 1 FROM users WHERE email = ?;
```

---

## Security Architecture

### Authentication Flow

```
┌──────────────┐
│ Client Login │
└──────┬───────┘
       │
       ▼
  ┌─────────────────┐
  │ Send Credentials│ (HTTPS only)
  │ email + password│
  └────────┬────────┘
           │
           ▼
    ┌──────────────────┐
    │ Spring Security  │
    │ (Filter Chain)   │
    └────────┬─────────┘
             │
             ▼
    ┌──────────────────┐
    │ AuthService      │
    │ ├─ Validate user │
    │ ├─ Check password│
    │ └─ Generate JWT  │
    └────────┬─────────┘
             │
             ▼
    ┌──────────────────┐
    │ Return JWT Token │
    │ (Valid 1 hour)   │
    └────────┬─────────┘
             │
             ▼
  ┌──────────────────────┐
  │ Client stores JWT    │
  │ (localStorage/cookie)│
  └──────────┬───────────┘
             │
    ┌────────▼────────┐
    │ Subsequent Req  │
    │ Header: Bearer  │
    │ <jwt_token>     │
    └────────┬────────┘
             │
             ▼
    ┌──────────────────────┐
    │ JwtAuthFilter        │
    │ ├─ Extract token     │
    │ ├─ Validate signature│
    │ ├─ Check expiry      │
    │ └─ Extract userId    │
    └────────┬─────────────┘
             │
   ┌─────────▼──────────┐
   │ Valid? ────No────> 401 Unauthorized
   │                    
   │ Yes
   │ │
   └─▼─────────────────┐
     │ Set Security    │
     │ Context         │
     └────────┬────────┘
              │
              ▼
        Request allowed
        (Protected endpoint)
```

### JWT Token Structure

```
Header (Algorithm & Type):
{
  "alg": "HS256",
  "typ": "JWT"
}
.
Payload (Claims):
{
  "sub": "1",           # userId
  "role": "USER",       # User role
  "email": "user@...",  # Email
  "iat": 1683700000,    # Issued at
  "exp": 1683703600     # Expires in (1 hour)
}
.
Signature:
HMACSHA256(
  header + "." + payload,
  SECRET_KEY
)

Full token: eyJhbGc...eyJzdWI...signature123
```

### Authorization Roles

```
┌──────────────┐
│ Role: USER   │ → Regular user, manage own account
└──────────────┘

┌──────────────┐
│ Role: ADMIN  │ → System admin, access all
└──────────────┘

Endpoint Protection:
┌─────────────────────────────────┐
│ @PreAuthorize("hasAnyRole")     │
├─────────────────────────────────┤
│ POST /api/accounts              │ → Requires token
│ GET /api/accounts/{id}          │ → Requires token
│ POST /api/transactions/*        │ → Requires token
└─────────────────────────────────┘
```

---

## Deployment Architecture

### Local Development (Docker)

```
┌────────────────────────────────────────────┐
│ Developer Machine (Docker Desktop)         │
├────────────────────────────────────────────┤
│                                             │
│  Docker Network: banking_network            │
│  ├─ postgres:5432 (inside container)       │
│  │  └─ Port mapped to 5433 (outside)       │
│  │                                          │
│  └─ banking-app:8080 (inside container)    │
│     └─ Port mapped to 8080 (outside)       │
│                                             │
│  Volume: postgres_data (persistent)        │
│                                             │
└────────────────────────────────────────────┘
         ▲
         │ docker-compose up
         │
    ~/.docker/config.json
```

**Commands:**
```bash
docker compose up -d          # Start services
docker compose ps             # Check status
docker compose logs -f        # Real-time logs
docker compose down           # Stop services
docker compose down -v        # Stop + remove volumes
```

### Production Deployment (Render + Neon)

```
┌──────────────────────────────────────────────────────────┐
│                    Internet/Client                        │
└────────────────────┬─────────────────────────────────────┘
                     │ HTTPS
                     ▼
        ┌────────────────────────────┐
        │  Render (https://...)      │
        │                            │
        │  ├─ Web Service Dyno       │
        │  │  └─ banking-app:8080    │
        │  │     (Auto-scaled)       │
        │  │                         │
        │  ├─ Auto Deploy            │
        │  │  (Push to git)          │
        │  │                         │
        │  └─ SSL/TLS               │
        │     (Auto-renew)          │
        └────────┬───────────────────┘
                 │ PostgreSQL Protocol
                 ▼
    ┌────────────────────────────────┐
    │ Neon Cloud (PostgreSQL)         │
    │                                 │
    │ ├─ banking_db                  │
    │ │  ├─ users (table)            │
    │ │  ├─ accounts (table)         │
    │ │  └─ transaction (table)      │
    │ │                              │
    │ ├─ Automatic backups (daily)   │
    │ ├─ Connection pooling          │
    │ └─ SSL/TLS encryption          │
    │                                 │
    └────────────────────────────────┘

Environment Variables (set in Render):
    DB_HOST=<neon-endpoint>
    DB_PORT=5432
    DB_NAME=banking_db
    DB_USERNAME=<user>
    DB_PASSWORD=<strong-password>
    DDL_AUTO=validate
    SHOW_SQL=false
```

**Deployment Flow:**

```
Developer pushes to GitHub
           │
           ▼
    Render webhook triggered
           │
           ▼
    Clone repository
           │
           ▼
    Build: ./mvnw clean package
           │
           ▼
    Docker build & image creation
           │
           ▼
    Deploy to Web Service
           │
           ▼
    Health check (auto-restart if fails)
           │
           ▼
    Live at https://banking-java.onrender.com
```

### Scaling Strategy

**Current:** 1 dyno (512 MB RAM, shared CPU)

**Scaling Options:**

```
Vertical Scaling:
  Standard 1 dyno → Standard-2x dyno (faster CPU/RAM)
  vs.
Horizontal Scaling:
  Multiple dynos behind load balancer
  (Render handles this automatically at higher tier)
```

---

## API Layer

### REST Conventions

```
GET    /api/resource        → List all resources
GET    /api/resource/{id}   → Get specific resource
POST   /api/resource        → Create new resource
PUT    /api/resource/{id}   → Update entire resource
PATCH  /api/resource/{id}   → Partial update
DELETE /api/resource/{id}   → Delete resource

Status Codes:
  200 → OK (GET, PUT, PATCH)
  201 → Created (POST)
  204 → No Content (DELETE)
  400 → Bad Request (validation error)
  401 → Unauthorized (missing/invalid token)
  403 → Forbidden (insufficient permissions)
  404 → Not Found (resource doesn't exist)
  409 → Conflict (duplicate, race condition)
  500 → Server Error
```

### Response Format

**Success:**
```json
{
  "status": "success",
  "message": "Resource created successfully",
  "data": {
    "id": 1,
    "email": "user@example.com"
  }
}
```

**Error:**
```json
{
  "status": "error",
  "message": "Validation failed",
  "timestamp": "2026-05-10T10:30:00Z",
  "errors": [
    {
      "field": "email",
      "message": "Email already exists"
    }
  ]
}
```

### API Endpoints

**Auth Endpoints:**
```
POST /api/auth/register
  Request: { email, password }
  Response: { id, email, role }

POST /api/auth/login
  Request: { email, password }
  Response: { token, expiresIn }
```

**Account Endpoints:**
```
GET /api/accounts/{accountId}
  Response: { id, userId, balance, createdAt }

POST /api/accounts
  Response: { id, userId, balance }

GET /api/accounts/user/{userId}
  Response: [{ id, balance }, ...]
```

**Transaction Endpoints:**
```
POST /api/transactions/deposit
  Request: { accountId, amount }
  Response: { id, type, status, amount }

POST /api/transactions/withdraw
  Request: { accountId, amount }
  Response: { id, type, status, amount }

POST /api/transactions/transfer
  Request: { fromAccountId, toAccountId, amount }
  Response: { id, type, status, fromId, toId, amount }

GET /api/transactions/{id}
  Response: { id, type, status, amount, fromId, toId, createdAt }

GET /api/transactions/account/{accountId}
  Response: [{ id, type, status, amount }, ...]
```

---

## Error Handling

### Exception Hierarchy

```
Exception
├── CustomBankingException (Abstract)
│   ├── ResourceNotFoundException
│   │   └─ "Account with id 999 not found"
│   ├── InsufficientFundsException
│   │   └─ "Insufficient balance. Required: 500, Available: 300"
│   ├── DuplicateResourceException
│   │   └─ "Email user@example.com already registered"
│   ├── InvalidTransactionException
│   │   └─ "Cannot transfer to same account"
│   ├── AuthenticationException
│   │   └─ "Invalid credentials"
│   └── AuthorizationException
│       └─ "Insufficient permissions"
│
└── System Exceptions
    ├── DatabaseException
    ├── ValidationException
    └── TimeoutException
```

### Global Exception Handler

```java
@RestControllerAdvice
class GlobalExceptionHandler {
  
  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ErrorResponse> handle(ResourceNotFoundException e) {
    return ResponseEntity.status(404)
      .body(ErrorResponse.of(e));  // 404
  }
  
  @ExceptionHandler(InsufficientFundsException.class)
  ResponseEntity<ErrorResponse> handle(InsufficientFundsException e) {
    return ResponseEntity.status(400)
      .body(ErrorResponse.of(e));  // 400
  }
  
  @ExceptionHandler(OptimisticLockException.class)
  ResponseEntity<ErrorResponse> handle(OptimisticLockException e) {
    return ResponseEntity.status(409)
      .body(ErrorResponse.of("Conflict: Resource was modified"));  // 409
  }
}
```

### Error Response Format

```json
{
  "status": "error",
  "message": "Account not found",
  "errorCode": "ACCOUNT_NOT_FOUND",
  "timestamp": "2026-05-10T10:30:00Z",
  "path": "/api/accounts/999",
  "details": {
    "accountId": 999
  }
}
```

---

## Scalability & Future

### Current Architecture Limits

```
Monolith Limits:
├─ Single deployment unit
├─ Shared database
├─ Hard to scale individual modules
├─ Technology stack locked-in
├─ Fault in one module affects all
└─ Difficult to test with real data

Current Performance:
├─ ~1000 requests/second (estimated)
├─ Response time: < 200ms avg
├─ DB connections: 10 pool size
└─ Memory: 512 MB (Render dyno)
```

### Future Migration Path

**Phase 1: Extract Services**
```
├─ User Service (independent)
├─ Account Service (independent)
└─ Transaction Service (independent)
```

**Phase 2: Add Infrastructure**
```
├─ API Gateway (request routing)
├─ Service Discovery (Eureka/Consul)
├─ Message Bus (Kafka for events)
└─ Circuit Breaker (Hystrix/Resilience4j)
```

**Phase 3: Advanced Features**
```
├─ Distributed Tracing (Jaeger)
├─ Centralized Logging (ELK Stack)
├─ Metrics & Monitoring (Prometheus)
├─ Rate Limiting & Throttling
├─ API Versioning (v1, v2, ...)
└─ Caching Layer (Redis)
```

---

## Performance Optimization

### Current Optimizations

```
✅ Database Indexes
   └─ users(email), accounts(user_id), transaction(status)

✅ Connection Pooling
   └─ HikariCP with 10 connections

✅ Query Optimization
   └─ Prepared statements, no N+1 queries

✅ Transaction Isolation
   └─ READ_COMMITTED level

✅ Concurrency Control
   └─ Optimistic locking with @Version
```

### Future Optimizations

```
Redis Caching:
├─ Cache user session info
├─ Cache account balance
└─ Idempotency key storage

Async Processing:
├─ Kafka events for notifications
├─ Worker services for batch jobs
└─ Message-driven architecture

Read Replicas:
├─ Separate read-only PostgreSQL
├─ Route SELECT queries to replica
└─ Route writes to primary

Sharding Strategy:
├─ Shard by userId
├─ Distribute across DB servers
└─ Each shard handles subset of users
```

---

## Monitoring & Observability

### Metrics to Track

```
Application-Level:
├─ Request count (GET, POST, etc.)
├─ Response time (p50, p95, p99)
├─ Error rate (4xx, 5xx)
├─ Active user sessions

Database-Level:
├─ Connection pool utilization
├─ Query execution time
├─ Transaction rate
├─ Lock wait times

Business-Level:
├─ Total transactions/day
├─ Total volume processed
├─ Failed transaction rate
└─ Average transaction value
```

### Health Checks

```
Spring Actuator Endpoint:
GET /actuator/health

Response:
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

---

## Summary

This **modular monolith** architecture provides:

✅ **Clear Separation** - Modules with defined boundaries  
✅ **Security** - JWT auth, role-based access  
✅ **Reliability** - ACID transactions, concurrency control  
✅ **Scalability** - Path to microservices  
✅ **Maintainability** - Clean layered design  
✅ **Deployability** - Docker + Cloud-ready  

**Key Technologies:**
- Java 17 + Spring Boot 3
- PostgreSQL 15
- Docker & Render
- JWT & Spring Security

**Deployment Status:** ✅ **Production Ready**

---

