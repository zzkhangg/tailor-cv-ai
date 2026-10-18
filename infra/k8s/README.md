# TailorCV on Google Kubernetes Engine

These manifests and scripts deploy the classroom TailorCV demo to a two-node GKE Standard cluster. They have been validated locally but have **not** been executed against a Google Cloud project. Run them from Google Cloud Shell after confirming billing, quota, and budget alerts.

The deployment exposes only the frontend through a public HTTP `LoadBalancer` Service. `resume-service`, `ai-service`, and PostgreSQL use private ClusterIP Services. Do not upload real applicant data: the demo has no authentication or TLS.

## Architecture and defaults

- Region/zone: `australia-southeast1` / `australia-southeast1-a`.
- Cluster: `tailorcv-demo`, GKE Standard, regular release channel.
- Nodes: two `e2-standard-2` VMs with 30 GB boot disks.
- Replicas: one frontend, two Java, two Python; one PostgreSQL StatefulSet. Java Pods prefer different nodes.
- Database storage: 10 Gi `standard-rwo` persistent volume.
- Public entry point: frontend Service `type: LoadBalancer` on HTTP port 80.
- Images: Artifact Registry with immutable tags; `latest` is rejected.

All defaults can be overridden before running the scripts:

```bash
export GCP_REGION=australia-southeast1
export GCP_ZONE=australia-southeast1-a
export GKE_CLUSTER=tailorcv-demo
export AR_REPOSITORY=tailorcv
export GKE_MACHINE_TYPE=e2-standard-2
export GKE_NUM_NODES=2
export K8S_NAMESPACE=tailorcv
```

## 1. Prepare Google Cloud

In the Cloud Console, select a project with billing enabled, confirm quota for two VMs in Sydney, and create a budget with alert thresholds. Open Cloud Shell in the repository root and set the project ID:

```bash
export GCP_PROJECT_ID=replace-with-project-id
gcloud auth list
gcloud config set project "$GCP_PROJECT_ID"
```

Creating the cluster incurs charges. The setup script refuses to continue without explicit confirmation:

```bash
CONFIRM_GCP_COSTS=yes ./scripts/gke/setup.sh
```

The script enables the required APIs, creates the Artifact Registry repository and cluster when absent, retrieves credentials, and prints the two nodes. It is safe to rerun for the same configuration.

## 2. Build immutable images

Commit and push the simplified manifests before using a fresh Cloud Shell checkout. Check that the checkout has the intended version, then use its commit as the release identifier:

```bash
git status --short
export IMAGE_TAG="$(git rev-parse --short=12 HEAD)"
./scripts/gke/build-images.sh
```

`git status --short` should be empty so the image tag identifies the code being built. Cloud Build builds and pushes the frontend, `resume-service`, and `ai-service` images. Never use `latest`; immutable tags make rollout and rollback evidence unambiguous.

## 3. Create the local secret input

Create `.env.k8s` at the repository root. It is ignored by Git through the existing `.env.*` rule.

```dotenv
POSTGRES_PASSWORD=replace-with-a-long-random-password
OPENAI_API_KEY=replace-with-your-provider-key
```

Do not commit, paste, screenshot, or print this file. `AI_PARSE_ENABLED` remains `false`, so upload parsing is local and does not create an additional paid provider call.

Keep `POSTGRES_PASSWORD` unchanged after PostgreSQL initializes its PVC. The deploy script rejects a different password because changing only the Kubernetes Secret would not update the existing database role.

## 4. Deploy

```bash
IMAGE_TAG="$IMAGE_TAG" ./scripts/gke/deploy.sh
```

The script creates the Kubernetes Secret without printing its values, renders Artifact Registry image names in a temporary directory, applies the Kustomize resources, waits for readiness, and prints the external address. It also removes the three PDBs from the older manifest set if present; `kubectl apply` alone would leave them behind.

Check the new layout: frontend `1/1`, `resume-service` `2/2`, `ai-service` `2/2`, and PostgreSQL `1/1`. Only the frontend Service should have an external address.

```bash
kubectl get deployments,statefulsets,services -n tailorcv
kubectl get pdb -n tailorcv
```

The second command should report no PDBs. The single frontend Pod can cause a brief interruption if it restarts; use the Java Pods for scaling, self-healing, rolling update, and rollback evidence.

If the address is still pending:

```bash
kubectl get service frontend -n tailorcv --watch
```

Once assigned:

```bash
export EXTERNAL_IP="$(kubectl get service frontend -n tailorcv \
  -o jsonpath='{.status.loadBalancer.ingress[0].ip}')"
curl --fail "http://$EXTERNAL_IP/api/v1/health"
```

## 5. Validate the fictional workflow

Run the free Phase 2 smoke test through Nginx and the public LoadBalancer:

```bash
python3 tests/phase2_smoke.py --api-url "http://$EXTERNAL_IP"
```

Run the Phase 4 smoke test only when one paid provider call with fictional data is intentional:

```bash
python3 tests/phase4_smoke.py --api-url "http://$EXTERNAL_IP"
```

Open `http://$EXTERNAL_IP` in a browser and use only the generated fictional fixture for screenshots.

## 6. Demonstrate the rubric

Every command below is scoped to the `tailorcv` namespace where applicable.

```bash
# Show both worker nodes, desired/ready replicas, Services, public IP, and images.
./scripts/gke/demo.sh status

# Show the frontend endpoint and multiple Pod IPs behind the Java and Python Services.
./scripts/gke/demo.sh endpoints

# Scale the Java API from two to three Pods without stopping its Service.
./scripts/gke/demo.sh scale 3

# Delete one Java Pod and wait until a replacement is ready.
./scripts/gke/demo.sh self-heal
```

For rolling update evidence, build a second tested immutable tag, deploy it only to Java, then roll back:

```bash
export RELEASE_TAG="demo-v2-$(git rev-parse --short=12 HEAD)"
IMAGE_TAG="$RELEASE_TAG" ./scripts/gke/build-images.sh

# Update the Java Deployment with maxUnavailable=0 and show revision history.
./scripts/gke/demo.sh release "$RELEASE_TAG"

# Restore the previous image and print the image before/after rollback.
./scripts/gke/demo.sh rollback
```

While scaling or deleting a Pod, repeatedly request `http://$EXTERNAL_IP/api/v1/health` to demonstrate continued availability. Capture the commands, their comments, timestamps, ready replica counts, node placement, image tags, and successful responses. A single PostgreSQL Pod remains a documented single point of failure.

To verify persistence across a database Pod replacement, record a fictional resume ID, delete only `postgres-0`, wait for the StatefulSet, then retrieve the same ID. This tests the PVC, not database high availability.

## 7. Tear down billable resources

Teardown permanently deletes the cluster, LoadBalancer, PVC, and demonstration database. It requires an exact confirmation:

```bash
CONFIRM_DELETE=DELETE_TAILORCV_CLUSTER ./scripts/gke/teardown.sh
```

Artifact Registry is retained by default. Delete it too only when the images are no longer needed:

```bash
CONFIRM_DELETE=DELETE_TAILORCV_CLUSTER \
DELETE_ARTIFACT_REPOSITORY=yes \
./scripts/gke/teardown.sh
```

Finally, inspect Billing, Kubernetes Engine, Compute Engine, Load Balancing, Persistent Disk, and Artifact Registry in the Cloud Console to confirm no unintended resources remain.

## Troubleshooting

```bash
kubectl get events -n tailorcv --sort-by=.lastTimestamp
kubectl describe pod -n tailorcv POD_NAME
kubectl logs -n tailorcv POD_NAME
kubectl get pvc -n tailorcv
```

- `ImagePullBackOff`: confirm `IMAGE_TAG`, Artifact Registry paths, and that Cloud Build completed.
- PostgreSQL PVC pending: confirm `standard-rwo` exists with `kubectl get storageclass`.
- Java not ready: check PostgreSQL and `ai-service` first because Java readiness depends on both.
- Cloud Build permission failure: grant its build service account Artifact Registry Writer in the selected project, then retry.
- External IP pending: inspect the frontend Service events and regional address/quota limits.
