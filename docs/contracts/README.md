# Phase 1 contracts

JSON fields use `snake_case`. `canonical-resume.json` is fictional test data and is the shared fixture for Java and Python validation. Project and education item shapes will be fixed when those features are implemented; the collections are empty in Phase 1.

Implemented public endpoint: `GET /api/v1/health` returns `200 {"status":"UP"}`. Implemented internal endpoint: `GET /health` returns the same body. Spring Actuator also exposes `/actuator/health/liveness` and `/actuator/health/readiness`.

Reserved public endpoints: `POST /api/v1/resumes`, `GET /api/v1/resumes/{id}`, `POST /api/v1/jobs`, `POST /api/v1/tailor`, `GET /api/v1/tailor/{run_id}`, `GET /api/v1/tailor/{run_id}/preview`, and `GET /api/v1/tailor/{run_id}/pdf`. Reserved internal endpoints: `POST /internal/parse` and `POST /internal/tailor`. These are intentionally not routed in Phase 1.

Public health is a fast process check. Readiness depends on PostgreSQL and the Python health endpoint. Java maps an unavailable Python service to a safe `503`, invalid downstream response to `502`, and timeout to `504` when its client is used. No resume text, prompt, or credential is included in these errors.
