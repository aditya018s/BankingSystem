# 🏦 Secure Bank — Full Stack Banking Application

A production-ready digital banking web application built with **Spring Boot**, **Thymeleaf**, **MySQL 8** and **Spring Security**.

---

## ✨ Features

### 👤 User Features
- Secure Signup & Login with BCrypt password hashing
- Dashboard with balance overview and recent transactions
- **Credit / Debit** — min ₹500 per transaction
- **Money Transfer** — instant peer-to-peer transfers
- **Transaction History** — paginated, searchable, filterable
- **PDF Export** — download full statement as PDF
- **Loan Module** — apply, track EMIs, make payments
- **EMI Calculator** — interactive loan calculator with sliders
- Profile management & password change
- Forgot / Reset password with token

### 🛡️ Admin Features
- Admin Dashboard with system stats
- Manage all users — credit, debit, block, delete
- View all transactions across the system
- Loan approval / rejection with remarks

### 🔐 Security
- BCrypt password encryption
- Rate limiting — account locked after 5 failed attempts (15 min)
- CSRF protection on all forms
- Session management with secure logout
- Role-based access (`ROLE_USER`, `ROLE_ADMIN`)

### 🌐 REST API
- JSON endpoints for all banking operations
- Session-based authentication
- 401 JSON response for unauthenticated API calls

---

## 🛠️ Tech Stack

| Layer       | Technology                          |
|-------------|-------------------------------------|
| Backend     | Spring Boot 3.5, Spring Data JPA    |
| Frontend    | Thymeleaf, HTML5, CSS3              |
| Database    | MySQL 8.0                           |
| Security    | Spring Security, BCrypt             |
| PDF Export  | OpenPDF 1.3.43                      |
| Testing     | JUnit 5, Mockito                    |
| Build       | Maven                               |

---

## ⚙️ Setup & Run

### Prerequisites
- Java 17+
- MySQL 8.0
- Maven

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/BankingSystem.git
cd BankingSystem

# 2. Create MySQL database
mysql -u root -p
CREATE DATABASE bankingsystem;
EXIT;

# 3. Update application.properties
# Set your MySQL username and password

# 4. Run the application
./mvnw spring-boot:run

# 5. Open in browser
http://localhost:9090
```

---

## 👤 Demo Credentials

| Role  | Username   | Password     |
|-------|------------|--------------|
| User  | demo_user  | DemoPass123  |
| Admin | admin      | Admin@1234   |

---

## 📡 REST API Endpoints

```
GET  /api/account          — Get logged-in user info
GET  /api/balance          — Get current balance
GET  /api/transactions     — Get transaction history (paginated)
POST /api/credit           — Credit money { "amount": 1000 }
POST /api/debit            — Debit money  { "amount": 500  }
POST /api/transfer         — Transfer     { "toUsername": "abc", "amount": 500 }
GET  /api/admin/users      — All users (admin only)
GET  /api/admin/stats      — System stats (admin only)
GET  /api/admin/transactions — All transactions (admin only)
```

---

## 📁 Project Structure

```
src/main/java/com/program/
├── api/
│   ├── controller/     — REST API controllers
│   └── dto/            — Data Transfer Objects
├── config/             — Security & DataSeeder
├── controller/         — Web controllers
├── entity/             — JPA entities
├── repository/         — Spring Data repositories
└── service/            — Business logic
```

---

## 🚀 Deployment

The app includes a `Dockerfile` for containerized deployment.

```bash
# Build JAR
./mvnw clean package -DskipTests

# Build Docker image
docker build -t secure-bank .

# Run with Docker Compose (includes MySQL)
docker-compose up
```

---

## 📄 License
MIT License — feel free to use for learning and portfolio purposes.