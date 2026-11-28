package smoketest;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Standalone smoke test for an S3-compatible object storage.
 *
 * Configuration (environment variables only, nothing hardcoded):
 *   AWS_ENDPOINT_URL_S3  (or S3_ENDPOINT)    endpoint URL, e.g. https://...
 *   AWS_ACCESS_KEY_ID    (or S3_ACCESS_KEY)
 *   AWS_SECRET_ACCESS_KEY(or S3_SECRET_KEY)
 *   AWS_REGION           (or S3_REGION)      default: us-east-1
 *   S3_BUCKET                                bucket name (must already exist)
 *   S3_PATH_STYLE                            "true" (default) or "false"
 *   S3_TEST_SIZE_MB                          test object size, default 10
 *
 * Exit code is 0 only if checks 1-3 pass. Check 4 (If-None-Match) is informational.
 */
public class S3SmokeTest {

    private enum Result { PASS, FAIL, UNSUPPORTED }

    private record Check(String name, Result result, String detail) { }

    private static final List<Check> CHECKS = new ArrayList<>();
    private static final List<String> CREATED_KEYS = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        String endpoint = env("AWS_ENDPOINT_URL_S3", "S3_ENDPOINT", null);
        String accessKey = env("AWS_ACCESS_KEY_ID", "S3_ACCESS_KEY", null);
        String secretKey = env("AWS_SECRET_ACCESS_KEY", "S3_SECRET_KEY", null);
        String region = env("AWS_REGION", "S3_REGION", "us-east-1");
        String bucket = env("S3_BUCKET", null, null);
        boolean pathStyle = !"false".equalsIgnoreCase(env("S3_PATH_STYLE", null, "true"));
        int sizeMb = Integer.parseInt(env("S3_TEST_SIZE_MB", null, "10"));

        if (endpoint == null || accessKey == null || secretKey == null || bucket == null) {
            System.err.println("Missing configuration. Required: AWS_ENDPOINT_URL_S3, AWS_ACCESS_KEY_ID, "
                    + "AWS_SECRET_ACCESS_KEY, S3_BUCKET (optional: AWS_REGION, S3_PATH_STYLE, S3_TEST_SIZE_MB)");
            System.exit(2);
        }
        System.out.println("Endpoint : " + endpoint);
        System.out.println("Bucket   : " + bucket + "  (path-style=" + pathStyle + ", region=" + region + ")");
        System.out.println("Size     : " + sizeMb + " MB");
        System.out.println();

        // Normal client: explicit timeouts, no retries so every failure is visible.
        S3Client s3 = buildClient(endpoint, accessKey, secretKey, region, pathStyle,
                Duration.ofSeconds(5), Duration.ofSeconds(30), Duration.ofSeconds(20), Duration.ofSeconds(60));

        Path tmp = Files.createTempFile("s3-smoketest", ".pdf");
        try {
            String key = "smoketest/" + UUID.randomUUID() + ".pdf";
            String sha = createTestFile(tmp, sizeMb);

            check1RoundTrip(s3, bucket, key, tmp, sha);
            check2NoSuchKey(s3, bucket);
            check3Timeout(endpoint, accessKey, secretKey, region, pathStyle, bucket, key);
            check4ConditionalWrite(s3, bucket, key, tmp);
        } finally {
            cleanup(s3, bucket);
            Files.deleteIfExists(tmp);
            s3.close();
        }

        System.out.println();
        System.out.println("================ SUMMARY ================");
        boolean hardFail = false;
        for (int i = 0; i < CHECKS.size(); i++) {
            Check c = CHECKS.get(i);
            System.out.printf("%d) %-34s %-12s %s%n", i + 1, c.name(), c.result(), c.detail());
            if (i < 3 && c.result() != Result.PASS) {
                hardFail = true;
            }
        }
        System.out.println("=========================================");
        System.out.println(hardFail
                ? "RESULT: NOT READY - at least one of checks 1-3 failed."
                : "RESULT: OK - checks 1-3 passed (check 4 is informational).");
        System.exit(hardFail ? 1 : 0);
    }

    // 1) PUT / HEAD / GET (streaming) round trip with SHA-256 verification
    private static void check1RoundTrip(S3Client s3, String bucket, String key, Path file, String sha) {
        String name = "Put/Head/Get round trip";
        try {
            long size = Files.size(file);
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType("application/pdf").build(),
                    RequestBody.fromFile(file));
            CREATED_KEYS.add(key);

            HeadObjectResponse head = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            if (head.contentLength() != size) {
                CHECKS.add(new Check(name, Result.FAIL, "size mismatch: head=" + head.contentLength() + " local=" + size));
                return;
            }

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            long read = 0;
            try (ResponseInputStream<GetObjectResponse> in =
                         s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
                 InputStream dis = new DigestInputStream(in, md)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = dis.read(buf)) != -1) {
                    read += n;
                }
            }
            String remoteSha = HexFormat.of().formatHex(md.digest());
            if (read == size && remoteSha.equals(sha)) {
                CHECKS.add(new Check(name, Result.PASS, size + " bytes, sha256 matches"));
            } else {
                CHECKS.add(new Check(name, Result.FAIL, "downloaded=" + read + " bytes, sha256 match=" + remoteSha.equals(sha)));
            }
        } catch (Exception e) {
            CHECKS.add(new Check(name, Result.FAIL, describe(e)));
        }
    }

    // 2) Missing key must be distinguishable from transient errors (permanent failure for the worker)
    private static void check2NoSuchKey(S3Client s3, String bucket) {
        String name = "Missing key -> NoSuchKey (404)";
        String missing = "smoketest/does-not-exist-" + UUID.randomUUID() + ".pdf";
        try {
            s3.getObject(GetObjectRequest.builder().bucket(bucket).key(missing).build()).close();
            CHECKS.add(new Check(name, Result.FAIL, "GET of a missing key unexpectedly succeeded"));
        } catch (S3Exception e) {
            String code = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "?";
            boolean ok = e.statusCode() == 404;
            CHECKS.add(new Check(name, ok ? Result.PASS : Result.FAIL,
                    "status=" + e.statusCode() + " errorCode=" + code));
        } catch (Exception e) {
            CHECKS.add(new Check(name, Result.FAIL, describe(e)));
        }
    }

    // 3) Client timeouts really fire. This verifies OUR client configuration (not the server):
    //    an impossibly small attempt timeout, no retries -> must fail fast with a timeout exception.
    private static void check3Timeout(String endpoint, String ak, String sk, String region, boolean pathStyle,
                                      String bucket, String existingKey) {
        String name = "Client timeout fires";
        try (S3Client tiny = buildClient(endpoint, ak, sk, region, pathStyle,
                Duration.ofMillis(1), Duration.ofMillis(1), Duration.ofMillis(1), Duration.ofMillis(5))) {
            long t0 = System.nanoTime();
            try {
                tiny.headObject(HeadObjectRequest.builder().bucket(bucket).key(existingKey).build());
                CHECKS.add(new Check(name, Result.FAIL, "request finished inside 1 ms - timeout not enforced"));
            } catch (ApiCallAttemptTimeoutException | ApiCallTimeoutException e) {
                long ms = (System.nanoTime() - t0) / 1_000_000;
                CHECKS.add(new Check(name, Result.PASS, e.getClass().getSimpleName() + " after " + ms + " ms"));
            } catch (SdkClientException e) {
                long ms = (System.nanoTime() - t0) / 1_000_000;
                // Socket/connect timeouts surface as SdkClientException wrapping the cause.
                boolean timeout = String.valueOf(e).toLowerCase().contains("timeout")
                        || String.valueOf(e.getCause()).toLowerCase().contains("timeout")
                        || String.valueOf(e.getCause()).toLowerCase().contains("timed out");
                CHECKS.add(new Check(name, timeout ? Result.PASS : Result.FAIL,
                        describe(e) + " after " + ms + " ms"));
            }
        } catch (Exception e) {
            CHECKS.add(new Check(name, Result.FAIL, describe(e)));
        }
    }

    // 4) Informational: does the endpoint honour If-None-Match: * on PutObject?
    //    The design does NOT depend on it (the DB is the arbiter, content-addressed keys are idempotent).
    private static void check4ConditionalWrite(S3Client s3, String bucket, String existingKey, Path file) {
        String name = "If-None-Match: * (informational)";
        try {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(existingKey).ifNoneMatch("*").build(),
                    RequestBody.fromFile(file));
            CHECKS.add(new Check(name, Result.UNSUPPORTED,
                    "overwrite of an existing key SUCCEEDED -> header ignored, do not rely on it"));
        } catch (S3Exception e) {
            if (e.statusCode() == 412) {
                CHECKS.add(new Check(name, Result.PASS, "412 PreconditionFailed on existing key (supported)"));
            } else {
                CHECKS.add(new Check(name, Result.UNSUPPORTED,
                        "status=" + e.statusCode() + " " + (e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "")));
            }
        } catch (Exception e) {
            CHECKS.add(new Check(name, Result.UNSUPPORTED, describe(e)));
        }
    }

    private static S3Client buildClient(String endpoint, String ak, String sk, String region, boolean pathStyle,
                                        Duration connect, Duration socket, Duration attempt, Duration apiCall) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(ak, sk)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build())
                .httpClientBuilder(ApacheHttpClient.builder()
                        .connectionTimeout(connect)
                        .socketTimeout(socket))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(attempt)
                        .apiCallTimeout(apiCall)
                        .retryStrategy(b -> b.maxAttempts(1))
                        .build())
                .build();
    }

    // Writes "%PDF-" + pseudo-random bytes and returns the SHA-256 of the file.
    private static String createTestFile(Path file, int sizeMb) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        Random rnd = new Random(42);
        byte[] buf = new byte[64 * 1024];
        long total = (long) sizeMb * 1024 * 1024;
        try (var out = Files.newOutputStream(file)) {
            byte[] header = "%PDF-1.7\n".getBytes();
            out.write(header);
            md.update(header);
            long written = header.length;
            while (written < total) {
                rnd.nextBytes(buf);
                int n = (int) Math.min(buf.length, total - written);
                out.write(buf, 0, n);
                md.update(buf, 0, n);
                written += n;
            }
        }
        return HexFormat.of().formatHex(md.digest());
    }

    private static void cleanup(S3Client s3, String bucket) {
        for (String k : CREATED_KEYS) {
            try {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(k).build());
            } catch (Exception e) {
                System.err.println("cleanup failed for " + k + ": " + describe(e));
            }
        }
    }

    private static String describe(Exception e) {
        if (e instanceof S3Exception s3e) {
            String code = s3e.awsErrorDetails() != null ? s3e.awsErrorDetails().errorCode() : "?";
            return "S3Exception status=" + s3e.statusCode() + " code=" + code;
        }
        Throwable c = e.getCause();
        return e.getClass().getSimpleName() + ": " + e.getMessage() + (c != null ? " (cause: " + c + ")" : "");
    }

    private static String env(String primary, String fallback, String def) {
        String v = System.getenv(primary);
        if ((v == null || v.isBlank()) && fallback != null) {
            v = System.getenv(fallback);
        }
        return (v == null || v.isBlank()) ? def : v.trim();
    }
}
