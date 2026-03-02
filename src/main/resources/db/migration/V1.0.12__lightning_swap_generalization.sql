ALTER TABLE engine.lightning_invoice
    ADD COLUMN IF NOT EXISTS swap_partner VARCHAR(32),
    ADD COLUMN IF NOT EXISTS swap_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS swap_lockup_address VARCHAR(200),
    ADD COLUMN IF NOT EXISTS swap_expected_amount BIGINT,
    ADD COLUMN IF NOT EXISTS swap_fee_percentage DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS swap_miner_fees BIGINT,
    ADD COLUMN IF NOT EXISTS swap_pair_hash VARCHAR(100),
    ADD COLUMN IF NOT EXISTS swap_request_payload TEXT,
    ADD COLUMN IF NOT EXISTS swap_response_payload TEXT,
    ADD COLUMN IF NOT EXISTS swap_status VARCHAR(64),
    ADD COLUMN IF NOT EXISTS swap_decoded_transaction_payload TEXT,
    ADD COLUMN IF NOT EXISTS swap_paid_amount_sats BIGINT,
    ADD COLUMN IF NOT EXISTS swap_status_payload TEXT,
    ADD COLUMN IF NOT EXISTS swap_paid_at TIMESTAMPTZ;

UPDATE engine.lightning_invoice
SET
    swap_partner = COALESCE(swap_partner, CASE WHEN boltz_swap_id IS NOT NULL THEN 'BOLTZ' END),
    swap_id = COALESCE(swap_id, boltz_swap_id),
    swap_lockup_address = COALESCE(swap_lockup_address, boltz_lockup_address),
    swap_expected_amount = COALESCE(swap_expected_amount, boltz_expected_amount),
    swap_fee_percentage = COALESCE(swap_fee_percentage, boltz_fee_percentage),
    swap_miner_fees = COALESCE(swap_miner_fees, boltz_miner_fees),
    swap_pair_hash = COALESCE(swap_pair_hash, boltz_pair_hash),
    swap_request_payload = COALESCE(swap_request_payload, boltz_request_payload),
    swap_response_payload = COALESCE(swap_response_payload, boltz_response_payload),
    swap_status = COALESCE(swap_status, boltz_status),
    swap_decoded_transaction_payload = COALESCE(swap_decoded_transaction_payload, boltz_decoded_transaction_payload),
    swap_paid_amount_sats = COALESCE(swap_paid_amount_sats, boltz_paid_amount_sats),
    swap_status_payload = COALESCE(swap_status_payload, boltz_status_payload),
    swap_paid_at = COALESCE(swap_paid_at, boltz_paid_at);

CREATE INDEX IF NOT EXISTS idx_lightning_invoice_swap_id ON engine.lightning_invoice (swap_id);

ALTER TABLE engine.lightning_invoice
    DROP COLUMN IF EXISTS boltz_swap_id,
    DROP COLUMN IF EXISTS boltz_lockup_address,
    DROP COLUMN IF EXISTS boltz_expected_amount,
    DROP COLUMN IF EXISTS boltz_fee_percentage,
    DROP COLUMN IF EXISTS boltz_miner_fees,
    DROP COLUMN IF EXISTS boltz_pair_hash,
    DROP COLUMN IF EXISTS boltz_request_payload,
    DROP COLUMN IF EXISTS boltz_response_payload,
    DROP COLUMN IF EXISTS boltz_status,
    DROP COLUMN IF EXISTS boltz_decoded_transaction_payload,
    DROP COLUMN IF EXISTS boltz_paid_amount_sats,
    DROP COLUMN IF EXISTS boltz_status_payload,
    DROP COLUMN IF EXISTS boltz_paid_at;
