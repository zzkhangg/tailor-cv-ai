#!/usr/bin/env bash
set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/common.sh"

require_command gcloud
require_command kubectl

usage() {
  cat <<'EOF'
Usage: scripts/gke/demo.sh COMMAND [ARGUMENT]

Commands:
  status                 Show nodes, workloads, Pods, Services, and images.
  endpoints              Show the Pod endpoints behind each Service.
  scale [COUNT]           Scale resume-service (default: 3) and wait.
  self-heal              Delete one resume-service Pod and show replacement.
  release IMAGE_TAG      Roll resume-service to an immutable image tag.
  rollback               Roll resume-service back one Deployment revision.
EOF
}

get_cluster_credentials
command_name="${1:-}"

case "$command_name" in
  status)
    kubectl get nodes -o wide
    kubectl get deployments,statefulsets,pods,services -n "$K8S_NAMESPACE" -o wide
    kubectl get deployment resume-service -n "$K8S_NAMESPACE" \
      -o jsonpath='resume-service image: {.spec.template.spec.containers[0].image}{"\n"}'
    ;;
  endpoints)
    kubectl get endpointslices -n "$K8S_NAMESPACE" \
      -l kubernetes.io/service-name=frontend -o wide
    kubectl get endpointslices -n "$K8S_NAMESPACE" \
      -l kubernetes.io/service-name=resume-service -o wide
    kubectl get endpointslices -n "$K8S_NAMESPACE" \
      -l kubernetes.io/service-name=ai-service -o wide
    ;;
  scale)
    count="${2:-3}"
    if [[ ! "$count" =~ ^[1-9][0-9]*$ ]]; then
      echo "Scale count must be a positive integer." >&2
      exit 1
    fi
    kubectl scale deployment/resume-service --replicas="$count" -n "$K8S_NAMESPACE"
    kubectl rollout status deployment/resume-service -n "$K8S_NAMESPACE" --timeout=5m
    kubectl get pods -n "$K8S_NAMESPACE" -l app.kubernetes.io/name=resume-service -o wide
    ;;
  self-heal)
    pod="$(kubectl get pods -n "$K8S_NAMESPACE" \
      -l app.kubernetes.io/name=resume-service \
      -o jsonpath='{.items[0].metadata.name}')"
    if [[ -z "$pod" ]]; then
      echo "No resume-service Pod was found." >&2
      exit 1
    fi
    echo "Deleting $pod in namespace $K8S_NAMESPACE. The Deployment should replace it."
    kubectl delete pod "$pod" -n "$K8S_NAMESPACE"
    replacement_ready=false
    for _ in $(seq 1 60); do
      desired="$(kubectl get deployment resume-service -n "$K8S_NAMESPACE" \
        -o jsonpath='{.spec.replicas}')"
      ready="$(kubectl get deployment resume-service -n "$K8S_NAMESPACE" \
        -o jsonpath='{.status.readyReplicas}')"
      old_pod="$(kubectl get pod "$pod" -n "$K8S_NAMESPACE" \
        --ignore-not-found -o name)"
      if [[ "$ready" == "$desired" && -z "$old_pod" ]]; then
        replacement_ready=true
        break
      fi
      sleep 5
    done
    if [[ "$replacement_ready" != "true" ]]; then
      echo "Replacement Pod did not become ready within five minutes." >&2
      exit 1
    fi
    kubectl get pods -n "$K8S_NAMESPACE" -l app.kubernetes.io/name=resume-service -o wide
    ;;
  release)
    tag="${2:-}"
    validate_image_tag "$tag"
    registry="$(image_registry)"
    echo "Current image:"
    kubectl get deployment resume-service -n "$K8S_NAMESPACE" \
      -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
    kubectl set image deployment/resume-service \
      "resume-service=$registry/resume-service:$tag" -n "$K8S_NAMESPACE"
    kubectl rollout status deployment/resume-service -n "$K8S_NAMESPACE" --timeout=8m
    kubectl rollout history deployment/resume-service -n "$K8S_NAMESPACE"
    ;;
  rollback)
    echo "Image before rollback:"
    kubectl get deployment resume-service -n "$K8S_NAMESPACE" \
      -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
    kubectl rollout undo deployment/resume-service -n "$K8S_NAMESPACE"
    kubectl rollout status deployment/resume-service -n "$K8S_NAMESPACE" --timeout=8m
    echo "Image after rollback:"
    kubectl get deployment resume-service -n "$K8S_NAMESPACE" \
      -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
    kubectl rollout history deployment/resume-service -n "$K8S_NAMESPACE"
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac
