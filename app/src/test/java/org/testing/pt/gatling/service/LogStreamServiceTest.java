package org.testing.pt.gatling.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.testing.pt.gatling.model.TestExecution;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the LogStreamService class.
 */
public class LogStreamServiceTest {
    
    private LogStreamService logStreamService;
    
    @BeforeEach
    public void setUp() {
        logStreamService = new LogStreamService();
    }
    
    @Test
    public void testCreateLogEmitter() {
        TestExecution execution = new TestExecution();
        execution.setId("test-execution-id");
        
        SseEmitter emitter = logStreamService.createLogEmitter(execution);
        
        assertNotNull(emitter);
        assertEquals(0L, emitter.getTimeout());
    }
    
    @Test
    public void testSendLogMessage() throws IOException {
        TestExecution execution = new TestExecution();
        execution.setId("test-execution-id");
        SseEmitter emitter = logStreamService.createLogEmitter(execution);
        
        // This should not throw an exception
        assertDoesNotThrow(() -> {
            logStreamService.sendLogMessage(execution.getId(), "Test log message");
        });
    }
    
    @Test
    public void testSendLogMessage_NoEmitter() {
        // This should not throw an exception even if no emitter exists
        assertDoesNotThrow(() -> {
            logStreamService.sendLogMessage("non-existent-id", "Test log message");
        });
    }
    
    // Note: The actual LogStreamService doesn't have a completeStream method
    // Completion is handled automatically by the emitter callbacks
    
    @Test
    public void testMultipleStreams() {
        TestExecution execution1 = new TestExecution();
        execution1.setId("test-execution-1");
        TestExecution execution2 = new TestExecution();
        execution2.setId("test-execution-2");
        
        SseEmitter emitter1 = logStreamService.createLogEmitter(execution1);
        SseEmitter emitter2 = logStreamService.createLogEmitter(execution2);
        
        assertNotNull(emitter1);
        assertNotNull(emitter2);
        assertNotSame(emitter1, emitter2);
        
        // Both streams should work independently
        assertDoesNotThrow(() -> {
            logStreamService.sendLogMessage(execution1.getId(), "Message for stream 1");
            logStreamService.sendLogMessage(execution2.getId(), "Message for stream 2");
        });
    }
}