# High-Level Design (HLD) for ImpactFlow

This document presents the system-level design, service responsibilities, technical selection rationale, and Service-Oriented Architecture (SOA) design notes for **ImpactFlow**.

---

## 1. System Architecture

ImpactFlow is designed as a cloud-native, microservices-based application implementing both synchronous RESTful patterns and asynchronous, event-driven integration patterns.

```
                  +----------------------------------------------+
                  |               Web Dashboard (UI)             |
                  +----------------------+-----------------------+
                                         |
                                         | REST HTTP / HTTPS
                                         v
                  +----------------------------------------------+
                  |         API Gateway (Spring Cloud)           |
                  +-----+----------------+-----------------+-----+
                        |                |                 |
                        | REST           | REST            | REST
                        v                v                 v
                 +------------+   +------------+   +---------------+
                 |  Eureka    |   |  Config    |   | Auth Service  |
                 |  Registry  |   |  Server    |   | (Spring/JWT)  |
                 +------------+   +------------+   +---------------+
                        ^                ^
                        | registration   | properties fetch
                        +-------+--------+
                                |
          +---------------------+---------------------------------+
          |                                                       |
          |       Asynchronous Messaging Bus (Apache Kafka)       |
          |                                                       |
          |  Topics:                                              |
          |    - change.submitted                                 |
          |    - change.enriched.dependencies                     |
          |    - change.enriched.metrics                          |
          |    - risk.scored                                      |
          |    - report.ready                                     |
          |                                                       |
          +---+--------------+---------------+---------------+----+
              ^              ^               ^               ^
              | consume/     | consume/      | consume/      | consume/
              | produce      | produce       | produce       | produce
              v              v               v               v
       +------------+ +------------+  +------------+  +-------------+
       | Ingestion  | | Dependency |  | Complexity |  | History     |
       | Service    | | Graph      |  | Metrics    |  | Service     |
       |            | | Service    |  | Service    |  |             |
       +------------+ +------------+  +------------+  +-------------+
              |              |               |               |
              v Db           v Db            v Db            v Db
            [Postgre]      [Neo4j/         [Postgre]       [Postgre]
                           Postgre]

              +--------------+---------------+---------------+
              |              |               |               |
              v consume/     v consume/      v consume/      v consume/
              | produce      | produce       | produce       | produce
       +------------+ +------------+  +------------+  +-------------+
       | Risk       | | Recommen-  |  | Report     |  | Notification|
       | Prediction | | dation     |  | Service    |  | Service     |
       | (FastAPI)  | | Service    |  |            |  | (Mock)      |
       +------------+ +------------+  +------------+  +-------------+
              |              |               |               |
              v Model        v Db            v Db            v Webhook/
            [ONNX/Joblib]  [Postgre]       [Postgre]        Email
```

---

## 2. Microservice Responsibilities

1. **`eureka-server`**: Service registry that allows downstream microservices to dynamically locate each other.
2. **`config-server`**: Centralized configuration management serving dynamic properties to services at startup.
3. **`api-gateway`**: Entry point for API routing, load balancing (`lb://`), CORS enforcement, and request dispatching.
4. **`auth-service`**: Manages users, credentials, role assignments, and issues JWT tokens.
5. **`change-ingestion-service`**: Receives change submissions (e.g., repository metadata and diff payloads) and publishes a `change.submitted` Kafka event.
6. **`dependency-graph-service`**: Tracks module and service dependencies, queryable to see which downstream modules are affected.
7. **`complexity-metrics-service`**: Computes code metrics (LOC change, cyclomatic complexity) and returns them.
8. **`history-service`**: Tracks historical code modifications and matches them with post-release bug metrics/incidents.
9. **`risk-prediction-service`**: (Python FastAPI) Uses a trained classifier model to estimate merge risk.
10. **`recommendation-service`**: Determines action items based on risk tiers (Low/Medium/High).
11. **`report-service`**: Consolidates all microservice responses (risk score, complexity, recommendations, dependency node lists) into a unified report.
12. **`notification-service`**: Dispatches alert hooks/messages.

---

## 3. Technology Justification

* **Java & Spring Boot**: Selected for standard enterprise microservices. Spring Cloud Gateway and Eureka Server provide robust, out-of-the-box discovery and routing features.
* **Database per Service**: Isolates data layers. Even if one database experiences a locking outage, other services continue operating, complying with core cloud resilience.
* **Apache Kafka**: Decouples services. Processing complexity metrics and fetching dependency nodes can occur in parallel when a change is submitted, rather than chaining synchronous REST blockages.
* **Python & FastAPI**: FastAPI provides asynchronous, lightweight Python serving, optimal for loading machine learning libraries (such as Scikit-Learn, pandas, joblib) that are native to Python.

---

## 4. Theoretical Note: Modern SOA vs. Classic SOAP SOA

In academic Service-Oriented Architecture (SOA), systems are designed around the principles of loose coupling, service contracts, autonomy, abstraction, reusability, and discoverability. The implementation of these principles has evolved significantly:

### Comparison Matrix

| Aspect | Classic SOA (SOAP) | Modern Cloud-Native SOA (REST & Event-Driven) |
|---|---|---|
| **Protocol / Transport** | SOAP (XML over HTTP/HTTPS/SMTP) | REST (JSON over HTTP/HTTPS), gRPC, and Asynchronous Event Streams (Kafka/RabbitMQ) |
| **Service Contract** | WSDL (XML Schema) | OpenAPI / Swagger Specs (JSON/YAML) or Avro/Protobuf Schemas |
| **Service Directory** | UDDI (Universal Description, Discovery, and Integration) | Netflix Eureka, Consul, or Kubernetes DNS |
| **Integration Style** | Centralized Enterprise Service Bus (ESB) doing heavy orchestration, routing, and message transformation | Smart endpoints, dumb pipes. Decentralized API Gateways (Spring Cloud Gateway) and distributed event brokers (Kafka) |
| **Data Separation** | Shared centralized databases with service-specific schemas | Database-per-service (No shared databases; loose coupling at data level) |

### How ImpactFlow Maps to SOA Principles

1. **Service Contracts**: Every service in ImpactFlow exposes a formal **OpenAPI / Swagger spec** (generated using `springdoc-openapi`). Communication patterns are strictly validated against these contracts.
2. **Service Discovery (Netflix Eureka)**: Replaces classic UDDI. Services dynamically register their network locations at startup and discover others client-side, eliminating hardcoded hostnames and enabling horizontal scaling.
3. **Loose Coupling (Database-per-Service & Kafka)**: Classic SOAP services often coupled themselves by sharing database instances. ImpactFlow isolates database stores per service. Kafka decouples change ingestion from metrics processing: the ingestion service does not need to know which consumers exist.
4. **Service Composition & Choreography**: Classic SOA relied on a heavy ESB to orchestrate flows. ImpactFlow uses **Event Choreography**: services listen to Kafka topics, complete their localized tasks, and emit new events (`change.submitted` -> `change.enriched.*` -> `risk.scored`), which naturally triggers the next phase without a single orchestrator bottleneck.
5. **Autonomy and Abstraction**: The `risk-prediction-service` is written in Python (FastAPI) because of ML package availability, while the rest are Java. Under the SOA model, implementation details are abstract; as long as the service conforms to its contract, the caller remains unaffected by language changes.
