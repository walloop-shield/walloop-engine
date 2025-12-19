CREATE TABLE IF NOT EXISTS engine.wallet_liquid
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID        NOT NULL,
    owner_id       UUID        NOT NULL,
    address        VARCHAR(200) NOT NULL,
    label          VARCHAR(200) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wallet_liquid_transaction_id ON engine.wallet_liquid (transaction_id);
CREATE INDEX IF NOT EXISTS idx_wallet_liquid_owner_id ON engine.wallet_liquid (owner_id);

