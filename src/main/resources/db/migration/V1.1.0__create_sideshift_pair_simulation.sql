CREATE TABLE IF NOT EXISTS engine.sideshift_pair_simulation
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id       UUID         NOT NULL,
    from_coin        VARCHAR(50)  NOT NULL,
    from_network     VARCHAR(50),
    to_coin          VARCHAR(50)  NOT NULL,
    to_network       VARCHAR(50),
    request_payload  TEXT         NOT NULL,
    response_payload TEXT         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sideshift_pair_simulation_process_id
    ON engine.sideshift_pair_simulation (process_id);
