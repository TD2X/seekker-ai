package src.main.java.gr.hua.td2x.seekkerai.api.messaging.model;

public record DocumentProcessingRequestedEvent(
        String documentId,
        String userId,
        String objectKey
) {
}