CREATE TABLE IF NOT EXISTS engine.lightning_invoice
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id UUID         NOT NULL,
    owner_id   UUID         NOT NULL,
    invoice    TEXT         NOT NULL,
    status     VARCHAR(32)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_lightning_invoice_process_id ON engine.lightning_invoice (process_id);
