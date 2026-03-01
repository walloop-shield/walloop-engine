# Contributing

## Scope

This repository contains the Walloop Engine service (Java 21, Spring Boot), responsible for workflow orchestration.

## Development Setup

1. Install Java 21 and Maven 3.9+
2. Start dependencies (`Postgres`, `RabbitMQ`) via local stack
3. Run:

```bash
mvn clean test
mvn spring-boot:run
```

## Branch and PR Flow

1. Create a feature branch from `main`
2. Keep commits focused and descriptive
3. Open PR to `main` with:
   - Problem statement
   - Implementation summary
   - Risks/rollback notes
   - Test evidence

## Quality Requirements

- All tests must pass
- New behavior must include automated tests
- Keep naming and test descriptions business-friendly (Given-When-Then)
- Follow project logging pattern:
  - `<ClassName> - <action> - key=value key=value`

## Security Requirements

- Never commit secrets, tokens, certificates, or private keys
- Use environment variables/Fly secrets for sensitive configuration
- Do not log sensitive values (keys, full invoices, private material)

## Commit Style

Recommended format:

```text
type(scope): short description
```

Examples:
- `fix(workflow): avoid duplicate withdrawal trigger`
- `chore(security): remove hardcoded defaults`
