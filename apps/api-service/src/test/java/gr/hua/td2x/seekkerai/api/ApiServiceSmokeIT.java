package gr.hua.td2x.seekkerai.api;

import gr.hua.td2x.seekkerai.api.document.port.ObjectStoragePort;
import gr.hua.td2x.seekkerai.api.document.repository.DocumentChunkRepository;
import gr.hua.td2x.seekkerai.api.document.repository.DocumentRepository;
import gr.hua.td2x.seekkerai.api.document.repository.ProcessingJobRepository;
import gr.hua.td2x.seekkerai.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * General smoke test of the api-service against the REAL external services.
 *
 * It starts the whole Spring application and checks, in order:
 *   1. the app starts (Hibernate "validate" also proves the entities match the SQL tables),
 *   2. every Flyway migration was applied and the expected tables exist,
 *   3. the database rules really reject bad data (status, foreign key, size, duplicate job attempt),
 *   4. the S3 storage works through the Spring bean (upload, exists, download, delete).
 *
 * Runs only when DB_HOST and AWS_ENDPOINT_URL_S3 are set (see scripts/test-all.sh):
 *   ./mvnw -q test -Dtest=ApiServiceSmokeIT
 * All database writes are rolled back; the only thing created in S3 is deleted at the end.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_HOST", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AWS_ENDPOINT_URL_S3", matches = ".+")
class ApiServiceSmokeIT {

    // PostgreSQL error codes for the rules we test
    private static final String CHECK_VIOLATION = "23514";
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String UNIQUE_VIOLATION = "23505";

    @Autowired DataSource dataSource;
    @Autowired ObjectStoragePort storage;
    @Autowired UserRepository users;
    @Autowired DocumentRepository documents;
    @Autowired DocumentChunkRepository chunks;
    @Autowired ProcessingJobRepository jobs;

    // ---------- 1 + 2: application, migrations, tables ----------

    @Test
    void allFlywayMigrationsWereApplied() throws Exception {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT count(*) FILTER (WHERE success), count(*) FILTER (WHERE NOT success) "
                             + "FROM flyway_schema_history");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            assertTrue(rs.getInt(1) >= 5, "expected at least V1..V5 to be applied, found " + rs.getInt(1));
            assertEquals(0, rs.getInt(2), "a Flyway migration is marked as failed");
        }
    }

    @Test
    void expectedTablesExist() throws Exception {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT count(*) FROM information_schema.tables "
                             + "WHERE table_schema = current_schema() AND table_name = ?")) {
            for (String table : List.of("users", "documents", "document_chunks", "processing_jobs")) {
                ps.setString(1, table);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(1, rs.getInt(1), "missing table: " + table);
                }
            }
        }
    }

    @Test
    void repositoriesCanQueryTheirTables() {
        assertTrue(users.count() >= 0);
        assertTrue(documents.count() >= 0);
        assertTrue(chunks.count() >= 0);
        assertTrue(jobs.count() >= 0);
    }

    // ---------- 3: the database rules ----------

    @Test
    void validUserDocumentAndJobCanBeInserted() throws Exception {
        inRolledBackTransaction(c -> {
            UUID user = insertUser(c);
            UUID document = insertDocument(c, user, 10, "PENDING");
            insertJob(c, document, 1);
        });
    }

    @Test
    void documentWithUnknownStatusIsRejected() {
        assertSqlState(CHECK_VIOLATION, c -> insertDocument(c, insertUser(c), 10, "BOGUS"));
    }

    @Test
    void documentWithZeroSizeIsRejected() {
        assertSqlState(CHECK_VIOLATION, c -> insertDocument(c, insertUser(c), 0, "PENDING"));
    }

    @Test
    void documentOfUnknownUserIsRejected() {
        assertSqlState(FOREIGN_KEY_VIOLATION, c -> insertDocument(c, UUID.randomUUID(), 10, "PENDING"));
    }

    @Test
    void sameJobAttemptTwiceIsRejected() {
        assertSqlState(UNIQUE_VIOLATION, c -> {
            UUID document = insertDocument(c, insertUser(c), 10, "PENDING");
            insertJob(c, document, 1);
            insertJob(c, document, 1);   // same document + same attempt number
        });
    }

    // ---------- 4: S3 through the Spring bean ----------

    @Test
    void storageRoundTripThroughSpringBean() throws Exception {
        byte[] content = ("%PDF-1.7\n" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        String key = "it/smoke-" + sha256 + ".pdf";   // content-addressed, like the real upload flow
        Path file = Files.createTempFile("smoke-", ".pdf");
        try {
            Files.write(file, content);
            storage.put(key, file, "application/pdf");
            assertTrue(storage.exists(key));
            try (InputStream in = storage.get(key)) {
                assertArrayEquals(content, in.readAllBytes());
            }
        } finally {
            Files.deleteIfExists(file);
            storage.delete(key);
        }
        assertFalse(storage.exists(key), "object should be gone after delete");
    }

    // ---------- helpers ----------

    private interface SqlWork {
        void run(Connection c) throws Exception;
    }

    /** Runs the work in one transaction and ALWAYS rolls it back, so the database stays clean. */
    private void inRolledBackTransaction(SqlWork work) throws Exception {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                work.run(c);
            } finally {
                c.rollback();
            }
        }
    }

    private void assertSqlState(String expected, SqlWork work) {
        SQLException e = assertThrows(SQLException.class, () -> inRolledBackTransaction(work));
        assertEquals(expected, e.getSQLState(), "unexpected database error: " + e.getMessage());
    }

    private static UUID insertUser(Connection c) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users (id, email, password_hash, first_name, last_name) VALUES (?, ?, 'x', 'Smoke', 'Test')")) {
            ps.setObject(1, id);
            ps.setString(2, "smoke-" + id + "@example.invalid");
            ps.executeUpdate();
        }
        return id;
    }

    private static UUID insertDocument(Connection c, UUID userId, long size, String status) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO documents (id, user_id, filename, object_key, content_type, file_size, status) "
                        + "VALUES (?, ?, 'smoke.pdf', ?, 'application/pdf', ?, ?)")) {
            ps.setObject(1, id);
            ps.setObject(2, userId);
            ps.setString(3, "it/smoke-" + id + ".pdf");
            ps.setLong(4, size);
            ps.setString(5, status);
            ps.executeUpdate();
        }
        return id;
    }

    private static void insertJob(Connection c, UUID documentId, int attempt) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO processing_jobs (id, document_id, attempt_number, idempotency_key) VALUES (?, ?, ?, ?)")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, documentId);
            ps.setInt(3, attempt);
            ps.setObject(4, UUID.randomUUID());
            ps.executeUpdate();
        }
    }
}
