package gr.hua.td2x.seekkerai.api.document.repository;

import gr.hua.td2x.seekkerai.api.document.model.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessingJobRepository
        extends JpaRepository<ProcessingJob, UUID> {

    List<ProcessingJob> findAllByDocumentIdOrderByAttemptNumberDesc(
            UUID documentId
    );

    Optional<ProcessingJob> findByIdempotencyKeyAndAttemptNumber(
            UUID idempotencyKey,
            Integer attemptNumber
    );

    List<ProcessingJob> findAllByIdempotencyKeyOrderByAttemptNumberAsc(
            UUID idempotencyKey
    );
}