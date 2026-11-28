package gr.hua.td2x.seekkerai.api.common.exception;

/**
 * Base class for object storage failures. Not retryable by itself
 * (e.g. bad credentials, bad request): see {@link StorageUnavailableException} for transient ones.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(String message) {
        super(message);
    }
}
