CREATE TABLE IF NOT EXISTS engine.deposit_monitor
(
    process_id UUID PRIMARY KEY,
    owner_id   UUID        NOT NULL,
    address    VARCHAR(200) NOT NULL,
    network    VARCHAR(64)  NOT NULL,
    status     VARCHAR(32)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_deposit_monitor_owner_id ON engine.deposit_monitor (owner_id);
