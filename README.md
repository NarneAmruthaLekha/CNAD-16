\# ImpactFlow



\*\*Risk-Aware Software Change Impact and Prediction Platform\*\*



ImpactFlow is a cloud-native microservices platform that analyzes software changes and predicts their potential risk.



\## Architecture



\- \*\*API Gateway:\*\* Entry point for client requests.

\- \*\*Eureka Server:\*\* Service discovery.

\- \*\*Config Server:\*\* Centralized configuration.

\- \*\*Auth Service:\*\* Authentication and authorization.

\- \*\*Change Analysis Service:\*\* Analyzes submitted changes and GitHub commits.

\- \*\*Risk Prediction Service:\*\* Predicts change risk using a machine learning model.

\- \*\*Report Service:\*\* Stores and exposes analysis reports.

\- \*\*Kafka:\*\* Asynchronous event communication.

\- \*\*Frontend:\*\* Web interface for submitting changes and viewing reports.

\- \*\*Helm and Kubernetes:\*\* Container orchestration and deployment.



\## Technology Stack



Java, Spring Boot, Python, FastAPI, scikit-learn, Apache Kafka, Docker, Kubernetes, Helm, and JavaScript.



\## Project Structure



\- `api-gateway/`

\- `auth-service/`

\- `change-analysis-service/`

\- `config-server/`

\- `eureka-server/`

\- `report-service/`

\- `ImpactFlow/`

&#x20; - `impactflow-frontend/`

&#x20; - `risk-prediction-service/`

&#x20; - `helm/`

&#x20; - `docker-compose.yml`



\## Getting Started



Each Spring Boot service has its own project configuration. Start the infrastructure dependencies, configure the service URLs and environment variables, and launch the services in the required order.



For the Python risk-prediction service, refer to its `Dockerfile` and `main.py`.



\## Deployment



The `ImpactFlow/helm/impactflow/` directory contains the Helm chart used to deploy the application to Kubernetes.



\## Note



This project is under development. Configuration and deployment steps may vary depending on the environment.



