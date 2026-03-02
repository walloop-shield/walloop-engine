# walloop-engine Installation Guide

## 1. Goal
This guide enables running `walloop-engine` locally in two modes:

1. Pure local JVM execution (`mvn spring-boot:run`)
2. Docker execution (`docker compose`)

It also includes smoke tests and troubleshooting.

## 2. Technical Summary
- Stack: Java 21, Spring Boot 3.4.1, Maven 3.9+, PostgreSQL, RabbitMQ, Flyway.
- Default HTTP port: `8080`.
- Database schema: `engine` (managed by Flyway).
- Main config file: `src/main/resources/application.yml`.
- Module compose file: `docker-compose.yml` (inside this module directory).

## 3. Prerequisites

### 3.1 Pure local JVM mode
1. JDK 21 installed and available in `PATH`.
2. Maven 3.9+ installed and available in `PATH`.
3. PostgreSQL reachable.
4. RabbitMQ reachable.
5. Liquid RPC reachable.
6. (Optional for Lightning flows) LND gRPC reachable.

### 3.2 Docker mode
1. Docker Desktop (or Docker Engine).
2. Docker Compose v2.

## 4. Quick Environment Check
Run:

```bash
java -version
mvn -version
docker --version
docker compose version
```

Expected:
1. Java 21+
2. Maven 3.9+
3. Docker and Compose available without errors

## 5. Required Environment Variables
In the current `application.yml`, several placeholders have no fallback values and must be provided.

Use this minimum baseline:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/walloop
SPRING_DATASOURCE_USERNAME=walloop
SPRING_DATASOURCE_PASSWORD=walloop

RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/

LIQUID_RPC_URL=http://localhost:7041
LIQUID_RPC_USERNAME=rpcuser
LIQUID_RPC_PASSWORD=rpcpassword

SIDESHIFT_BASE_URL=https://sideshift.ai/api/v2
SIDESHIFT_SECRET=dummy
SIDESHIFT_AFFILIATE_ID=dummy

FIXEDFLOAT_BASE_URL=https://ff.io/api/v2
FIXEDFLOAT_API_KEY=dummy
FIXEDFLOAT_API_SECRET=dummy
FIXEDFLOAT_REFCODE=dummy

BOLTZ_BASE_URL=https://api.boltz.exchange
BOLTZ_CLAIM_SIGNER_URL=http://localhost:8090

LND_GRPC_HOST=localhost
LND_GRPC_PORT=10009
LND_GRPC_MACAROON_BASE64=dummy
LND_GRPC_CERT_BASE64=dummy

WALLOOP_WALLET_BASE_URL=http://localhost:8084
WALLOOP_LIGHTNING_LSP_BASE_URL=https://api.amboss.space
WALLOOP_LIGHTNING_LSP_API_KEY=dummy

COINCAP_BASE_URL=https://api.coincap.io/v2
WALLOOP_FEE_COINCAP_API_KEY=dummy

EUREKA_CLIENT_ENABLED=false
EUREKA_CLIENT_SERVICE_URL=http://localhost:8761/eureka/
```

Notes:
- `dummy` values are enough for application startup.
- Real partner flows require valid credentials and reachable endpoints.
- Never use `dummy` credentials outside local development.

## 6. Option A: Pure Local JVM Execution

### 6.1 Start dependencies
Make sure these services are available locally:
1. PostgreSQL on `localhost:5432` with database `walloop`.
2. RabbitMQ on `localhost:5672`.
3. Liquid RPC on `localhost:7041`.

### 6.2 Export environment variables (PowerShell example)

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/walloop"
$env:SPRING_DATASOURCE_USERNAME="walloop"
$env:SPRING_DATASOURCE_PASSWORD="walloop"
$env:RABBITMQ_HOST="localhost"
$env:RABBITMQ_PORT="5672"
$env:RABBITMQ_USER="guest"
$env:RABBITMQ_PASSWORD="guest"
$env:RABBITMQ_VHOST="/"
$env:LIQUID_RPC_URL="http://localhost:7041"
$env:LIQUID_RPC_USERNAME="rpcuser"
$env:LIQUID_RPC_PASSWORD="rpcpassword"
$env:SIDESHIFT_BASE_URL="https://sideshift.ai/api/v2"
$env:SIDESHIFT_SECRET="dummy"
$env:SIDESHIFT_AFFILIATE_ID="dummy"
$env:FIXEDFLOAT_BASE_URL="https://ff.io/api/v2"
$env:FIXEDFLOAT_API_KEY="dummy"
$env:FIXEDFLOAT_API_SECRET="dummy"
$env:FIXEDFLOAT_REFCODE="dummy"
$env:BOLTZ_BASE_URL="https://api.boltz.exchange"
$env:BOLTZ_CLAIM_SIGNER_URL="http://localhost:8090"
$env:LND_GRPC_HOST="localhost"
$env:LND_GRPC_PORT="10009"
$env:LND_GRPC_MACAROON_BASE64="dummy"
$env:LND_GRPC_CERT_BASE64="dummy"
$env:WALLOOP_WALLET_BASE_URL="http://localhost:8084"
$env:WALLOOP_LIGHTNING_LSP_BASE_URL="https://api.amboss.space"
$env:WALLOOP_LIGHTNING_LSP_API_KEY="dummy"
$env:COINCAP_BASE_URL="https://api.coincap.io/v2"
$env:WALLOOP_FEE_COINCAP_API_KEY="dummy"
$env:EUREKA_CLIENT_ENABLED="false"
$env:EUREKA_CLIENT_SERVICE_URL="http://localhost:8761/eureka/"
```

### 6.3 Start the module
From `walloop-engine` directory:

```bash
mvn clean spring-boot:run
```

## 7. Option B: Docker Execution

The module `docker-compose.yml` brings up:
1. `app` (engine)
2. `db` (PostgreSQL)
3. `rabbitmq`
4. `otel-collector`
5. `liquid-node`
6. `bitcoin-node`

The compose file already maps all required environment variables from `application.yml`.

### 7.1 Start stack
From `walloop-engine` directory:

```bash
docker compose up --build
```

### 7.2 Stop stack

```bash
docker compose down -v
```

## 8. Ports and Common Conflicts
Default ports:
1. `8080` (engine HTTP)
2. `5432` (PostgreSQL)
3. `5672` (RabbitMQ AMQP)
4. `15672` (RabbitMQ UI)
5. `4317` (OTEL collector)
6. `7041` (Liquid RPC)
7. `18332` (Bitcoin RPC testnet)

If there is a conflict:
1. Change the left side of the mapping in compose `ports`.
2. Restart with `docker compose up --build`.

## 9. Local Execution Matrix

With `dummy` values:
1. Application boots and migrations run.
2. Health/admin endpoints work.
3. External partner-dependent flows may fail.

With real credentials:
1. SideShift, FixedFloat, Boltz, LSP/Amboss, and Wallet integrations can run end-to-end.
2. Recommended for full integration/business-flow validation.

## 10. Smoke Tests
With service running:

1. Health:
```bash
curl http://localhost:8080/actuator/health
```

2. Simple endpoint:
```bash
curl http://localhost:8080/pairs/BTC
```

3. Manual workflow start:
```bash
curl -X POST http://localhost:8080/workflows/start \
  -H "Content-Type: application/json" \
  -d "{\"processId\":\"11111111-1111-1111-1111-111111111111\",\"ownerId\":\"22222222-2222-2222-2222-222222222222\",\"stepKey\":\"await_walloop_deposit\"}"
```

## 11. Startup Success Criteria
Consider startup successful when:
1. No unresolved placeholder errors at boot.
2. No PostgreSQL/Flyway connectivity errors at boot.
3. `GET /actuator/health` returns `UP`.

## 12. Troubleshooting

### 12.1 Unresolved placeholder errors
Example:
`Could not resolve placeholder 'WALLOOP_LIGHTNING_LSP_BASE_URL'`

Root cause:
- A required environment variable is missing.

Action:
1. Define all variables from section 5.
2. Restart the application.

### 12.2 PostgreSQL connection error
Example:
`Connection to localhost:5432 refused`

Root cause:
- Database unavailable or wrong datasource configuration.

Action:
1. Confirm PostgreSQL is running.
2. Validate `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

### 12.3 External integration failures (SideShift, FixedFloat, Boltz, LSP, Wallet)
Root cause:
- `dummy` credentials or unreachable external services.

Action:
1. Keep `dummy` only for local startup checks.
2. Use real credentials/endpoints for end-to-end flow validation.
