package gr.hua.td2x.seekkerai.api.document.repository;

import gr.hua.td2x.seekkerai.api.document.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, UUID> {
}