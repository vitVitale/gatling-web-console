package org.testing.pt.gatling.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestParameters;
import org.testing.pt.gatling.model.TestStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for the GatlingService class.
 */
@ExtendWith(MockitoExtension.class)
public class GatlingServiceTest {
    
    @Mock
    private LogStreamService logStreamService;
    
    private GatlingService gatlingService;
    private static final String TEST_WORKSPACE = "./test-workspace";
    private static final String TEST_RESULTS = "./test-results";
    
    @BeforeEach
    public void setUp() throws IOException {
        gatlingService = new GatlingService();
        
        // Set up test directories using reflection
        ReflectionTestUtils.setField(gatlingService, "testsDirectory", TEST_WORKSPACE);
        ReflectionTestUtils.setField(gatlingService, "resultsDirectory", TEST_RESULTS);
        ReflectionTestUtils.setField(gatlingService, "logStreamService", logStreamService);
        
        // Create test directories
        Files.createDirectories(Paths.get(TEST_WORKSPACE));
        Files.createDirectories(Paths.get(TEST_RESULTS));
    }
    
    @Test
    public void testGetAllExecutions_EmptyList() {
        List<TestExecution> executions = gatlingService.getAllTestExecutions();
        assertNotNull(executions);
        assertTrue(executions.isEmpty());
    }
    
    // Note: The actual runTest method requires file validation and complex setup
    // Testing it would require mocking the FileScanner and process execution
    
    @Test
    public void testGetExecution_NotFound() {
        TestExecution execution = gatlingService.getTestExecution("non-existent-id");
        assertNull(execution);
    }
    
    @Test
    public void testStopExecution_NotFound() {
        boolean stopped = gatlingService.stopTest("non-existent-id");
        assertFalse(stopped);
    }
}