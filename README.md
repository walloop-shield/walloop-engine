# walloop-engine
Engine Service that centralizes orchestration logic, transaction workflows, and integrations for the Walloop platform.

## Business Overview
The engine is the operational brain of Walloop: it coordinates each transaction journey end-to-end, connects platform modules and external partners, and ensures that swaps, conversions, and settlement steps progress with traceability and business control.

## Stack
- Java 21, Spring Boot 3.3.4, Spring Cloud 2023.0.3
- CSR (Controller-Service-Repository) with Spring Data JPA + PostgreSQL (db `walloop`, schema `engine`, UUID IDs)
- Flyway for schema versioning (migrations in `src/main/resources/db/migration`)
- RabbitMQ for messaging
- WebSockets (STOMP/SockJS), OpenFeign clients, and (optional) Eureka for service discovery
- Observability with Micrometer/OTLP (OTEL collector in docker-compose)
- MapStruct for DTO mapping and Lombok to reduce boilerplate

## Local environment with Docker Compose
1. Prerequisites: recent Docker and Docker Compose.
2. Start the stack (app, Postgres, RabbitMQ, and OTEL collector):
   ```bash
   docker compose up --build
   ```
3. Exposed services:
   - Application: `http://localhost:8080`
   - PostgreSQL: db `walloop`, schema `engine`, username/password `walloop` / `walloop` (port `5432`)
   - RabbitMQ: `amqp://localhost:5672` (console at `http://localhost:15672`)
   - OTLP receiver: `http://localhost:4317`
4. To stop and clean containers/volumes:
   ```bash
   docker compose down -v
   ```

## Manual run (without Docker)
1. Requirements: JDK 21+ and Maven 3.9+ installed.
2. Ensure Postgres and RabbitMQ are reachable locally. Default values are in `application.yml`:
   - JDBC: `jdbc:postgresql://localhost:5432/walloop`
   - Default schema: `engine` (hibernate.default_schema / Flyway default-schema)
   - Username/password: `walloop` / `walloop`
3. Run migrations and start the app:
   ```bash
   mvn spring-boot:run
   ```

## Lightning (LND gRPC)
The engine creates invoices via direct gRPC to LND. Configuration is in `application.yml`
and must be provided through environment variables.

### Local (files)
To run locally, use file paths for `tls.cert` and `admin.macaroon`:

```
LND_GRPC_HOST=localhost
LND_GRPC_PORT=10009
LND_GRPC_CERT_FILE=/path/to/tls.cert
LND_GRPC_MACAROON_FILE=/path/to/admin.macaroon
```

### Production (base64)
In production, use base64 through secrets (e.g., Fly secrets). This avoids keeping
sensitive files on the application filesystem:

```
LND_GRPC_HOST=walloop-lightning-node.internal
LND_GRPC_PORT=10009
LND_GRPC_CERT_BASE64=...
LND_GRPC_MACAROON_BASE64=...
```

Note: an admin-permission macaroon provides full LND access. Treat it as a secret.

### Quick local test
To validate gRPC, run the app and create an invoice through the engine flow.
If LND is reachable and the macaroon is valid, the invoice should be created successfully.

## Lightning / LSP (Amboss Magma)
The engine can request inbound liquidity via LSP using Amboss GraphQL API.
The trigger happens in `create_lightning_invoice`: if inbound liquidity
is below the target and there is no pending request, the engine creates an LSP order
and schedules a retry.

Main settings (`application.yml`):
- `walloop.lightning.inbound-target-sats`: global inbound target.
- `walloop.lightning.inbound-check-enabled`: enables inbound check before invoice.
- `walloop.lightning.lsp.base-url`: GraphQL API base URL.
- `walloop.lightning.lsp.api-key`: API key (Bearer).
- `walloop.lightning.lsp.node-pubkey`: node pubkey (override).

Note: GraphQL endpoint is fixed in code (`/graphql`).

## Cache (Caffeine)
The engine uses Caffeine to reduce external calls for pair availability and rates.

Active caches:
- `pairAvailability`: used by `PairAvailabilityService` (configurable TTL).
- `fxRates`: used by `CoinCapFxRateProvider` (configurable TTL).

Config (`application.yml`):
```
walloop:
  pair-availability:
    cache-seconds: 60
  fee:
    rate-provider:
      type: coincap
    coincap:
      cache-seconds: 300
```

Note: adjust TTLs via `walloop.pair-availability.cache-seconds` and
`walloop.fee.coincap.cache-seconds` according to environment needs.

## Flyway
- Migrations live in `src/main/resources/db/migration`.
- Baseline migration `V1.0.0__init_customers.sql` creates schema `engine`, enables `pgcrypto`, and creates table `customers` with UUID primary key.
- The application starts with `spring.jpa.hibernate.ddl-auto=validate` and `baseline-on-migrate=true` to keep schema controlled by Flyway.

## Liquid + Bitcoin (Docker Compose)
`docker-compose.yml` includes `vulpemventures/liquid` and `vulpemventures/bitcoin` to validate peg-ins via RPC.

### Testnet (current config)
- `liquid-node`: `-chain=liquidtestnet` + `-validatepegin=1` with `mainchainrpc*` pointing to `bitcoin-node`.
- `bitcoin-node`: `-testnet` and RPC on `18332`.

### Mainnet (how to switch)
1. In `liquid-node`, change `-chain=liquidtestnet` to `-chain=liquidv1`.
2. In `bitcoin-node`, remove `-testnet` (or use `-mainnet`) and use RPC `8332`.
3. If exposing ports, change `18332:18332` to `8332:8332`.

## Kubernetes
Use `kubernetes/deployment.yaml` as a base. The deployment expects a `walloop-db` secret with `username` and `password`, plus endpoints for Postgres, RabbitMQ, and (if needed) Eureka. Adjust credentials to match database `walloop` and schema `engine` (variables `SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA`, `SPRING_FLYWAY_DEFAULT_SCHEMA`, and `SPRING_FLYWAY_SCHEMAS`).
