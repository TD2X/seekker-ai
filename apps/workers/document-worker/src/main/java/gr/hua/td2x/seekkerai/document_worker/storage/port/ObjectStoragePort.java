package gr.hua.td2x.seekkerai.document_worker.storage.port;

import java.io.InputStream;

/**
 * Port to the external S3-compatible object storage, from the point of view of the worker.
 * The worker only READS the PDFs that the API stored: there is no put/delete here on purpose.
 */
public interface ObjectStoragePort {

    /** True if the key exists. */
    boolean exists(String key);

    /**
     * Streams the object. The caller MUST close the stream.
     *
     * @throws gr.hua.td2x.seekkerai.document_worker.common.exception.ObjectNotFoundException  key is missing (permanent)
     * @throws gr.hua.td2x.seekkerai.document_worker.common.exception.StorageUnavailableException storage unreachable (transient)
     */
    InputStream get(String key);
}
