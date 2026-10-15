#!/usr/bin/env bash

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"

GCP_REGION="${GCP_REGION:-australia-southeast1}"
GCP_ZONE="${GCP_ZONE:-australia-southeast1-a}"
GKE_CLUSTER="${GKE_CLUSTER:-tailorcv-demo}"
AR_REPOSITORY="${AR_REPOSITORY:-tailorcv}"
GKE_MACHINE_TYPE="${GKE_MACHINE_TYPE:-e2-standard-2}"
GKE_NUM_NODES="${GKE_NUM_NODES:-2}"
K8S_NAMESPACE="${K8S_NAMESPACE:-tailorcv}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

require_project() {
  if [[ -z "${GCP_PROJECT_ID:-}" ]]; then
    echo "Set GCP_PROJECT_ID to the Google Cloud project that has billing enabled." >&2
    exit 1
  fi
  if [[ ! "$GCP_PROJECT_ID" =~ ^[a-z][a-z0-9-]{4,28}[a-z0-9]$ ]]; then
    echo "GCP_PROJECT_ID has an unexpected format." >&2
    exit 1
  fi
}

validate_defaults() {
  local value
  for value in "$GCP_REGION" "$GCP_ZONE" "$GKE_CLUSTER" "$AR_REPOSITORY" \
               "$GKE_MACHINE_TYPE" "$GKE_NUM_NODES" "$K8S_NAMESPACE"; do
    if [[ ! "$value" =~ ^[a-zA-Z0-9._-]+$ ]]; then
      echo "Deployment setting contains unsupported characters: $value" >&2
      exit 1
    fi
  done
  if [[ ! "$GKE_NUM_NODES" =~ ^[1-9][0-9]*$ ]]; then
    echo "GKE_NUM_NODES must be a positive integer." >&2
    exit 1
  fi
}

validate_image_tag() {
  local tag="$1"
  if [[ -z "$tag" || "$tag" == "latest" || ! "$tag" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
    echo "Use a valid immutable IMAGE_TAG; the tag 'latest' is not allowed." >&2
    exit 1
  fi
}

image_registry() {
  printf '%s-docker.pkg.dev/%s/%s' "$GCP_REGION" "$GCP_PROJECT_ID" "$AR_REPOSITORY"
}

get_cluster_credentials() {
  gcloud container clusters get-credentials "$GKE_CLUSTER" \
    --zone "$GCP_ZONE" --project "$GCP_PROJECT_ID" >/dev/null
}

require_project
validate_defaults
