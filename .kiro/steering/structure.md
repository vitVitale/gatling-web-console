# Project Structure

## Multi-Module Maven Project

The project follows a standard Maven multi-module structure with clear separation of concerns:

```
gatling-web-console/
├── pom.xml                    # Parent POM with shared dependencies
├── server/                    # Backend API module
├── web-ui/                    # Frontend web interface module
├── simulations/               # Test simulation JAR files
├── gatling-results/           # Generated test results
└── gatling-tests/             # Test execution workspace
```

## Server Module Structure

```
server/
├── pom.xml
├── src/main/java/org/testing/pt/server/
│   ├── ServerApplication.java          # Main Spring Boot application
│   ├── config/
│   │   └── SecurityConfig.java         # Security configuration
│   ├── controller/
│   │   ├── FileController.java         # File management REST API
│   │   ├── LogStreamController.java    # Log streaming endpoints
│   │   └── TestController.java         # Test execution REST API
│   ├── model/
│   │   ├── FileInfo.java              # File metadata model
│   │   ├── TestExecution.java         # Test execution model
│   │   ├── TestParameters.java        # Test configuration model
│   │   └── TestStatus.java            # Test status enum
│   └── service/
│       ├── FileScanner.java           # Scheduled file scanning
│       ├── GatlingService.java        # Test execution service
│       └── LogStreamService.java      # Log streaming service
└── src/main/resources/
    └── application.properties          # Server configuration
```

## Web UI Module Structure

```
web-ui/
├── pom.xml
├── src/main/java/org/testing/pt/webui/
│   ├── WebUiApplication.java           # Main Spring Boot application
│   ├── controller/
│   │   ├── DashboardController.java    # Web page controllers
│   │   └── LogController.java          # Log proxy controller
│   ├── model/                          # DTOs for server communication
│   │   ├── TestExecution.java
│   │   ├── TestParameters.java
│   │   └── TestStatus.java
│   └── service/
│       └── TestService.java            # Server API client
└── src/main/resources/
    ├── application.properties           # Web UI configuration
    └── templates/                       # Thymeleaf templates
        ├── dashboard.html
        ├── layout.html
        ├── result-details.html
        ├── results.html
        └── run-test.html
```

## Package Naming Convention

- **Base Package**: `org.testing.pt`
- **Server Module**: `org.testing.pt.server`
- **Web UI Module**: `org.testing.pt.webui`
- **Subpackages**: `config`, `controller`, `model`, `service`

## Key Directories

- **simulations/**: Contains Gatling test JAR files to be executed
- **gatling-results/**: Output directory for test results and reports
- **gatling-tests/**: Working directory for test execution
- **target/**: Maven build output (excluded from version control)

## Configuration Files

- **Root pom.xml**: Defines shared properties and dependency management
- **Module pom.xml**: Module-specific dependencies and build configuration
- **application.properties**: Spring Boot configuration for each module