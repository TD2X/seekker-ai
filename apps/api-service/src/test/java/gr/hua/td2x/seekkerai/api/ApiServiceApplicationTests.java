package gr.hua.td2x.seekkerai.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

// Starts the whole app, which needs the real database and S3 (environment variables).
// Without them this test is skipped, so a plain "./mvnw test" does not fail on a clean machine.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_HOST", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AWS_ENDPOINT_URL_S3", matches = ".+")
class ApiServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
