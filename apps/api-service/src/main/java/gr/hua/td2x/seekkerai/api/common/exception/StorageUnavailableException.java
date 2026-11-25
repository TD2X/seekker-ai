package gr.hua.td2x.seekkerai.api.common.exception;

/**
 * The storage could not be reached or answered with a server-side error / timeout.
 * Transient: the caller may retry. For a write the outcome is UNCERTAIN (the object may or may not exist).
 */
public class StorageUnavailableException extends StorageException {

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
