# E-Bookstore Spring Boot Backend API

## Overview
REST API backend for an e-commerce book store built with **Spring Boot 3** and **PostgreSQL**.

Developed as part of the AI Specialist Capstone Project using **IBM BOB** (Agentic IDE).

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL (H2 for tests) |
| API Documentation | SpringDoc OpenAPI 3 / Swagger UI |
| Build | Maven |

---

## Modules / Features

| Feature | Endpoints |
|---------|-----------|
| User Registration & Login | `POST /api/v1/auth/register`, `/login` |
| User Profile | `GET /api/v1/auth/me` |
| Product Catalogue (browse, search, filter) | `GET /api/v1/products` |
| Product Detail + Related Products | `GET /api/v1/products/{id}`, `/related` |
| Categories | `GET /api/v1/categories` |
| Brands | `GET /api/v1/brands` |
| Shopping Cart | `GET/POST/PUT/DELETE /api/v1/cart` |
| Delivery Addresses | `GET/POST/DELETE /api/v1/addresses` |
| Orders (place, view, cancel, buy-again) | `/api/v1/orders` |
| Payment + Gift Points | `/api/v1/payments` |
| Personalised Recommendations | `GET /api/v1/products/recommendations` |

---

## Prerequisites

- **Java 21** – [Download](https://adoptium.net)
- **Maven 3.9+**
- **PostgreSQL** – [Download](https://www.postgresql.org/download/)

---

## Setup

### 1. Create the database

```sql
CREATE DATABASE ebookstore;
CREATE USER ebookstore_user WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE ebookstore TO ebookstore_user;
```

### 2. Configure environment

Copy `.env.example` to `.env` and fill in your values:

```
DB_URL=jdbc:postgresql://localhost:5432/ebookstore
DB_USERNAME=ebookstore_user
DB_PASSWORD=yourpassword
JWT_SECRET=<generate with: openssl rand -base64 32>
ALLOWED_ORIGINS=http://localhost:3000
```

> **Security note:** `.env` is listed in `.gitignore` and must never be committed.

### 3. Load environment variables

```bash
export $(cat .env | xargs)
```

### 4. Build and run

```bash
# Clean build
./mvnw clean install

# Run
./mvnw spring-boot:run
```

Application starts at **http://localhost:8080**

### 5. Load seed data

```bash
psql -U ebookstore_user -d ebookstore -f src/main/resources/db/seed.sql
```

---

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:

```
http://localhost:8080/v3/api-docs
```

---

## Running Tests

```bash
./mvnw test
```

Tests use an **H2 in-memory database** – no PostgreSQL required.

---

## Project Structure

```
src/main/java/com/capstone/ebookstore/
├── EbookstoreApplication.java
├── config/
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   ├── ProductController.java
│   ├── CategoryController.java
│   ├── BrandController.java
│   ├── CartController.java
│   ├── AddressController.java
│   ├── OrderController.java
│   └── PaymentController.java
├── dto/
│   ├── AuthDto.java
│   ├── ProductDto.java
│   ├── CategoryDto.java
│   ├── BrandDto.java
│   ├── CartDto.java
│   ├── AddressDto.java
│   └── OrderDto.java
├── entity/
│   ├── User.java
│   ├── Product.java
│   ├── Category.java
│   ├── Brand.java
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Order.java
│   ├── OrderItem.java
│   └── Address.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   └── ConflictException.java
├── repository/
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── CategoryRepository.java
│   ├── BrandRepository.java
│   ├── CartRepository.java
│   ├── CartItemRepository.java
│   ├── AddressRepository.java
│   └── OrderRepository.java
├── security/
│   ├── JwtTokenProvider.java
│   └── JwtAuthenticationFilter.java
└── service/
    ├── AuthService.java
    ├── ProductService.java
    ├── CartService.java
    ├── AddressService.java
    └── OrderService.java
```

---

## Git Workflow

```bash
# Initialise repository (if new)
git init

# Create feature branch
git checkout -b feature/api-implementation

# Stage and commit
git add .
git commit -m "Implement Spring Boot API for e-commerce bookstore"

# Push and open PR
git push origin feature/api-implementation
```

---

## Capstone Checklist

- [x] OpenAPI specification generated (`openapi.yaml`)
- [x] Data model designed and implemented
- [x] Spring Boot application scaffolded
- [x] PostgreSQL configured
- [x] REST APIs for all user journeys (browse, cart, order, payment)
- [x] JWT authentication
- [x] Gift points / redemption
- [x] Order cancellation within 48h
- [x] Buy-again from order history
- [x] Related products & personalised recommendations
- [x] Swagger UI available
- [ ] Deploy to local machine and run
- [ ] Video walkthrough recorded
- [ ] PR raised on personal GitHub
