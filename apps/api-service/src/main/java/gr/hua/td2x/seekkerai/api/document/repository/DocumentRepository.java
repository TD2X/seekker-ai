package gr.hua.td2x.seekkerai.api.document.repository;

import gr.hua.td2x.seekkerai.api.document.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}