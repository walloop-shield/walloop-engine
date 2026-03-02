ALTER TABLE engine.lightning_invoice
    ADD COLUMN IF NOT EXISTS liquid_fee_sats BIGINT,
    ADD COLUMN IF NOT EXISTS liquid_fee_conf_target INTEGER,
    ADD COLUMN IF NOT EXISTS liquid_fee_vbytes INTEGER;
