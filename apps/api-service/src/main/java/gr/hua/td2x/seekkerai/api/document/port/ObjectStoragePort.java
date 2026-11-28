package gr.hua.td2x.seekkerai.api.document.port;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Port to an external S3-compatible object storage. The domain depends only on this interface;
 * the HTTP/S3 details live in an adapter.
 *
 * Keys are content-addressed (e.g. documents/&lt;sha256&gt;.pdf), so writing the same key twice
 * with the same bytes is idempotent and therefore safe to retry.
 */
public interface ObjectStoragePort {

    /** Uploads the file under the key (overwrites with identical content if it already exists). */
    void put(String key, Path file, String contentType);

    /** True if the key exists. */
    boolean exists(String key);

    /**
     * Streams the object. The caller MUST close the stream.
     *
     * @throws gr.hua.td2x.seekkerai.api.common.exception.ObjectNotFoundException if the key is missing
     */
    InputStream get(String key);

    /** Deletes the key (no error if it does not exist). */
    void delete(String key);
}
