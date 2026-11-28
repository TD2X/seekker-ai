ALTER TABLE processing_jobs
    ADD COLUMN idempotency_key UUID;

UPDATE processing_jobs
SET idempotency_key = gen_random_uuid()
WHERE idempotency_key IS NULL;

ALTER TABLE processing_jobs
    ALTER COLUMN idempotency_key SET NOT NULL;

CREATE INDEX idx_processing_jobs_idempotency_key
    ON processing_jobs(idempotency_key);