CREATE TABLE IF NOT EXISTS engine.conversion_order
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id           UUID         NOT NULL,
    partner              VARCHAR(32)  NOT NULL,
    partner_order_id     VARCHAR(50)  NOT NULL,
    partner_order_token  VARCHAR(200) NOT NULL,
    status               VARCHAR(30),
    from_ccy             VARCHAR(30),
    to_ccy               VARCHAR(30),
    amount               VARCHAR(50),
    to_address           VARCHAR(200),
    confirmations        INTEGER,
    payment_request      TEXT,
    payment_status       VARCHAR(32),
    payment_error        TEXT,
    payment_preimage     VARCHAR(128),
    payment_hash         VARCHAR(128),
    payment_attempted_at TIMESTAMPTZ,
    payment_attempts     INTEGER,
    payment_completed_at TIMESTAMPTZ,
    request_payload      TEXT         NOT NULL,
    response_payload     TEXT         NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at         TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_conversion_order_process_id
    ON engine.conversion_order (process_id);

CREATE INDEX IF NOT EXISTS idx_conversion_order_process_partner
    ON engine.conversion_order (process_id, partner);

CREATE INDEX IF NOT EXISTS idx_conversion_order_completed_at
    ON engine.conversion_order (completed_at);

INSERT INTO engine.conversion_order (
    id,
    process_id,
    partner,
    partner_order_id,
    partner_order_token,
    status,
    from_ccy,
    to_ccy,
    amount,
    to_address,
    confirmations,
    payment_request,
    payment_status,
    payment_error,
    payment_preimage,
    payment_hash,
    payment_attempted_at,
    payment_attempts,
    payment_completed_at,
    request_payload,
    response_payload,
    created_at,
    updated_at,
    completed_at
)
SELECT
    id,
    process_id,
    'FIXEDFLOAT',
    order_id,
    order_token,
    status,
    from_ccy,
    to_ccy,
    amount,
    to_address,
    confirmations,
    payment_request,
    payment_status,
    payment_error,
    payment_preimage,
    payment_hash,
    payment_attempted_at,
    payment_attempts,
    payment_completed_at,
    request_payload,
    response_payload,
    created_at,
    updated_at,
    completed_at
FROM engine.fixedfloat_order;

DROP TABLE IF EXISTS engine.fixedfloat_order;
