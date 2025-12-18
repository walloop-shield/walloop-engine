CREATE TABLE IF NOT EXISTS engine.workflow_executions
(
    id              UUID PRIMARY KEY,
    workflow_name   VARCHAR(255) NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    next_step_index INT          NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE TABLE IF NOT EXISTS engine.workflow_step_executions
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_id UUID        NOT NULL REFERENCES engine.workflow_executions (id) ON DELETE CASCADE,
    step_index   INT         NOT NULL,
    step_key     VARCHAR(255) NOT NULL,
    status       VARCHAR(32) NOT NULL,
    detail       TEXT,
    executed_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_workflow_step_executions_execution_id
    ON engine.workflow_step_executions (execution_id);

