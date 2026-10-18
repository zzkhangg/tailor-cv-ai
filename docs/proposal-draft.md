# TailorCV cloud application proposal — draft

> Submission constraints: maximum 1,000 words and two A4 pages. This draft describes the completed MVP and clearly identifies the GKE deployment as planned. Update the cloud tense, exact configuration, cost total, and architecture figure only after deployment is verified.

## Introduction

### Background and motivation

TailorCV is a web application that converts a text-based PDF resume into a consistent, job-targeted resume. Applicants frequently adapt the same resume for different roles, but manually identifying relevant evidence, reorganising content, and recreating a readable PDF is repetitive and error-prone. General-purpose generative AI can accelerate this work, yet unconstrained generation may invent skills, employers, dates, or achievements. TailorCV addresses this problem through a structured workflow that retains the source resume and validates AI selections against it.

### Objective and features

The objective is to provide a responsive, end-to-end resume-tailoring workflow without requiring an account. The implemented MVP provides six user-facing functions: PDF upload and validation; structured extraction and confirmation; job-description storage; evidence-grounded AI tailoring; HTML preview with change and missing-requirement summaries; and ATS-friendly PDF download. The system accepts selectable-text PDFs up to 5 MB and stores resumes, jobs, and tailoring runs. Tailored output must be reviewed by the user and is not presented as a guaranteed ATS score.

### Limitations of traditional solutions

A desktop-only implementation ties processing and stored data to one machine, complicates updates, and cannot share capacity when demand changes. A single server also couples the user interface, document processing, AI integration, and database failure domains. Manual deployment creates downtime and inconsistent environments. Although cloud deployment does not improve the correctness of AI automatically, it provides repeatable container execution, independent service scaling, health-based traffic routing, and controlled updates.

### Cloud benefits

Containerised services separate the public workflow from AI processing and persistence. Kubernetes can maintain multiple replicas, replace failed Pods, distribute requests through Services, and apply rolling updates with rollback. Readiness probes prevent traffic reaching an unavailable service, while persistent storage separates PostgreSQL data from container lifetimes. Environment configuration and Secrets keep deployment settings and credentials out of images. These properties support a more scalable and resilient service than a single manually managed host.

## Technical solution

The React 19 and TypeScript frontend is built with Vite and served by Nginx. It calls only the Java 21 Spring Boot `resume-service`, which validates PDFs, extracts text with PDFBox, owns the public REST API, persists data through Spring Data JPA/Flyway, and produces matching HTML/PDF output with Thymeleaf and OpenHTMLtoPDF. PostgreSQL 17 stores original PDF bytes, extracted text, canonical JSON, job descriptions, and tailoring runs.

The stateless Python 3.12 FastAPI `ai-service` owns strict Pydantic parsing and tailoring contracts. Default extraction is conservative and rule-based. Optional OpenAI structured output is bounded by timeouts and schema validation. Tailoring selects source skills and experience bullets by index, after which the service reconstructs the result from source data to reduce unsupported claims. Provider errors are sanitized. Docker Compose currently runs all four containers locally and health/readiness checks verify service dependencies.

The planned cloud deployment uses Google Kubernetes Engine. Separate Deployments will run one frontend Pod and at least two Java and Python Pods. The frontend LoadBalancer Service will be public; Java and Python will use private ClusterIP Services. ConfigMaps, Secrets, probes, and resource requests/limits will control operation. PostgreSQL will use a persistent-volume-backed Pod for the course demonstration. This database remains a documented single point of failure; a managed, highly available database would be preferable in production.

For a small always-on classroom deployment in Sydney, the provisional estimate is USD $141–294 per month: GKE management $0–73, two worker nodes $120–190, load balancing $18–25, and under $5 for disk, images, low egress, and demo-scale model usage. The final submission will replace this range with one dated Google Cloud Pricing Calculator configuration. Development remains local, billing alerts will be enabled, and cloud resources will be deleted after evidence is captured.

## Architecture design

```text
Browser → Cloud Load Balancer/Ingress → React/Nginx Pods
                                      → resume-service Pods
                                           ├─ PostgreSQL + persistent volume
                                           └─ ai-service Pods → OpenAI API
```

The browser never calls Python or PostgreSQL directly. Java orchestrates upload, persistence, tailoring, and export; Python remains stateless and independently scalable. The assessment demonstration will show the complete workflow, replica scaling, load balancing, Pod replacement, rolling update, and rollback. Current limitations include no authentication, no OCR for scanned PDFs, variable PDF text ordering, external-model availability, and non-replicated PostgreSQL.
