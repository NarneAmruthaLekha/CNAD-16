# Software Requirements Specification (SRS) for ImpactFlow

## 1. Introduction
This document defines the requirements for **ImpactFlow**, a cloud-native platform designed to analyze proposed software changes and predict their downstream impact, risks, and recommended actions before deployment.

### 1.1 Purpose
ImpactFlow targets DevOps, QA, and software engineering teams running distributed microservice systems. Traditional CI/CD pipelines check code correctness via tests, but fail to evaluate systemic risk (e.g., changes to shared modules that trigger unexpected failures in downstream dependencies).

---

## 2. Overall Description

### 2.1 Product Perspective
ImpactFlow operates as a service-oriented suite integrated into CI/CD pipelines (e.g., via GitHub Actions or GitLab CI) and accessible via a web-based dashboard. It consists of multiple Java Spring Boot services and a Python FastAPI ML service, utilizing Eureka for discovery, Spring Cloud Gateway for routing, and Apache Kafka for asynchronous communication.

### 2.2 Scoping & Phasing Note
Due to the architectural complexity of building, containerizing, and orchestrating 12 microservices, the system development is phased:
* **Phase 1**: Foundations, central registries, and gateway infrastructure.
* **Phase 2**: Authentication, change-ingestion, dependency graph, and code metrics.
* **Phase 3**: Kafka asynchronous messaging framework and historical change logging.
* **Phase 4**: Machine Learning risk prediction service.
* **Phase 5**: Recommendation generation, reporting, and dashboard UI.
* **Phases 6-8**: DevSecOps, Kubernetes deployment, Helm, and Prometheus/Grafana observability.

---

## 3. Requirements

### 3.1 Functional Requirements (FR)

| Req ID | Title | Description | Priority |
|---|---|---|---|
| **FR-1.1** | User Authentication | Users must authenticate via JWT-based credentials (roles: Developer, Admin). | High |
| **FR-1.2** | Submit Proposed Change | Developer submits a change payload (repository URL, commit diff, or list of changed modules). | High |
| **FR-1.3** | Parse Change Diff | System parses code changes to extract impacted modules and code line count. | High |
| **FR-1.4** | Maintain Dependency Graph | System tracks service-to-service and module-to-module dependencies. | High |
| **FR-1.5** | Compute Complexity | System calculates cyclomatic complexity, churn, and lines of code (LOC) delta. | Medium |
| **FR-1.6** | Train & Store Risk Models | Historical code change records and their production outcomes (incidents) must be persisted to train/serve risk prediction models. | Medium |
| **FR-1.7** | Evaluate Risk Score | ML-based prediction service evaluates changes and returns a risk score (Low/Medium/High) and confidence metrics. | High |
| **FR-1.8** | Synthesize Recommendations | recommendation-service provides actionable advice based on risk scores (e.g., "run staging smoke tests"). | Medium |
| **FR-1.9** | Compile Final Report | Aggregates all parsed data, complexity metrics, risk score, and recommendations into an editable report. | High |
| **FR-1.10** | External Notifications | Push final report notifications to external channels (Slack/Email/Webhooks). | Low |
| **FR-1.11** | Dynamic Routing | The API Gateway routes client requests dynamically based on Eureka service registry states. | High |

### 3.2 Non-Functional Requirements (NFR)

| Req ID | Title | Description | Priority |
|---|---|---|---|
| **NFR-2.1** | Service Registry | All services must dynamically register with Eureka and retrieve configurations from Config Server. | High |
| **NFR-2.2** | Response Time | Risk evaluation for standard-sized PRs (under 50 files) must return in < 5 seconds. | High |
| **NFR-2.3** | Availability | The platform must achieve 99.5% availability, utilizing replicated service pods. | High |
| **NFR-2.4** | Database Isolation | Each service must operate on its own separate database (database-per-service paradigm). | High |
| **NFR-2.5** | Secure Ingress | Single API entry point via Spring Cloud Gateway with CORS policies and JWT check. | High |
| **NFR-2.6** | Elastic Scalability | Services must scale horizontally under Kubernetes using Horizontal Pod Autoscalers (HPA). | Medium |
| **NFR-2.7** | Monitoring & Metrics | Services must expose `/actuator/health` and Prometheus metrics. | High |

---

## 4. Use Case Specifications

### Use Case 1: Submit Proposed Change
* **Primary Actor**: Developer (CI/CD Pipeline)
* **Preconditions**: Developer is authenticated with a valid JWT token.
* **Basic Flow**:
  1. Developer triggers a commit/PR, invoking the CI/CD pipeline which calls the API Gateway.
  2. The Gateway routes the request to `change-ingestion-service`.
  3. `change-ingestion-service` validates the repository details and publishes a `change.submitted` event to Kafka.
  4. Client receives an asynchronous `202 Accepted` response with a tracking ID.
* **Postconditions**: The change metadata is successfully ingested and queued for analysis.

### Use Case 2: Query Impact Report
* **Primary Actor**: Developer / QA Engineer
* **Preconditions**: Report has been processed and compiled.
* **Basic Flow**:
  1. User navigates to the ImpactFlow dashboard.
  2. Dashboard queries the API Gateway at `/api/reports/{id}`.
  3. Gateway routes the request to `report-service`.
  4. `report-service` retrieves the persisted report (containing risk scores, complexity, recommendations, and graph nodes) and returns it.
* **Postconditions**: The user views a detailed risk assessment report.

---

## 5. Requirements Traceability Matrix (RTM)

| Req ID | Use Case | Component | Verification Method |
|---|---|---|---|
| **FR-1.1** | UC-1, UC-2 | `auth-service` / API Gateway | Integration test with valid/invalid JWTs. |
| **FR-1.2** | UC-1 | `change-ingestion-service` | REST contract validation tests. |
| **FR-1.3** | UC-1 | `change-ingestion-service` | Unit test checking diff parser logic. |
| **FR-1.4** | UC-1, UC-2 | `dependency-graph-service` | Graph query tests on service relationship nodes. |
| **FR-1.5** | UC-1 | `complexity-metrics-service` | Unit tests checking LOC/churn calculators. |
| **FR-1.6** | UC-1 | `history-service` | Database unit tests for past incidents table. |
| **FR-1.7** | UC-1 | `risk-prediction-service` | Unit and ML performance tests (Precision/Recall). |
| **FR-1.8** | UC-1 | `recommendation-service` | Asserting recommendations based on various risk profiles. |
| **FR-1.9** | UC-1, UC-2 | `report-service` | Checking final consolidated JSON structure. |
| **FR-1.10** | UC-1 | `notification-service` | Verifying mock notification console logs/payloads. |
| **FR-1.11** | UC-1, UC-2 | `api-gateway` / `eureka-server` | Running local requests and verifying routing. |
