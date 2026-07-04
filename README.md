# Secure Microservices Reference

A production-inspired microservices reference platform built with Java 21 and Spring Boot 4.1, demonstrating secure identity, service discovery, caching, persistence, and clean API design patterns.

---

## Architecture Overview

```
                        ┌─────────────────────────────────────┐
                        │            Keycloak (8080)           │
                        │      OAuth2 / JWT Identity Provider  │
                        └────────────────┬────────────────────┘
                                         │ issues JWT tokens
                   ┌─────────────────────┼─────────────────────┐
                   │                     │                      │
          ┌────────▼────────┐   ┌────────▼────────┐   ┌────────▼────────┐
          │  auth-service   │   │ catalog-data    │   │ catalog-cache   │
          │   (port 8081)   │   │   (port 8082)   │   │  (port 8083)    │
          │                 │   │                 │   │                 │
          │ - /me           │   │ - CRUD books    │   │ - Cache-aside   │
          │ - /admin/ping   │   │ - Pagination    │   │   with Redis    │
          │ - RBAC roles    │   │ - Idempotency   │   │ - TTL: 5 min   │
          └─────────────────┘   └────────┬────────┘   └────────┬────────┘
                                         │                      │
                                ┌────────▼────────┐   ┌────────▼────────┐
                                │   PostgreSQL    │   │      Redis      │
                                │  (catalog DB)   │   │  (cache store)  │
                                └─────────────────┘   └─────────────────┘
```

**Service responsibilities at a glance:**

`authentication-service` demonstrates Keycloak/OAuth2 integration, JWT validation via Spring Security's native resource server support, and role-based access control (`@PreAuthorize`).

`catalog-data-service` demonstrates domain-driven REST API design: contract-first OpenAPI spec, pagination/sort/filter, idempotency guards for safe retries, RFC 7807 problem-details error model, and versioned schema migrations with Flyway.

`catalog-cache-service` demonstrates the cache-aside pattern with Redis: on a cache miss the service fetches from `catalog-data-service`, writes to Redis with a configurable TTL (default 5 minutes), and serves subsequent requests entirely from the cache — with measurable latency improvement.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1, Spring Security, Spring Cloud Gateway |
| Identity | Keycloak 26, OAuth2, JWT |
| Persistence | PostgreSQL 16, Spring Data JPA, Flyway |
| Caching | Redis 7, Lettuce client |
| API | REST, OpenAPI 3 / Swagger UI (springdoc) |
| HTTP Client | Spring 6 `HttpExchange` (declarative, no Feign dependency) |
| Service Discovery | Eureka (Spring Cloud Netflix) |
| Containerisation | Docker, Docker Compose |
| Testing | JUnit 5, Mockito, Testcontainers (real Postgres + Redis) |
| Build | Maven |

---

## Prerequisites

- Docker Desktop (running)
- Java 21+
- Maven 3.9+

No other local setup required — Keycloak, PostgreSQL, Redis, and Eureka all start via Docker Compose.

---

## Quick Start

```bash
git clone https://github.com/rabiayurdakul/secure-microservices-reference.git
cd secure-microservices-reference
docker-compose up --build
```

Wait for all services to become healthy (approximately 60–90 seconds on first run — Maven downloads dependencies inside the build containers). You will see each service log `Started <ServiceName>Application` when ready.

> **Note:** Some services (particularly `authentication-service` and `catalog-cache-service`) may restart once or twice before becoming healthy. This is expected — they start before Keycloak is fully ready and retry automatically via `restart: on-failure`. This is not an error; within 30–60 seconds all services stabilise.

**Service endpoints once running:**

| Service | URL |
|---|---|
| Keycloak Admin | http://localhost:8080 (admin / admin) |
| Authentication Service | http://localhost:8081 |
| Catalog Data Service | http://localhost:8082/swagger-ui.html |
| Catalog Cache Service | http://localhost:8083 |
| Eureka Dashboard | http://localhost:8761 |

---

## Running Tests

Each service has its own test suite. To run all tests:

```bash
cd authentication-service && mvn test
cd ../catalog-data-service && mvn test
cd ../catalog-cache-service && mvn test
```

Tests use **Testcontainers** — real PostgreSQL and Redis containers spin up automatically per test run, no mocking of infrastructure. Docker must be running.

---

## API Walkthrough

### 1. Get an Access Token

All write operations and protected endpoints require a JWT issued by Keycloak. A pre-configured test user is included in the realm export:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -d "client_id=demo-client" \
  -d "client_secret=demo-secret" \
  -d "grant_type=password" \
  -d "username=testuser" \
  -d "password=password" | jq -r .access_token)
```

### 2. Authentication Service

```bash
# Public — no token required
curl http://localhost:8081/api/v1/public/health

# Authenticated — returns claims from the validated JWT
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/v1/me

# Role-protected — testuser has ADMIN role in the demo realm
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/v1/admin/ping
```

### 3. Catalog Data Service

```bash
# Create a book — Idempotency-Key prevents duplicate on retry
curl -X POST http://localhost:8082/api/v1/books \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: my-unique-key-001" \
  -d '{"title":"Clean Code","author":"Robert Martin","isbn":"9780132350884","publishedYear":2008}'

# Retry with the same key — returns the original response, no duplicate created
# Response includes header: Idempotent-Replayed: true
curl -X POST http://localhost:8082/api/v1/books \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: my-unique-key-001" \
  -d '{"title":"Clean Code","author":"Robert Martin","isbn":"9780132350884","publishedYear":2008}'

# List with pagination, sorting, and filtering
curl "http://localhost:8082/api/v1/books?author=Robert+Martin&page=0&size=10&sort=title,asc"

# RFC 7807 problem-details on validation failure (missing required title)
curl -X POST http://localhost:8082/api/v1/books \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"author":"Someone"}'
```

### 4. Catalog Cache Service — Cache-Aside Demo

```bash
BOOK_ID=<id-from-create-response>

# First request — cache MISS, fetches from catalog-data-service (~50-200ms)
time curl http://localhost:8083/api/v1/books/$BOOK_ID

# Second request — cache HIT, served from Redis (~1-5ms)
time curl http://localhost:8083/api/v1/books/$BOOK_ID

# Verify the entry in Redis directly
redis-cli GET "book:$BOOK_ID"

# Evict the cache entry
curl -X DELETE http://localhost:8083/api/v1/books/$BOOK_ID/cache

# Next request is a MISS again — proves eviction worked
time curl http://localhost:8083/api/v1/books/$BOOK_ID
```

---

## Key Design Decisions

**Why `HttpExchange` instead of Feign for the inter-service client?**
Spring 6 ships a native declarative HTTP client (`@HttpExchange`) that gives the same clean interface style as Feign without pulling in a Spring Cloud dependency just for HTTP calls. This is the approach now recommended in Spring's own documentation.

**Why `service_healthy` conditions in Docker Compose?**
`depends_on` without a health condition only waits for the container to start, not for the process inside it to be ready. Flyway migrations fail if they run before Postgres is accepting connections. Using `condition: service_healthy` with each database's `pg_isready` healthcheck guarantees migrations run against a ready database.

**Why is cache TTL 5 minutes?**
Book catalog data (title, author, ISBN) changes infrequently. A 5-minute TTL allows the cache to absorb repeated reads while keeping staleness acceptable. For higher-churn data (stock levels, prices) a shorter TTL or event-driven invalidation via Kafka would be more appropriate.

**Why are read endpoints on `catalog-data-service` public (no auth required)?**
This is an intentional contrast with `authentication-service`, where all endpoints require a token. A public catalog browsing API is a common real-world pattern — it demonstrates granular security configuration rather than "lock everything or lock nothing."

**Why does the idempotency filter return the original response on replay rather than an error?**
The Stripe/PayPal pattern: a client that timed out and retried does not know whether its original request succeeded. Returning the original `201 Created` response (with `Idempotent-Replayed: true` header) gives the client a definitive answer without creating a duplicate. An error response would leave the client's state uncertain.

**Why is there an index on `books.author`?**
The list endpoint supports filtering by author (`?author=Robert Martin`). Without an index, every filter request performs a full table scan. Adding `idx_books_author` in the Flyway migration makes the intent explicit and version-controlled — the same way a production team would ship a performance change: as a reviewed, numbered migration, not a silent schema alteration.

**Why does the cache-aside service not cache a "not found" result?**
Four scenarios are handled deliberately:
- **Cache HIT** → return from Redis, never call `catalog-data-service`.
- **Cache MISS, data found** → call `catalog-data-service`, write result to Redis with TTL, return response.
- **Cache MISS, data not found** → call `catalog-data-service`, return 404, **do not write to Redis**. Caching an empty result would mean a legitimate book created moments later would still return 404 for the remainder of the TTL.
- **Evict** → delete the Redis key; the next request becomes a MISS and fetches fresh data.

**Why Testcontainers instead of H2 for integration tests?**
H2's SQL dialect differs subtly from PostgreSQL. Running tests against the same database engine used in production catches issues (index behaviour, UUID generation, Flyway dialect) that an in-memory substitute would miss.

---

## Postman Collection

A ready-to-run Postman collection covering all endpoints and scenarios (idempotency replay, cache hit/miss timing, RFC 7807 errors, 401 enforcement) is included at the root of the repository:

```
secure-microservices-reference.postman_collection.json
```

Import it into Postman and run **"Get Token"** first — the collection automatically stores the JWT and the created book ID in collection variables, so all subsequent requests are pre-wired.

---

## Project Structure

```
secure-microservices-reference/
├── docker-compose.yml
├── keycloak/
│   └── realm-export.json          # auto-imported on first start
├── authentication-service/
│   ├── Dockerfile
│   └── src/
│       └── main/java/.../
│           ├── config/            # SecurityConfig, KeycloakRoleProperties
│           ├── controller/        # ProfileController, HealthController, AdminController
│           └── converter/         # JwtAuthConverter (Keycloak role extraction)
├── catalog-data-service/
│   ├── Dockerfile
│   └── src/
│       └── main/
│           ├── java/.../
│           │   ├── config/        # SecurityConfig, OpenApiConfig
│           │   ├── controller/    # BookController
│           │   ├── service/       # BookService
│           │   ├── repository/    # BookRepository, IdempotencyKeyRepository
│           │   ├── entity/        # Book, IdempotencyKeyRecord
│           │   ├── dto/           # BookResponse, CreateBookRequest, PagedResponse
│           │   ├── filter/        # IdempotencyFilter
│           │   └── exception/     # GlobalExceptionHandler, BookNotFoundException
│           └── resources/
│               └── db/migration/  # V1__create_books_table.sql, V2__create_idempotency_keys_table.sql
└── catalog-cache-service/
    ├── Dockerfile
    └── src/
        └── main/java/.../
            ├── config/            # RedisConfig, ClientConfig
            ├── controller/        # BookCacheController
            ├── service/           # BookCacheService
            ├── client/            # CatalogDataClient (HttpExchange)
            ├── dto/               # BookResponse
            └── exception/         # GlobalExceptionHandler, BookNotFoundException
```

---

## About

Built by [Rabia Yurdakul Telef](https://www.linkedin.com/in/rabia-yurdakul-telef-889196a7) — Senior Java Backend Engineer (8+ years) specialising in Spring Boot microservices, Keycloak/OAuth2, and Redis performance engineering.

Available for outside-IR35 contracts across the UK (remote/hybrid). [GitHub](https://github.com/rabiayurdakul) · [Udemy](https://www.udemy.com/user/rabia-yurdakul-telef-3/)
