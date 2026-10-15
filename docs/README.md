# TailorCV documentation

Documentation status as of 15 October 2026:

- [LaTeX proposal](proposal/proposal.tex) is the two-page submission source with an embedded vector GKE diagram; its [build instructions](proposal/README.md) keep generated files outside the repository.
- [Markdown proposal draft](proposal-draft.md) remains the editable content reference. GKE details and costs are provisional until cloud deployment is verified.
- [Architecture](architecture.md) separates the implemented Docker Compose architecture from the planned Kubernetes architecture.
- [Cost estimate](cost-estimate.md) records provisional assumptions and the official pricing sources that must be checked before submission.
- [Demo checklist](demo-checklist.md) covers local verification and the required four-minute cloud demonstration evidence.
- [GKE deployment guide](../infra/k8s/README.md) covers guarded cluster setup, immutable images, deployment, rubric demonstrations, troubleshooting, and teardown.
- [API and shared JSON contracts](contracts/README.md) describe the implemented public/internal endpoints and canonical resume schema.

Phases 1–4 are implemented locally. Phase 6 now has GKE manifests and guarded automation scripts, but the cloud deployment, scaling/load-balancing evidence, rollout/rollback evidence, and k6 load tests are not yet verified. Do not describe those items as complete in the proposal or demonstration until they have been run successfully.
