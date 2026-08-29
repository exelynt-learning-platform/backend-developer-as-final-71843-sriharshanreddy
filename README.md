# Resource Booking System

A RESTful API for booking shared resources (rooms, vehicles, equipment), built with **Spring Boot 3 / Java 17**, **Spring Security + JWT**, and **JPA/Hibernate** on **PostgreSQL or MySQL**.

## Features

- JWT login via `POST /auth/login`
- Role-based access control: `ADMIN` and `USER`
  - `ADMIN`: full CRUD on resources and reservations
  - `USER`: read-only on resources; can create reservations and see only their own
- Reservation identity is always taken from the JWT `sub` claim — never trusted from the request body
- Reservation statuses: `PENDING`, `CONFIRMED`, `CANCELLED`
- Decimal reservation pricing (`BigDecimal`, precision 10, scale 2)
- Filtering by `status`, `minPrice`, `maxPrice`
- Pagination (`page`, `size`) and optional sorting (`sort=field,direction`)
- Centralized validation and structured JSON error responses
- Swagger / OpenAPI UI with JWT bearer auth built in
- Seed data: one `ADMIN` and one `USER` account, plus sample resources

## Tech Stack

| Layer          | Choice                                   |
|----------------|-------------------------------------------|
| Language       | Java 17                                   |
| Framework      | Spring Boot 3.3                           |
| Security       | Spring Security + JJWT (HS256)            |
| Persistence    | Spring Data JPA / Hibernate               |
| Database       | PostgreSQL (default) or MySQL             |
| Docs           | springdoc-openapi (Swagger UI)            |
| Build          | Maven                                     |

## Project Structure

```
src/main/java/com/booking/
  config/        SecurityConfig, OpenApiConfig, DataSeeder
  security/      JwtUtil, JwtAuthFilter, CustomUserDetailsService
  model/         User, ResourceEntity, Reservation, Role, ReservationStatus
  repository/    UserRepository, ResourceRepository, ReservationRepository
  dto/           Request/response payloads
  service/       AuthService, ResourceService, ReservationService
  controller/    AuthController, ResourceController, ReservationController
  exception/     GlobalExceptionHandler + custom exceptions
```

## Prerequisites

- JDK 17+
- Maven 3.8+
- PostgreSQL 13+ **or** MySQL 8+
- (Optional) Postman, for importing `postman_collection.json`

## 1. Database Setup

### Option A — PostgreSQL (default)

```sql
CREATE DATABASE booking_db;
```

No profile needed — the default configuration targets PostgreSQL.

### Option B — MySQL

```sql
CREATE DATABASE booking_db;
```

Run with the `mysql` Spring profile active (see below). This switches the datasource URL, driver, and Hibernate dialect via `application-mysql.yml`.

## 2. Environment Variables

Copy `.env.example` to `.env` (or export the variables in your shell / IDE run configuration):

```bash
cp .env.example .env
```

| Variable             | Default (Postgres)                                   | Description                                  |
|----------------------|--------------------------------------------------------|-----------------------------------------------|
| `SERVER_PORT`        | `8080`                                                 | HTTP port                                     |
| `DB_URL`             | `jdbc:postgresql://localhost:5432/booking_db`          | JDBC URL                                      |
| `DB_USERNAME`        | `postgres`                                             | DB username                                   |
| `DB_PASSWORD`        | `postgres`                                             | DB password                                   |
| `DB_DRIVER`          | `org.postgresql.Driver`                                | JDBC driver class                             |
| `DB_DIALECT`         | `org.hibernate.dialect.PostgreSQLDialect`              | Hibernate dialect                             |
| `DDL_AUTO`           | `update`                                               | Hibernate `ddl-auto` (`validate` recommended in prod) |
| `SHOW_SQL`           | `false`                                                | Log SQL statements                            |
| `JWT_SECRET`         | *(sample key included — replace in production)*       | Base64-encoded HMAC secret (≥256-bit for HS256) |
| `JWT_EXPIRATION_MS`  | `86400000` (24h)                                       | Token lifetime in milliseconds                |

Generate a strong secret:

```bash
openssl rand -base64 32
```

> ⚠️ The default `JWT_SECRET` in `application.yml` is provided only so the app runs out of the box for evaluation. **Always override it via environment variable in any real deployment.**

## 3. Build & Run

```bash
# PostgreSQL (default)
mvn clean install
mvn spring-boot:run

# MySQL
mvn clean install
SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run
```

Or run the packaged jar:

```bash
java -jar target/resource-booking-system-1.0.0.jar
# with MySQL:
SPRING_PROFILES_ACTIVE=mysql java -jar target/resource-booking-system-1.0.0.jar
```

The app starts on `http://localhost:8080` (or `$SERVER_PORT`).

## 4. Seed Users

Created automatically on first startup (`DataSeeder`):

| Username | Password    | Role  |
|----------|-------------|-------|
| `admin`  | `Admin@123` | ADMIN |
| `user`   | `User@123`  | USER  |

Three sample resources (a room, a vehicle, and equipment) are also seeded.

## 5. API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Postman: import `postman_collection.json` (includes a login request that auto-captures the JWT into a collection variable)

In Swagger UI, click **Authorize** and paste `Bearer <token>` (or just the raw token, depending on your springdoc version) after logging in via `/auth/login`.

## 6. API Reference

### Auth

| Method | Path          | Access | Description        |
|--------|---------------|--------|---------------------|
| POST   | `/auth/login` | Public | Returns a JWT token |

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
```

### Resources

| Method | Path                  | Access       | Description               |
|--------|-----------------------|--------------|----------------------------|
| GET    | `/api/resources`      | Any user     | Paginated list             |
| GET    | `/api/resources/{id}` | Any user     | Get one resource           |
| POST   | `/api/resources`      | ADMIN only   | Create resource            |
| PUT    | `/api/resources/{id}` | ADMIN only   | Update resource            |
| DELETE | `/api/resources/{id}` | ADMIN only   | Delete resource            |

### Reservations

| Method | Path                              | Access                              | Description                             |
|--------|-----------------------------------|--------------------------------------|------------------------------------------|
| POST   | `/api/reservations`               | Any user                             | Create reservation (owner = JWT subject) |
| GET    | `/api/reservations`               | Any user                             | ADMIN sees all, USER sees only their own |
| GET    | `/api/reservations/{id}`          | Owner or ADMIN                       | Get one reservation                      |
| PUT    | `/api/reservations/{id}`          | Owner or ADMIN                       | Update reservation                       |
| PATCH  | `/api/reservations/{id}/status`   | ADMIN only                           | Change status directly                   |
| POST   | `/api/reservations/{id}/cancel`   | Owner or ADMIN                       | Cancel a reservation                     |
| DELETE | `/api/reservations/{id}`          | Owner or ADMIN                       | Delete a reservation                     |

**Query parameters on `GET /api/reservations`:**

- `status` — `PENDING` | `CONFIRMED` | `CANCELLED`
- `minPrice`, `maxPrice` — decimal bounds (inclusive)
- `page`, `size` — pagination (zero-indexed page)
- `sort` — e.g. `sort=price,desc` or `sort=startTime,asc` (repeatable for multi-field sort)

Example:

```bash
curl "http://localhost:8080/api/reservations?status=PENDING&minPrice=20&maxPrice=100&page=0&size=10&sort=price,desc" \
  -H "Authorization: Bearer <token>"
```

### Create a reservation

```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Authorization: Bearer <user_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceId": 1,
    "startTime": "2026-09-01T10:00:00",
    "endTime": "2026-09-01T12:00:00",
    "price": 49.99
  }'
```

Note there is **no** `userId`/`username` field in the request — the server derives the owner from the authenticated JWT subject, so a user can never book on behalf of someone else.

## 7. Error Response Format

All errors return a consistent JSON shape:

```json
{
  "timestamp": "2026-08-29T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": ["price must not be negative"]
}
```

| Status | Scenario                                             |
|--------|-------------------------------------------------------|
| 400    | Validation errors, invalid business rules             |
| 401    | Missing/invalid/expired JWT, bad login credentials    |
| 403    | Authenticated but insufficient role/ownership         |
| 404    | Resource or reservation not found                     |
| 500    | Unexpected server error                                |

## 8. Security Notes

- Passwords are hashed with BCrypt.
- Stateless sessions — every request is authenticated via the `Authorization: Bearer <token>` header.
- Endpoint-level role checks are enforced both in `SecurityConfig` (URL matchers) and with `@PreAuthorize` at the method level, plus explicit ownership checks in `ReservationService` for defense in depth.
- CORS is open (`*`) for ease of testing — restrict `allowedOriginPatterns` in `SecurityConfig` before deploying.

## 9. Running Tests

```bash
mvn test
```

Tests run against an in-memory H2 database (see `src/test/resources/application.yml`) and cover authentication, RBAC enforcement, and reservation ownership.

## 10. Switching `ddl-auto` for Production

`update` is convenient for development but not recommended for production. Set `DDL_AUTO=validate` (or `none`) and manage schema changes with a migration tool (Flyway/Liquibase) in a real deployment.
