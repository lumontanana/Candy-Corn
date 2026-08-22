# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the full test suite (H2 in-memory, no Docker needed)
./mvnw.cmd test

# Run a single test class
./mvnw.cmd test -Dtest=OrderServiceTest
# Package-glob patterns for -Dtest (e.g. 'com.foo.**') are unreliable with this
# Surefire setup and silently under-run — prefer exact class names, or omit
# -Dtest to run everything.

# Compile only / full verify (what CI runs)
./mvnw.cmd compile
./mvnw.cmd verify

# Run the app locally (needs Postgres)
cp .env.example .env
docker compose up -d
./mvnw.cmd spring-boot:run
```

On non-Windows shells, use `./mvnw` instead of `./mvnw.cmd`.

## Architecture

Layered, one-way dependency flow: `controller -> service -> repository -> entity`. Entities are never returned from a controller — every response goes through a DTO (Java `record`) built via a static `from(entity)` factory (e.g. `ProductResponse.from(product)`).

### Package layout

`catalog` and `order` are **sibling** top-level domain packages under `com.candycorn.shop` (not nested inside each other), each mirroring the same `controller` / `dto` / `entity` / `repository` / `service` sub-structure. `common.exception` holds the cross-cutting error types and `GlobalExceptionHandler`. A `user` package and `common.validation`/`common.response` are anticipated by the plan but don't exist yet — see `docs/BACKEND_PLAN.md`.

### Validation happens at two layers, deliberately

- **Service layer**: validates request-boundary concerns (pagination bounds, required fields, price ranges) and throws `InvalidRequestException` (→ 400) or `ResourceNotFoundException` (→ 404), both mapped to a common `ApiError` JSON shape by `GlobalExceptionHandler`.
- **Entity layer**: enforces its own invariants independent of any HTTP call (`Product.changeStock` rejects negative stock, `Order.changeStatus` rejects invalid state transitions), throwing `IllegalArgumentException`/`IllegalStateException`. This is a safety net for anything that calls the entity directly (other services, batch jobs, tests) — it isn't meant to be the primary way callers hit the API 400 path, the service layer validates first so the entity check rarely triggers through HTTP.

### `Order` is an aggregate root

`OrderItem` has no repository of its own and a **package-private constructor** — it can only be created through `Order.addItem(product, quantity)`. That method also copies (`snapshots`) the product's current name and price into the item, so a historical order doesn't change if the product's price changes later. When adding order-related behavior, extend `Order`, don't reach into `OrderItem` directly.

### Dynamic filtering via Specifications

Product search (`GET /api/v1/products`) composes `org.springframework.data.jpa.domain.Specification` predicates (`ProductSpecifications.isActive()`, `.nameContains(...)`, etc.) with `.and(...)` in `ProductService`, rather than a derived-query-method explosion or hand-built JPQL. `ProductRepository` extends `JpaSpecificationExecutor<Product>` to support this.

### Schema ownership: Flyway vs. Hibernate

`spring.jpa.hibernate.ddl-auto=validate` in the real app — the schema is owned entirely by Flyway migrations (`src/main/resources/db/migration/V*.sql`); Hibernate only checks the entity mappings match. **Tests run differently**: `application-test.properties` sets `ddl-auto=create-drop` against H2 in PostgreSQL-compatibility mode with `spring.flyway.enabled=false`, so the test schema is generated straight from the entities and never touches the migration files. When you change a table, update both the entity mapping and the corresponding `V*.sql` migration by hand — nothing will catch a mismatch between them in the test run.

### Jackson 3, not classic Jackson

This project (Spring Boot 4.1 / `spring-boot-starter-webmvc`) pulls in **Jackson 3**, whose Maven coordinates and packages moved from `com.fasterxml.jackson.*` to `tools.jackson.*` (e.g. `tools.jackson.databind.ObjectMapper`). Code or examples written against classic Jackson 2 imports will not compile here.

### No auth (yet), on purpose

There is no Spring Security in this codebase — see `docs/BACKEND_PLAN.md` for why (educational/local-execution phase; admin endpoints, when added, live under `/api/v1/admin` and are considered local-only). Anywhere access needs to be scoped to "the requesting customer" without a security context, it's done by matching a plain field instead — e.g. `OrderRepository.findByIdAndCustomerEmail(id, email)`. Don't assume a `SecurityContext`/`Authentication`/`Principal` is available anywhere in the request pipeline.

### Test pyramid actually in use

- **Pure unit tests** for services/entities: constructed directly with `mock(...)` dependencies, no Spring context (`OrderServiceTest`, `OrderTest`, `ProductTest`).
- **Controller slice tests**: `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())` — fast, no `@WebMvcTest`/Spring context, but you must register `GlobalExceptionHandler` manually or error responses won't be exercised.
- **One integration test** (`CatalogRepositoryTest`) uses `@SpringBootTest @ActiveProfiles("test")` against the real (H2) persistence stack; this is the only place that boots full Spring context in tests today.

### Branching

One feature branch per plan step (`feature/catalog`, `feature/order`, ...), each merged to `main` via its own PR. Keep unrelated feature work off a branch once it's ready to be PR'd — this repo has already required a history rewrite once to fix a branch that had two features mixed together.

## Reference docs

- `docs/BACKEND_PLAN.md` — architecture plan, domain rules, implementation order, entity-relationship diagram.
- `docs/INTERVIEW_NOTES.md` — syntax/theory notes on patterns used across the codebase (JPA, transactions, records, testing).
- `README.md` — stack, endpoint list, local setup instructions.
