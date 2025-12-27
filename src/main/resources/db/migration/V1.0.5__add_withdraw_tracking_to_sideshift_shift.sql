ALTER TABLE engine.sideshift_shift
    ADD COLUMN IF NOT EXISTS withdraw_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS withdraw_completed_at TIMESTAMPTZ;

UPDATE engine.sideshift_shift
SET status = 'WITHDRAW_COMPLETED',
    updated_at = NOW()
WHERE status = 'DEPOSIT_SENT';

UPDATE engine.sideshift_shift
SET status = 'CREATED',
    updated_at = NOW()
WHERE status IS NULL;
