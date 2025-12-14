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

## Flyway
- As migrations vivem em `src/main/resources/db/migration`.
- Migracao baseline `V1.0.0__init_customers.sql` cria o schema `engine`, habilita `pgcrypto` e cria a tabela `customers` com chave primaria UUID.
- A aplicacao inicia com `spring.jpa.hibernate.ddl-auto=validate` e `baseline-on-migrate=true` para manter o schema controlado pelo Flyway.

## Kubernetes
Use `kubernetes/deployment.yaml` como base. O deployment espera um secret `walloop-db` com `username` e `password`, e endpoints para Postgres, RabbitMQ e, se necessario, Eureka. Ajuste as credenciais para refletir o database `walloop` e o schema `engine` (variaveis `SPRING_JPA_PROPERTIES_HIBERNATE_DEFAULT_SCHEMA`, `SPRING_FLYWAY_DEFAULT_SCHEMA` e `SPRING_FLYWAY_SCHEMAS`).
