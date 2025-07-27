package org.testing.pt.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.testing.pt.server.model.TestExecution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for streaming logs from test executions.
 */
@Service
public class LogStreamService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogStreamService.class);
    
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    @Autowired
    private GatlingService gatlingService;
    
    /**
     * Creates an SSE emitter for streaming logs from a test execution.
     * 
     * @param testId the test execution ID
     * @return an SSE emitter
     */
    public SseEmitter createLogEmitter(String testId) {
        TestExecution execution = gatlingService.getTestExecution(testId);
//        TestExecution execution = null;
        if (execution == null) {
            throw new IllegalArgumentException("Test execution not found: " + testId);
        }
        
        // Create a new emitter with a timeout
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        
        // Store the emitter
        emitters.put(testId, emitter);
        
        // Set up completion callbacks
        emitter.onCompletion(() -> {
            logger.info("Log stream completed for test: {}", testId);
            emitters.remove(testId);
        });
        
        emitter.onTimeout(() -> {
            logger.info("Log stream timed out for test: {}", testId);
            emitters.remove(testId);
        });
        
        emitter.onError(e -> {
            logger.error("Error in log stream for test: {}", testId, e);
            emitters.remove(testId);
        });
        
        // Start streaming logs if the test is running
        if (execution.getStatus() == org.testing.pt.server.model.TestStatus.RUNNING) {
            Process process = execution.getProcess();
            if (process != null && process.isAlive()) {
                streamProcessLogs(testId, process, emitter);
            }
        }
        
        return emitter;
    }
    
    /**
     * Streams logs from a process to an SSE emitter.
     * 
     * @param testId the test execution ID
     * @param process the process to stream logs from
     * @param emitter the SSE emitter to send logs to
     */
    private void streamProcessLogs(String testId, Process process, SseEmitter emitter) {
        executorService.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String logLine = line;
                    try {
                        emitter.send(SseEmitter.event()
//                                .name("log")
                                .data(logLine));
                        
                        logger.debug("Sent log line for test {}: {}", testId, logLine);
                    } catch (IOException e) {
                        logger.error("Error sending log line for test: {}", testId, e);
                        emitter.completeWithError(e);
                        break;
                    }
                }
                
                // Complete the emitter when the process ends
                emitter.complete();
            } catch (IOException e) {
                logger.error("Error reading logs for test: {}", testId, e);
                emitter.completeWithError(e);
            }
        });
    }
    
    /**
     * Sends a log message to all clients subscribed to a test execution.
     * 
     * @param testId the test execution ID
     * @param message the log message
     */
    public void sendLogMessage(String testId, String message) {
        SseEmitter emitter = emitters.get(testId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("log")
                        .data(message));
                
                logger.debug("Sent log message for test {}: {}", testId, message);
            } catch (IOException e) {
                logger.error("Error sending log message for test: {}", testId, e);
                emitter.completeWithError(e);
                emitters.remove(testId);
            }
        }
    }
}