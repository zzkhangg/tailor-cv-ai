# Local architecture through Phase 2

The browser reaches the frontend on localhost:3000. Nginx serves the React build and forwards `/api/` to `resume-service`. Java owns the public API and PostgreSQL schema. Java checks Python over HTTP at the Compose service name `ai-service`; Python has no database connection. PostgreSQL and Python have no published host ports. This leaves stable UUID relationships for a later application tracker.

Java liveness checks only its process. Readiness checks the database and the Python `/health` endpoint. The direct public `/api/v1/health` is deliberately fast and independent of parsing. Phase 2 validates and extracts text from PDFs up to 5 MB, calls Python `/internal/parse`, then stores the PDF bytes in PostgreSQL `BYTEA` with raw text and JSONB. The Java service also stores job descriptions. Export will be generated on demand from JSON in a later phase.
