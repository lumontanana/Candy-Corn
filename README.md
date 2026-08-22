<div align="center">

# 🍬 CandyCorn Shop API

**API REST para una tienda de chuches, construida como proyecto educativo de backend con Spring Boot.**

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-migrations-CC0200?logo=flyway&logoColor=white)](https://flywaydb.org/)
[![Maven](https://img.shields.io/badge/Maven-build-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![CI](https://github.com/lumontanana/shop/actions/workflows/ci.yml/badge.svg)](https://github.com/lumontanana/shop/actions/workflows/ci.yml)
[![Estado](https://img.shields.io/badge/estado-en%20desarrollo-yellow)](docs/BACKEND_PLAN.md)

</div>

---

## ✨ Qué es esto

Un backend REST hecho desde cero para practicar diseño de una API real: capas bien separadas, entidades JPA con invariantes de dominio, migraciones versionadas, DTOs inmutables, manejo de errores centralizado y tests en varios niveles. Sin frontend, sin autenticación (todavía) — el foco está puesto en que el backend esté bien construido.

El plan completo, con el orden de implementación y las decisiones de diseño, vive en [`docs/BACKEND_PLAN.md`](docs/BACKEND_PLAN.md).

## 🧰 Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 4.1 (Web MVC + Data JPA) |
| Base de datos | PostgreSQL 17 (H2 en memoria para tests) |
| Migraciones | Flyway |
| JSON | Jackson 3 |
| Build | Maven |
| Tests | JUnit 5 · Mockito · AssertJ · MockMvc |
| CI | GitHub Actions |

## 🏗️ Arquitectura

```text
controller  →  service  →  repository  →  entity
   (HTTP)     (casos de uso,   (Spring Data JPA)   (JPA + reglas
              transacciones)                        de dominio)
```

```text
com.candycorn.shop
├── common
│   └── exception        # ApiError, GlobalExceptionHandler, excepciones de dominio
├── catalog               # categorías, productos, stock, búsqueda
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
├── order                 # pedidos, líneas, direcciones, estados
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
└── ShopApplication.java
```

Las entidades nunca se exponen directamente: cada respuesta pasa por un DTO (`record` de Java) con un método `from(entidad)`. El esquema de relaciones completo (con los campos de cada entidad) está en [`docs/BACKEND_PLAN.md`](docs/BACKEND_PLAN.md#esquema-de-relaciones).

## 🚀 Puesta en marcha

**Requisitos**: Java 17, Docker (para PostgreSQL).

```bash
# 1. Clonar y entrar en el proyecto
git clone https://github.com/lumontanana/shop.git
cd shop

# 2. Variables de entorno
cp .env.example .env

# 3. Levantar PostgreSQL
docker compose up -d

# 4. Arrancar la aplicación (aplica las migraciones de Flyway automáticamente)
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

```bash
# Ejecutar la suite de tests (usa H2 en memoria, no necesita Docker)
./mvnw test
```

## 📡 Endpoints

<details open>
<summary><strong>Catálogo</strong> — públicos, sin autenticación</summary>

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/v1/products` | Lista productos activos. Filtros opcionales: `search`, `category`, `minPrice`, `maxPrice`, `page`, `size` |
| `GET` | `/api/v1/products/{id}` | Detalle de un producto por id |
| `GET` | `/api/v1/products/slug/{slug}` | Detalle de un producto por slug |
| `GET` | `/api/v1/categories` | Lista categorías activas |
| `GET` | `/api/v1/categories/{id}/products` | Productos de una categoría (paginado) |

</details>

<details open>
<summary><strong>Pedidos</strong></summary>

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/orders` | Crea un pedido: valida stock y lo reserva de forma transaccional |
| `GET` | `/api/v1/orders/{id}?email=...` | Consulta un pedido, acotado por email del cliente (no hay auth aún) |

</details>

Todos los errores siguen el mismo formato JSON (`timestamp`, `status`, `error`, `message`), generado por un `@RestControllerAdvice` central.

## 🗺️ Roadmap

- [x] **Catálogo** — categorías, productos, stock, consultas públicas
- [x] **Pedidos** — pedido, líneas con snapshot de precio, direcciones, estados
- [ ] **Administración local** — gestión de catálogo y pedidos sin autenticación, bajo `/api/v1/admin`
- [ ] **Pagos** — proveedor, webhooks y estados de pago
- [ ] **Calidad** — OpenAPI, tests de integración con Testcontainers, observabilidad

## 📎 Documentación

- [`docs/BACKEND_PLAN.md`](docs/BACKEND_PLAN.md) — plan de arquitectura, reglas de dominio y esquema de relaciones
- [`docs/INTERVIEW_NOTES.md`](docs/INTERVIEW_NOTES.md) — notas de repaso de la sintaxis y teoría usadas en el proyecto

---

<div align="center">

Proyecto personal de aprendizaje — sin afiliación con ninguna marca real.

</div>
