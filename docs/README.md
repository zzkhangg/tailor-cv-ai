# TailorCV documentation

Documentation status as of 20 October 2026:

- [LaTeX proposal](proposal/proposal.tex) is the two-page submission source with an embedded vector GKE diagram; its [build instructions](proposal/README.md) keep generated files outside the repository.
- [Markdown proposal draft](proposal-draft.md) remains the editable content reference. Cloud cost is a planning estimate, not an observed bill.
- [Architecture](architecture.md) separates local Compose from the GKE deployment topology.
- [Cost estimate](cost-estimate.md) records dated assumptions and official pricing sources to check before submission.
- [Demo checklist](demo-checklist.md) covers local verification and the required four-minute cloud demonstration evidence.
- [GKE deployment guide](../infra/k8s/README.md) covers guarded cluster setup, immutable images, deployment, rubric demonstrations, troubleshooting, and teardown.
- [API and shared JSON contracts](contracts/README.md) describe the implemented public/internal endpoints and canonical resume schema.

Phases 1–4 are implemented locally. Phase 6 has GKE manifests and guarded automation scripts; the project operator reports having run the cluster and tested the rollout/rollback. This repository does not independently verify the live cluster, recorded demo outputs, billing configuration, or k6 results. Confirm those items before presenting them as measured results.
