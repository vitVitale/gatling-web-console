# Implementation Plan

- [x] 1. Create unified project structure and Maven configuration
  - Create new single-module Maven project structure with consolidated dependencies
  - Merge dependencies from both server and web-ui modules into single pom.xml
  - Configure Spring Boot Maven plugin for single executable JAR output
  - _Requirements: 1.1, 1.3, 4.3_

- [x] 2. Create main application class and base package structure
  - Create GatlingWebConsoleApplication.java as unified main class
  - Set up base package org.testing.pt.gatling with proper sub-packages (config, controller, model, service)
  - Enable scheduling and configure Spring Boot application properties
  - _Requirements: 1.2, 4.1, 4.2_

- [x] 3. Consolidate and organize model classes
  - Copy all model classes (FileInfo, TestExecution, TestParameters, TestStatus) to unified model package
  - Remove duplicate model classes and ensure single source of truth
  - Update package imports throughout codebase
  - _Requirements: 2.1, 4.2_

- [x] 4. Merge and organize service layer
  - Copy GatlingService, FileScanner, and LogStreamService to unified service package
  - Remove HTTP client dependencies from services (eliminate inter-service communication)
  - Update service imports and ensure proper Spring annotations
  - _Requirements: 2.2, 2.3, 4.2_

- [x] 5. Create unified security configuration
  - Merge SecurityConfig classes into single configuration
  - Configure dual authentication (session-based for web, basic auth for API)
  - Set up security rules for both /api/* endpoints and web pages
  - _Requirements: 3.2, 5.1_

- [x] 6. Implement API controllers with /api prefix
  - Copy FileController and TestController to controller/api package
  - Add /api prefix to all REST endpoint mappings
  - Update controllers to use direct service injection instead of HTTP calls
  - _Requirements: 2.1, 2.2, 5.1_

- [x] 7. Implement web controllers for UI pages
  - Copy DashboardController and LogController to controller/web package
  - Update web controllers to use direct service injection instead of TestService HTTP client
  - Remove TestService dependency and HTTP client calls
  - _Requirements: 2.1, 2.4_

- [x] 8. Consolidate application configuration
  - Merge application.properties files into single configuration
  - Set unified port configuration (default 8080)
  - Remove server.api.url configuration (no longer needed)
  - Configure file paths and security credentials
  - _Requirements: 1.2, 3.1, 3.3_

- [x] 9. Copy and organize Thymeleaf templates and static resources
  - Copy all HTML templates from web-ui to src/main/resources/templates
  - Ensure proper template organization and accessibility
  - Update any template references if needed
  - _Requirements: 2.1, 4.4, 4.5_

- [x] 10. Implement log streaming controller for real-time logs
  - Create LogStreamController for Server-Sent Events functionality
  - Implement LogStreamService for managing log streams
  - Ensure real-time log streaming works with unified application
  - _Requirements: 2.3_

- [x] 11. Update and consolidate unit tests
  - Copy existing tests to appropriate test packages
  - Update test imports and package references
  - Ensure all service classes have proper unit test coverage
  - Remove HTTP client mocking from web controller tests
  - _Requirements: 4.2_

- [x] 12. Create integration tests for unified application
  - Write integration tests for web controllers using MockMvc
  - Write integration tests for API controllers using TestRestTemplate
  - Test security configuration for both web and API endpoints
  - Verify file upload and test execution workflows
  - _Requirements: 2.1, 2.2, 2.4_

- [x] 13. Clean up and remove old module directories
  - Remove server/ and web-ui/ module directories
  - Update root pom.xml to remove module references
  - Clean up any remaining references to old module structure
  - _Requirements: 1.1, 4.1_

- [ ] 14. Verify and test complete application functionality
  - Test application startup and port binding
  - Verify all web pages load and function correctly
  - Test REST API endpoints respond correctly
  - Verify file scanning, test execution, and log streaming work
  - Test security authentication for both web and API access
  - _Requirements: 1.4, 2.1, 2.2, 2.3, 2.4, 2.5, 3.4, 5.2, 5.3, 5.4_