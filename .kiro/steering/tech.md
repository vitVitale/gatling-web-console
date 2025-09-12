# Technology Stack

## Build System & Dependencies

- **Build Tool**: Maven 3.x with multi-module project structure
- **Java Version**: Java 17 (LTS)
- **Spring Boot**: 3.2.3 for both modules
- **Gatling**: 3.10.3 (for performance testing framework integration)

## Server Module Tech Stack

- **Spring Boot Web**: REST API endpoints
- **Spring Boot Actuator**: Health checks and monitoring
- **Spring Security**: Basic authentication for API endpoints
- **Commons IO**: File operations and utilities
- **Lombok**: Reducing boilerplate code with annotations
- **Spring Scheduling**: Automated file scanning tasks

## Web UI Module Tech Stack

- **Spring Boot Web**: Web application framework
- **Thymeleaf**: Server-side templating engine
- **Bootstrap 5.3.2**: CSS framework via WebJars
- **jQuery 3.7.1**: JavaScript library via WebJars
- **Spring Boot DevTools**: Development hot-reload

## Common Build Commands

### Root Project
```bash
# Build entire project
mvn clean install

# Run tests for all modules
mvn test

# Package all modules
mvn package
```

### Server Module
```bash
# Run server (from server directory)
mvn spring-boot:run

# Build server JAR
mvn clean package

# Run server JAR
java -jar target/server-1.0-SNAPSHOT.jar
```

### Web UI Module
```bash
# Run web UI (from web-ui directory)
mvn spring-boot:run

# Build web UI JAR
mvn clean package

# Run web UI JAR
java -jar target/web-ui-1.0-SNAPSHOT.jar
```

## Configuration

- **Server Port**: 8081 (configurable via application.properties)
- **Web UI Port**: 8080 (configurable via application.properties)
- **Default Credentials**: admin/password (configurable)
- **File Scanning**: Every 20 seconds via @Scheduled annotation