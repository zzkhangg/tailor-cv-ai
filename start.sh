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

# A pytest cache created on Windows can block Docker's WSL build-context scan.
rm -rf -- "$project_dir/services/ai-service/.pytest_cache"

docker compose config --quiet
docker compose up --build --wait

frontend_port="$(docker compose port frontend 80)"
api_port="$(docker compose port resume-service 8080)"
echo "TailorCV is ready."
echo "Frontend: http://localhost:${frontend_port##*:}"
echo "API health: http://localhost:${api_port##*:}/api/v1/health"
echo "Stop services with: docker compose down"
