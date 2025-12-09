# walloop-engine
Walloop – Core Engine Service  | The central processing engine that orchestrates business logic, transaction flows, and system intelligence across the entire Walloop platform.

## Stack

- Spring Boot 3.3 with CSR (Controller-Service-Repository) layering
- Spring Data JPA with PostgreSQL
- RabbitMQ consumers/producers
- WebSockets (STOMP/SockJS)
- OpenFeign clients
- Eureka client for service discovery
- Kubernetes readiness with manifests under `k8s/`
- OpenTelemetry via micrometer/OTLP
- MapStruct for DTO mapping
- Lombok for boilerplate reduction

## Running locally

1. Ensure PostgreSQL and RabbitMQ are available (defaults point to `localhost`).
2. Start Eureka and any dependent services (e.g., notification-service for the OpenFeign client).
3. Run the application:

```bash
mvn spring-boot:run
```

## Kubernetes

Use the manifests in `k8s/deployment.yaml` as a starting point. The deployment expects a `wallowop-db` secret with `username` and `password` keys and service endpoints for Postgres, RabbitMQ, and Eureka.
