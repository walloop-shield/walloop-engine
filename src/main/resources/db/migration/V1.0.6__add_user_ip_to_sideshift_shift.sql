ALTER TABLE engine.sideshift_shift
    ADD COLUMN IF NOT EXISTS user_ip VARCHAR(64);
