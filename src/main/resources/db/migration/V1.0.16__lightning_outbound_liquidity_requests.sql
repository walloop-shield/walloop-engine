CREATE TABLE IF NOT EXISTS engine.lightning_outbound_liquidity_requests
(
    id                                 UUID PRIMARY KEY,
    provider                           VARCHAR(64)              NOT NULL,
    target_node_pubkey                 VARCHAR(128)             NOT NULL,
    target_channel_id                  BIGINT                   NOT NULL,
    target_channel_capacity_sats       BIGINT                   NOT NULL,
    target_channel_local_balance_sats  BIGINT                   NOT NULL,
    target_channel_remote_balance_sats BIGINT                   NOT NULL,
    target_channel_local_reserve_sats  BIGINT                   NOT NULL,
    target_channel_commit_fee_sats     BIGINT                   NOT NULL,
    target_channel_spendable_sats      BIGINT                   NOT NULL,
    requested_sats                     BIGINT                   NOT NULL,
    invoice                            TEXT                     NOT NULL,
    payment_hash                       VARCHAR(128)             NOT NULL,
    invoice_memo                       VARCHAR(255),
    invoice_expiry_seconds             BIGINT                   NOT NULL,
    status                             VARCHAR(32)              NOT NULL,
    error_message                      TEXT,
    poll_attempts                      INTEGER                  NOT NULL DEFAULT 0,
    last_polled_at                     TIMESTAMP WITH TIME ZONE,
    created_at                         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                         TIMESTAMP WITH TIME ZONE NOT NULL,
    paid_at                            TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_lightning_outbound_liquidity_requests_status_created_at
    ON engine.lightning_outbound_liquidity_requests (status, created_at);

CREATE INDEX IF NOT EXISTS idx_lightning_outbound_liquidity_requests_target_node_pubkey
    ON engine.lightning_outbound_liquidity_requests (target_node_pubkey);
