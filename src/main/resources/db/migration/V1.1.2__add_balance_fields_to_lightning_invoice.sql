ALTER TABLE engine.lightning_invoice
    ADD COLUMN IF NOT EXISTS balance_btc VARCHAR(100),
    ADD COLUMN IF NOT EXISTS balance_sats BIGINT,
    ADD COLUMN IF NOT EXISTS balance_msats BIGINT,
    ADD COLUMN IF NOT EXISTS balance_usdt VARCHAR(100);
