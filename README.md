# Task Management System

Spring Boot 4 + PostgreSQL REST API with a React (Vite, TypeScript) frontend. Users can create,
update, delete, assign, filter and complete tasks.

## Run it

Prerequisites: Java 21, Node 20+, Docker.

```bash
docker compose up -d                       # PostgreSQL 17 on localhost:5432

cd backend
./mvnw spring-boot:run                     # API on http://localhost:8080

cd ../frontend
npm install
npm run dev                                # UI on http://localhost:5173, proxies /api to :8080
```

If port 8080 is taken, run the backend with `SERVER_PORT=8081 ./mvnw spring-boot:run` and the
frontend with `API_PROXY_TARGET=http://localhost:8081 npm run dev`.

Database settings come from `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
(defaults match `docker-compose.yml`; see `.env.example`). Flyway creates the schema and seeds three
users (Alice, Bob, Carol) to assign tasks to.

## Tests

```bash
cd backend && ./mvnw test      # unit, WebMvc and Testcontainers (Docker required) tests
cd frontend && npm test        # Vitest + React Testing Library
cd frontend && npm run lint    # oxlint
```

## API

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/tasks?status=&priority=&assigneeId=&unassigned=&search=&page=&size=&sort=` | Paged, filtered, sorted task list |
| GET | `/api/tasks/summary` | Task counts by status for the dashboard |
| GET | `/api/tasks/{id}` | Task details |
| POST | `/api/tasks` | Create a task (201 + `Location`) |
| PUT | `/api/tasks/{id}` | Replace a task's editable fields |
| PATCH | `/api/tasks/{id}/assignee` | Assign (`{"assigneeId": 2}`) or unassign (`{"assigneeId": null}`) |
| PATCH | `/api/tasks/{id}/complete` | Mark done; 409 if already done |
| DELETE | `/api/tasks/{id}` | Delete (204) |
| GET | `/api/users` | Paged user list |
| POST | `/api/users` | Create a user; 409 on duplicate email |

Errors use RFC 9457 `application/problem+json` (`status`, `title`, `detail`, `instance`); validation
failures add `errors: [{ "field", "message" }]`. If PostgreSQL is unreachable the API answers
`503 The database is unavailable` within 5 s (`DB_CONNECTION_TIMEOUT_MS`) and recovers on its own
once the database is back. Other unexpected errors return a generic 500 and are logged server-side.

## Design notes

- **Backend layout**: packaged by feature (`task`, `user`) with `common` for the page DTO and
  exception handling. Controllers only bind and validate; services own transactions and rules;
  entities never leave the service layer (records as request/response DTOs, `TaskMapper` maps).
- **JPA**: `Task` `@ManyToOne(fetch = LAZY)` → `User`, inverse `@OneToMany(mappedBy = "assignee")`.
  The FK is `ON DELETE SET NULL`. The paged list uses an `@EntityGraph` to load assignees in the
  same query (no N+1). `@Version` gives optimistic locking (409 on concurrent write).
- **Filtering**: a JPA `Specification` composes status / priority / assignee / unassigned /
  case-insensitive text search; `%` and `_` in search input are escaped so they match literally.
- **Completion rule**: `Task.changeStatus` sets `completedAt` when a task moves to `DONE` and clears
  it when reopened. The time comes from an injected `Clock`, so tests are deterministic.
- **Frontend state**: filters, sort and page live in the URL (`useSearchParams`), so they survive
  reloads and the back button. A small `useApi` hook owns loading/error/data with `AbortController`
  cleanup; search is debounced. All HTTP goes through `src/api`, and every mutation refreshes the
  data it affects (list + summary on the dashboard, the task itself on the details page).

## Scope and assumptions

- No authentication: the brief did not ask for it, so "users" are people tasks are assigned to,
  not logged-in accounts. Users have list and create endpoints only (no edit/delete UI).
- Task fields: title (required, max 150), description (max 2000), status `TODO` / `IN_PROGRESS` /
  `DONE`, priority `LOW` / `MEDIUM` / `HIGH`, optional due date, optional assignee.
- "Complete" is a dedicated action that sets `DONE` and records `completedAt`. Completing an
  already-done task returns 409. A done task can be reopened by editing its status.
- Sorting by priority is not offered, because priority is stored as text and would sort
  alphabetically. Filter by priority instead.
- `PUT` does not take a client-supplied version, so `@Version` guards only truly concurrent writes,
  not lost updates between two open browser tabs.
