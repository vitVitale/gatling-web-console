# Gatling Web Console Server

This module provides the server-side functionality for the Gatling Web Console application.

## Features

### Process Management
- Run JAR files as subprocesses
- Stream real-time stdout/stderr logs
- Start/stop processes
- Get process status

### File Scanning
- Scan a configured folder for files
- Maintain a list of files in memory
- Expose file information via REST API

## Configuration

The application can be configured using the `application.properties` file:

```properties
# Server configuration
server.port=8081
server.servlet.context-path=/

# Gatling configuration
gatling.tests.dir=./gatling-tests
gatling.results.dir=./gatling-results

# Simulations folder configuration
simulations.folder=./simulations

# Security configuration
api.security.username=admin
api.security.password=password
```

## API Endpoints

### Process Management

- `POST /api/tests/upload` - Upload and run a test JAR file
- `GET /api/tests` - Get all test executions
- `GET /api/tests/{id}` - Get a test execution by ID
- `POST /api/tests/{id}/stop` - Stop a running test
- `DELETE /api/tests/{id}` - Delete a test execution

### Log Streaming

- `GET /api/logs/{id}` - Stream logs for a test execution (Server-Sent Events)

### File Scanning

- `GET /api/files` - Get all files in the simulations folder

## File Scanning Feature

The file scanning feature scans a configured folder for files and exposes them via a REST API. It has the following characteristics:

- Scans the folder every 20 seconds
- Only scans the specified folder (no subfolders)
- Maintains a list of files in memory (no database)
- Returns file information as JSON via the REST API

### File Information

The file information includes:

- Name - The name of the file
- Path - The full path to the file
- Size - The size of the file in bytes
- Last Modified - The timestamp when the file was last modified

### Usage

1. Configure the simulations folder in `application.properties`:

   ```properties
   simulations.folder=/path/to/simulations
   ```

2. Access the file information via the REST API:

   ```
   GET /api/files
   ```

   Example response:

   ```json
   [
     {
       "name": "test1.jar",
       "path": "/path/to/simulations/test1.jar",
       "size": 1024,
       "lastModified": "2025-07-27T10:15:30Z"
     },
     {
       "name": "test2.jar",
       "path": "/path/to/simulations/test2.jar",
       "size": 2048,
       "lastModified": "2025-07-27T10:20:45Z"
     }
   ]
   ```

## Implementation Details

The file scanning feature is implemented using the following components:

- `FileInfo` - A model class that represents information about a file
- `FileScanner` - A service that scans the configured folder and maintains a list of files
- `FileController` - A REST controller that exposes the file information via a REST API

The `FileScanner` service uses Spring's `@Scheduled` annotation to scan the folder every 20 seconds. It uses Java's NIO API to read file information and stores it in a thread-safe list.