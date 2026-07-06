# Secure Microservices Reference

<<<<<<< HEAD
A production-inspired microservices reference project built with Java 21 and Spring Boot 4.1. It demonstrates secure identity, API gateway routing, service discovery, persistence, Redis caching, and a small but realistic service-to-service flow.

The goal of this repository is not to be a full production platform. It is a practical backend reference that shows how the main building blocks fit together in a secure Spring Boot microservices setup.
=======
A production-inspired microservices reference platform built with Java 21 and Spring Boot 4.1, demonstrating secure identity, service discovery, caching, persistence, and clean API design patterns.
>>>>>>> f183cb3f2b0765538d425157d3600b250a7dbf0b

---

## Architecture Overview

```text
                         ┌──────────────────────────────────────┐
                         │              Client / Postman          │
                         └──────────────────┬───────────────────┘
                                            │
                                            ▼
                         ┌──────────────────────────────────────┐
                         │          edge-gateway (8088)          │
                         │   Routing, JWT validation, API edge   │
                         └───────────────┬──────────────┬───────┘
                                         │              │
                         ┌───────────────┘              └────────────────┐
                         ▼                                               ▼
          ┌──────────────────────────┐                    ┌──────────────────────────┐
          │ authentication-service   │                    │ catalog-cache-service    │
          │        (8081)             │                    │        (8083)             │
          │ /me, /admin/ping, RBAC    │                    │ Cache-aside reads         │
          └─────────────┬────────────┘                    └─────────────┬────────────┘
                        │                                               │
                        │                                               ▼
                        │                                  ┌──────────────────────────┐
                        │                                  │          Redis            │
                        │                                  │      cache store          │
                        │                                  └──────────────────────────┘
                        │
                        │                                  ┌──────────────────────────┐
                        └─────────────────────────────────►│ catalog-data-service     │
                                                           │        (8082)             │
                                                           │ CRUD, pagination, Flyway  │
                                                           └─────────────┬────────────┘
                                                                         │
                                                                         ▼
                                                           ┌──────────────────────────┐
                                                           │        PostgreSQL         │
                                                           │        catalog DB         │
                                                           └──────────────────────────┘

          ┌──────────────────────────┐
          │      Keycloak (8080)      │  Issues JWT access tokens
          │ OAuth2 / JWT provider     │  used by the gateway and protected services
          └──────────────────────────┘

          ┌──────────────────────────┐
          │ discovery-server (8761)   │  Eureka registry
          │ Service registration      │  used by the gateway for lb:// service routing
          └──────────────────────────┘
```

## Services

`edge-gateway` is the main entry point for API traffic. It routes requests to downstream services through Eureka and validates JWT access tokens at the edge.

`authentication-service` demonstrates Keycloak/OAuth2 integration, JWT validation with Spring Security resource server support, and role-based access control.

`catalog-data-service` owns the book catalog data. It includes REST endpoints, pagination, sorting, filtering, idempotency handling for safe retries, RFC 7807-style error responses, OpenAPI documentation with springdoc, and Flyway migrations.

`catalog-cache-service` demonstrates the cache-aside pattern with Redis. On a cache miss, it fetches from `catalog-data-service`, writes the result to Redis with a configurable TTL, and serves later reads from cache.

`discovery-server` runs Eureka so services can register themselves and the gateway can route using logical service names instead of fixed host/port pairs.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1, Spring Security |
| API Gateway | Spring Cloud Gateway Server MVC |
| Service Discovery | Eureka / Spring Cloud Netflix |
| Identity | Keycloak 26, OAuth2, JWT |
| Persistence | PostgreSQL 16, Spring Data JPA, Flyway |
| Caching | Redis 7, Lettuce client |
| API Docs | OpenAPI 3 / Swagger UI with springdoc |
| HTTP Client | Spring RestClient |
| Containerisation | Docker, Docker Compose |
| Testing | JUnit 5, Mockito, Testcontainers |
| Build | Maven |

---

## Prerequisites

- Docker Desktop running
- Java 21+
- Maven 3.9+
- `jq` for the curl examples that extract the access token

Keycloak, PostgreSQL, Redis, Eureka, the gateway, and all Spring Boot services are started through Docker Compose.

---

## Quick Start

```bash
git clone https://github.com/rabiayurdakul/secure-microservices-reference.git
cd secure-microservices-reference
docker compose up --build
```

The first run can take a little longer because Maven dependencies are downloaded inside the build containers. Wait until the Spring services log that they have started.

**Main URLs**

| Service | URL |
|---|---|
| Edge Gateway | http://localhost:8088 |
| Keycloak Admin | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Authentication Service | http://localhost:8081 |
| Catalog Data Service / Swagger | http://localhost:8082/swagger-ui.html |
| Catalog Cache Service | http://localhost:8083 |

Use `edge-gateway` for normal API access. The direct service ports are exposed mainly for debugging and local inspection.

Demo credentials:

```text
Keycloak admin: admin / admin
Demo user:      testuser / password
Client:         demo-client / demo-secret
```

---

## Running Tests

Each service has its own test suite.

```bash
cd discovery-server && mvn test
cd ../authentication-service && mvn test
cd ../catalog-data-service && mvn test
cd ../catalog-cache-service && mvn test
cd ../edge-gateway && mvn test
```

The catalog services use Testcontainers so integration tests run against real PostgreSQL and Redis containers. Docker must be running.

---

## API Walkthrough

The examples below use the gateway on port `8088`, which is the preferred way to call the system.

### 1. Get an access token

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -d "client_id=demo-client" \
  -d "client_secret=demo-secret" \
  -d "grant_type=password" \
  -d "username=testuser" \
  -d "password=password" | jq -r .access_token)
```

### 2. Authentication through the gateway

```bash
# Public endpoint
curl http://localhost:8088/api/v1/public/health

# Authenticated endpoint
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8088/api/v1/me

# Admin-protected endpoint
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8088/api/v1/admin/ping
```

### 3. Catalog write flow

```bash
# Create a book
curl -i -X POST http://localhost:8088/api/v1/books \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: my-unique-key-001" \
  -d '{"title":"Clean Code","author":"Robert Martin","isbn":"9780132350884","publishedYear":2008}'
```

The response contains the created book id. Save it for the cache demo:

```bash
BOOK_ID=<id-from-create-response>
```

Retrying with the same `Idempotency-Key` returns the original response instead of creating a duplicate:

```bash
curl -i -X POST http://localhost:8088/api/v1/books \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: my-unique-key-001" \
  -d '{"title":"Clean Code","author":"Robert Martin","isbn":"9780132350884","publishedYear":2008}'
```

### 4. Catalog read and cache-aside flow

```bash
# List books with pagination and sorting
curl "http://localhost:8088/api/v1/books?page=0&size=10&sort=createdAt,desc"

# First request: cache MISS, loads from catalog-data-service
curl -i http://localhost:8088/api/v1/books/$BOOK_ID

# Second request: cache HIT, served from Redis
curl -i http://localhost:8088/api/v1/books/$BOOK_ID

# Evict the cache entry
curl -i -X DELETE http://localhost:8088/api/v1/books/$BOOK_ID/cache \
  -H "Authorization: Bearer $TOKEN"

# Next request becomes a MISS again
curl -i http://localhost:8088/api/v1/books/$BOOK_ID
```

### 5. A couple of failure cases

```bash
# No token on protected write endpoint: expect 401
curl -i -X POST http://localhost:8088/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Clean Code","author":"Robert Martin"}'

# Validation failure: expect 400 problem response
curl -i -X POST http://localhost:8088/api/v1/books \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: validation-test-001" \
  -d '{"author":"Robert Martin"}'

# Missing book: expect 404 problem response
curl -i http://localhost:8088/api/v1/books/00000000-0000-0000-0000-000000000000
```

---

## Design Notes

**Why an edge gateway?**

The gateway gives clients one stable entry point. It keeps internal service locations out of the client, centralises routing rules, validates JWTs at the edge, and uses Eureka to route by service name.

**Why Spring Cloud Gateway Server MVC?**

The other services in this project use Spring MVC, so Gateway Server MVC keeps the programming model consistent while still showing API gateway routing, load-balanced service lookup, and edge security.

**Why Eureka?**

Eureka lets services register themselves and lets the gateway route with names like `lb://catalog-data-service` instead of hard-coded container addresses. In a small demo this may look like extra infrastructure, but it makes the routing model closer to a real microservice setup.

**Why RestClient instead of Feign?**

The cache service only needs a small synchronous internal call to `catalog-data-service`. Spring's `RestClient` keeps that simple without adding Feign just for one client.

**Why `service_healthy` in Docker Compose?**

`depends_on` by itself only waits until a container starts. It does not mean the process inside is ready. The catalog service runs Flyway migrations on startup, so PostgreSQL needs to be accepting connections first.

**Why cache TTL is 5 minutes?**

Book catalog data does not change often. A five-minute TTL is enough to show the cache-aside pattern without keeping stale data around for too long. For frequently changing data, a shorter TTL or event-driven invalidation would be safer.

**Why not cache missing books?**

The cache service deliberately does not cache 404 responses. If a book is created shortly after a failed lookup, caching the miss would keep returning 404 until the TTL expires.

**Why Testcontainers instead of H2?**

The project uses PostgreSQL in runtime, so tests should catch PostgreSQL-specific behavior as early as possible. H2 is useful for simple tests, but it can hide dialect and migration problems.

---

## Postman Collection

A Postman collection is included for the main scenarios: token retrieval, gateway calls, idempotency replay, cache hit/miss behavior, validation errors, 401 checks, and direct-service debug requests.

```text
Secure_Microservices_Reference_Edge_Gateway.postman_collection.json
```

Import the collection, run **Get Token** first, then run the **Edge Gateway** folder for the normal API flow. The direct service folders are there for debugging.

---

## Project Structure

```text
secure-microservices-reference/
├── docker-compose.yml
├── keycloak/
│   └── realm-export.json
├── discovery-server/
│   ├── Dockerfile
│   └── src/
│       └── main/java/.../
│           └── DiscoveryServerApplication
├── edge-gateway/
│   ├── Dockerfile
│   └── src/
│       └── main/java/.../
│           ├── EdgeGatewayApplication
│           └── config/
│               └── SecurityConfig
├── authentication-service/
│   ├── Dockerfile
│   └── src/
│       └── main/java/.../
│           ├── config/
│           ├── controller/
│           └── converter/
├── catalog-data-service/
│   ├── Dockerfile
│   └── src/
│       └── main/
│           ├── java/.../
│           │   ├── config/
│           │   ├── controller/
│           │   ├── service/
│           │   ├── repository/
│           │   ├── entity/
│           │   ├── dto/
│           │   ├── filter/
│           │   └── exception/
│           └── resources/
│               └── db/migration/
└── catalog-cache-service/
    ├── Dockerfile
    └── src/
        └── main/java/.../
            ├── config/
            ├── controller/
            ├── service/
            ├── client/
            ├── response/
            └── exception/
```

---

## About

Built by [Rabia Yurdakul Telef](https://www.linkedin.com/in/rabia-yurdakul-telef-889196a7), a Java backend engineer focused on Spring Boot microservices, OAuth2/JWT security, Keycloak, Redis, and production-style backend architecture.

[GitHub](https://github.com/rabiayurdakul) · [Udemy](https://www.udemy.com/user/rabia-yurdakul-telef-3/)
