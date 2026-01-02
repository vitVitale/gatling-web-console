package org.testing.pt.gatling.controller.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestParameters;
import org.testing.pt.gatling.service.GatlingService;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * REST controller for managing Gatling tests.
 */
@RestController
@RequestMapping("/api/tests")
public class TestController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    
    @Autowired
    private GatlingService gatlingService;
    
    /**
     * Runs a test with the provided parameters.
     * 
     * @param parameters test parameters
     * @return test execution
     */
    @PostMapping("/run")
    public ResponseEntity<TestExecution> runTestWithParameters(@RequestBody @Valid TestParameters parameters) {
        try {
            // Use a default test file for API testing
            TestExecution execution = gatlingService.runTest(
                    "default-test.jar",
                    "",
                    "Engine",
                    "API test execution",
                    parameters);
            return ResponseEntity.ok(execution);
        } catch (IllegalArgumentException e) {
            logger.error("Error running test", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Gets all test executions.
     * 
     * @return a list of all test executions
     */
    @GetMapping
    public ResponseEntity<List<TestExecution>> getAllTests() {
        List<TestExecution> executions = gatlingService.getAllTestExecutions();
        return ResponseEntity.ok(executions);
    }
    
    /**
     * Gets a test execution by ID.
     * 
     * @param id the test execution ID
     * @return the test execution
     */
    @GetMapping("/{id}")
    public ResponseEntity<TestExecution> getTest(@PathVariable String id) {
        TestExecution execution = gatlingService.getTestExecution(id);
        if (execution != null) {
            return ResponseEntity.ok(execution);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Stops a running test.
     * 
     * @param id the test execution ID
     * @return a response indicating success or failure
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<Void> stopTest(@PathVariable("id") String id) {
        boolean stopped = gatlingService.stopTest(id);
        if (stopped) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Deletes a test execution.
     * 
     * @param id the test execution ID
     * @return a response indicating success or failure
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTest(@PathVariable String id) {
        boolean deleted = gatlingService.deleteTestExecution(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}