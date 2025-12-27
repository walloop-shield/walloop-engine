ALTER TABLE engine.lightning_invoice
    ADD COLUMN IF NOT EXISTS boltz_swap_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS boltz_lockup_address VARCHAR(200),
    ADD COLUMN IF NOT EXISTS boltz_expected_amount BIGINT,
    ADD COLUMN IF NOT EXISTS liquid_tx_id VARCHAR(200),
    ADD COLUMN IF NOT EXISTS boltz_request_payload TEXT,
    ADD COLUMN IF NOT EXISTS boltz_response_payload TEXT;
