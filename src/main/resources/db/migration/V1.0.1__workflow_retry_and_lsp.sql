ALTER TABLE engine.workflow_executions
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_workflow_executions_next_retry_at
    ON engine.workflow_executions (next_retry_at);

CREATE TABLE IF NOT EXISTS engine.lightning_inbound_liquidity_requests
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id           UUID         NOT NULL,
    provider             VARCHAR(100) NOT NULL,
    status               VARCHAR(32)  NOT NULL,
    target_inbound_sats  BIGINT       NOT NULL,
    current_inbound_sats BIGINT       NOT NULL,
    requested_sats       BIGINT       NOT NULL,
    external_id          VARCHAR(200),
    response_payload     TEXT,
    error_message        TEXT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_lightning_inbound_liquidity_requests_status_created_at
    ON engine.lightning_inbound_liquidity_requests (status, created_at);

CREATE INDEX IF NOT EXISTS idx_lightning_inbound_liquidity_requests_process_id
    ON engine.lightning_inbound_liquidity_requests (process_id);
