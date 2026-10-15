#!/usr/bin/env bash
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/common.sh"

require_command gcloud
require_command git

IMAGE_TAG="${IMAGE_TAG:-$(git -C "$repo_root" rev-parse --short=12 HEAD)}"
validate_image_tag "$IMAGE_TAG"
registry="$(image_registry)"

gcloud artifacts repositories describe "$AR_REPOSITORY" \
  --location "$GCP_REGION" --project "$GCP_PROJECT_ID" >/dev/null

echo "Building immutable image tag: $IMAGE_TAG"
gcloud builds submit "$repo_root/frontend" \
  --tag "$registry/frontend:$IMAGE_TAG" --project "$GCP_PROJECT_ID"
gcloud builds submit "$repo_root/services/resume-service" \
  --tag "$registry/resume-service:$IMAGE_TAG" --project "$GCP_PROJECT_ID"
gcloud builds submit "$repo_root/services/ai-service" \
  --tag "$registry/ai-service:$IMAGE_TAG" --project "$GCP_PROJECT_ID"

echo
echo "Images pushed to $registry with tag $IMAGE_TAG"
echo "Next: IMAGE_TAG=$IMAGE_TAG scripts/gke/deploy.sh"
