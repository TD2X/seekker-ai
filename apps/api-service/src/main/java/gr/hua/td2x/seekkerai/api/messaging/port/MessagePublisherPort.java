package src.main.java.gr.hua.td2x.seekkerai.api.messaging.port;

import src.main.java.gr.hua.td2x.seekkerai.api.messaging.model.DocumentProcessingRequestedEvent;

public interface MessagePublisherPort {

    void publish(DocumentProcessingRequestedEvent event);

}