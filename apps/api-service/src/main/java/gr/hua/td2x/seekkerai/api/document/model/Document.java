package gr.hua.td2x.seekkerai.api.document.model;

import gr.hua.td2x.seekkerai.api.user.model.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "document")
    private List<DocumentChunk> chunks = new ArrayList<>();

    @OneToMany(mappedBy = "document")
    private List<ProcessingJob> processingJobs = new ArrayList<>();

    protected Document() {
        // Required by JPA
    }
    public List<DocumentChunk> getChunks() {
        return chunks;
    }
    public List<ProcessingJob> getProcessingJobs() {
        return processingJobs;
    }
    public UUID getId() {
        return id;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public String getObjectKey() {
        return objectKey;
    }
    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public Long getFileSize() {
        return fileSize;
    }
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void addChunk(DocumentChunk chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("DocumentChunk cannot be null");
        }

        if (!chunks.contains(chunk)) {
            chunks.add(chunk);
        }

        chunk.setDocument(this);
    }

    public void addProcessingJob(ProcessingJob processingJob) {
        if (processingJob == null) {
            throw new IllegalArgumentException("ProcessingJob cannot be null");
        }

        if (!processingJobs.contains(processingJob)) {
            processingJobs.add(processingJob);
        }

        processingJob.setDocument(this);
    }
}