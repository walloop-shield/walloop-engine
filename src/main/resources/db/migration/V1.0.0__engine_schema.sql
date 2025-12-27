CREATE SCHEMA IF NOT EXISTS engine;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Workflow executions
CREATE TABLE IF NOT EXISTS engine.workflow_executions
(
    id              UUID PRIMARY KEY,
    workflow_name   VARCHAR(255) NOT NULL,
    transaction_id  UUID,
    owner_id        UUID,
    status          VARCHAR(32)  NOT NULL,
    next_step_index INT          NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE TABLE IF NOT EXISTS engine.workflow_step_executions
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_id UUID         NOT NULL REFERENCES engine.workflow_executions (id) ON DELETE CASCADE,
    step_index   INT          NOT NULL,
    step_key     VARCHAR(255) NOT NULL,
    status       VARCHAR(32)  NOT NULL,
    detail       TEXT,
    executed_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_workflow_step_executions_execution_id
    ON engine.workflow_step_executions (execution_id);

-- Liquid wallets
CREATE TABLE IF NOT EXISTS engine.wallet_liquid
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID         NOT NULL,
    owner_id       UUID         NOT NULL,
    address        VARCHAR(200) NOT NULL,
    label          VARCHAR(200) NOT NULL,
    private_key    VARCHAR(200),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wallet_liquid_transaction_id ON engine.wallet_liquid (transaction_id);
CREATE INDEX IF NOT EXISTS idx_wallet_liquid_owner_id ON engine.wallet_liquid (owner_id);

-- Deposit monitor
CREATE TABLE IF NOT EXISTS engine.deposit_monitor
(
    process_id UUID PRIMARY KEY,
    owner_id   UUID         NOT NULL,
    address    VARCHAR(200) NOT NULL,
    network    VARCHAR(64)  NOT NULL,
    status     VARCHAR(32)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_deposit_monitor_owner_id ON engine.deposit_monitor (owner_id);

-- SideShift shift
CREATE TABLE IF NOT EXISTS engine.sideshift_shift
(
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id             UUID         NOT NULL,
    shift_id               VARCHAR(100),
    request_payload        TEXT         NOT NULL,
    response_payload       TEXT         NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deposit_address        VARCHAR(200),
    deposit_network        VARCHAR(64),
    deposit_tx_id          VARCHAR(200),
    status                 VARCHAR(32)  NOT NULL DEFAULT 'CREATED',
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deposit_sent_at        TIMESTAMPTZ,
    settled_at             TIMESTAMPTZ,
    withdraw_requested_at  TIMESTAMPTZ,
    withdraw_completed_at  TIMESTAMPTZ,
    user_ip                VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_sideshift_shift_process_id ON engine.sideshift_shift (process_id);
CREATE INDEX IF NOT EXISTS idx_sideshift_shift_shift_id ON engine.sideshift_shift (shift_id);

-- Lightning invoice
CREATE TABLE IF NOT EXISTS engine.lightning_invoice
(
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id                      UUID         NOT NULL,
    owner_id                        UUID         NOT NULL,
    invoice                         TEXT         NOT NULL,
    status                          VARCHAR(32)  NOT NULL,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    boltz_swap_id                   VARCHAR(100),
    boltz_lockup_address            VARCHAR(200),
    boltz_expected_amount           BIGINT,
    liquid_tx_id                    VARCHAR(200),
    boltz_request_payload           TEXT,
    boltz_response_payload          TEXT,
    boltz_status                    VARCHAR(64),
    boltz_status_payload            TEXT,
    boltz_paid_at                   TIMESTAMPTZ,
    balance_btc                     VARCHAR(100),
    balance_sats                    BIGINT,
    balance_msats                   BIGINT,
    balance_usdt                    VARCHAR(100),
    boltz_decoded_transaction_payload TEXT,
    boltz_paid_amount_sats          BIGINT
);

CREATE INDEX IF NOT EXISTS idx_lightning_invoice_process_id ON engine.lightning_invoice (process_id);

-- SideShift pair simulation
CREATE TABLE IF NOT EXISTS engine.sideshift_pair_simulation
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id       UUID         NOT NULL,
    from_coin        VARCHAR(50)  NOT NULL,
    from_network     VARCHAR(50),
    to_coin          VARCHAR(50)  NOT NULL,
    to_network       VARCHAR(50),
    amount           VARCHAR(100),
    last_balance     VARCHAR(100),
    min              VARCHAR(100),
    max              VARCHAR(100),
    rate             VARCHAR(100),
    deposit_coin     VARCHAR(50),
    settle_coin      VARCHAR(50),
    deposit_network  VARCHAR(50),
    settle_network   VARCHAR(50),
    request_payload  TEXT         NOT NULL,
    response_payload TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sideshift_pair_simulation_process_id
    ON engine.sideshift_pair_simulation (process_id);

-- FixedFloat order
CREATE TABLE IF NOT EXISTS engine.fixedfloat_order
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id       UUID         NOT NULL,
    order_id         VARCHAR(50)  NOT NULL,
    order_token      VARCHAR(200) NOT NULL,
    status           VARCHAR(30),
    from_ccy         VARCHAR(30),
    to_ccy           VARCHAR(30),
    amount           VARCHAR(50),
    to_address       VARCHAR(200),
    confirmations    INTEGER,
    request_payload  TEXT         NOT NULL,
    response_payload TEXT         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_fixedfloat_order_process_id
    ON engine.fixedfloat_order (process_id);

-- Walloop withdrawal
CREATE TABLE IF NOT EXISTS engine.walloop_withdrawal
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id           UUID         NOT NULL,
    destination_address  VARCHAR(200),
    chain                VARCHAR(30),
    requested_at         TIMESTAMPTZ,
    completed_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_walloop_withdrawal_process_id
    ON engine.walloop_withdrawal (process_id);
