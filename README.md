# walloop-engine
Core Engine Service que centraliza orquestracao de logicas, fluxos de transacao e integracoes para a plataforma Walloop.

## Stack
- Java 21, Spring Boot 3.3.4, Spring Cloud 2023.0.3
- CSR (Controller-Service-Repository) com Spring Data JPA + PostgreSQL (db `walloop`, schema `engine`, IDs UUID)
- Flyway para versionamento de schema (migrations em `src/main/resources/db/migration`)
- RabbitMQ para mensageria
- WebSockets (STOMP/SockJS), OpenFeign clients e (opcional) Eureka para service discovery
- Observabilidade com Micrometer/OTLP (collector OTEL no docker-compose)
- MapStruct para mapeamento de DTOs e Lombok para reduzir boilerplate

## Ambiente local com Docker Compose
1. Pre-requisitos: Docker e Docker Compose recentes.
2. Suba o stack (app, Postgres, RabbitMQ e OTEL collector):
   ```bash
   docker compose up --build
   ```
3. Servicos expostos:
   - Aplicacao: `http://localhost:8080`
   - PostgreSQL: db `walloop`, schema `engine`, usuario/senha `walloop` / `walloop` (porta `5432`)
   - RabbitMQ: `amqp://localhost:5672` (console em `http://localhost:15672`)
   - OTLP receiver: `http://localhost:4317`
4. Para parar e limpar containers/volumes:
   ```bash
   docker compose down -v
   ```

## Execucao manual (sem Docker)
1. Requisitos: JDK 21+ e Maven 3.9+ instalados.
2. Garanta Postgres e RabbitMQ acessiveis localmente. Valores padrao em `application.yml`:
   - JDBC: `jdbc:postgresql://localhost:5432/walloop`
   - Schema padrao: `engine` (hibernate.default_schema / Flyway default-schema)
   - Usuario/senha: `walloop` / `walloop`
3. Rode migrations e aplicacao:
   ```bash
   mvn spring-boot:run
   ```

## Lightning (LND gRPC)
O engine gera invoices via gRPC direto no LND. A configuracao fica no `application.yml`
e deve ser fornecida por variaveis de ambiente.

### Local (arquivos)
Para rodar localmente, use caminhos para os arquivos `tls.cert` e `admin.macaroon`:

```
LND_GRPC_HOST=localhost
LND_GRPC_PORT=10009
LND_GRPC_CERT_FILE=/caminho/para/tls.cert
LND_GRPC_MACAROON_FILE=/caminho/para/admin.macaroon
```

### Producao (base64)
Em producao, use base64 via secrets (ex: Fly secrets). Isso evita manter arquivos
sensiveis no filesystem da aplicacao:

```
LND_GRPC_HOST=walloop-lightning-node.internal
LND_GRPC_PORT=10009
LND_GRPC_CERT_BASE64=...
LND_GRPC_MACAROON_BASE64=...
```

Observacao: o macaroon com permissao admin da acesso total ao LND. Trate como segredo.

### Teste rapido (local)
Para validar o gRPC, rode a aplicacao e crie uma invoice pelo fluxo do engine.
Se o LND estiver acessivel e o macaroon valido, a invoice sera criada sem erro.

## Lightning / LSP (Amboss Magma)
O engine pode contratar liquidez inbound via LSP usando a API GraphQL da Amboss.
O gatilho acontece no step `create_lightning_invoice`: se a liquidez inbound
ficar abaixo do alvo e nao houver pedido pendente, o engine cria uma ordem no LSP
e agenda retry.

Configuracoes principais (application.yml):
- `walloop.lightning.inbound-target-sats`: alvo global de inbound.
- `walloop.lightning.inbound-check-enabled`: habilita o check de inbound antes da invoice.
- `walloop.lightning.lsp.base-url`: base URL da API GraphQL.
- `walloop.lightning.lsp.api-key`: API key (Bearer).
- `walloop.lightning.lsp.node-pubkey`: pubkey do node (override).

Observacao: o endpoint GraphQL e fixo no codigo (`/graphql`).

## Cache (Caffeine)
O engine usa Caffeine para reduzir chamadas externas em consultas de par e cotacao.

Caches ativos:
- `pairAvailability`: usado em `PairAvailabilityService` (TTL configuravel).
- `fxRates`: usado pelo `CoinCapFxRateProvider` (TTL configuravel).

Config (application.yml):
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

Observacao: altere os TTLs via `walloop.pair-availability.cache-seconds` e
`walloop.fee.coincap.cache-seconds` conforme a necessidade do ambiente.

## Flyway
- As migrations vivem em `src/main/resources/db/migration`.
- Migracao baseline `V1.0.0__init_customers.sql` cria o schema `engine`, habilita `pgcrypto` e cria a tabela `customers` com chave primaria UUID.
- A aplicacao inicia com `spring.jpa.hibernate.ddl-auto=validate` e `baseline-on-migrate=true` para manter o schema controlado pelo Flyway.

## Liquid + Bitcoin (Docker Compose)
O `docker-compose.yml` inclui `vulpemventures/liquid` e `vulpemventures/bitcoin` para validar pegins via RPC.

### Testnet (config atual)
- `liquid-node`: `-chain=liquidtestnet` + `-validatepegin=1` com `mainchainrpc*` apontando para `bitcoin-node`.
- `bitcoin-node`: `-testnet` e RPC em `18332`.

### Mainnet (como mudar)
1. Em `liquid-node`, troque `-chain=liquidtestnet` por `-chain=liquidv1`.
2. Em `bitcoin-node`, remova `-testnet` (ou use `-mainnet`) e use RPC `8332`.
3. Se expor portas, ajuste `18332:18332` para `8332:8332`.

## Kubernetes
Use `kubernetes/deployment.yaml` como base. O deployment espera um secret `walloop-db` com `username` e `password`, e endpoints para Postgres, RabbitMQ e, se necessario, Eureka. Ajuste as credenciais para refletir o database `walloop` e o schema `engine` (variaveis `SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA`, `SPRING_FLYWAY_DEFAULT_SCHEMA` e `SPRING_FLYWAY_SCHEMAS`).

