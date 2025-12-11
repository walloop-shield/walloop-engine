# walloop-engine
Core Engine Service — centraliza orquestração de lógicas, fluxos de transação e integrações para a plataforma Walloop.

## Stack
- Java 21, Spring Boot 3.3.4, Spring Cloud 2023.0.3
- CSR (Controller-Service-Repository) com Spring Data JPA + PostgreSQL
- Flyway para versionamento de schema (migrations em `src/main/resources/db/migration`)
- RabbitMQ para mensageria
- WebSockets (STOMP/SockJS), OpenFeign clients e (opcional) Eureka para service discovery
- Observabilidade com Micrometer/OTLP (collector OTEL no docker-compose)
- MapStruct para mapeamento de DTOs e Lombok para redução de boilerplate

## Ambiente local com Docker Compose
1. Pré-requisitos: Docker e Docker Compose recentes.
2. Suba o stack (app, Postgres, RabbitMQ e OTEL collector):
   ```bash
   docker compose up --build
   ```
3. Serviços expostos:
   - Aplicação: `http://localhost:8080`
   - PostgreSQL (`walloop_engine` / `walloop_engine`): porta `5432`
   - RabbitMQ: `amqp://localhost:5672` (console em `http://localhost:15672`)
   - OTLP receiver: `http://localhost:4317`
4. Para parar e limpar containers/volumes:
   ```bash
   docker compose down -v
   ```

## Execução manual (sem Docker)
1. Requisitos: JDK 21+ e Maven 3.9+ instalados.
2. Garanta Postgres e RabbitMQ acessíveis localmente. Valores padrão em `application.yml`:
   - JDBC: `jdbc:postgresql://localhost:5432/walloop_engine`
   - Usuário/senha: `walloop_engine` / `walloop_engine`
3. Rode migrations e aplicação:
   ```bash
   mvn spring-boot:run
   ```

## Flyway
- As migrations vivem em `src/main/resources/db/migration`.
- A aplicação inicia com `spring.jpa.hibernate.ddl-auto=validate` e `baseline-on-migrate=true` para manter o schema controlado pelo Flyway.

## Kubernetes
Use `k8s/deployment.yaml` como base. O deployment espera um secret `walloop-db` com `username` e `password`, e endpoints para Postgres, RabbitMQ e, se necessário, Eureka. Ajuste as credenciais para refletir o database `walloop_engine`.
