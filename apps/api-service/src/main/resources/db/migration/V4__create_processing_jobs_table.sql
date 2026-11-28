CREATE TABLE processing_jobs (
                                 id UUID PRIMARY KEY,
                                 document_id UUID NOT NULL,
                                 status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                                 attempt_number INTEGER NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 started_at TIMESTAMPTZ,
                                 completed_at TIMESTAMPTZ,
                                 error_message TEXT,

                                 CONSTRAINT fk_processing_jobs_document
                                     FOREIGN KEY (document_id)
                                         REFERENCES documents(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT uq_processing_jobs_attempt
                                     UNIQUE (document_id, attempt_number),

                                 CONSTRAINT chk_processing_jobs_status
                                     CHECK (
                                         status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
                                         ),

                                 CONSTRAINT chk_processing_jobs_attempt
                                     CHECK (attempt_number > 0),

                                 CONSTRAINT chk_processing_jobs_dates
                                     CHECK (
                                         completed_at IS NULL
                                             OR (
                                             started_at IS NOT NULL
                                                 AND completed_at >= started_at
                                             )
                                         )
);

CREATE INDEX idx_processing_jobs_document_id
    ON processing_jobs(document_id);

CREATE INDEX idx_processing_jobs_status
    ON processing_jobs(status);