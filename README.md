# walloop-engine
Engine service that orchestrates Walloop transaction workflows across on-chain, Liquid, and Lightning integrations.

## Platform Purpose
`walloop-engine` is the workflow orchestrator of the platform. It coordinates the transaction lifecycle end-to-end, integrates internal services (wallet, core) and external providers (swap, conversion, LSP), and resumes workflows asynchronously through events and schedulers.

## Core Responsibilities
- Start and resume workflow executions from messages and API triggers.
- Orchestrate sequential business steps (`WalloopEngineWorkflow`).
- Manage transaction execution and flow control on the Liquid network.
- Integrate with partner APIs (SideShift, FixedFloat, Boltz, Amboss LSP).
- Create Lightning invoices through LND gRPC.
- Coordinate withdrawals/deposit monitoring through RabbitMQ events.

## Stack
- Java 21, Spring Boot 3.4.1, Spring Cloud 2024.0.0
- Spring Data JPA + PostgreSQL (`walloop` database, `engine` schema)
- Flyway migrations (`src/main/resources/db/migration`)
- RabbitMQ messaging
- OpenFeign, WebSockets, optional Eureka discovery
- Micrometer + OTLP for observability

## Quick Start
1. Docker path (recommended for local setup):
```bash
docker compose up --build
```

2. Local JVM path (requires environment variables from `INSTALLATION.md`):
```bash
mvn clean spring-boot:run
```

Detailed setup, required environment variables, smoke tests, and troubleshooting are in [INSTALLATION.md](./INSTALLATION.md).

## Key Endpoints (local defaults)
- `GET /actuator/health`
- `GET /pairs/{network}`
- `POST /workflows/start`
- `POST /liquidity/outbound/invoice`
- `POST /webhooks/amboss`

## Best Practices
- `application.yml` is the source of truth for runtime configuration keys.
- Keep secrets out of Git (use environment variables/Fly secrets).
- Use product-friendly logs in the format:
  - `<ClassName> - <action> - key=value key=value`
- Use business-readable tests with Given-When-Then naming.
- Prefer the module `docker-compose.yml` for local reproducibility before testing against shared environments.

## Additional Docs
- Installation guide: [INSTALLATION.md](./INSTALLATION.md)
- Contribution flow: [CONTRIBUTING.md](./CONTRIBUTING.md)
- Security policy: [SECURITY.md](./SECURITY.md)
- Agent/module notes: [AGENTS.md](./AGENTS.md)
