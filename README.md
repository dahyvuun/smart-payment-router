# Smart Payment Router

결제 요청을 여러 PG사(Payment Gateway)로 라우팅하는 Spring Boot 기반 백엔드 시스템입니다.
장애 복원력(Circuit Breaker), 비동기 이벤트 처리(Kafka), 그리고 AI 기반 결제 실패 원인
분석(RAG)을 포함한 실전형 백엔드 아키텍처를 목표로 만들었습니다.

**🔗 Live Demo:** https://smart-payment-router-production.up.railway.app

---

## 주요 기능

- **결제 라우팅**: 여러 PG사 중 최적의 라우트를 선택하여 결제 처리
- **장애 복원력**: Resilience4j 기반 Circuit Breaker + Retry로 외부 API(환율, PG사) 장애 격리
- **비동기 이벤트 처리**: Kafka를 통한 결제 이벤트 발행/구독 (알림, 후처리 등)
- **JWT 인증 + RBAC**: 이메일/비밀번호 로그인, USER/ADMIN 역할 기반 인가
- **AI 기반 실패 분석 (RAG)**: 결제 실패 발생 시 OpenAI 임베딩으로 벡터화하여 저장하고,
  이후 유사한 과거 실패 사례를 검색해 LLM이 근본 원인과 대응 방안을 분석
- **Rate Limiting**: Bucket4j 기반 요청 제한
- **모니터링**: Actuator + Prometheus 메트릭 노출

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language / Framework | Java 21, Spring Boot 3.4 |
| Database | PostgreSQL (pgvector extension) |
| Cache | Redis |
| Message Queue | Apache Kafka |
| AI / RAG | Spring AI, OpenAI (text-embedding-3-small, gpt-4o-mini) |
| Auth | Spring Security, JWT |
| Resilience | Resilience4j (Circuit Breaker, Retry), Bucket4j (Rate Limiting) |
| Test | JUnit 5, Testcontainers |
| Build | Gradle |

## 배포 인프라 (전부 무료 티어)

| 서비스 | 제공처 |
|---|---|
| Application Hosting | [Railway](https://railway.app) |
| PostgreSQL + pgvector | [Neon](https://neon.tech) |
| Redis | [Upstash](https://upstash.com) |
| Kafka | [Confluent Cloud](https://confluent.cloud) |

## 아키텍처

```
Client
  │
  ▼
[Spring Boot API] ──JWT 인증──▶ [Security Filter Chain]
  │
  ├─▶ [PaymentService] ──▶ Circuit Breaker ──▶ [PG사 / 환율 API]
  │         │
  │         └─▶ 실패 시 [FailureEmbeddingService] ──▶ OpenAI Embedding ──▶ [pgvector]
  │
  ├─▶ [FailureAnalysisController] ──▶ 유사도 검색(pgvector) ──▶ OpenAI Chat ──▶ 원인 분석 결과
  │
  └─▶ [PaymentEventProducer] ──▶ Kafka ──▶ [Consumer: 알림/후처리]
```

## API 개요

| Method | Endpoint | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/auth/register` | 회원가입 | - |
| POST | `/api/auth/login` | 로그인 (JWT 발급) | - |
| POST | `/api/payments` | 결제 요청 | USER |
| GET | `/api/v1/admin/failures/analyze?reason=` | 결제 실패 원인 분석 (RAG) | ADMIN |
| GET | `/actuator/health` | 헬스체크 | - |

## 로컬 개발 환경 실행

### 1. 의존 서비스 실행 (Docker)

```bash
docker-compose up -d
```

Postgres(pgvector), Redis, Kafka가 로컬에 뜹니다.

### 2. 환경변수 설정

```bash
export OPENAI_API_KEY=sk-...
export EXCHANGE_RATE_API_KEY=...
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 4. 테스트 실행

```bash
./gradlew test
```

Testcontainers를 사용해 격리된 환경에서 통합 테스트가 실행됩니다.

## 배포 환경 구성

프로덕션 환경은 아래 환경변수로 로컬 Docker 서비스를 대체합니다.

```
DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD   # Neon Postgres
REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, REDIS_SSL_ENABLED   # Upstash Redis
KAFKA_BOOTSTRAP_SERVERS, KAFKA_SECURITY_PROTOCOL,
KAFKA_SASL_MECHANISM, KAFKA_SASL_JAAS_CONFIG        # Confluent Cloud Kafka
OPENAI_API_KEY, EXCHANGE_RATE_API_KEY
ADMIN_INITIAL_PASSWORD                               # 최초 관리자 계정 비밀번호
```

무료 티어 리소스 제약(메모리 등)에 맞춰 `JAVA_TOOL_OPTIONS=-Xmx400m`으로 JVM 힙을
제한하여 운영 중입니다.

## 향후 개선 계획

- [ ] Role 기반 인가(RBAC) 세분화 — 현재는 USER/ADMIN 2단계, 추후 권한 세분화 예정
- [ ] Swagger/OpenAPI 문서 공개 엔드포인트 추가
- [ ] CI/CD 파이프라인 (GitHub Actions → Railway 자동 배포)
- [ ] Kafka DLQ(Dead Letter Queue) 구성으로 메시지 처리 실패 대응

## 트러블슈팅 기록

개발 및 배포 과정에서 다룬 주요 이슈들입니다.

- **Testcontainers 미적용 이슈**: `application-test.yml`이 pgvector 미지원 이미지의
  legacy JDBC URL을 사용하고 있어 테스트가 실패 → `@ServiceConnection` 기반
  `TestcontainersConfig`를 명시적으로 import하도록 수정
- **Windows에서 `gradlew` 실행 권한 누락**: Git이 실행 비트를 보존하지 않아 Railway
  빌드가 `Permission denied`로 실패 → `git update-index --chmod=+x gradlew`로 해결
- **Kafka Consumer OutOfMemoryError**: 무료 티어 메모리 한도 내에서 Kafka 클라이언트가
  과도한 힙을 요구 → `-Xmx400m`으로 JVM 힙 제한
