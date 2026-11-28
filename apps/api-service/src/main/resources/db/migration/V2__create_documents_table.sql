CREATE TABLE documents (
                           id UUID PRIMARY KEY,
                           user_id UUID NOT NULL,
                           filename VARCHAR(255) NOT NULL,
                           object_key VARCHAR(512) NOT NULL,
                           content_type VARCHAR(100) NOT NULL,
                           file_size BIGINT NOT NULL,
                           status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_documents_user
                               FOREIGN KEY (user_id)
                                   REFERENCES users(id)
                                   ON DELETE CASCADE,

                           CONSTRAINT chk_documents_file_size
                               CHECK (file_size > 0),

                           CONSTRAINT chk_documents_status
                               CHECK (status IN ('PENDING', 'PROCESSING', 'INDEXED', 'FAILED'))
);

CREATE INDEX idx_documents_user_id
    ON documents(user_id);

CREATE INDEX idx_documents_status
    ON documents(status);