# Smart Payment Router

A Spring Boot backend that routes payment requests across multiple payment
gateways (PGs). Built to demonstrate a production-style backend architecture
with fault tolerance (Circuit Breaker), asynchronous event processing
(Kafka), and AI-powered payment failure analysis (RAG).

**Live Demo:** https://smart-payment-router-production.up.railway.app
**Health Check:** https://smart-payment-router-production.up.railway.app/actuator/health

---

## Key Features

- **Payment Routing**: Selects the optimal route among multiple payment gateways
- **Fault Tolerance**: Resilience4j Circuit Breaker + Retry to isolate failures
  in external APIs (exchange rate, payment gateways)
- **Asynchronous Event Processing**: Kafka-based publish/subscribe for
  payment events (notifications, post-processing, etc.)
- **JWT Authentication + RBAC**: Email/password login with USER/ADMIN
  role-based authorization
- **AI-Powered Failure Analysis (RAG)**: On payment failure, the failure
  context is embedded via OpenAI and stored as a vector. On request, similar
  past failures are retrieved and an LLM generates a root-cause analysis and
  recommended action
- **Rate Limiting**: Bucket4j-based request throttling
- **Monitoring**: Actuator + Prometheus metrics exposed

## Tech Stack

| Category | Technology |
|---|---|
| Language / Framework | Java 21, Spring Boot 3.4 |
| Database | PostgreSQL (pgvector extension) |
| Cache | Redis |
| Message Queue | Apache Kafka |
| AI / RAG | Spring AI, OpenAI (text-embedding-3-small, gpt-4o-mini) |
| Auth | Spring Security, JWT |
| Resilience | Resilience4j (Circuit Breaker, Retry), Bucket4j (Rate Limiting) |
| Testing | JUnit 5, Testcontainers |
| Build | Gradle |

## Deployment Infrastructure (all free tier)

| Service | Provider |
|---|---|
| Application Hosting | [Railway](https://railway.app) |
| PostgreSQL + pgvector | [Neon](https://neon.tech) |
| Redis | [Upstash](https://upstash.com) |
| Kafka | [Confluent Cloud](https://confluent.cloud) |

## Architecture

```
Client
  │
  ▼
[Spring Boot API] ──JWT Auth──▶ [Security Filter Chain]
  │
  ├─▶ [PaymentService] ──▶ Circuit Breaker ──▶ [Payment Gateway / Exchange Rate API]
  │         │
  │         └─▶ On failure ──▶ [FailureEmbeddingService] ──▶ OpenAI Embedding ──▶ [pgvector]
  │
  ├─▶ [FailureAnalysisController] ──▶ Similarity Search (pgvector) ──▶ OpenAI Chat ──▶ Root cause analysis
  │
  └─▶ [PaymentEventProducer] ──▶ Kafka ──▶ [Consumer: notifications / post-processing]
```

## API Overview

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | User registration | - |
| POST | `/api/auth/login` | Login (issues JWT) | - |
| POST | `/api/payments` | Submit a payment request | USER |
| GET | `/api/v1/admin/failures/analyze?reason=` | Analyze payment failure root cause (RAG) | ADMIN |
| GET | `/actuator/health` | Health check | - |

## Running Locally

### 1. Start dependent services (Docker)

```bash
docker-compose up -d
```

This starts Postgres (with pgvector), Redis, and Kafka locally.

### 2. Set environment variables

```bash
export OPENAI_API_KEY=sk-...
export EXCHANGE_RATE_API_KEY=...
```

### 3. Run the application

```bash
./gradlew bootRun
```

### 4. Run tests

```bash
./gradlew test
```

Integration tests run in an isolated environment using Testcontainers.

## Production Configuration

The production environment replaces local Docker services with the
following environment variables:

```
DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD   # Neon Postgres
REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, REDIS_SSL_ENABLED   # Upstash Redis
KAFKA_BOOTSTRAP_SERVERS, KAFKA_SECURITY_PROTOCOL,
KAFKA_SASL_MECHANISM, KAFKA_SASL_JAAS_CONFIG        # Confluent Cloud Kafka
OPENAI_API_KEY, EXCHANGE_RATE_API_KEY
ADMIN_INITIAL_PASSWORD                               # Initial admin account password
```

To fit within the free tier's memory limits, the JVM heap is capped with
`JAVA_TOOL_OPTIONS=-Xmx400m` in production.

## Roadmap

- [ ] Finer-grained RBAC — currently a two-tier USER/ADMIN model; plan to
  expand into more granular permissions
- [ ] Publicly exposed Swagger/OpenAPI documentation endpoint
- [ ] CI/CD pipeline (GitHub Actions → automatic deployment to Railway)
- [ ] Kafka Dead Letter Queue (DLQ) for handling message processing failures

## Troubleshooting Notes

Notable issues encountered during development and deployment.

- **Test failures due to Testcontainers not being applied**
  `application-test.yml` was using a legacy Postgres JDBC URL
  (`jdbc:tc:postgresql`) that didn't support pgvector, causing tests against
  pgvector-based entities to fail. Fixed by explicitly `@Import`-ing a
  `@ServiceConnection`-based `TestcontainersConfig` into the test class.

- **Vector dimension mismatch during the Ollama → OpenAI migration**
  Migrating from local-only Ollama (`nomic-embed-text`, 768 dimensions) to
  cloud-deployable OpenAI (`text-embedding-3-small`, 1536 dimensions)
  required updating the pgvector column dimension setting accordingly to
  avoid a schema mismatch.

- **Missing executable permission on `gradlew` on Windows**
  A `gradlew` file committed from Windows lacked the Unix executable bit,
  causing the Linux-based Railway build to fail with `Permission denied`.
  Resolved with `git update-index --chmod=+x gradlew` to fix the
  Git-tracked file permission.

- **OutOfMemoryError in the Kafka consumer**
  Within Railway's free-tier memory limit, the Kafka client's default heap
  usage caused the container to crash. Stabilized by capping the JVM heap
  with `JAVA_TOOL_OPTIONS=-Xmx400m`.

- **Environment variable updates not taking effect on the PaaS**
  Updating an environment variable (`DATABASE_URL`) via Railway's web
  dashboard kept reverting to the old value on redeploy. Re-entering the
  value through the UI didn't resolve it, so the value was set directly via
  the Railway CLI (`railway variables --set`) as a workaround.

- **Reproducible initialization of the admin account**
  To avoid manually recreating an admin account every time the deployment
  or local environment changed, added a `CommandLineRunner`-based
  `AdminAccountSeeder`. It's idempotent (checks for existing email) and
  reads the initial password from an environment variable
  (`ADMIN_INITIAL_PASSWORD`) so a default credential is never exposed in
  production.
