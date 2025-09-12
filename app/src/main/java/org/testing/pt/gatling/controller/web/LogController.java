package org.testing.pt.gatling.controller.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.service.GatlingService;
import org.testing.pt.gatling.service.LogStreamService;

/**
 * Controller for streaming logs from test executions.
 * This controller provides direct access to log streams without HTTP proxying.
 */
@RestController
@RequestMapping("/logs")
public class LogController {
    
    private static final Logger logger = LoggerFactory.getLogger(LogController.class);
    
    @Autowired
    private LogStreamService logStreamService;
    
    @Autowired
    private GatlingService gatlingService;
    
    /**
     * Streams logs for a test execution.
     * 
     * @param id the test execution ID
     * @return an SSE emitter for streaming logs
     */
    @GetMapping(value = "/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@PathVariable String id) {
        logger.info("Starting log stream for test: {}", id);
        
        TestExecution execution = gatlingService.getTestExecution(id);
        if (execution == null) {
            logger.error("Test execution not found: {}", id);
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new IllegalArgumentException("Test execution not found: " + id));
            return emitter;
        }
        
        return logStreamService.createLogEmitter(execution);
    }
}