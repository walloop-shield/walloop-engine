ALTER TABLE engine.workflow_executions
    ADD COLUMN IF NOT EXISTS transaction_id UUID;

ALTER TABLE engine.workflow_executions
    ADD COLUMN IF NOT EXISTS owner_id UUID;

CREATE INDEX IF NOT EXISTS idx_workflow_executions_transaction_id
    ON engine.workflow_executions (transaction_id);

CREATE INDEX IF NOT EXISTS idx_workflow_executions_owner_id
    ON engine.workflow_executions (owner_id);

