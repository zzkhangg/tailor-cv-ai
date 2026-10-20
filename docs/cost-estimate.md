# Cloud cost estimate

Google Cloud Pricing Calculator export dated 20 October 2026 supplies the cluster, node, and boot-disk subtotal below. Remaining items are planning estimates, not a bill. Prices are USD per 730-hour month before tax. The export is the untracked CSV in the repository root; do not confuse its subtotal with the full application cost.

## Assumptions

- Sydney (`australia-southeast1`), zonal GKE Standard cluster in `australia-southeast1-a`.
- Two `e2-standard-2` nodes running 730 hours per month; the course demo should run only when needed.
- One frontend Pod, two Java Pods, two Python Pods, and one PostgreSQL Pod.
- Two 30 GiB node boot disks, one 10 GiB `standard-rwo` PostgreSQL PVC, and about 5 GiB of stored container images.
- One public frontend `LoadBalancer` Service (GKE regional external passthrough Network Load Balancer over HTTP), two node external IPv4 addresses, and about 5 GiB of internet egress. Java, Python, and PostgreSQL Services are private `ClusterIP`.
- Cloud Build usage inside the free tier; a small number of paid `gpt-4o-mini` requests. No paid AI load testing.
- Eligible GKE free-tier credit covers the management fee for one zonal cluster. Without it, add about $73/month.

## Estimated full-month cost

| Resource | Basis | USD/month |
|---|---|---:|
| GKE management | Calculator: one zonal cluster | $73.00 |
| Compute Engine nodes | Calculator: 2,920 core-hours and 11,680 GiB-hours for two `e2-standard-2` nodes | $138.82 |
| Node boot disks | Calculator: 60 GiB balanced PD in Sydney | $8.10 |
| **Calculator subtotal** | **Before any GKE free-tier credit; excludes all items below** | **$219.92** |
| Eligible GKE credit | Assumed available for one zonal cluster on this billing account | −$73.00 |
| **Calculator items after assumed credit** | **Not the complete application cost** | **$146.92** |
| Load balancing and network | Estimate from published rates: one forwarding rule, two node IPv4 addresses, 5 GiB egress to Australia, low-volume L4 processing | ~$27 |
| PostgreSQL PVC | Provisional: separate 10 GiB balanced PD (`standard-rwo`), at the export's disk rate | ~$1.35 |
| Artifact Registry | Provisional: 5 GiB images, assuming 0.5 GiB free tier | ~$0.45 |
| Build and AI | Free-tier builds, demo-scale model calls | <$1 |
| **Full planning total** | **With assumed credit; rounded, not a Calculator result** | **~$176–177** |

Without the GKE credit, the full planning total is **~$249–250/month**. The calculator CSV does **not** include the separate PostgreSQL PVC, public LoadBalancer, network traffic, registry, or AI provider charges. The $73 credit is not guaranteed if the billing account has already used its monthly zonal/Autopilot allowance. These are planning figures, not an observed bill; egress destination, load-balancer traffic, other billable resources, and actual AI token usage can change the result. A short-lived demo should cost less than a 730-hour month, but deleting Pods alone does not stop all charges.

The ~$27 network estimate comprises a [forwarding rule](https://cloud.google.com/vpc/network-pricing) at $0.025/hour × 730 = $18.25, two [in-use VM external IPv4 addresses](https://cloud.google.com/vpc/pricing) at $0.005/hour × 2 × 730 = $7.30, and assumed 5 GiB of [Premium Tier egress to Australia](https://cloud.google.com/vpc/network-pricing) at $0.19/GiB = $0.95, plus approximately $0.04 of low-volume load-balancer processing. This totals ~$26.54, rounded to ~$27. If nodes do not have external IPv4 addresses, or traffic volume/destination differs, recalculate. The PostgreSQL PVC estimate uses the CSV's balanced-disk rate: $8.10 × 10/60 = $1.35. The registry estimate assumes the published 0.5 GiB free tier and 5 GiB stored. None of these additional costs appears in the provided Calculator CSV.

## Cost controls and sources

- Set a GCP budget and billing alerts before running the cluster.
- Use fictional resumes and job descriptions; keep provider-backed tests opt-in.
- Test scaling and load balancing with a non-LLM endpoint.
- After the demo, delete the cluster and confirm the load balancer, disks, addresses, and unused images no longer incur charges.

Check current pricing with the official [GKE pricing](https://cloud.google.com/kubernetes-engine/pricing), [Compute Engine pricing](https://cloud.google.com/products/compute/pricing/general-purpose), [Network Load Balancing and forwarding-rule pricing](https://cloud.google.com/vpc/network-pricing), [external IPv4 pricing](https://cloud.google.com/vpc/pricing), [Artifact Registry pricing](https://cloud.google.com/artifact-registry/pricing), [Cloud Build pricing](https://cloud.google.com/build/pricing), and [OpenAI API pricing](https://openai.com/api/pricing/) pages. For a defensible submission figure, retain a dated calculator configuration or billing screenshot if available; the assignment does not explicitly require a screenshot.
