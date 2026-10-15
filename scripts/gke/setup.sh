#!/usr/bin/env bash
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/common.sh"

require_command gcloud
require_command kubectl

if [[ "${CONFIRM_GCP_COSTS:-}" != "yes" ]]; then
  echo "This creates billable GKE, VM, disk, registry, and networking resources." >&2
  echo "Review the budget, then rerun with CONFIRM_GCP_COSTS=yes." >&2
  exit 1
fi

echo "Project: $GCP_PROJECT_ID"
echo "Cluster: $GKE_CLUSTER ($GCP_ZONE, $GKE_NUM_NODES x $GKE_MACHINE_TYPE)"
echo "Artifact Registry: $AR_REPOSITORY ($GCP_REGION)"

gcloud projects describe "$GCP_PROJECT_ID" >/dev/null
gcloud config set project "$GCP_PROJECT_ID" >/dev/null
gcloud services enable \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  compute.googleapis.com \
  container.googleapis.com \
  --project "$GCP_PROJECT_ID"

if ! gcloud artifacts repositories describe "$AR_REPOSITORY" \
  --location "$GCP_REGION" --project "$GCP_PROJECT_ID" >/dev/null 2>&1; then
  gcloud artifacts repositories create "$AR_REPOSITORY" \
    --repository-format=docker \
    --location "$GCP_REGION" \
    --description="TailorCV classroom container images" \
    --project "$GCP_PROJECT_ID"
else
  echo "Artifact Registry repository already exists."
fi

if ! gcloud container clusters describe "$GKE_CLUSTER" \
  --zone "$GCP_ZONE" --project "$GCP_PROJECT_ID" >/dev/null 2>&1; then
  gcloud container clusters create "$GKE_CLUSTER" \
    --zone "$GCP_ZONE" \
    --machine-type "$GKE_MACHINE_TYPE" \
    --num-nodes "$GKE_NUM_NODES" \
    --disk-size 30 \
    --enable-ip-alias \
    --release-channel regular \
    --workload-pool="${GCP_PROJECT_ID}.svc.id.goog" \
    --project "$GCP_PROJECT_ID" \
    --quiet
else
  echo "GKE cluster already exists."
fi

get_cluster_credentials
kubectl get nodes -o wide

echo
echo "Cluster setup is complete. Configure a GCP budget alert before continuing."
echo "Next: IMAGE_TAG=<immutable-tag> scripts/gke/build-images.sh"
