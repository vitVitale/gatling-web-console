# Project Guidelines

# Gatling Controller Agent - Project Requirements

## Overview

This Java application ("Agent") is responsible for:

- Accepting requests to run arbitrary JAR files as subprocesses
- Streaming real-time stdout/stderr logs from those subprocesses
- Exposing an interface for a web UI or controller to:
    - Start/stop subprocesses
    - Receive live logs

No test metrics, dashboards, or historical storage are required for this phase.

---

## 1. Core Features

### 1.1 Run JAR as Subprocess

- Accept a command (e.g. via REST API or socket) to launch a given JAR file with specified arguments.
- Each subprocess must be uniquely identified (e.g., by a generated run ID).
- Support only one or multiple concurrent processes (define which as MVP).

### 1.2 Live Log Streaming

- Capture and stream both stdout and stderr output from the subprocess in real time.
- Provide a way for clients to receive log lines as soon as they are produced.

### 1.3 Process Control

- Allow starting and stopping subprocesses via API.
- Expose status (running, finished, error) for each process.

---

## 2. Interface

### 2.1 API Options

Choose one (or both) of:
- **REST API:** For starting/stopping processes, basic status queries
- **WebSocket or Server-Sent Events:** For pushing real-time logs to clients

### 2.2 API Endpoints (suggested)

- `POST /process/start`
    - Request: JAR file path, arguments (optional)
    - Response: process/run ID
- `POST /process/stop`
    - Request: process/run ID
    - Response: status
- `GET /process/status/{id}`
    - Response: running, finished, error, exit code
- `GET /process/logs/{id}` (WebSocket or SSE)
    - Streams live log lines for the given process ID

---

## 3. Security

- Only allow launching JARs from a designated folder.
- Validate input to prevent injection or unauthorized execution.
- Optional: Basic API authentication (token or password).

---

## 4. Environment

- Java 17+ (for latest Process API features and better performance)
- Build with Maven or Gradle
- Use Spring Boot for REST/WebSocket (optional but recommended)

---

## 5. Other Considerations

- Handle subprocess failures and resource cleanup.
- Limit maximum number of concurrent processes (configurable).
- Log agent actions to its own log (not just subprocess logs).
- Graceful shutdown of running subprocesses when agent stops.

---

## 6. Example Sequence

1. Client sends `POST /process/start` with JAR path/args.
2. Agent launches the JAR as a subprocess, returns a process ID.
3. Client connects to `/process/logs/{id}` via WebSocket or SSE.
4. Agent streams each log line to the client as soon as it appears.
5. Client can stop process via `POST /process/stop`.
6. Client can query process status via `GET /process/status/{id}`.

---

## 7. Out of Scope

- No result/metrics parsing or dashboards
- No database for persistent storage
- No support for uploading JARs (JARs must be present on disk)
- No user management or advanced authentication

---

## 8. Stretch Goals (Future)

- Support uploading JAR files
- Historical log storage
- Multiple agent support (distributed execution)
- Metrics extraction and basic dashboards
- User authentication & authorization
