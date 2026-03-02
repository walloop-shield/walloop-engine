ALTER TABLE engine.lightning_inbound_liquidity_requests
    ADD COLUMN IF NOT EXISTS node_address VARCHAR(200);
