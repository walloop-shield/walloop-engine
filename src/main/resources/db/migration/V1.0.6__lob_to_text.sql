ALTER TABLE engine.fee_calculation
    ALTER COLUMN payload TYPE TEXT
        USING (
            CASE
                WHEN payload IS NULL THEN NULL
                WHEN pg_typeof(payload) = 'oid'::regtype THEN convert_from(lo_get(payload::oid), 'UTF8')
                ELSE payload::text
            END
        );

ALTER TABLE engine.fixedfloat_order
    ALTER COLUMN payment_request TYPE TEXT
        USING (
            CASE
                WHEN payment_request IS NULL THEN NULL
                WHEN pg_typeof(payment_request) = 'oid'::regtype THEN convert_from(lo_get(payment_request::oid), 'UTF8')
                ELSE payment_request::text
            END
        ),
    ALTER COLUMN payment_error TYPE TEXT
        USING (
            CASE
                WHEN payment_error IS NULL THEN NULL
                WHEN pg_typeof(payment_error) = 'oid'::regtype THEN convert_from(lo_get(payment_error::oid), 'UTF8')
                ELSE payment_error::text
            END
        ),
    ALTER COLUMN request_payload TYPE TEXT
        USING (
            CASE
                WHEN request_payload IS NULL THEN NULL
                WHEN pg_typeof(request_payload) = 'oid'::regtype THEN convert_from(lo_get(request_payload::oid), 'UTF8')
                ELSE request_payload::text
            END
        ),
    ALTER COLUMN response_payload TYPE TEXT
        USING (
            CASE
                WHEN response_payload IS NULL THEN NULL
                WHEN pg_typeof(response_payload) = 'oid'::regtype THEN convert_from(lo_get(response_payload::oid), 'UTF8')
                ELSE response_payload::text
            END
        );

ALTER TABLE engine.lightning_inbound_liquidity_requests
    ALTER COLUMN response_payload TYPE TEXT
        USING (
            CASE
                WHEN response_payload IS NULL THEN NULL
                WHEN pg_typeof(response_payload) = 'oid'::regtype THEN convert_from(lo_get(response_payload::oid), 'UTF8')
                ELSE response_payload::text
            END
        );

ALTER TABLE engine.sideshift_pair_simulation
    ALTER COLUMN request_payload TYPE TEXT
        USING (
            CASE
                WHEN request_payload IS NULL THEN NULL
                WHEN pg_typeof(request_payload) = 'oid'::regtype THEN convert_from(lo_get(request_payload::oid), 'UTF8')
                ELSE request_payload::text
            END
        ),
    ALTER COLUMN response_payload TYPE TEXT
        USING (
            CASE
                WHEN response_payload IS NULL THEN NULL
                WHEN pg_typeof(response_payload) = 'oid'::regtype THEN convert_from(lo_get(response_payload::oid), 'UTF8')
                ELSE response_payload::text
            END
        );

ALTER TABLE engine.sideshift_shift
    ALTER COLUMN request_payload TYPE TEXT
        USING (
            CASE
                WHEN request_payload IS NULL THEN NULL
                WHEN pg_typeof(request_payload) = 'oid'::regtype THEN convert_from(lo_get(request_payload::oid), 'UTF8')
                ELSE request_payload::text
            END
        ),
    ALTER COLUMN response_payload TYPE TEXT
        USING (
            CASE
                WHEN response_payload IS NULL THEN NULL
                WHEN pg_typeof(response_payload) = 'oid'::regtype THEN convert_from(lo_get(response_payload::oid), 'UTF8')
                ELSE response_payload::text
            END
        );

ALTER TABLE engine.lightning_invoice
    ALTER COLUMN boltz_request_payload TYPE TEXT
        USING (
            CASE
                WHEN boltz_request_payload IS NULL THEN NULL
                WHEN pg_typeof(boltz_request_payload) = 'oid'::regtype THEN convert_from(lo_get(boltz_request_payload::oid), 'UTF8')
                ELSE boltz_request_payload::text
            END
        ),
    ALTER COLUMN boltz_response_payload TYPE TEXT
        USING (
            CASE
                WHEN boltz_response_payload IS NULL THEN NULL
                WHEN pg_typeof(boltz_response_payload) = 'oid'::regtype THEN convert_from(lo_get(boltz_response_payload::oid), 'UTF8')
                ELSE boltz_response_payload::text
            END
        ),
    ALTER COLUMN boltz_decoded_transaction_payload TYPE TEXT
        USING (
            CASE
                WHEN boltz_decoded_transaction_payload IS NULL THEN NULL
                WHEN pg_typeof(boltz_decoded_transaction_payload) = 'oid'::regtype THEN convert_from(lo_get(boltz_decoded_transaction_payload::oid), 'UTF8')
                ELSE boltz_decoded_transaction_payload::text
            END
        ),
    ALTER COLUMN boltz_status_payload TYPE TEXT
        USING (
            CASE
                WHEN boltz_status_payload IS NULL THEN NULL
                WHEN pg_typeof(boltz_status_payload) = 'oid'::regtype THEN convert_from(lo_get(boltz_status_payload::oid), 'UTF8')
                ELSE boltz_status_payload::text
            END
        );
