# TailorCV cloud application proposal — draft

> Submission constraints: maximum 1,000 words and two A4 pages. This draft matches the configured GKE topology and the cloud run reported by the project operator. The LaTeX PDF is the submission source of truth.

## Introduction

### Background and motivation

TailorCV is a web application that converts a text-based PDF resume into a consistent, job-targeted resume. Applicants frequently adapt the same resume for different roles, but manually identifying relevant evidence, reorganising content, and recreating a readable PDF is repetitive and error-prone. General-purpose generative AI can accelerate this work, yet unconstrained generation may invent skills, employers, dates, or achievements. TailorCV addresses this problem through a structured workflow that retains the source resume and validates AI selections against it.

### Objective and features

The objective is to provide a responsive, end-to-end resume-tailoring workflow without requiring an account. The implemented MVP provides six user-facing functions: PDF upload and validation; structured extraction and confirmation; job-description storage; evidence-grounded AI tailoring; HTML preview with change and missing-requirement summaries; and ATS-friendly PDF download. The system accepts selectable-text PDFs up to 5 MB and stores resumes, jobs, and tailoring runs. Tailored output must be reviewed by the user and is not presented as a guaranteed ATS score.

### Limitations of traditional solutions

A desktop-only implementation ties processing and stored data to one machine, complicates updates, and cannot share capacity when demand changes. A single server also couples the user interface, document processing, AI integration, and database failure domains. Manual deployment creates downtime and inconsistent environments. Although cloud deployment does not improve the correctness of AI automatically, it provides repeatable container execution, independent service scaling, health-based traffic routing, and controlled updates.

### Cloud benefits

Containerised services separate the public workflow from AI processing and persistence. Kubernetes can maintain multiple replicas, replace failed Pods, distribute requests through Services, and apply rolling updates with rollback. Readiness probes keep unready Pods out of Service endpoints, while persistent storage separates PostgreSQL data from container lifetimes. Environment configuration and Secrets keep deployment settings and credentials out of images. These properties improve resilience but do not guarantee zero downtime.

## Technical solution

The React 19 and TypeScript frontend is built with Vite. Nginx serves the built React app and reverse-proxies `/api/v1/*` requests to the internal Java 21 Spring Boot `resume-service`. Java validates PDFs, extracts text with PDFBox, owns the public REST API, persists data through Spring Data JPA/Flyway, and produces matching HTML/PDF output with Thymeleaf and OpenHTMLtoPDF. PostgreSQL 17 stores original PDF bytes, extracted text, canonical JSON, job descriptions, and tailoring runs.

The stateless Python 3.12 FastAPI `ai-service` owns strict Pydantic parsing and tailoring contracts. Default extraction is conservative and rule-based. Optional OpenAI structured output is bounded by timeouts and schema validation. Tailoring selects source skills and experience bullets by index, after which the service reconstructs the result from source data to reduce unsupported claims. Provider errors are sanitized. Docker Compose currently runs all four containers locally and health/readiness checks verify service dependencies.

The cloud deployment uses a two-node, zonal GKE Standard cluster in Sydney. Separate Deployments specify one frontend Pod, two Java Pods, and two Python Pods. Only the frontend is public through an HTTP LoadBalancer Service, implemented by GKE as a regional external passthrough Network Load Balancer; Java, Python, and PostgreSQL use private ClusterIP Services. A ConfigMap, Secret, probes, and resource requests/limits control operation. PostgreSQL runs in a one-replica StatefulSet with a 10 Gi persistent-volume claim. Backend replicas and Pod replacement improve resilience but cannot guarantee uninterrupted service; the single frontend Pod and database are documented single points of failure.

The Google Cloud Pricing Calculator CSV exported on 20 October 2026 gives USD $219.92 per 730-hour month for the zonal cluster fee ($73), two `e2-standard-2` nodes ($138.82), and 60 GiB balanced node boot disks ($8.10). An eligible GKE zonal-cluster credit would offset the $73 fee, leaving $146.92 for these listed items. It does not cover compute or disks. The CSV excludes the public L4 LoadBalancer, node IPv4, network egress, the separate 10 GiB PostgreSQL PVC, registry storage, and OpenAI calls. Using provisional amounts of about $27 for networking and $2 for the PVC and images, plus under $1 for demo model usage, the full planning total is about $176–177/month with the eligible credit or $249–250 without it. Taxes, unusual traffic, and extra billable resources are excluded. A short-lived course demo costs less; billing alerts and teardown limit expenditure.

## Architecture design

```text
Browser → external L4 LoadBalancer Service → React/Nginx Pod (1)
                                             → resume-service Service → Java Pods (2)
                                                                          ├─ PostgreSQL Pod + PVC
                                                                          └─ ai-service Service → Python Pods (2) → OpenAI API
```

The browser never calls Python or PostgreSQL directly. Java orchestrates upload, persistence, tailoring, and export; Python remains stateless and independently scalable. The assessment demonstration covers the complete workflow, replica scaling, Service routing, Pod replacement, rolling update, and rollback. Current limitations include no authentication, HTTPS, or OCR; variable PDF text ordering; external-model availability; one frontend Pod; and non-replicated PostgreSQL.
