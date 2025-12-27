CREATE TABLE IF NOT EXISTS engine.sideshift_shift
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_id       UUID         NOT NULL,
    shift_id         VARCHAR(100),
    request_payload  TEXT         NOT NULL,
    response_payload TEXT         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sideshift_shift_process_id ON engine.sideshift_shift (process_id);
CREATE INDEX IF NOT EXISTS idx_sideshift_shift_shift_id ON engine.sideshift_shift (shift_id);
