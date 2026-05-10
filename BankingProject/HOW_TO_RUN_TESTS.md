# 🚀 How to Run Tests - Banking Project

## 📋 Quick Start

### Run ALL Tests
```bash
./mvnw test
```

### Run Tests with Quiet Output (Faster)
```bash
./mvnw test -q
```

### Run Tests with Clean Build
```bash
./mvnw clean test
```

---

## 🎯 Run Specific Tests

### Run Single Test Class
```bash
./mvnw test -Dtest=AccountServiceTest
```

### Run Specific Test Method
```bash
./mvnw test -Dtest=AccountServiceTest#deposit_shouldIncreaseBalance
```

### Run Multiple Test Classes
```bash
./mvnw test -Dtest=AccountServiceTest,AuthServiceTest,TransactionServiceTest
```

### Run All Tests in a Package
```bash
./mvnw test -Dtest=com.example.bankingproject.account.service.*
```

---

## 📊 High-Value Tests (Run These!)

### 🔥 Test 1: Deposit Success
```bash
./mvnw test -Dtest=AccountServiceTest#deposit_shouldIncreaseBalance
```

### 🔥 Test 2: Withdraw Insufficient Balance
```bash
./mvnw test -Dtest=AccountServiceTest#withdraw_shouldThrowWhenBalanceIsInsufficient
```

### 🔥 Test 3: Transfer Success
```bash
./mvnw test -Dtest=TransactionServiceTest#transfer_shouldCompleteSuccessfully
```

### 🔥 Test 4: Login JWT
```bash
./mvnw test -Dtest=AuthServiceTest#login_shouldReturnJwtToken
```

### Run All 4 Critical Tests
```bash
./mvnw test -Dtest=AccountServiceTest,AuthServiceTest,TransactionServiceTest
```

---

## 🔍 Advanced Options

### Show Full Output
```bash
./mvnw test -e
```

### Skip Tests During Build
```bash
./mvnw clean install -DskipTests
```

### Run Tests with Coverage Report
```bash
./mvnw test jacoco:report
# Report location: target/site/jacoco/index.html
```

### Run Tests in Parallel (Faster)
```bash
./mvnw test -T 1C
```

### Run Tests with Debug Output
```bash
./mvnw test -X
```

### Run Tests Matching a Pattern
```bash
./mvnw test -Dtest=*ServiceTest
```

---

## 🏃 Expected Output

### Successful Test Run
```
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Failed Test Run
```
[ERROR] Tests run: 11, Failures: 1, Errors: 0, Skipped: 0
[ERROR] BUILD FAILURE
```

---

## 📁 Test Files Location

```
src/test/java/com/example/bankingproject/
├── account/service/
│   └── AccountServiceTest.java
├── user/service/
│   └── AuthServiceTest.java
└── transaction/service/
    └── TransactionServiceTest.java
```

---

## 🎨 Running Tests from IDE (JetBrains IntelliJ)

### Method 1: Right-Click on Test File
1. Open test file in editor
2. Right-click → "Run 'ClassName'"
3. Results shown in built-in console

### Method 2: Right-Click on Test Method
1. Open test file in editor
2. Right-click on test method → "Run 'methodName()'"
3. Runs just that single test

### Method 3: Run Menu
1. Click "Run" in top menu
2. Select "Run..." or "Run 'ClassName'"
3. Choose from recent tests

### Method 4: Keyboard Shortcuts
- **Run current test:** Ctrl+Shift+F10 (Windows/Linux) or Cmd+Shift+R (Mac)
- **Run all tests:** Ctrl+Shift+F10 on test class

### Method 5: Gutter Click
1. Open test file
2. Click on the green play icon in line number gutter
3. Select "Run" or "Debug"

---

## 📊 View Test Results

### In Terminal
```bash
./mvnw test
# Output shows:
# ✓ Test passed
# ✗ Test failed
# → Test skipped
```

### HTML Report (After Test Run)
- Location: `target/surefire-reports/`
- Open: `target/surefire-reports/index.html` in browser

### In IDE
- View → Tool Windows → Run
- Click "Test Results" tab to see detailed breakdown

---

## ✅ Common Commands Summary

| Command | Purpose |
|---------|---------|
| `./mvnw test` | Run all tests |
| `./mvnw test -q` | Run all tests (quiet) |
| `./mvnw clean test` | Clean & run all tests |
| `./mvnw test -Dtest=ClassName` | Run specific test class |
| `./mvnw test -Dtest=ClassName#methodName` | Run specific method |
| `./mvnw test -DskipTests` | Build without tests |
| `./mvnw test jacoco:report` | Run with coverage |

---

## 🚨 Troubleshooting

### "Command not found: mvnw"
Solution: Make sure you're in the correct directory
```bash
cd /Users/vedantgadge1512/VG\ Codes/Banking/BankingProject
```

### "Permission denied: mvnw"
Solution: Make the script executable
```bash
chmod +x mvnw
```

### "Tests failed"
Solution: Check the error message in output
```bash
./mvnw test -e  # Show full error stack trace
```

### "Test hangs"
Solution: Add timeout
```bash
./mvnw test -Dorg.junit.jupiter.execution.parallel.enabled=true
```

---

## 💡 Pro Tips

### 1. Watch Tests on Save (Continuous Integration)
```bash
./mvnw -T 1C test -f --also-make -amd
```

### 2. Run Only Failed Tests
```bash
./mvnw test --fail-at-end
```

### 3. Skip Specific Tests
```bash
./mvnw test -Dtest=!AccountServiceTest
```

### 4. Generate Coverage Report
```bash
./mvnw clean test jacoco:report
open target/site/jacoco/index.html  # View in browser
```

### 5. Run Tests in Specific Order
```bash
./mvnw test -Dtest=AccountServiceTest,TransactionServiceTest,AuthServiceTest
```

---

## 🎯 Recommended Workflow

### Quick Check (30 seconds)
```bash
./mvnw test -q -Dtest=AccountServiceTest,AuthServiceTest,TransactionServiceTest
```

### Full Test Suite
```bash
./mvnw clean test
```

### Before Committing
```bash
./mvnw clean test -e
```

### Final Verification
```bash
./mvnw clean test jacoco:report
```

---

## 🔔 Example: Running the 4 High-Value Tests

### One by one:
```bash
# Test 1
./mvnw test -Dtest=AccountServiceTest#deposit_shouldIncreaseBalance

# Test 2
./mvnw test -Dtest=AccountServiceTest#withdraw_shouldThrowWhenBalanceIsInsufficient

# Test 3
./mvnw test -Dtest=TransactionServiceTest#transfer_shouldCompleteSuccessfully

# Test 4
./mvnw test -Dtest=AuthServiceTest#login_shouldReturnJwtToken
```

### All together:
```bash
./mvnw test -Dtest=AccountServiceTest,AuthServiceTest,TransactionServiceTest -q
```

---

## 📝 Current Status

Your tests are fully implemented and ready to run! ✅

```
Total Tests: 11
Failures: 0
Errors: 0
Status: ALL PASSING ✅
```

---

Need help? Just ask! 🚀

