package gr.hua.td2x.seekkerai.document_worker.common.exception;

/**
 * The storage could not be reached or answered with a server-side error / timeout.
 * Transient: the job may be retried (the worker only reads, so retrying is always safe).
 */
public class StorageUnavailableException extends StorageException {

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
