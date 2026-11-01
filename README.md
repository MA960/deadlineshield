# DeadlineShield

DeadlineShield is a project deadline and client-delay tracking platform. It combines a Spring Boot REST API, a PostgreSQL database, and a Next.js agency dashboard.

> **Version note:** The backend is configured for Spring Boot **3.5.7** and Java **21**. The frontend package currently pins Next.js **16.4.3** (with React 19.3.0), not Next.js 14. This README describes the checked-in dependency versions.

## Architecture

```text
Browser
  |
  | Next.js App Router dashboard (frontend/, port 3000)
  | TypeScript API client
  v
Spring Boot REST API (port 8080)
  | controller -> service -> repository -> JPA models
  v
PostgreSQL
```

- **Frontend:** Next.js App Router, React, TypeScript, Tailwind CSS, and Lucide icons. Routes are in `frontend/app/`, the dashboard UI is in `frontend/components/`, HTTP calls are in `frontend/services/`, and shared contracts are in `frontend/types/`.
- **Backend:** Java 21 and Spring Boot 3.5.7. Code is organized under `com.deadlineshield.backend` into `controller`, `service`, `repository`, `model`, `dto`, `exception`, and `config`.
- **Persistence:** Spring Data JPA and PostgreSQL. Hibernate currently uses `ddl-auto=update`; this is suitable for local development, not a substitute for reviewed schema migrations in production.
- **Audit trail:** Clock pauses and client reviews write audit-log records in the same service transaction as the associated state change. See [database schema and audit limitations](docs/DATABASE_SCHEMA.md#audit-log-and-immutability).

## Requirements

- Java 21
- Maven 3.9 or later
- Node.js compatible with the installed Next.js version (Node.js 20.9 or later recommended)
- npm
- PostgreSQL

## Quickstart

### 1. Create a local PostgreSQL database

Start PostgreSQL, then create the database used by the local backend:

```sql
CREATE DATABASE deadlineshield;
```

The database user configured for the backend must have permission to connect to it and create/update tables.

### 2. Start the backend

From the repository root, set the database connection environment variables and run Spring Boot. In PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/deadlineshield"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:DB_PASSWORD = "<your-local-postgres-password>"
$env:FRONTEND_ORIGIN = "http://localhost:3000"
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. `src/main/resources/application.properties` sets the default JDBC URL to the PostgreSQL `postgres` database, so the `SPRING_DATASOURCE_URL` override above points it at the newly created `deadlineshield` database. `DB_PASSWORD` must be set because the configured datasource password resolves from that variable.

### 3. Start the frontend

In another terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). Set `NEXT_PUBLIC_API_BASE_URL` if the backend is hosted somewhere other than `http://localhost:8080`.

The dashboard calls `GET /api/projects`. The project and milestone records need to exist in PostgreSQL for populated dashboard data and successful pause/review operations. The repository currently has no project-creation endpoint or seed-data script.

## Environment variables

| Variable | Component | Default / required | Purpose |
|---|---|---|---|
| `DB_PASSWORD` | Backend | Required | PostgreSQL password referenced by `spring.datasource.password`. |
| `SPRING_DATASOURCE_URL` | Backend | `jdbc:postgresql://localhost:5432/postgres` | JDBC URL. Override to select the local `deadlineshield` database. |
| `SPRING_DATASOURCE_USERNAME` | Backend | `postgres` | PostgreSQL login. |
| `SERVER_PORT` | Backend | `8080` | HTTP port (Spring's environment override for `server.port`). |
| `FRONTEND_ORIGIN` | Backend | `http://localhost:3000` | Single browser origin allowed by API CORS configuration. |
| `NEXT_PUBLIC_API_BASE_URL` | Frontend | `http://localhost:8080` | Backend base URL used by the browser API client. |

Use local shell variables or ignored `.env` files for secrets. Do not commit credentials.

## Verification

From the repository root:

```powershell
mvn test
```

From `frontend/`:

```powershell
npm run lint
npm run build
```

## API and data model

- [REST API specification](docs/API_SPECIFICATION.md)
- [PostgreSQL schema and relationships](docs/DATABASE_SCHEMA.md)
