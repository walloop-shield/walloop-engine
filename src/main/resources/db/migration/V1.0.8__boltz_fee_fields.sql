ALTER TABLE engine.lightning_invoice
    ADD COLUMN IF NOT EXISTS boltz_fee_percentage DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS boltz_miner_fees BIGINT,
    ADD COLUMN IF NOT EXISTS boltz_pair_hash VARCHAR(100);
