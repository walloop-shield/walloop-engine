CREATE TABLE IF NOT EXISTS engine.fee_calculation
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    amount_sats BIGINT NOT NULL,
    amount_btc NUMERIC(30, 12),
    amount_usd NUMERIC(30, 12),
    amount_brl NUMERIC(30, 12),
    fee_percent NUMERIC(10, 6) NOT NULL,
    fee_sats BIGINT NOT NULL,
    fee_btc NUMERIC(30, 12),
    fee_usd NUMERIC(30, 12),
    fee_brl NUMERIC(30, 12),
    payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fee_calculation_process_id
    ON engine.fee_calculation (process_id);
