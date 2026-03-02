# Payment Router

A fintech backend service that intelligently routes payments through multiple payment gateways based on configurable strategies.

Built with Spring Boot 3.4, this project demonstrates production-grade patterns including fault tolerance, caching, event-driven architecture, and observability.

---

## Tech Stack

| Category | Technology |
|---|---|
| Framework | Spring Boot 3.4, Java 21 |
| Database | PostgreSQL 16, Spring Data JPA |
| Cache | Redis 7 |
| Messaging | Apache Kafka |
| Security | Spring Security, JWT |
| Resilience | Resilience4j (Circuit Breaker, Retry) |
| Rate Limiting | Bucket4j |
| Documentation | SpringDoc OpenAPI 3.0 (Swagger UI) |
| Testing | JUnit 5, Testcontainers |
| Containerization | Docker, Docker Compose |

---

## Features

- **JWT Authentication** — Stateless token-based auth with BCrypt password encoding
- **Multi-currency Wallets** — Create and manage wallets in USD, EUR, GBP, KRW and more
- **Smart Payment Routing** — Pluggable routing strategies using the Strategy pattern
  - `LOWEST_FEE` — routes to the gateway with the lowest transaction fee
  - `HIGHEST_SUCCESS_RATE` — routes to the gateway with the best success rate
- **Idempotency** — Safe payment retries using `X-Idempotency-Key` header backed by Redis
- **Exchange Rate Integration** — Real-time rates via ExchangeRate-API with Redis caching
- **Circuit Breaker** — Resilience4j protects against cascading failures from external APIs
- **Rate Limiting** — Bucket4j token bucket algorithm per user (10 req/min for payments)
- **Event-driven** — Kafka publishes payment events for downstream consumers
- **Integration Tests** — Full test suite using Testcontainers (real PostgreSQL, Redis, Kafka)
- **API Documentation** — Interactive Swagger UI with JWT auth support

---

## Getting Started

### Prerequisites

- Docker Desktop
- Java 21

### Run with Docker Compose

```bash
# Clone the repository
git clone https://github.com/dahyvuun/payment-router.git
cd payment-router

# Start infrastructure (PostgreSQL, Redis, Kafka)
docker compose up -d

# Run the application
./gradlew bootRun
```

The API will be available at `http://localhost:8080`

### API Documentation

Open Swagger UI in your browser:

```
http://localhost:8080/swagger-ui.html
```

---

## API Overview

### Authentication

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

### Process a Payment

```http
POST /api/payments
Authorization: Bearer <token>
X-Idempotency-Key: payment-2026-001
Content-Type: application/json

{
  "amount": 100.00,
  "currency": "USD",
  "strategy": "LOWEST_FEE"
}
```

**Response:**
```json
{
  "transactionId": 1,
  "amount": 100.00,
  "currency": "USD",
  "status": "COMPLETED",
  "selectedPaymentMethod": "BANK_TRANSFER",
  "feeRate": 0.5,
  "successRate": 98.0,
  "createdAt": "2026-03-01T12:00:00"
}
```

### Wallets

```http
POST   /api/wallets          # Create wallet
GET    /api/wallets          # Get all wallets
GET    /api/wallets/{id}     # Get wallet by ID
DELETE /api/wallets/{id}     # Delete wallet
```

### Exchange Rates

```http
GET /api/exchange-rates/{baseCurrency}    # Get rates (cached in Redis)
GET /api/exchange-rates/{from}/{to}       # Get specific rate
```

### Circuit Breaker Monitoring

```http
GET /api/v1/admin/circuit-breakers              # All circuit breakers
GET /api/v1/admin/circuit-breakers/{name}       # Specific circuit breaker
```

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    REST API Layer                    │
│         (Spring MVC + JWT + Rate Limiting)           │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│                  Service Layer                       │
│   PaymentService  │  ExchangeRateService             │
│   AuthService     │  WalletService                   │
└──────┬────────────┴──────────┬──────────────────────┘
       │                       │
┌──────▼──────┐    ┌───────────▼──────────────────────┐
│  Routing    │    │         Infrastructure            │
│  Engine     │    │  PostgreSQL │ Redis │ Kafka        │
│  (Strategy) │    └──────────────────────────────────┘
└─────────────┘
```

### Payment Flow

```
Client → RateLimitInterceptor → PaymentController
       → PaymentService (check idempotency key in Redis)
       → RoutingEngine (select strategy)
       → CircuitBreaker (call payment gateway)
       → Save Transaction to PostgreSQL
       → Publish PaymentEvent to Kafka
       → Return PaymentResponse
```

---

## Resilience Patterns

### Circuit Breaker (Resilience4j)

The circuit breaker protects against cascading failures from external services.

| State | Behavior |
|---|---|
| `CLOSED` | Requests pass through normally |
| `OPEN` | Requests fail immediately, fallback is returned |
| `HALF_OPEN` | A few test requests are allowed through |

Configuration:
- Failure rate threshold: 50%
- Minimum calls before opening: 5
- Wait duration in OPEN state: 10s

### Rate Limiting (Bucket4j)

Token bucket algorithm per authenticated user:

| Endpoint | Limit |
|---|---|
| `POST /api/payments` | 10 requests/minute |
| `GET /api/exchange-rates/**` | 30 requests/minute |
| All other `/api/**` | 60 requests/minute |

---

## Testing

```bash
# Run all tests (starts Testcontainers automatically)
./gradlew test
```

Integration tests use **Testcontainers** to spin up real PostgreSQL, Redis, and Kafka instances — no mocking, no H2 in-memory database.

---

## Project Structure

```
src/
├── main/java/com/dahyvuun/payment_router/
│   ├── config/          # Redis, Kafka, OpenAPI, Rate Limit config
│   ├── controller/      # REST controllers
│   ├── domain/          # JPA entities (User, Wallet, Transaction, PaymentRoute)
│   ├── dto/             # Request/Response DTOs
│   ├── exception/       # Global exception handler
│   ├── repository/      # Spring Data JPA repositories
│   ├── routing/         # Strategy pattern (LowestFee, HighestSuccessRate)
│   ├── security/        # JWT filter, SecurityConfig
│   └── service/         # Business logic
└── test/
    └── java/            # Testcontainers integration tests
```

---

## License

MIT License — see [LICENSE](LICENSE) for details.