# Implemented service contracts

JSON fields use `snake_case`. `canonical-resume.json` is fictional test data and is the shared fixture for Java and Python validation. Project and education item shapes will be fixed when those features are implemented; the collections are empty in Phase 1.

Implemented public endpoints:

- `GET /api/v1/health` returns `200 {"status":"UP"}`.
- `POST /api/v1/resumes` accepts multipart field `file` containing a text-based PDF up to 5 MB. Returns `201` with `resume_id`, `raw_text`, and `parsed_resume`.
- `GET /api/v1/resumes/{id}` returns metadata and `parsed_resume`, without original PDF bytes; unknown IDs return `404`.
- `POST /api/v1/jobs` accepts `{ "title": "optional", "description": "30 to 20000 characters" }` and returns `201` with `job_id`, title, description, and creation time.
- `POST /api/v1/tailor` accepts `{ "resume_id": "UUID", "job_id": "UUID" }`. With AI configured, returns `201` with `run_id`, `SUCCEEDED`, `tailored_resume`, `change_summary`, and `missing_requirements`. An unconfigured provider returns `503`; unsupported claims return `422`.
- `GET /api/v1/tailor/{run_id}` returns the stored run status and result; unknown IDs return `404`.

Implemented internal endpoints: `GET /health` returns `200 {"status":"UP"}`; `POST /internal/parse` accepts `{ "text": "..." }` and returns canonical resume JSON; `POST /internal/tailor` accepts `{ "resume": { ...canonical resume... }, "job_description": "..." }` and returns `{ "tailored_resume": { ... }, "change_summary": [...], "missing_requirements": [...] }`. Spring Actuator also exposes `/actuator/health/liveness` and `/actuator/health/readiness`.

Reserved public endpoints: `GET /api/v1/tailor/{run_id}/preview` and `GET /api/v1/tailor/{run_id}/pdf`.

The default parser recognizes contact details, common summary and skills headings, categorized competencies, and experience lines in the form `Company | Title | YYYY-MM | YYYY-MM` followed by `- ` bullets. Other data is left empty rather than invented. The original PDF and extracted text are stored in PostgreSQL. The default parser does not call an LLM.

Public health is a fast process check. Readiness depends on PostgreSQL and the Python health endpoint. Java maps an unavailable Python service to a safe `503`, invalid downstream response to `502`, and timeout to `504` when its client is used. No resume text, prompt, or credential is included in these errors.

Phase 3 tailoring uses the configured OpenAI model only when `OPENAI_API_KEY` is set. The structured AI output selects original skills and experience bullets by index; the service reconstructs the resume from the original. Invalid or repeated indices and unverified missing requirements are discarded and flagged in `change_summary`. Optional AI parsing (`AI_PARSE_ENABLED=true`) extracts canonical fields from PDF text and rejects values absent from the text. The default parser remains rule-based. This deliberately conservative behavior does not rewrite prose or guarantee that the result suits a job; the user must review it.
