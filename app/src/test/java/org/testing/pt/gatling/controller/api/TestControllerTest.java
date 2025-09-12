package org.testing.pt.gatling.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestParameters;
import org.testing.pt.gatling.model.TestStatus;
import org.testing.pt.gatling.service.GatlingService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for the TestController API class.
 */
@ExtendWith(MockitoExtension.class)
public class TestControllerTest {
    
    @Mock
    private GatlingService gatlingService;
    
    private TestController testController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    public void setUp() {
        testController = new TestController();
        ReflectionTestUtils.setField(testController, "gatlingService", gatlingService);
        mockMvc = MockMvcBuilders.standaloneSetup(testController).build();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    public void testGetAllExecutions() throws Exception {
        List<TestExecution> executions = Arrays.asList(
            createTestExecution("1", "test1.jar", TestStatus.COMPLETED),
            createTestExecution("2", "test2.jar", TestStatus.RUNNING)
        );
        
        when(gatlingService.getAllTestExecutions()).thenReturn(executions);
        
        mockMvc.perform(get("/api/tests"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));
        
        verify(gatlingService).getAllTestExecutions();
    }
    
    @Test
    public void testGetExecution_Found() throws Exception {
        String executionId = "test-execution-id";
        TestExecution execution = createTestExecution(executionId, "test.jar", TestStatus.COMPLETED);
        
        when(gatlingService.getTestExecution(executionId)).thenReturn(execution);
        
        mockMvc.perform(get("/api/tests/{id}", executionId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(executionId))
                .andExpect(jsonPath("$.testClass").value("test.jar"));
        
        verify(gatlingService).getTestExecution(executionId);
    }
    
    @Test
    public void testGetExecution_NotFound() throws Exception {
        String executionId = "non-existent-id";
        
        when(gatlingService.getTestExecution(executionId)).thenReturn(null);
        
        mockMvc.perform(get("/api/tests/{id}", executionId))
                .andExpect(status().isNotFound());
        
        verify(gatlingService).getTestExecution(executionId);
    }
    
    // Note: The actual API doesn't have a simple POST /api/tests endpoint
    // It has /api/tests/upload and /api/tests/run endpoints instead
    
    @Test
    public void testStopExecution_Success() throws Exception {
        String executionId = "test-execution-id";
        
        when(gatlingService.stopTest(executionId)).thenReturn(true);
        
        mockMvc.perform(post("/api/tests/{id}/stop", executionId))
                .andExpect(status().isOk());
        
        verify(gatlingService).stopTest(executionId);
    }
    
    @Test
    public void testStopExecution_NotFound() throws Exception {
        String executionId = "non-existent-id";
        
        when(gatlingService.stopTest(executionId)).thenReturn(false);
        
        mockMvc.perform(post("/api/tests/{id}/stop", executionId))
                .andExpect(status().isBadRequest());
        
        verify(gatlingService).stopTest(executionId);
    }
    
    // Note: Log streaming is handled by LogStreamController, not TestController
    
    private TestExecution createTestExecution(String id, String testClass, TestStatus status) {
        TestExecution execution = new TestExecution();
        execution.setId(id);
        execution.setTestClass(testClass);
        execution.setStatus(status);
        execution.setStartTime(LocalDateTime.now());
        execution.setParameters(new TestParameters(10, 30, 60));
        return execution;
    }
}