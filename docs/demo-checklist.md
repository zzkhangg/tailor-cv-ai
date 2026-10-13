# Phase 1 local demonstration

1. Run `docker compose up --build --wait`.
2. Open `http://localhost:3000` and confirm API availability.
3. Request `http://localhost:8080/api/v1/health`, `/actuator/health/liveness`, and `/actuator/health/readiness`.
4. Inspect `docker compose ps`; Python and PostgreSQL are internal services.

Cluster scaling and rollback demonstrations are reserved for later phases.
