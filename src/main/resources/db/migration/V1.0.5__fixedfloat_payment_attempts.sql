alter table engine.fixedfloat_order
    add column if not exists payment_attempts integer;
