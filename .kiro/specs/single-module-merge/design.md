# Design Document

## Overview

This design document outlines the consolidation of the current multi-module Maven project (server and web-ui modules) into a single unified Spring Boot application. The merged application will maintain all existing functionality while simplifying the project structure, build process, and deployment model.

The current architecture consists of two separate Spring Boot applications:
- **Server Module** (port 8081): REST API backend for test execution and file management
- **Web UI Module** (port 8080): Frontend web interface that communicates with the server API

The target architecture will be a single Spring Boot application that serves both the REST API endpoints and the web interface on the same port, eliminating the need for inter-service communication and simplifying deployment.

## Architecture

### Current Architecture
```
┌─────────────────┐    HTTP/REST    ┌─────────────────┐
│   Web UI        │ ──────────────► │   Server        │
│   (Port 8080)   │                 │   (Port 8081)   │
│                 │                 │                 │
│ - Controllers   │                 │ - REST API      │
│ - Thymeleaf     │                 │ - Gatling Exec  │
│ - Templates     │                 │ - File Scanning │
│ - Static Assets │                 │ - Log Streaming │
└─────────────────┘                 └─────────────────┘
```

### Target Architecture
```
┌─────────────────────────────────────────────────────┐
│              Unified Application                    │
│                 (Port 8080)                         │
│                                                     │
│  ┌─────────────────┐    ┌─────────────────────────┐ │
│  │  Web Interface  │    │     REST API            │ │
│  │                 │    │                         │ │
│  │ - Controllers   │    │ - File Management       │ │
│  │ - Thymeleaf     │    │ - Test Execution        │ │
│  │ - Templates     │    │ - Log Streaming         │ │
│  │ - Static Assets │    │ - Gatling Service       │ │
│  └─────────────────┘    └─────────────────────────┘ │
│                                                     │
│  ┌─────────────────────────────────────────────────┐ │
│  │            Shared Services                      │ │
│  │ - File Scanner - Security Config - Models      │ │
│  └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

## Components and Interfaces

### Package Structure
The merged application will follow this package organization:
```
org.testing.pt.gatling/
├── GatlingWebConsoleApplication.java    # Main application class
├── config/
│   └── SecurityConfig.java              # Unified security configuration
├── controller/
│   ├── web/                             # Web interface controllers
│   │   ├── DashboardController.java
│   │   └── LogController.java
│   └── api/                             # REST API controllers
│       ├── FileController.java
│       ├── LogStreamController.java
│       └── TestController.java
├── model/
│   ├── FileInfo.java
│   ├── TestExecution.java
│   ├── TestParameters.java
│   └── TestStatus.java
└── service/
    ├── FileScanner.java
    ├── GatlingService.java
    └── LogStreamService.java
```

### Controller Layer Design

**Web Controllers** (`/controller/web/`):
- Handle browser requests and return Thymeleaf templates
- Serve static content and web pages
- Proxy API calls to internal services (eliminating HTTP calls)

**API Controllers** (`/controller/api/`):
- Maintain existing REST endpoints for external integrations
- Use `/api` path prefix to distinguish from web routes
- Return JSON responses for programmatic access

### Service Layer Integration
The service layer will be shared between web and API controllers:
- **GatlingService**: Test execution and management
- **FileScanner**: Scheduled file scanning functionality
- **LogStreamService**: Real-time log streaming via Server-Sent Events

### Security Configuration
Unified security configuration will handle both web and API authentication:
- Web interface: Session-based authentication with login forms
- API endpoints: HTTP Basic authentication for external integrations
- Shared credentials configuration via application.properties

## Data Models

### Unified Models
All existing model classes will be consolidated into a single `model` package:
- **TestExecution**: Test run metadata and status
- **TestParameters**: Configuration for test execution
- **TestStatus**: Enumeration of test states
- **FileInfo**: File metadata for simulation management

No changes to model structure are required as both modules currently use identical models.

## Error Handling

### Web Error Handling
- Custom error pages for 404, 500, and other HTTP errors
- User-friendly error messages in web interface
- Graceful degradation when services are unavailable

### API Error Handling
- Consistent JSON error responses
- Proper HTTP status codes
- Detailed error messages for debugging

### Logging Strategy
- Unified logging configuration
- Separate log levels for web and API components
- Structured logging for better observability

## Testing Strategy

### Unit Testing
- Test all service classes independently
- Mock external dependencies (file system, process execution)
- Maintain existing test coverage levels

### Integration Testing
- Test web controller endpoints with MockMvc
- Test API endpoints with TestRestTemplate
- Verify security configuration for both interfaces

### End-to-End Testing
- Test complete user workflows through web interface
- Verify API functionality with external clients
- Test file upload and test execution scenarios

## Migration Strategy

### Phase 1: Project Structure Consolidation
1. Create new single-module Maven project structure
2. Merge dependencies from both modules
3. Consolidate application.properties files

### Phase 2: Code Integration
1. Merge source code into unified package structure
2. Resolve any naming conflicts or duplications
3. Update import statements and references

### Phase 3: Configuration Unification
1. Merge security configurations
2. Consolidate port and service configurations
3. Update template and static resource paths

### Phase 4: Testing and Validation
1. Run all existing tests in new structure
2. Verify all functionality works as expected
3. Test deployment and startup procedures

## Design Decisions and Rationales

### Single Port Design
**Decision**: Use port 8080 for the unified application
**Rationale**: Simplifies deployment and eliminates the need for load balancers or reverse proxies in simple deployments. External integrations can still access API endpoints on the same port.

### Package Organization
**Decision**: Separate web and API controllers into different sub-packages
**Rationale**: Maintains clear separation of concerns while allowing code reuse. Makes it easier to apply different security policies or middleware to different controller types.

### Shared Service Layer
**Decision**: Use the same service instances for both web and API controllers
**Rationale**: Eliminates duplication and ensures consistency. Removes the overhead of HTTP communication between modules.

### Template and Static Resource Handling
**Decision**: Keep all Thymeleaf templates and static resources in the standard Spring Boot locations
**Rationale**: Follows Spring Boot conventions and requires minimal configuration changes.

### Security Model
**Decision**: Implement dual authentication (session-based for web, basic auth for API)
**Rationale**: Provides the best user experience for web users while maintaining API compatibility for external integrations.

### Configuration Management
**Decision**: Use a single application.properties file with unified configuration
**Rationale**: Simplifies configuration management and eliminates the need to maintain separate configuration files.

## Dependencies and Build Configuration

### Maven Dependencies
The merged application will include all dependencies from both modules:
- Spring Boot Web (for both web and API functionality)
- Spring Boot Security (unified authentication)
- Thymeleaf (template engine)
- WebJars (Bootstrap, jQuery)
- Commons IO (file operations)
- Lombok (code generation)
- Spring Boot Actuator (monitoring)

### Build Configuration
- Single executable JAR output
- Spring Boot Maven plugin configuration
- Unified version management
- Single build command for entire application

## Deployment Considerations

### Single JAR Deployment
The merged application will produce a single executable JAR file that contains all functionality, simplifying deployment and reducing operational complexity.

### Port Configuration
The application will use a single configurable port (default 8080) for all traffic, eliminating the need to manage multiple ports in firewall and load balancer configurations.

### Resource Requirements
Memory and CPU requirements should be similar to running both modules separately, but with reduced overhead from eliminating inter-service HTTP communication.