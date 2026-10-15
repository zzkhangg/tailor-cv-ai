#!/usr/bin/env bash
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/common.sh"

require_command gcloud
require_command kubectl

if [[ "${CONFIRM_DELETE:-}" != "DELETE_TAILORCV_CLUSTER" ]]; then
  echo "This deletes the public LoadBalancer, cluster, Pods, PVC, and PostgreSQL data." >&2
  echo "Rerun with CONFIRM_DELETE=DELETE_TAILORCV_CLUSTER." >&2
  exit 1
fi

if gcloud container clusters describe "$GKE_CLUSTER" \
  --zone "$GCP_ZONE" --project "$GCP_PROJECT_ID" >/dev/null 2>&1; then
  get_cluster_credentials
  kubectl delete namespace "$K8S_NAMESPACE" --ignore-not-found --wait=true --timeout=5m
  gcloud container clusters delete "$GKE_CLUSTER" \
    --zone "$GCP_ZONE" --project "$GCP_PROJECT_ID" --quiet
else
  echo "GKE cluster does not exist; skipping cluster deletion."
fi

if [[ "${DELETE_ARTIFACT_REPOSITORY:-no}" == "yes" ]]; then
  gcloud artifacts repositories delete "$AR_REPOSITORY" \
    --location "$GCP_REGION" --project "$GCP_PROJECT_ID" --quiet
else
  echo "Artifact Registry retained. Set DELETE_ARTIFACT_REPOSITORY=yes to delete it too."
fi

echo "Teardown complete. Check the Google Cloud Console for remaining billable resources."
