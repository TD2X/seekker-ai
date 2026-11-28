CREATE TABLE document_chunks (
                                 id UUID PRIMARY KEY,
                                 document_id UUID NOT NULL,
                                 chunk_index INTEGER NOT NULL,
                                 content TEXT NOT NULL,
                                 page_start INTEGER NOT NULL,
                                 page_end INTEGER NOT NULL,
                                 qdrant_point_id UUID NOT NULL,

                                 CONSTRAINT fk_document_chunks_document
                                     FOREIGN KEY (document_id)
                                         REFERENCES documents(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT uq_document_chunks_index
                                     UNIQUE (document_id, chunk_index),

                                 CONSTRAINT uq_document_chunks_qdrant_point
                                     UNIQUE (qdrant_point_id),

                                 CONSTRAINT chk_document_chunks_index
                                     CHECK (chunk_index >= 0),

                                 CONSTRAINT chk_document_chunks_pages
                                     CHECK (
                                         page_start > 0
                                             AND page_end >= page_start
                                         )
);

CREATE INDEX idx_document_chunks_document_id
    ON document_chunks(document_id);