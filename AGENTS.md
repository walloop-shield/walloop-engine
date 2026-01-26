# AGENTS.md (walloop-engine)

## Overview
- Spring Boot service (Java 21) that orchestrates Walloop transaction workflows.
- Main workflow: `WalloopEngineWorkflow` in `src/main/java/com/walloop/engine/workflow/walloop/WalloopEngineWorkflow.java`.
- Entry point: `src/main/java/com/walloop/engine/WalloopEngineApplication.java` (Feign + Scheduling enabled).
- Database: Postgres schema `engine` (migrations in `src/main/resources/db/migration`).
- Messaging: RabbitMQ (exchanges `walloop.core.exchange` and `walloop.engine.exchange`).

## Main flow (workflow walloop_engine_workflow_v1)
Orchestrated by `SequentialWorkflowOrchestrator` (sequential execution; stops at `WAITING` and resumes via events/schedulers).
Step order:
1) `await_walloop_deposit`: creates/reads deposit monitor and publishes `DepositMonitorMessage` to `walloop-core`.
2) `calculate_fees`: calculates fees (placeholder).
3) `create_liquid_wallet`: creates liquid wallet using `walloop-liquid-node`.
4) `create_lightning_invoice`: creates LN invoice using `walloop-lightning-node`.
5) `swap_to_liquid`: creates swap via partner + requests withdraw in `walloop-core` (waits for confirmation).
6) `pay_liquid_to_lightning`: creates lightning swap via partner and sends L-BTC to lockup.
7) `convert_lightning_to_walloop`: uses conversion partner (FixedFloat) to send to a new destination wallet.
8) `return_to_main_wallet`: requests withdraw to the customer main wallet.

## Messaging (RabbitMQ)
Engine consumers:
- `transaction.engine.queue` (routing key `engine.initialization`): starts workflow via `TransactionStartMessage` (processId, ownerId, sessionToken).
- `engine.deposit.queue` (routing key `monitor.detected`): `DepositDetectedMessage` (processId) resumes the workflow.
- `engine.withdraw.queue` (routing key `balance.sent`): `WithdrawCompletedMessage` (processId) updates swap order/Withdrawal and resumes workflow.

Engine publishes (to core):
- `monitor.waiting` with `DepositMonitorMessage` (address, network, owner, processId).
- `balance.process` with `WithdrawRequestMessage` (processId, destination).

Exchange/queue config:
- `src/main/java/com/walloop/engine/messaging/*MessagingConfiguration.java`.
- DLQ:
  - `walloop.engine.dlx`
  - `transaction.engine.queue` -> `transaction.engine.dlq` (routing key `engine.initialization.dlq`)
  - `engine.deposit.queue` -> `engine.deposit.dlq` (routing key `monitor.detected.dlq`)
  - `engine.withdraw.queue` -> `engine.withdraw.dlq` (routing key `balance.sent.dlq`)
- Listener retry config:
  - `spring.rabbitmq.listener.simple.retry.*` in `src/main/resources/application.yml`
  - Env overrides: `WALLOOP_RABBITMQ_RETRY_MAX_ATTEMPTS`, `WALLOOP_RABBITMQ_RETRY_INITIAL_INTERVAL`,
    `WALLOOP_RABBITMQ_RETRY_MULTIPLIER`, `WALLOOP_RABBITMQ_RETRY_MAX_INTERVAL`
  - `default-requeue-rejected=false` so failures after retries go to DLQ

## External integrations
- Swap (partner): `Swap*` + `SideShift*` (swap to Liquid).
- Conversion partner: `Conversion*` + `FixedFloat*` (conversion + send to destination wallet).
- Lightning swap partner: `LightningSwap*` + `Boltz*` (pay LN via L-BTC).
- Liquid RPC: `liquid.rpc.*` (Liquid node) `walloop-liquid-node`.
- LND gRPC: `org.tbk.lightning.lnd.grpc.*` (create invoice) `walloop-lightning-node`.
- Wallet (network catalog): Feign `WalletNetworkClient` at `/v1/chains` using `walloop.wallet.base-url`.

## Schedulers
Resume workflows when external status changes:
- `ConversionStatusScheduler`
- `SwapStatusScheduler`
- `LightningSwapStatusScheduler`

## Key config (application.yml)
- Postgres: `SPRING_DATASOURCE_*`, schema `engine`.
- RabbitMQ: `SPRING_RABBITMQ_*`.
- LND gRPC: `LND_GRPC_*`.
- Liquid RPC: `LIQUID_RPC_*`.
- Wallet network catalog: `WALLOOP_WALLET_BASE_URL` (Fly internal default `http://walloop-wallet.internal:8080`).

## How to run
- Local: `mvn spring-boot:run` (with Postgres + RabbitMQ running).
- Docker: `docker compose up --build`.

## Where to change flow
- Workflow order: `WalloopEngineWorkflow`.
- Step logic: `src/main/java/com/walloop/engine/workflow/walloop/steps/*`.
- Messages/queues: `src/main/java/com/walloop/engine/messaging/*`.
