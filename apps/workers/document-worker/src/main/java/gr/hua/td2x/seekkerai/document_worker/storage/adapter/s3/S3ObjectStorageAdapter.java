package gr.hua.td2x.seekkerai.document_worker.storage.adapter.s3;

import gr.hua.td2x.seekkerai.document_worker.common.exception.ObjectNotFoundException;
import gr.hua.td2x.seekkerai.document_worker.common.exception.StorageException;
import gr.hua.td2x.seekkerai.document_worker.common.exception.StorageUnavailableException;
import gr.hua.td2x.seekkerai.document_worker.config.storage.StorageProperties;
import gr.hua.td2x.seekkerai.document_worker.storage.port.ObjectStoragePort;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

/**
 * Adapter: implements the worker's read-only {@link ObjectStoragePort} on top of the AWS SDK v2.
 *
 * Error mapping (so the worker logic never sees SDK types):
 *   404                                  -> ObjectNotFoundException      (permanent: job should be marked FAILED)
 *   5xx, timeouts, connection problems   -> StorageUnavailableException  (transient: job may be retried)
 *   other 4xx (403, 400, ...)            -> StorageException             (configuration problem)
 */
@Component
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    private final S3Client s3;
    private final String bucket;

    public S3ObjectStorageAdapter(S3Client s3, StorageProperties properties) {
        this.s3 = s3;
        this.bucket = properties.bucket();
    }

    @Override
    public boolean exists(String key) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw translate("exists", key, e);
        } catch (RuntimeException e) {
            throw translate("exists", key, e);
        }
    }

    @Override
    public InputStream get(String key) {
        try {
            return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (RuntimeException e) {
            throw translate("get", key, e);
        }
    }

    private StorageException translate(String operation, String key, RuntimeException e) {
        String what = "S3 " + operation + " failed for key " + key;
        if (e instanceof S3Exception s3e) {
            int status = s3e.statusCode();
            if (status == 404) {
                return new ObjectNotFoundException(key, e);
            }
            if (status >= 500) {
                return new StorageUnavailableException(what + " (HTTP " + status + ")", e);
            }
            return new StorageException(what + " (HTTP " + status + ")", e);
        }
        if (e instanceof SdkClientException) {
            // includes connect/read/attempt/call timeouts and dropped connections
            return new StorageUnavailableException(what + " (no usable response)", e);
        }
        return new StorageException(what, e);
    }
}
