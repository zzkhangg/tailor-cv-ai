#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$project_dir"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required. Install Docker with the Compose plugin first." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose is required. Install the Docker Compose plugin first." >&2
  exit 1
fi

if [[ ! -f .env ]]; then
  echo "Missing .env. Copy .env.example to .env and set POSTGRES_PASSWORD, then rerun this script." >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "Python 3 is required for the automatic Phase 2 smoke test." >&2
  exit 1
fi

# Keep Windows-owned test caches out of Docker's WSL build-context scan.
build_dir="$(mktemp -d)"
trap 'rm -rf -- "$build_dir"' EXIT
mkdir -p "$build_dir/ai-service/app"
cp "$project_dir/services/ai-service/Dockerfile" \
   "$project_dir/services/ai-service/requirements.lock" "$build_dir/ai-service/"
cp "$project_dir/services/ai-service/app/"*.py "$build_dir/ai-service/app/"
cat > "$build_dir/compose.override.yaml" <<EOF
services:
  ai-service:
    build:
      context: $build_dir/ai-service
EOF

compose=(docker compose -f "$project_dir/compose.yaml" -f "$build_dir/compose.override.yaml")
"${compose[@]}" config --quiet
"${compose[@]}" up --build --wait

frontend_port="$("${compose[@]}" port frontend 80)"
api_port="$("${compose[@]}" port resume-service 8080)"
echo "TailorCV is ready."
echo "Frontend: http://localhost:${frontend_port##*:}"
echo "API health: http://localhost:${api_port##*:}/api/v1/health"
PYTHONDONTWRITEBYTECODE=1 python3 tests/phase2_smoke.py --api-url "http://localhost:${api_port##*:}"
if [[ "${RUN_PHASE4_SMOKE:-false}" == "true" ]]; then
  echo "Running one fictional Phase 4 tailoring call through the configured AI provider."
  PYTHONDONTWRITEBYTECODE=1 python3 tests/phase4_smoke.py --api-url "http://localhost:${api_port##*:}"
fi
echo "Stop services with: docker compose down"
