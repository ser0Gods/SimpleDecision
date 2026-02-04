# SimpleDecision

An example full-stack application that follows a decision graph question-by-question.

Backend: Spring Boot (Java 17) with H2 database and JPA.
Frontend: Vue 3 + Vite (Element Plus for styling).

## Features
- Pet recommendation flow using 3–5 narrowing questions
- Questions and answers stored in a local H2 database
- Each question has related answers; each answer can point to the next question (or end)
- Frontend displays the current question and related answers; selecting an answer moves to the next question
- User’s chosen answers are persisted per-session in the database and can be viewed at any point

## Running

1. Build frontend into backend resources
   - From `frontend/`: `npm install` then `npm run build`

2. Run backend
   - From `backend/`: `mvn spring-boot:run`
   - App will be available at http://localhost:8080

The frontend is served by Spring Boot (prebuilt into `backend/src/main/resources/public`).

### Configuring the backend base address (frontend)

The frontend uses a configurable base URL for API requests.

- Default: `http://localhost:8080/`
- Override: set `VITE_API_BASE` in a Vite env file (for example: `frontend/.env`, `frontend/.env.development`, or `frontend/.env.production`).

Example `frontend/.env`:

```
VITE_API_BASE=http://api.example.com/
```

The app’s API paths (like `api/graph/...`) are automatically resolved against this base URL.

H2 console is available at: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/decisiondb` (as configured)
- Username: `sa`
- Password: empty

## API documentation (OpenAPI / Swagger)
- Swagger UI: http://localhost:8080/swagger-ui.html (or http://localhost:8080/swagger-ui/index.html)
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml

## API (used by the frontend)
- GET `/api/graph/processes` → list of available processes to start (each item: `{ id, name }`)
- POST `/api/graph/process/{processId}/start` → start the selected process; returns its starting questions as an array
- GET `/api/graph/current` → array of current active questions with answers; returns `null` if multiple processes exist and none selected; auto-starts if only one process exists
- POST `/api/graph/answer/{answerId}` → select an answer; returns updated array of current questions (empty array means process finished)
- GET `/api/graph/history` → list of selected answers for the current session (persisted)
- POST `/api/graph/reset` → reset current session (clears persisted answers for this session)

### Admin CRUD API
Endpoints to manage all models. All under `/api/admin`:

- Questions
  - GET `/questions` → list questions
  - GET `/questions/{id}` → get question
  - POST `/questions` → create question `{ text, root }`
  - PUT `/questions/{id}` → update question `{ text, root }`
  - DELETE `/questions/{id}` → delete question (cascades answers)

- Answers
  - GET `/answers` → list answers
  - GET `/answers/{id}` → get answer
  - POST `/answers` → create `{ text, questionId, nextQuestionId? }`
  - PUT `/answers/{id}` → update `{ text, questionId, nextQuestionId? }`
  - DELETE `/answers/{id}` → delete answer

- Processes
  - GET `/processes` → list processes
  - GET `/processes/{id}` → get process
  - POST `/processes` → create `{ name, startingQuestionIds?[] }`
  - PUT `/processes/{id}` → update `{ name, startingQuestionIds?[] }`
  - DELETE `/processes/{id}` → delete process

- Answer Records
  - GET `/answer-records` → list answer records
  - GET `/answer-records/{id}` → get answer record
  - DELETE `/answer-records/{id}` → delete answer record
