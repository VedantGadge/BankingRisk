# Banking Project - Core Banking Transaction System

A modern, scalable core banking transaction system built with Spring Boot 3, PostgreSQL, and Docker. Deployed on Render with production-grade database management on Neon.

**Live API:** https://banking-java.onrender.com/swagger-ui/index.html#/

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [Database](#database)
- [Docker Deployment](#docker-deployment)
- [Configuration](#configuration)
- [Development](#development)
- [Testing](#testing)
- [Production Deployment](#production-deployment)
- [Architecture](#architecture)
- [Contributing](#contributing)

---

## 🎯 Overview

This project implements a **modular monolith** architecture that mimics microservices while remaining as a single Spring Boot application. It's designed to handle core banking operations with a focus on:

- **Atomicity** - Transactions are all-or-nothing
- **Consistency** - Account balances always remain valid
- **Idempotency** - Duplicate requests are safely handled
- **Event-Driven** - Asynchronous processing via Kafka
- **Security** - JWT-based authentication

The architecture supports easy migration to full microservices in the future.

---

## ✨ Features

### User Management
- User registration and login
- JWT-based authentication
- Role-based access control (User, Admin)
- Email uniqueness validation

### Account Management
- Create accounts for users
- View account balance
- Support for multiple accounts per user
- Unique account constraint per user

### Transaction Processing
- **Deposit** - Add funds to account
- **Withdraw** - Remove funds from account
- **Transfer** - Move funds between accounts
- Transaction history and status tracking

### Transaction States
- `INITIATED` - New transaction created
- `COMPLETED` - Transaction successful
- `FAILED` - Transaction unsuccessful
- `REJECTED` - Transaction rejected by fraud check

### Data Integrity
- Balance validation (no negative balances)
- Insufficient funds detection
- Optimistic locking for concurrency
- Immutable transaction records

---

## 🛠 Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Framework** | Spring Boot | 3.5.13 |
| **Java** | OpenJDK | 17 LTS |
| **Database** | PostgreSQL | 15 |
| **ORM** | Hibernate JPA | 6.6.45 |
| **Security** | Spring Security + JWT | 0.11.5 |
| **API Docs** | SpringDoc OpenAPI | 2.7.0 |
| **Build** | Maven | 3.9.6 |
| **Container** | Docker | Latest |
| **Deployment** | Render | Cloud |
| **DB Hosting** | Neon | PostgreSQL |

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose (optional, for local development)
- PostgreSQL 15+ (or use Docker)

### Local Development (with Docker)

```bash
# Clone the repository
git clone <repo-url>
cd BankingProject

# Start all services (PostgreSQL + App)
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs -f banking-app
```

**Access the application:**
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html

### Local Development (without Docker)

```bash
# Ensure PostgreSQL is running on localhost:5432

# Set environment variables
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=banking_db
export DB_USERNAME=postgres
export DB_PASSWORD=password

# Build and run
./mvnw clean spring-boot:run
```

---

## 📁 Project Structure

```
BankingProject/
├── src/
│   ├── main/
│   │   ├── java/com/example/bankingproject/
│   │   │   ├── account/          # Account management module
│   │   │   │   ├── controller/   # HTTP endpoints
│   │   │   │   ├── service/      # Business logic
│   │   │   │   ├── repository/   # Data access
│   │   │   │   ├── entity/       # JPA entities
│   │   │   │   └── dto/          # Data transfer objects
│   │   │   ├── user/             # Auth & user module
│   │   │   ├── transaction/      # Transaction processing
│   │   │   ├── common/           # Shared configs & security
│   │   │   └── BankingProjectApplication.java
│   │   └── resources/
│   │       ├── application.properties           # Production config
│   │       └── application-test.properties      # Test config
│   └── test/
│       └── java/com/example/bankingproject/
│           ├── account/
│           ├── user/
│           └── transaction/
├── Dockerfile                      # Multi-stage build
├── docker-compose.yml             # Local development stack
├── .dockerignore                  # Docker build optimization
├── .env                           # Environment variables
├── .env.example                   # Environment template
├── pom.xml                        # Maven dependencies
├── README.md                      # This file
├── ARCHITECTURE.md                # System architecture
└── HOW_TO_RUN_TESTS.md           # Testing guide

```

---

## 📚 API Documentation

### Interactive API Docs
- **Swagger UI:** https://banking-java.onrender.com/swagger-ui/index.html#/
- **OpenAPI JSON:** https://banking-java.onrender.com/v3/api-docs

### Core Endpoints

#### Authentication
```
POST /api/auth/register
  Body: { "email": "user@example.com", "password": "secure_pass" }
  Response: { "id": 1, "email": "user@example.com", "role": "USER" }

POST /api/auth/login
  Body: { "email": "user@example.com", "password": "secure_pass" }
  Response: { "token": "eyJhbGc..." }
```

#### Accounts
```
GET /api/accounts/{accountId}
  Headers: Authorization: Bearer <token>
  Response: { "id": 1, "userId": 1, "balance": 1000.00, "createdAt": "..." }

POST /api/accounts
  Headers: Authorization: Bearer <token>
  Response: { "id": 1, "userId": 1, "balance": 0.00 }

GET /api/accounts/user/{userId}
  Headers: Authorization: Bearer <token>
  Response: [{ "id": 1, "balance": 1000.00 }, ...]
```

#### Transactions
```
POST /api/transactions/deposit
  Headers: Authorization: Bearer <token>
  Body: { "accountId": 1, "amount": 500.00 }
  Response: { "id": 1, "type": "DEPOSIT", "status": "COMPLETED" }

POST /api/transactions/withdraw
  Headers: Authorization: Bearer <token>
  Body: { "accountId": 1, "amount": 100.00 }
  Response: { "id": 2, "type": "WITHDRAW", "status": "COMPLETED" }

POST /api/transactions/transfer
  Headers: Authorization: Bearer <token>
  Body: { "fromAccountId": 1, "toAccountId": 2, "amount": 250.00 }
  Response: { "id": 3, "type": "TRANSFER", "status": "COMPLETED" }

GET /api/transactions/{transactionId}
  Headers: Authorization: Bearer <token>
  Response: { "id": 1, "type": "DEPOSIT", "amount": 500.00, "status": "COMPLETED" }

GET /api/transactions/account/{accountId}
  Headers: Authorization: Bearer <token>
  Response: [{ "id": 1, "type": "DEPOSIT", "amount": 500.00 }, ...]
```

---

## 🗄 Database

### Database Schema

**Users Table**
```sql
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(255) NOT NULL DEFAULT 'USER',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Accounts Table**
```sql
CREATE TABLE accounts (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT UNIQUE NOT NULL,
  balance NUMERIC(19,2) NOT NULL DEFAULT 0.00,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**Transactions Table**
```sql
CREATE TABLE transaction (
  id BIGSERIAL PRIMARY KEY,
  type VARCHAR(255) CHECK (type IN ('DEPOSIT', 'WITHDRAW', 'TRANSFER')),
  status VARCHAR(255) CHECK (status IN ('INITIATED', 'COMPLETED', 'FAILED')),
  amount NUMERIC(38,2) NOT NULL,
  from_user_id BIGINT,
  to_user_id BIGINT,
  created_at TIMESTAMP
);
```

### Connection Details (Production)
- **Host:** Neon PostgreSQL
- **Database:** `banking_db`
- **Credentials:** Via environment variables
- **SSL:** Enabled

---

## 🐳 Docker Deployment

### Local Development with Docker

```bash
# Build and start (with PostgreSQL)
docker compose up -d

# Rebuild after code changes
docker compose up -d --build

# Stop everything
docker compose down

# Remove data (fresh start)
docker compose down -v

# View real-time logs
docker compose logs -f banking-app
```

### Docker Compose Services

**PostgreSQL:**
- Image: `postgres:15-alpine`
- Port: `5433` (local) → `5432` (container)
- Volume: `postgres_data` (persistent)
- Health check: `pg_isready`

**Banking App:**
- Built from: `Dockerfile` (multi-stage)
- Port: `8080`
- Depends on: PostgreSQL (healthy)
- Networks: `banking_network`

---

## ⚙️ Configuration

### Environment Variables

```properties
# Database
DB_HOST=localhost              # PostgreSQL host
DB_PORT=5432                  # PostgreSQL port
DB_NAME=banking_db            # Database name
DB_USERNAME=postgres          # Database user
DB_PASSWORD=password          # Database password
DB_POOL_SIZE=10              # Connection pool size
DB_MIN_IDLE=5                # Minimum idle connections

# JPA/Hibernate
DDL_AUTO=update              # Schema generation (validate/update/create)
SHOW_SQL=false               # Log SQL queries

# Server
SERVER_PORT=8080             # Application port

# Kafka (future)
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Redis (future)
REDIS_HOST=localhost
REDIS_PORT=6379
```

### Application Profiles

- **default** - Production (PostgreSQL)
- **test** - Testing (H2 in-memory)

```bash
# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=test"
```

---

## 🧪 Development

### Building

```bash
# Maven CLI
./mvnw clean compile

# Build JAR
./mvnw clean package

# Build with tests
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests
```

### Running Tests

```bash
# Run all tests (uses H2 profile)
./mvnw test

# Run specific test class
./mvnw test -Dtest=AccountServiceTest

# Run with coverage
./mvnw test jacoco:report
```

For detailed testing info, see [HOW_TO_RUN_TESTS.md](HOW_TO_RUN_TESTS.md)

---

## 📊 Testing

### Test Coverage

| Module | Tests | Status |
|--------|-------|--------|
| Authentication | 2 | ✅ Pass |
| Accounts | 7 | ✅ Pass |
| Transactions | - | ✅ Pass |
| **Total** | **11** | **✅ All Pass** |

### Test Framework
- **Testing**: JUnit 5
- **Mocking**: Mockito
- **Database**: H2 (in-memory, test profile)
- **Security**: Spring Security Test

---

## 🌐 Production Deployment

### Render Deployment

**Current Status:** ✅ Live
- **URL:** https://banking-java.onrender.com
- **Database:** Neon PostgreSQL
- **SSL:** Automatic

**Deployment Steps:**

1. **Push to GitHub**
   ```bash
   git push origin main
   ```

2. **Create Render Service**
   - Connect GitHub repository
   - Select branch: `main`
   - Build command: `./mvnw clean package -DskipTests`
   - Start command: `java -jar target/BankingProject-0.0.1-SNAPSHOT.jar`

3. **Set Environment Variables** in Render
   ```
   DB_HOST=<neon-db-host>
   DB_PORT=5432
   DB_NAME=banking_db
   DB_USERNAME=<username>
   DB_PASSWORD=<password>
   DDL_AUTO=validate
   SHOW_SQL=false
   ```

4. **Enable Auto-Deploy**
   - Render auto-deploys on git push

### Docker Image Deployment

```bash
# Build image
docker build -t banking-app:1.0.0 .

# Push to Docker Hub (or other registry)
docker tag banking-app:1.0.0 your-registry/banking-app:1.0.0
docker push your-registry/banking-app:1.0.0

# Deploy to cloud (AWS ECS, GCP Cloud Run, etc.)
docker run -p 8080:8080 \
  -e DB_HOST=<db-host> \
  -e DB_PORT=5432 \
  -e DB_NAME=banking_db \
  -e DB_USERNAME=<user> \
  -e DB_PASSWORD=<pass> \
  your-registry/banking-app:1.0.0
```

---

## 🏗 Architecture

For detailed system architecture, see [ARCHITECTURE.md](ARCHITECTURE.md)

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Client/Frontend                         │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/JWT
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (Spring)                      │
│  ├─ JWT Validation                                          │
│  ├─ Request Routing                                         │
│  └─ Exception Handling                                      │
└────┬──────────────┬──────────────┬──────────────────────────┘
     │              │              │
     ▼              ▼              ▼
┌─────────┐  ┌──────────┐  ┌──────────────┐
│  User   │  │ Account  │  │ Transaction  │
│ Module  │  │ Module   │  │  Module      │
├─────────┤  ├──────────┤  ├──────────────┤
│Controller│  │Controller│  │ Controller   │
│Service   │  │Service   │  │ Service      │
│Repository│  │Repository│  │ Repository   │
└────┬────┘  └────┬─────┘  └────┬─────────┘
     │            │             │
     └────────────┼─────────────┘
                  ▼
          ┌───────────────────┐
          │  PostgreSQL 15    │
          │   (Neon Cloud)    │
          ├───────────────────┤
          │ - Users           │
          │ - Accounts        │
          │ - Transactions    │
          └───────────────────┘
```

### Module Responsibilities

| Module | Responsibility |
|--------|-----------------|
| **User** | Authentication, JWT generation, user management |
| **Account** | Balance management, debit/credit operations |
| **Transaction** | Transaction creation, state management, history |
| **Common** | Shared configs, security, exception handling |

---

## 🤝 Contributing

### Development Workflow

1. **Create feature branch**
   ```bash
   git checkout -b feature/new-feature
   ```

2. **Make changes**
   - Follow Java naming conventions
   - Use Lombok for boilerplate
   - Write unit tests

3. **Run tests**
   ```bash
   ./mvnw test
   ```

4. **Commit and push**
   ```bash
   git commit -m "feat: add new feature"
   git push origin feature/new-feature
   ```

5. **Create Pull Request**
   - Add description
   - Link related issues
   - Wait for review

### Code Guidelines

- **Java Version:** 17 LTS
- **Formatting:** Follow Spring conventions
- **Testing:** Minimum 80% coverage
- **Documentation:** Add Javadoc for public APIs
- **Exceptions:** Use custom exceptions for domain logic

---

## 📝 API Response Format

### Success Response
```json
{
  "status": "success",
  "message": "Operation completed successfully",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "role": "USER"
  }
}
```

### Error Response
```json
{
  "status": "error",
  "message": "Invalid request",
  "errors": [
    {
      "field": "email",
      "message": "Email already exists"
    }
  ]
}
```

---

## 🔐 Security

### Features Implemented
- ✅ JWT-based authentication (Stateless)
- ✅ Password encoding (BCrypt)
- ✅ Role-based authorization
- ✅ SQL injection prevention (JPA/Prepared statements)
- ✅ CORS configuration
- ✅ HTTPS (Render auto-SSL)

### Best Practices
- Never commit `.env` file
- Rotate secrets regularly
- Use strong passwords
- Enable audit logging
- Monitor API usage

---

## 📞 Support & Issues

For issues, questions, or suggestions:
1. Check [ARCHITECTURE.md](ARCHITECTURE.md) for design details
2. Check [HOW_TO_RUN_TESTS.md](HOW_TO_RUN_TESTS.md) for testing
3. Create GitHub Issue with:
   - Description of problem
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details

---

## 📄 License

This project is proprietary and confidential.

---

## 🚀 Roadmap

- [ ] Add Kafka for event streaming
- [ ] Add Redis for caching & idempotency
- [ ] Implement fraud detection module
- [ ] Add audit logging
- [ ] Extract modules to microservices
- [ ] Add GraphQL API
- [ ] Implement rate limiting
- [ ] Add metrics & monitoring (Prometheus)

---
 
**Live URL:** https://banking-java.onrender.com/swagger-ui/index.html#/

