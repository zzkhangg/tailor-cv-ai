# Cloud cost estimate (draft)

This is a planning estimate for a small classroom GKE demonstration, not a bill or a final calculator quote. Before submission, replace provisional figures with a dated Google Cloud Pricing Calculator export for the selected region and configuration. Prices exclude taxes and are shown in USD.

## Assumptions

- Region: `australia-southeast1` (Sydney), to be confirmed.
- GKE Standard cluster with two small worker nodes for multi-node orchestration evidence.
- Approximately 730 running hours per month. For the course demo, the cluster will run only when needed.
- Two replicas each for the frontend, `resume-service`, and `ai-service` during demonstrations.
- One PVC-backed PostgreSQL Pod; this is economical but not highly available.
- Low traffic, 10 GB persistent data, 5 GB image storage, and 5 GB internet egress.
- A small number of `gpt-4o-mini` tailoring requests. LLM usage is usage-based and is not load-tested.

## Provisional monthly estimate

| Resource | Planning assumption | Provisional USD/month |
|---|---|---:|
| GKE management | One cluster; account for the cluster management fee/free-tier credit as applicable | $0–73 |
| Compute Engine nodes | Two general-purpose nodes running 730 hours | $120–190 |
| Persistent disk | 10 GB balanced persistent disk | $1–2 |
| External HTTP(S) load balancer | One forwarding rule plus low-volume processing | $18–25 |
| Artifact Registry | About 5 GB of container images | $0.50–1 |
| Network egress | About 5 GB, destination-dependent | $1–2 |
| OpenAI API | Demo-scale `gpt-4o-mini` input/output | Less than $1 |
| **Estimated total** | Before tax; depends primarily on nodes and GKE fee | **$141–294/month** |

The range is intentionally conservative because the exact node type, cluster mode, free-tier eligibility, and load-balancer usage have not been finalized. The final proposal must state one configuration and one calculated total rather than presenting this range.

## Cost controls

- Develop and run automated tests locally with Docker Compose before cloud deployment.
- Configure a GCP budget and billing alerts before creating the cluster.
- Keep OpenAI calls opt-in and use only fictional data.
- Load-test `/api/v1/health` or another non-LLM endpoint.
- Delete the LoadBalancer, cluster, persistent disk, and unused images after evidence is captured.
- Verify deletion in the Cloud Console; stopping workloads alone may not stop all charges.

Useful pricing inputs must be checked immediately before submission against the official [GKE pricing](https://cloud.google.com/kubernetes-engine/pricing), [Compute Engine pricing](https://cloud.google.com/compute/vm-instance-pricing), [Cloud Load Balancing pricing](https://cloud.google.com/vpc/network-pricing), and [OpenAI API pricing](https://openai.com/api/pricing/) pages.
