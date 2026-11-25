package gr.hua.td2x.seekkerai.document_worker.storage.adapter.s3;

import gr.hua.td2x.seekkerai.document_worker.common.exception.ObjectNotFoundException;
import gr.hua.td2x.seekkerai.document_worker.config.storage.S3ClientConfig;
import gr.hua.td2x.seekkerai.document_worker.config.storage.StorageProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test of the worker's read-only adapter against the REAL external storage.
 * The test plays the role of the API: it uploads a file directly with the S3 client,
 * then checks that the worker adapter can find and download it.
 * Runs only when AWS_ENDPOINT_URL_S3 is set:   ./mvnw -q test -Dtest=S3ObjectStorageAdapterIT
 */
@EnabledIfEnvironmentVariable(named = "AWS_ENDPOINT_URL_S3", matches = ".+")
class S3ObjectStorageAdapterIT {

    private static S3Client client;
    private static String bucket;
    private static S3ObjectStorageAdapter adapter;
    private static final List<String> created = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        bucket = System.getenv().getOrDefault("S3_BUCKET", "seekker-pdfs");
        StorageProperties props = new StorageProperties(
                System.getenv("AWS_ENDPOINT_URL_S3"),
                System.getenv().getOrDefault("AWS_REGION", "eu-central-1"),
                System.getenv("AWS_ACCESS_KEY_ID"),
                System.getenv("AWS_SECRET_ACCESS_KEY"),
                bucket,
                true,
                Duration.ofSeconds(3), Duration.ofSeconds(30), Duration.ofSeconds(60),
                Duration.ofSeconds(150), Duration.ofSeconds(5), 3);
        client = S3ClientConfig.build(props);
        adapter = new S3ObjectStorageAdapter(client, props);
    }

    @AfterAll
    static void tearDown() {
        for (String key : created) {
            try {
                client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            } catch (RuntimeException e) {
                System.err.println("cleanup failed for " + key + ": " + e.getMessage());
            }
        }
        client.close();
    }

    @Test
    void workerCanFindAndDownloadWhatTheApiStored() throws Exception {
        String key = "it-worker/" + UUID.randomUUID() + ".pdf";
        byte[] content = ("%PDF-1.7\n" + UUID.randomUUID()).getBytes();
        client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(), RequestBody.fromBytes(content));
        created.add(key);

        assertTrue(adapter.exists(key));
        try (InputStream in = adapter.get(key)) {
            assertArrayEquals(content, in.readAllBytes());
        }
    }

    @Test
    void existsIsFalseForMissingKey() {
        assertFalse(adapter.exists("it-worker/missing-" + UUID.randomUUID() + ".pdf"));
    }

    @Test
    void getOfMissingKeyThrowsObjectNotFound() {
        assertThrows(ObjectNotFoundException.class,
                () -> adapter.get("it-worker/missing-" + UUID.randomUUID() + ".pdf"));
    }
}
