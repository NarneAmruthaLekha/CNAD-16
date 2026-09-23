# UML Use Case Diagram

This document contains the use case modeling for **ImpactFlow**.

```mermaid
left-to-right-direction
actor Developer as dev
actor "CI/CD Pipeline" as cicd
actor Admin as admin

rectangle ImpactFlow {
  usecase "Authenticate (Login/Register)" as UC1
  usecase "Submit Proposed Code Change" as UC2
  usecase "View System Dependency Graph" as UC3
  usecase "Query Impact & Risk Report" as UC4
  usecase "Configure Risk Assessment Strategy" as UC5
  usecase "Retrain ML Model" as UC6
  usecase "Receive Risk Notification" as UC7
}

dev --> UC1
dev --> UC2
dev --> UC3
dev --> UC4

cicd --> UC1
cicd --> UC2
cicd --> UC7

admin --> UC1
admin --> UC5
admin --> UC6
admin --> UC3
```
