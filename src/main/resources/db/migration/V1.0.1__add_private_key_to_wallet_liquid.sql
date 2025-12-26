ALTER TABLE engine.wallet_liquid
    ADD COLUMN IF NOT EXISTS private_key VARCHAR(200);
