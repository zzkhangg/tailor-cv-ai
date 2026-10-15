# TailorCV demonstration checklist

The assessed presentation is limited to four minutes, followed by two minutes of questions. Rehearse the complete sequence and keep recorded fallback evidence for cloud operations.

## Before the marking session

- [ ] Use only a clearly fictional PDF and job description.
- [ ] Confirm the public URL, routing, and OpenAI quota.
- [ ] Confirm all Pods are ready and the database contains only demonstration data.
- [ ] Save the exact image tags used for the stable and update versions.
- [ ] Prepare commented commands explaining each command and parameter.
- [ ] Capture backup screenshots/logs of scaling, self-healing, rollout, and rollback.
- [ ] Confirm billing alerts and the post-demo teardown procedure.

## Four-minute presentation sequence

1. **Problem and architecture:** Explain grounded resume tailoring and show the frontend, Java orchestration/database owner, stateless Python AI service, PostgreSQL, Kubernetes Services, and external model.
2. **Functional workflow:** Upload a fictional PDF, confirm parsed fields, save a job description, tailor, review changes/missing requirements, and download the PDF.
3. **Database:** Show resumes, job descriptions, and tailoring runs. Explain that original PDFs are stored as bounded `BYTEA` and exports are stateless.
4. **Cloud behavior:** Show replicas and Services, scale a backend, delete one Pod and observe replacement, then show rollout history and rollback.
5. **Results and limitations:** State that the parser needs selectable text, AI output requires review, OpenAI is an external dependency, there is no authentication, and a single PostgreSQL Pod is not highly available.

## Local validation

```bash
# Validate Compose interpolation without starting containers.
docker compose config --quiet

# Build the stack and run the fictional Phase 2 smoke workflow.
./start.sh

# Run exactly one paid, fictional Phase 4 tailoring/export smoke test.
RUN_PHASE4_SMOKE=true ./start.sh

# Stop containers while retaining the local database volume.
docker compose down
```

Run the frontend tests/build, Python pytest suite, and Maven tests using the commands in the top-level README. Do not run the paid smoke workflow without an intentionally configured key.

## Planned Kubernetes evidence

These commands are examples until the Phase 6 manifests and actual resource names exist. Add comments to the submitted/demo command sheet and never delete resources outside the demo namespace.

```bash
# Display desired/ready replicas and service endpoints in the TailorCV namespace.
kubectl get deployments,pods,services -n tailorcv

# Increase Java API capacity from two to three Pods without stopping the Service.
kubectl scale deployment/resume-service --replicas=3 -n tailorcv

# Observe rollout completion after applying a tested, immutable image tag.
kubectl rollout status deployment/resume-service -n tailorcv

# Display revision history, then return to the previous working revision.
kubectl rollout history deployment/resume-service -n tailorcv
kubectl rollout undo deployment/resume-service -n tailorcv
```

Also record requests succeeding while multiple Pods serve traffic and while one selected backend Pod is deleted and replaced. Run k6 only against a non-LLM endpoint. Do not claim these criteria are complete until the commands have been executed successfully in the cloud.
