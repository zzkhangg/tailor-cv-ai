# Phase 1 architecture

The browser reaches the frontend on localhost:3000. Nginx serves the React build and forwards `/api/` to `resume-service`. Java owns the public API and PostgreSQL schema. Java checks Python over HTTP at the Compose service name `ai-service`; Python has no database connection. PostgreSQL and Python have no published host ports. This leaves stable UUID relationships for a later application tracker.

Java liveness checks only its process. Readiness checks the database and the Python `/health` endpoint. The direct public `/api/v1/health` is deliberately fast and independent of AI. The original PDF storage decision is PostgreSQL `BYTEA` under a future 5 MB upload limit; no uploads are accepted in Phase 1. Export will be generated on demand from JSON.
