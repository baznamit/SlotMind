# SlotMind

SlotMind is a reactive appointment re-scheduling engine for CareBridge clinics. When a provider goes offline or a slot is cancelled, it asynchronously creates a validated recommendation for the front desk. A human accepts or rejects the recommendation; acceptance moves the appointment and books the replacement slot transactionally.

## Architecture

The project is a modular Spring Boot monolith (Java 17, Spring Boot 3.3, JPA, H2) with an Angular 17 front end. A shared `SchedulingStrategy` contract is selected by a runtime registry. The deterministic rule strategy is always available; the optional LLM strategy uses an `LlmGateway`, validates the response against current data, and falls back to rules on failure. Disruption events are handled asynchronously after commit.

## Prerequisites

- JDK 17
- Maven 3.9+ (or a compatible Maven installation)
- Node.js and npm compatible with Angular CLI 17
- A browser for the front desk UI
- Optional: access and credentials for the configured LiteLLM-compatible gateway

## Run and test

From the repository root (`C:\Users\zycus\Desktop\new-project-namit` on the supplied workspace):

```text
mvn clean test
mvn spring-boot:run
```

In a second terminal:

```text
cd frontend
npm ci
npm run build
npm start
```

The Angular development server uses `frontend/proxy.conf.json` to forward API calls to `http://localhost:8080`; open the URL printed by `ng serve` (normally `http://localhost:4200`). The repository's frontend test script is `npm test`; verify the local Angular CLI arguments before relying on it in automation.

## Configuration

The backend defaults to rule-based scheduling and an in-memory H2 database. Optional environment variables are:

| Variable | Purpose |
|---|---|
| `SLOTMIND_SCHEDULING_STRATEGY` | `rule-based` (default) or `llm` |
| `SLOTMIND_LLM_ENDPOINT` | LiteLLM-compatible Chat Completions endpoint |
| `SLOTMIND_LLM_MODEL` | Gateway model identifier |
| `SLOTMIND_LLM_PRODUCT` | Product header value (the configured default is `PC1`) |
| `SLOTMIND_LLM_API_KEY` | Secret gateway key; provide through the environment only |
| `SLOTMIND_LLM_TIMEOUT` | Java duration such as `PT10S` |

Do not put keys in source, Markdown, `.env` files committed to the project, logs, or `LLM_keys.txt`. No secret value is required for the default rule-based demo.

## Five-minute demo

1. Start the backend and frontend and show the seeded appointments.
2. Mark `PRV-001` (Dr. Sarah Chen) offline with `PATCH /providers/PRV-001/status` and body `{"status":"OFFLINE"}`.
3. Wait for polling or click **Refresh**. The two affected confirmed appointments become pending and receive `PROVIDER_OFFLINE` suggestions.
4. Show the recommended slot, provider, confidence, reasoning, and trigger badge.
5. Accept one suggestion. Confirm that the appointment is `RESCHEDULED`, the old slot is open, and the replacement is booked.
6. If two pending suggestions share a slot, accept the first and demonstrate that the second receives `409 Conflict`; refresh and reject the stale suggestion.

## API endpoints

- `POST /appointments` — book a provider and slot
- `GET /appointments?status=RESCHEDULE_PENDING` — list/filter appointments
- `GET /providers` and `PATCH /providers/{id}/status`
- `GET /slots` and `PATCH /slots/{id}/status`
- `POST /appointments/{id}/suggest?triggerReason=INITIAL` — manual recommendation
- `GET /suggestions?status=PENDING`
- `PATCH /suggestions/{id}` with `{"decision":"ACCEPTED"}` or `{"decision":"REJECTED"}`

Status changes return before post-commit disruption processing completes. Use polling/refresh to observe generated suggestions.

## Known limitations and submission hygiene

Pending suggestions are not slot reservations, so independent appointments may recommend the same open slot and later require rejection after one is accepted. The demo uses in-memory H2 data and in-process async handling; data and queued work do not survive a process restart. Live LLM use, a full end-to-end test run, and a complete manual demo are not claimed here unless separately verified.

`frontend/package-lock.json` is the lockfile used by the Angular project. The root `package-lock.json` is an empty npm workspace lockfile and is not used by the Maven backend or frontend commands; it is retained pending an explicit repository cleanup decision. `src.zip` is retained as an existing source archive and is not needed to build or run the application. Build outputs and local secrets are excluded by `.gitignore`.
