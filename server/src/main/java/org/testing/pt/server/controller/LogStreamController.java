package org.testing.pt.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.testing.pt.server.service.LogStreamService;

/**
 * Controller for streaming logs from test executions.
 */
@RestController
@RequestMapping("/api/logs")
public class LogStreamController {
    
    private static final Logger logger = LoggerFactory.getLogger(LogStreamController.class);
    
    @Autowired
    private LogStreamService logStreamService;
    
    /**
     * Streams logs for a test execution.
     * 
     * @param id the test execution ID
     * @return an SSE emitter for streaming logs
     */
    @GetMapping(value = "/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@PathVariable("id") String id) {
        logger.info("Starting log stream for test: {}", id);
        return logStreamService.createLogEmitter(id);
    }
}