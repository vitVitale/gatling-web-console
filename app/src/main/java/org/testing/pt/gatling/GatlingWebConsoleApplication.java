package org.testing.pt.gatling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the unified Gatling Web Console.
 * 
 * This application combines both the REST API backend and web interface
 * into a single Spring Boot application, serving both on the same port.
 * 
 * Features:
 * - REST API endpoints for external integrations
 * - Web interface for interactive test management
 * - Scheduled file scanning for simulation discovery
 * - Real-time log streaming via Server-Sent Events
 * - Unified security configuration for both web and API access
 */
@SpringBootApplication
@EnableScheduling
public class GatlingWebConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatlingWebConsoleApplication.class, args);
    }
}