ALTER TABLE engine.lightning_invoice
    ADD COLUMN IF NOT EXISTS boltz_decoded_transaction_payload TEXT,
    ADD COLUMN IF NOT EXISTS boltz_paid_amount_sats BIGINT;
