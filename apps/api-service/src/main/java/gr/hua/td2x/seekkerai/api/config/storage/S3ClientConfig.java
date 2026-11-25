package gr.hua.td2x.seekkerai.api.config.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Builds the S3 client with EXPLICIT timeouts and a BOUNDED retry policy.
 *
 * - connect/socket timeouts: fail fas+t if the storage is unreachable or silent.
 * - attempt timeout: limit of one HTTP attempt; call timeout: limit of the whole operation incl. retries.
 * - max-idle-time (+ reaper): drop pooled connections before the server closes them, because a stale
 *   keep-alive connection produced NoHttpResponseException in our smoke test.
 * - max-attempts: bounded retries (default 3). Only used for operations that are safe to repeat
 *   (reads, and writes to a content-addressed key).
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3ClientConfig {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(StorageProperties p) {
        return build(p);
    }

    /** Static so that tests can build the same client without starting Spring. */
    public static S3Client build(StorageProperties p) {
        return S3Client.builder()
                .endpointOverride(URI.create(p.endpoint()))
                .region(Region.of(p.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(p.accessKey(), p.secretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(p.pathStyle())
                        .build())
                .httpClientBuilder(ApacheHttpClient.builder()
                        .connectionTimeout(p.connectTimeout())
                        .socketTimeout(p.socketTimeout())
                        .connectionMaxIdleTime(p.maxIdleTime())
                        .useIdleConnectionReaper(true))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(p.attemptTimeout())
                        .apiCallTimeout(p.callTimeout())
                        .retryStrategy(b -> b.maxAttempts(p.maxAttempts()))
                        .build())
                .build();
    }
}
