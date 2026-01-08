ALTER TABLE engine.fixedfloat_order
  ADD COLUMN IF NOT EXISTS payment_request TEXT,
  ADD COLUMN IF NOT EXISTS payment_status VARCHAR(32),
  ADD COLUMN IF NOT EXISTS payment_error TEXT,
  ADD COLUMN IF NOT EXISTS payment_preimage VARCHAR(128),
  ADD COLUMN IF NOT EXISTS payment_hash VARCHAR(128),
  ADD COLUMN IF NOT EXISTS payment_attempted_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS payment_completed_at TIMESTAMPTZ;
