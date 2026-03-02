ALTER TABLE engine.fee_calculation
    ADD COLUMN onchain_fee_sats BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN total_fee_sats BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN total_fee_btc NUMERIC(30, 12),
    ADD COLUMN total_fee_usd NUMERIC(30, 12),
    ADD COLUMN total_fee_brl NUMERIC(30, 12);
