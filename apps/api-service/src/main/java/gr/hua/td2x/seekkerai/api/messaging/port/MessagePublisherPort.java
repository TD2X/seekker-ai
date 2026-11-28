package gr.hua.td2x.seekkerai.api.messaging.port;

import gr.hua.td2x.seekkerai.api.messaging.model.DocumentProcessingRequestedEvent;

public interface MessagePublisherPort {

    void publish(DocumentProcessingRequestedEvent event);

}