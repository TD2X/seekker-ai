package gr.hua.td2x.seekkerai.api.config.storage;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Settings of the external S3-compatible storage. Bound from the {@code storage.s3.*} properties. */
@Validated
@ConfigurationProperties(prefix = "storage.s3")
public record StorageProperties(
        @NotBlank String endpoint,
        @NotBlank String region,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String bucket,
        @DefaultValue("true") boolean pathStyle,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("30s") Duration socketTimeout,
        @DefaultValue("60s") Duration attemptTimeout,
        @DefaultValue("150s") Duration callTimeout,
        @DefaultValue("5s") Duration maxIdleTime,
        @DefaultValue("3") int maxAttempts
) {
}
