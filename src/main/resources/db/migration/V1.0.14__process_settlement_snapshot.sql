CREATE TABLE IF NOT EXISTS engine.process_settlement_snapshot
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id UUID NOT NULL,
    blockchain VARCHAR(64),
    fee_percent NUMERIC(30, 18),
    fee_amount NUMERIC(78, 18),
    rate_idx_dollar_real NUMERIC(30, 12),
    rate_idx_bitcoin_dollar NUMERIC(30, 12),
    rate_idx_bitcoin_chain NUMERIC(30, 12),
    initial_amount_detected NUMERIC(78, 0),
    liquid_tx_url VARCHAR(512),
    liquid_amount NUMERIC(78, 18),
    liquid_fee NUMERIC(78, 18),
    lightning_tx_url VARCHAR(512),
    lightning_amount NUMERIC(78, 18),
    lightning_fee NUMERIC(78, 18),
    conversion_tx_url VARCHAR(512),
    conversion_amount NUMERIC(78, 18),
    conversion_fee NUMERIC(78, 18),
    destination_tx_url VARCHAR(512),
    destination_amount NUMERIC(78, 18),
    destination_fee NUMERIC(78, 18),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_process_settlement_snapshot_process_id
    ON engine.process_settlement_snapshot (process_id);
