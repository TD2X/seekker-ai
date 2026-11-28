package gr.hua.td2x.seekkerai.document_worker.config.storage;

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
 * Builds the S3 client with EXPLICIT timeouts and a BOUNDED retry policy (same policy as the API).
 * The worker is a separate process with its own configuration: it does not share code with the API.
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
