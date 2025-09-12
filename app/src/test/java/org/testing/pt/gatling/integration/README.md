# Integration Tests for Unified Gatling Web Console

This directory contains comprehensive integration tests for the unified Gatling Web Console application.

## Test Classes

### 1. WebControllerIntegrationTest
Tests the web interface controllers using MockMvc:
- Login page functionality
- Dashboard access with authentication
- Run test page functionality
- Results page functionality
- Form submissions and redirects
- Static resource access

### 2. ApiControllerIntegrationTest
Tests the REST API controllers using TestRestTemplate:
- File listing endpoints
- Test execution endpoints
- Test management (start, stop, status)
- File upload functionality
- Authentication requirements
- JSON response validation

### 3. SecurityIntegrationTest
Tests the security configuration for both web and API endpoints:
- Basic authentication for API endpoints
- Session-based authentication for web interface
- Login/logout functionality
- Access control and authorization
- Security headers
- Cross-authentication isolation (API auth vs web auth)

### 4. WorkflowIntegrationTest
Tests complete end-to-end workflows:
- File upload and test execution workflow
- Test lifecycle management (start, monitor, stop)
- Log streaming functionality
- Error handling scenarios
- Concurrent test execution
- File management workflows

## Test Configuration

- Uses `application-test.properties` for test-specific configuration
- Mocks external dependencies (FileScanner, GatlingService)
- Uses random ports to avoid conflicts
- Includes proper security test setup

## Key Features Tested

### Security
- HTTP Basic Authentication for `/api/**` endpoints
- Session-based authentication for web interface
- Proper isolation between authentication mechanisms
- Login/logout functionality
- Access control for protected resources

### File Management
- File upload via API and web interface
- File listing and metadata
- File validation and error handling

### Test Execution
- Test parameter validation
- Test lifecycle management
- Real-time log streaming
- Status monitoring
- Error handling and recovery

### Web Interface
- Template rendering with proper model attributes
- Form submissions and validation
- Flash messages and redirects
- Responsive design compatibility

## Running the Tests

```bash
# Run all integration tests
mvn test -Dtest="*IntegrationTest"

# Run specific test class
mvn test -Dtest="SecurityIntegrationTest"

# Run with debug logging
mvn test -Dtest="*IntegrationTest" -Dlogging.level.org.testing.pt.gatling=DEBUG
```

## Test Requirements Coverage

These integration tests verify the requirements specified in the spec:
- **Requirement 2.1**: Unified security configuration for both web and API endpoints
- **Requirement 2.2**: File upload and test execution workflows
- **Requirement 2.4**: Integration between web interface and backend services

The tests ensure that the unified application maintains the functionality of both the original server and web-ui modules while providing a seamless integrated experience.