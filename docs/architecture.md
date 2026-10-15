# TailorCV architecture

## Implemented local architecture (Phases 1–4)

```mermaid
flowchart LR
    U[Browser] -->|HTTP :3000| F[React SPA<br/>Nginx]
    F -->|/api/v1/*| R[resume-service<br/>Spring Boot]
    R -->|JDBC| P[(PostgreSQL 17)]
    R -->|/internal/parse<br/>/internal/tailor| A[ai-service<br/>FastAPI]
    A -->|optional provider call| O[OpenAI API]
```

Docker Compose runs the frontend, `resume-service`, `ai-service`, and PostgreSQL as separate containers. Only the frontend and Java API bind to `127.0.0.1`; Python and PostgreSQL remain on the private Compose network. The browser communicates only with Java through Nginx's `/api/` proxy.

### Responsibilities and data flow

1. The browser uploads a text-based PDF of at most 5 MB.
2. Java validates the file and extracts text with PDFBox.
3. Python converts the text to the strict canonical resume contract. The default parser is local and rule-based; optional AI parsing must be explicitly enabled.
4. Java validates and persists the original PDF, extracted text, and canonical JSON in PostgreSQL.
5. Java stores the job description and sends the selected resume and job text to Python for grounded tailoring.
6. Python asks the configured model to select source skills and experience bullets by index. It reconstructs and validates the result against the source before returning it.
7. Java persists the tailoring run. The result page displays the stored change summary and loads an escaped Thymeleaf preview.
8. Java renders the same template to PDF with OpenHTMLtoPDF. Export is generated in memory and is not persisted.

PostgreSQL is owned only by `resume-service`; `ai-service` is stateless. Java liveness checks only its process. Readiness also checks PostgreSQL and Python. Provider failures and timeouts are mapped to sanitized public errors.

## Planned cloud architecture (Phase 6)

```mermaid
flowchart LR
    U[Browser] --> LB[Google Cloud Load Balancer<br/>Ingress]
    LB --> F[frontend Service<br/>2+ Pods]
    F --> R[resume-service Service<br/>2+ Pods]
    R --> P[(PostgreSQL<br/>PVC-backed Pod)]
    R --> A[ai-service Service<br/>2+ Pods]
    A --> O[OpenAI API]
```

The intended GKE deployment uses separate Deployments and ClusterIP Services for the frontend and both application services, with at least two backend replicas, readiness/liveness probes, resource requests/limits, ConfigMaps, and Kubernetes Secrets. An Ingress or LoadBalancer provides the public entry point. PostgreSQL uses a persistent disk through a PVC.

This cloud design is **planned, not yet implemented or verified**. A single PostgreSQL Pod remains a single point of failure; the application must not be described as end-to-end highly available until a replicated or managed database is used. Cloud completion requires recorded evidence of scaling, load balancing, Pod replacement, rolling update, and rollback.
