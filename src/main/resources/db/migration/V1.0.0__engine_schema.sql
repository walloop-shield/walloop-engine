CREATE SCHEMA IF NOT EXISTS engine;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Workflow executions
CREATE TABLE IF NOT EXISTS engine.workflow_executions
(
    id              UUID PRIMARY KEY,
    workflow_name   VARCHAR(255) NOT NULL,
    transaction_id  UUID,
    owner_id        UUID,
    status          VARCHAR(32)  NOT NULL,
    next_step_index INT          NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE TABLE IF NOT EXISTS engine.workflow_step_executions
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_id UUID        NOT NULL REFERENCES engine.workflow_executions (id) ON DELETE CASCADE,
    step_index   INT         NOT NULL,
    step_key     VARCHAR(255) NOT NULL,
    status       VARCHAR(32) NOT NULL,
    detail       TEXT,
    executed_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_workflow_step_executions_execution_id
    ON engine.workflow_step_executions (execution_id);

-- Liquid wallets
CREATE TABLE IF NOT EXISTS engine.wallet_liquid
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID         NOT NULL,
    owner_id       UUID         NOT NULL,
    address        VARCHAR(200) NOT NULL,
    label          VARCHAR(200) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wallet_liquid_transaction_id ON engine.wallet_liquid (transaction_id);
CREATE INDEX IF NOT EXISTS idx_wallet_liquid_owner_id ON engine.wallet_liquid (owner_id);
