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

- **Testcontainers 미적용으로 인한 테스트 실패**
  `application-test.yml`이 pgvector를 지원하지 않는 legacy Postgres JDBC URL(`jdbc:tc:postgresql`)을
  사용하고 있어, pgvector 기반 엔티티를 검증하는 테스트가 실패했습니다. `@ServiceConnection` 기반의
  `TestcontainersConfig`를 테스트 클래스에 명시적으로 `@Import`하도록 수정해 해결했습니다.

- **Ollama → OpenAI 마이그레이션 시 벡터 차원 불일치**
  로컬 전용 Ollama(`nomic-embed-text`, 768차원)에서 클라우드 배포 가능한 OpenAI
  (`text-embedding-3-small`, 1536차원)로 전환하면서, pgvector 컬럼의 차원 설정을 함께
  변경하지 않으면 스키마 불일치가 발생함을 확인 — 설정값을 동기화하여 해결했습니다.

- **Windows 환경에서 `gradlew` 실행 권한 누락**
  Windows에서 커밋된 `gradlew` 파일은 Unix 실행 비트가 없어, Linux 기반의 Railway 빌드에서
  `Permission denied`로 빌드가 실패했습니다. `git update-index --chmod=+x gradlew`로 Git이
  추적하는 파일 권한을 수정해 해결했습니다.

- **Kafka Consumer의 OutOfMemoryError**
  Railway 무료 티어의 메모리 한도 내에서 Kafka 클라이언트가 기본 힙 설정으로 과도한 메모리를
  요구하며 컨테이너가 크래시했습니다. `JAVA_TOOL_OPTIONS=-Xmx400m`으로 JVM 힙 상한을 명시해
  안정화했습니다.

- **PaaS 환경변수 저장이 반영되지 않는 문제**
  Railway 웹 대시보드에서 환경변수(`DATABASE_URL`)를 수정해도 재배포 시 이전 값으로 되돌아가는
  현상을 겪었습니다. UI를 통한 재입력으로 해결되지 않아, Railway CLI(`railway variables --set`)로
  값을 직접 설정하여 우회했습니다.

- **관리자 계정의 재현 가능한 초기화**
  배포/로컬 환경이 바뀔 때마다 수동으로 관리자 계정을 만들어야 하는 번거로움을 없애기 위해,
  `CommandLineRunner` 기반의 `AdminAccountSeeder`를 작성했습니다. 이메일 존재 여부로
  idempotent하게 동작하며, 초기 비밀번호는 환경변수(`ADMIN_INITIAL_PASSWORD`)로 주입받아
  기본값이 그대로 운영 환경에 노출되지 않도록 했습니다.
