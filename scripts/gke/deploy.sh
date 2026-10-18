#!/usr/bin/env bash
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/common.sh"

require_command gcloud
require_command kubectl
require_command git

IMAGE_TAG="${IMAGE_TAG:-$(git -C "$repo_root" rev-parse --short=12 HEAD)}"
validate_image_tag "$IMAGE_TAG"
secret_file="${K8S_SECRET_FILE:-$repo_root/.env.k8s}"

if [[ ! -f "$secret_file" ]]; then
  echo "Missing $secret_file. Create it with POSTGRES_PASSWORD and OPENAI_API_KEY." >&2
  exit 1
fi
if ! grep -Eq '^POSTGRES_PASSWORD=.+$' "$secret_file"; then
  echo "$secret_file must contain a non-empty POSTGRES_PASSWORD." >&2
  exit 1
fi
if ! grep -Eq '^OPENAI_API_KEY=.+$' "$secret_file"; then
  echo "$secret_file must contain a non-empty OPENAI_API_KEY." >&2
  exit 1
fi

get_cluster_credentials
kubectl create namespace "$K8S_NAMESPACE" \
  --dry-run=client -o yaml | kubectl apply -f -

existing_deployment=false
if kubectl get deployment resume-service -n "$K8S_NAMESPACE" >/dev/null 2>&1; then
  existing_deployment=true
fi

new_postgres_password="$(kubectl create secret generic tailorcv-secrets \
  --namespace "$K8S_NAMESPACE" --from-env-file="$secret_file" \
  --dry-run=client -o jsonpath='{.data.POSTGRES_PASSWORD}')"
existing_postgres_password="$(kubectl get secret tailorcv-secrets \
  --namespace "$K8S_NAMESPACE" -o jsonpath='{.data.POSTGRES_PASSWORD}' \
  2>/dev/null || true)"
if [[ -n "$existing_postgres_password" && \
      "$existing_postgres_password" != "$new_postgres_password" ]]; then
  echo "POSTGRES_PASSWORD differs from the initialized cluster Secret." >&2
  echo "Keep the original password or intentionally reset the demo namespace and database." >&2
  exit 1
fi

kubectl create secret generic tailorcv-secrets \
  --namespace "$K8S_NAMESPACE" \
  --from-env-file="$secret_file" \
  --dry-run=client -o yaml | kubectl apply -f -

render_dir="$(mktemp -d)"
trap 'rm -rf -- "$render_dir"' EXIT
cp -R "$repo_root/infra/k8s/." "$render_dir/"

registry="$(image_registry)"
sed -i \
  -e "s|newName: tailorcv/frontend|newName: $registry/frontend|" \
  -e "s|newName: tailorcv/resume-service|newName: $registry/resume-service|" \
  -e "s|newName: tailorcv/ai-service|newName: $registry/ai-service|" \
  -e "s|newTag: dev|newTag: $IMAGE_TAG|g" \
  "$render_dir/kustomization.yaml"
sed -i "s|^namespace: tailorcv$|namespace: $K8S_NAMESPACE|" \
  "$render_dir/kustomization.yaml"
sed -i "s|^  name: tailorcv$|  name: $K8S_NAMESPACE|" \
  "$render_dir/namespace.yaml"

kubectl apply -k "$render_dir"

# Older deployments included these PDBs; apply does not remove deleted manifests.
kubectl delete pdb frontend resume-service ai-service \
  -n "$K8S_NAMESPACE" --ignore-not-found

if [[ "$existing_deployment" == "true" ]]; then
  kubectl rollout restart deployment/ai-service deployment/resume-service \
    -n "$K8S_NAMESPACE"
fi

kubectl rollout status statefulset/postgres -n "$K8S_NAMESPACE" --timeout=5m
kubectl rollout status deployment/ai-service -n "$K8S_NAMESPACE" --timeout=5m
kubectl rollout status deployment/resume-service -n "$K8S_NAMESPACE" --timeout=8m
kubectl rollout status deployment/frontend -n "$K8S_NAMESPACE" --timeout=5m

external_address=""
for _ in $(seq 1 60); do
  external_address="$(kubectl get service frontend -n "$K8S_NAMESPACE" \
    -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || true)"
  if [[ -z "$external_address" ]]; then
    external_address="$(kubectl get service frontend -n "$K8S_NAMESPACE" \
      -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true)"
  fi
  [[ -n "$external_address" ]] && break
  sleep 10
done

kubectl get deployments,statefulsets,pods,services -n "$K8S_NAMESPACE" -o wide
echo
if [[ -n "$external_address" ]]; then
  echo "TailorCV: http://$external_address"
  echo "Phase 2 smoke: python3 tests/phase2_smoke.py --api-url http://$external_address"
  echo "Paid fictional Phase 4 smoke: python3 tests/phase4_smoke.py --api-url http://$external_address"
else
  echo "The LoadBalancer address is still pending. Check:" >&2
  echo "kubectl get service frontend -n $K8S_NAMESPACE --watch" >&2
fi
