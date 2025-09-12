package org.testing.pt.gatling.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Simple test to verify that the Spring Boot context loads correctly for integration tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class IntegrationTestRunner {

    @Test
    void contextLoads() {
        // This test will pass if the application context loads successfully
    }
}