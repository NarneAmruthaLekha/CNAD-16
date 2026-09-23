# ImpactFlow Agile Product Backlog & Sprint Planning

This document tracks the Agile process for **ImpactFlow**, covering the overall Product Backlog, Sprint 1 Plan, and user stories.

---

## 1. Product Backlog

| ID | Title | Priority | Story Points | Status | Target Phase |
|---|---|---|---|---|---|
| US-101 | Platform Service Registration and Gateway Routing | High | 5 | To Do | Phase 1 |
| US-102 | User Registration & Role-based JWT Auth | High | 8 | To Do | Phase 2 |
| US-103 | Code Change Ingestion & Validation Service | High | 5 | To Do | Phase 2 |
| US-104 | System & Module Dependency Graph Management | High | 8 | To Do | Phase 2 |
| US-105 | Code Complexity & Churn Metric Computation | Medium | 5 | To Do | Phase 2 |
| US-106 | Microservices Event-Driven Messaging Integration (Kafka) | High | 8 | To Do | Phase 3 |
| US-107 | Code Change & Incident Outcome History Logging | Medium | 5 | To Do | Phase 3 |
| US-108 | ML-Based Risk Score Evaluation (Python/FastAPI) | High | 13 | To Do | Phase 4 |
| US-109 | Risk-Score Recommendation Synthesis | Medium | 5 | To Do | Phase 5 |
| US-110 | Aggregated Impact Report Persistence & Retrieval | High | 8 | To Do | Phase 5 |
| US-111 | Report Notification Pushes (Slack/Email/Webhooks Mock) | Low | 3 | To Do | Phase 5 |
| US-112 | ImpactFlow Interactive Web Dashboard UI | High | 13 | To Do | Phase 5 |
| US-113 | Microservice Containerization & Kubernetes Helm Orchestration | High | 13 | To Do | Phase 6 |
| US-114 | Automated CI/CD Pipelines (GitHub Actions + Security Scanning) | High | 8 | To Do | Phase 6 |
| US-115 | Prometheus / Grafana Observability Dashboard Integration | Medium | 8 | To Do | Phase 7 |

---

## 2. Sprint 1 Plan (Phase 1: Foundation & Gateway Infrastructure)

**Goal**: Establish the repository framework, draft requirements/architecture specs (SRS, HLD), set up UML Use Case and Class diagrams, and deploy the core service discovery (Eureka), configuration server, and API gateway routing skeleton.

**Sprint Backlog**:
- US-101: Platform Service Registry & Gateway routing skeleton (5 SP)
- Scaffolding & Setup: Parent pom, service directory structure, build files (3 SP)
- Architectural & Requirements Docs: SRS, HLD, Use-Case & Class Diagrams (5 SP)

**Sprint Commitment**: 13 Story Points.

### Sprint 1 User Stories (Detailed)

#### US-101: Service Registry and Gateway Routing
* **As a** developer deploying microservices,
* **I want** a centralized registry and gateway,
* **So that** services can dynamically discover each other and routes are unified under a single entry point.
* **Acceptance Criteria**:
  1. Eureka registry dashboard compiles and runs on `http://localhost:8761`.
  2. Spring Cloud Config Server serves service configurations under `http://localhost:8888`.
  3. API Gateway boots up, connects to Config Server, registers with Eureka registry, and routes requests to downstream microservice routing stubs.
  4. Gateway handles service instances dynamically using Consul/Eureka-backed load balancing (`lb://`).
  5. The API Gateway includes default routes with proper fallback mechanisms.

---

## 3. Sprint 1 Burndown Log (Simulated)

| Day | Task Remaining (Hours) | Actual Work Completed / Notes |
|---|---|---|
| Day 1 | 40 | Sprint Planning completed. Initial directory scaffolding and parent POM setup done. |
| Day 2 | 32 | SRS and HLD requirements documentation drafted and pushed. |
| Day 3 | 24 | Use Case and Class UML diagrams modeled using Mermaid. |
| Day 4 | 16 | Eureka Server and Config Server coded, configured, and verified running. |
| Day 5 | 8 | API Gateway skeleton implemented, routed dynamically through Eureka server. |
| Day 6 | 0 | Dynamic verification complete. All logs clean. Phase 1 sprint review/retro held. |

---

## 4. Sprint 1 Retrospective

* **What went well**:
  - Choice of a Maven multi-module structure simplified dependency management.
  - Using a local `native` profile for Config Server removed the need for Git remote complexity.
  - Mermaid markdown allows quick documentation updates alongside code.
* **What could be improved**:
  - Dependency incompatibilities between Spring Boot and Spring Cloud versions require strict constraints. Pinning Spring Boot to `3.2.7` resolved this.
* **Action Items**:
  - Strictly use pinned versions in the parent POM.
