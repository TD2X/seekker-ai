package gr.hua.td2x.seekkerai.document_worker.common.exception;

/** The requested object key does not exist in the bucket (permanent: retrying will not help). */
public class ObjectNotFoundException extends StorageException {

    public ObjectNotFoundException(String key, Throwable cause) {
        super("Object not found: " + key, cause);
    }
}
