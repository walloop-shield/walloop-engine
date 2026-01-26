CREATE TABLE IF NOT EXISTS engine.swap_order
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id            UUID         NOT NULL,
    partner               VARCHAR(32)  NOT NULL,
    partner_order_id      VARCHAR(100),
    deposit_address       VARCHAR(200),
    deposit_network       VARCHAR(64),
    deposit_tx_id         VARCHAR(200),
    user_ip               VARCHAR(64),
    status                VARCHAR(32)  NOT NULL DEFAULT 'CREATED',
    request_payload       TEXT         NOT NULL,
    response_payload      TEXT         NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    withdraw_requested_at TIMESTAMPTZ,
    withdraw_completed_at TIMESTAMPTZ,
    settled_at            TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_swap_order_process_id ON engine.swap_order (process_id);
CREATE INDEX IF NOT EXISTS idx_swap_order_partner_order_id ON engine.swap_order (partner_order_id);
CREATE INDEX IF NOT EXISTS idx_swap_order_status ON engine.swap_order (status);
CREATE INDEX IF NOT EXISTS idx_swap_order_partner_status ON engine.swap_order (partner, status);

CREATE TABLE IF NOT EXISTS engine.swap_quote
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id      UUID         NOT NULL,
    partner         VARCHAR(32)  NOT NULL,
    from_coin       VARCHAR(50)  NOT NULL,
    from_network    VARCHAR(50),
    to_coin         VARCHAR(50)  NOT NULL,
    to_network      VARCHAR(50),
    amount          VARCHAR(100),
    last_balance    VARCHAR(100),
    min             VARCHAR(100),
    max             VARCHAR(100),
    rate            VARCHAR(100),
    deposit_coin    VARCHAR(50),
    settle_coin     VARCHAR(50),
    deposit_network VARCHAR(50),
    settle_network  VARCHAR(50),
    request_payload TEXT         NOT NULL,
    response_payload TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_swap_quote_process_id ON engine.swap_quote (process_id);
CREATE INDEX IF NOT EXISTS idx_swap_quote_partner ON engine.swap_quote (partner);

INSERT INTO engine.swap_order
(
    process_id,
    partner,
    partner_order_id,
    deposit_address,
    deposit_network,
    deposit_tx_id,
    user_ip,
    status,
    request_payload,
    response_payload,
    created_at,
    updated_at,
    withdraw_requested_at,
    withdraw_completed_at,
    settled_at
)
SELECT
    process_id,
    'SIDESHIFT',
    shift_id,
    deposit_address,
    deposit_network,
    deposit_tx_id,
    user_ip,
    status,
    request_payload,
    response_payload,
    created_at,
    updated_at,
    withdraw_requested_at,
    withdraw_completed_at,
    settled_at
FROM engine.sideshift_shift
WHERE process_id IS NOT NULL;

INSERT INTO engine.swap_quote
(
    process_id,
    partner,
    from_coin,
    from_network,
    to_coin,
    to_network,
    amount,
    last_balance,
    min,
    max,
    rate,
    deposit_coin,
    settle_coin,
    deposit_network,
    settle_network,
    request_payload,
    response_payload,
    created_at
)
SELECT
    process_id,
    'SIDESHIFT',
    from_coin,
    from_network,
    to_coin,
    to_network,
    amount,
    last_balance,
    min,
    max,
    rate,
    deposit_coin,
    settle_coin,
    deposit_network,
    settle_network,
    request_payload,
    response_payload,
    created_at
FROM engine.sideshift_pair_simulation
WHERE process_id IS NOT NULL;
