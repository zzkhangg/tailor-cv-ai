# Implemented service contracts

JSON fields use `snake_case`. `canonical-resume.json` is fictional test data and is the shared fixture for Java and Python validation. Project and education item shapes will be fixed when those features are implemented; the collections are empty in Phase 1.

Implemented public endpoints:

- `GET /api/v1/health` returns `200 {"status":"UP"}`.
- `POST /api/v1/resumes` accepts multipart field `file` containing a text-based PDF up to 5 MB. Returns `201` with `resume_id`, `raw_text`, and `parsed_resume`.
- `GET /api/v1/resumes/{id}` returns metadata and `parsed_resume`, without original PDF bytes; unknown IDs return `404`.
- `POST /api/v1/jobs` accepts `{ "title": "optional", "description": "30 to 20000 characters" }` and returns `201` with `job_id`, title, description, and creation time.

Implemented internal endpoints: `GET /health` returns `200 {"status":"UP"}`; `POST /internal/parse` accepts `{ "text": "..." }` and returns canonical resume JSON. Spring Actuator also exposes `/actuator/health/liveness` and `/actuator/health/readiness`.

Reserved public endpoints: `POST /api/v1/tailor`, `GET /api/v1/tailor/{run_id}`, `GET /api/v1/tailor/{run_id}/preview`, and `GET /api/v1/tailor/{run_id}/pdf`. Reserved internal endpoint: `POST /internal/tailor`.

Phase 2 parsing recognizes contact details, `Summary`, comma-separated `Skills`, and experience lines in the form `Company | Title | YYYY-MM | YYYY-MM` followed by `- ` bullets. Other data is left empty rather than invented. The original PDF and extracted text are stored in PostgreSQL for later parsing improvements; the parser does not call an LLM.

Public health is a fast process check. Readiness depends on PostgreSQL and the Python health endpoint. Java maps an unavailable Python service to a safe `503`, invalid downstream response to `502`, and timeout to `504` when its client is used. No resume text, prompt, or credential is included in these errors.
