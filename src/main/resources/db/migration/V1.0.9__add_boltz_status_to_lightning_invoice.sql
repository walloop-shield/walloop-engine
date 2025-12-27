ALTER TABLE engine.lightning_invoice
    ADD COLUMN IF NOT EXISTS boltz_status VARCHAR(64),
    ADD COLUMN IF NOT EXISTS boltz_status_payload TEXT,
    ADD COLUMN IF NOT EXISTS boltz_paid_at TIMESTAMPTZ;
