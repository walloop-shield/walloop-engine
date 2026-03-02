ALTER TABLE engine.lightning_invoice
    ADD COLUMN IF NOT EXISTS swap_claim_public_key VARCHAR(130),
    ADD COLUMN IF NOT EXISTS swap_claim_tree TEXT,
    ADD COLUMN IF NOT EXISTS swap_claim_pub_nonce VARCHAR(200),
    ADD COLUMN IF NOT EXISTS swap_claim_tx_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS swap_claim_partial_signature VARCHAR(200),
    ADD COLUMN IF NOT EXISTS swap_claim_status VARCHAR(64),
    ADD COLUMN IF NOT EXISTS swap_claim_submitted_at TIMESTAMPTZ;
