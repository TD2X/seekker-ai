package gr.hua.td2x.seekkerai.document_worker;

import gr.hua.td2x.seekkerai.document_worker.common.exception.ObjectNotFoundException;
import gr.hua.td2x.seekkerai.document_worker.storage.port.ObjectStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * General smoke test of the document-worker: the whole Spring application starts (database
 * connection included) and its read-only S3 storage bean reaches the real storage.
 *
 *   ./mvnw -q test -Dtest=DocumentWorkerSmokeIT
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_HOST", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AWS_ENDPOINT_URL_S3", matches = ".+")
class DocumentWorkerSmokeIT {

    @Autowired ObjectStoragePort storage;

    @Test
    void storageBeanReachesTheRealStorage() {
        assertFalse(storage.exists("it/missing-" + UUID.randomUUID() + ".pdf"));
    }

    @Test
    void missingObjectIsReportedAsNotFound() {
        assertThrows(ObjectNotFoundException.class,
                () -> storage.get("it/missing-" + UUID.randomUUID() + ".pdf"));
    }
}
