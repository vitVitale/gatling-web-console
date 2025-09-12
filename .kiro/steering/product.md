# Product Overview

## Gatling Web Console

A web-based management console for running and monitoring Gatling performance tests. This application provides a user-friendly interface to execute Gatling test JAR files and view real-time results, serving as an open-source alternative to Gatling Enterprise Frontline.

### Key Features

- **Test Execution**: Upload and run Gatling test JAR files with configurable parameters
- **Real-time Monitoring**: Stream live logs from running tests via Server-Sent Events
- **File Management**: Scan and manage test simulation files in configured directories
- **Results Dashboard**: View test execution history, status, and detailed results
- **Web Interface**: Clean, responsive UI built with Bootstrap and Thymeleaf

### Architecture

The application follows a microservices architecture with two main modules:
- **Server Module** (port 8081): REST API backend for test execution and file management
- **Web UI Module** (port 8080): Frontend web interface that communicates with the server API

### Target Users

Performance testers and DevOps engineers who need to run Gatling tests without the complexity of command-line execution or the cost of enterprise solutions.