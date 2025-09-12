package org.testing.pt.gatling;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test for the main application class.
 * Verifies that the Spring Boot application context loads successfully.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.simulation.directory=test-simulations",
    "app.results.directory=test-results",
    "app.workspace.directory=test-workspace"
})
class GatlingWebConsoleApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring Boot application context
        // can be loaded successfully with all configurations
    }
}