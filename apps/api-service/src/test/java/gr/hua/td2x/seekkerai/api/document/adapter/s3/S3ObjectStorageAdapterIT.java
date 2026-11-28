package gr.hua.td2x.seekkerai.api.document.adapter.s3;

import gr.hua.td2x.seekkerai.api.common.exception.ObjectNotFoundException;
import gr.hua.td2x.seekkerai.api.config.storage.S3ClientConfig;
import gr.hua.td2x.seekkerai.api.config.storage.StorageProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test against the REAL external storage. Runs only when AWS_ENDPOINT_URL_S3 is set:
 *   ./mvnw -q test -Dtest=S3ObjectStorageAdapterIT
 * (it is named *IT, so a plain "./mvnw test" does not run it).
 */
@EnabledIfEnvironmentVariable(named = "AWS_ENDPOINT_URL_S3", matches = ".+")
class S3ObjectStorageAdapterIT {

    private static S3Client client;
    private static S3ObjectStorageAdapter adapter;
    private static final List<String> created = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        StorageProperties props = new StorageProperties(
                System.getenv("AWS_ENDPOINT_URL_S3"),
                System.getenv().getOrDefault("AWS_REGION", "eu-central-1"),
                System.getenv("AWS_ACCESS_KEY_ID"),
                System.getenv("AWS_SECRET_ACCESS_KEY"),
                System.getenv().getOrDefault("S3_BUCKET", "seekker-pdfs"),
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
                adapter.delete(key);
            } catch (RuntimeException e) {
                System.err.println("cleanup failed for " + key + ": " + e.getMessage());
            }
        }
        client.close();
    }

    @Test
    void putThenExistsThenGetReturnsSameBytes() throws Exception {
        String key = "it/" + UUID.randomUUID() + ".pdf";
        byte[] content = ("%PDF-1.7\n" + UUID.randomUUID()).getBytes();
        Path file = Files.createTempFile("it-", ".pdf");
        try {
            Files.write(file, content);
            adapter.put(key, file, "application/pdf");
            created.add(key);

            assertTrue(adapter.exists(key));
            try (InputStream in = adapter.get(key)) {
                assertArrayEquals(content, in.readAllBytes());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void existsIsFalseForMissingKey() {
        assertFalse(adapter.exists("it/missing-" + UUID.randomUUID() + ".pdf"));
    }

    @Test
    void getOfMissingKeyThrowsObjectNotFound() {
        assertThrows(ObjectNotFoundException.class,
                () -> adapter.get("it/missing-" + UUID.randomUUID() + ".pdf"));
    }

    @Test
    void samePutTwiceIsIdempotent() throws Exception {
        String key = "it/" + UUID.randomUUID() + ".pdf";
        byte[] content = ("%PDF-1.7\n" + UUID.randomUUID()).getBytes();
        Path file = Files.createTempFile("it-", ".pdf");
        try {
            Files.write(file, content);
            adapter.put(key, file, "application/pdf");
            created.add(key);
            adapter.put(key, file, "application/pdf");   // same key, same bytes: safe to repeat

            try (InputStream in = adapter.get(key)) {
                assertArrayEquals(content, in.readAllBytes());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
