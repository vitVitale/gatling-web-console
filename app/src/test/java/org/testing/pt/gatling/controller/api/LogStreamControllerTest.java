package org.testing.pt.gatling.controller.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.testing.pt.gatling.service.LogStreamService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.testing.pt.gatling.service.GatlingService;
import org.testing.pt.gatling.model.TestExecution;

/**
 * Tests for the LogStreamController API class.
 */
@ExtendWith(MockitoExtension.class)
public class LogStreamControllerTest {
    
    @Mock
    private LogStreamService logStreamService;
    
    @Mock
    private GatlingService gatlingService;
    
    private LogStreamController logStreamController;
    private MockMvc mockMvc;
    
    @BeforeEach
    public void setUp() {
        logStreamController = new LogStreamController();
        ReflectionTestUtils.setField(logStreamController, "logStreamService", logStreamService);
        ReflectionTestUtils.setField(logStreamController, "gatlingService", gatlingService);
        mockMvc = MockMvcBuilders.standaloneSetup(logStreamController).build();
    }
    
    @Test
    public void testStreamLogs() throws Exception {
        String executionId = "test-execution-id";
        TestExecution mockExecution = new TestExecution();
        SseEmitter mockEmitter = new SseEmitter();
        
        when(gatlingService.getTestExecution(executionId)).thenReturn(mockExecution);
        when(logStreamService.createLogEmitter(mockExecution)).thenReturn(mockEmitter);
        
        mockMvc.perform(get("/api/logs/{id}", executionId))
                .andExpect(status().isOk());
        
        verify(gatlingService).getTestExecution(executionId);
        verify(logStreamService).createLogEmitter(mockExecution);
    }
    
    @Test
    public void testStreamLogsWithInvalidId() throws Exception {
        String executionId = "non-existent-id";
        
        when(gatlingService.getTestExecution(executionId)).thenReturn(null);
        
        mockMvc.perform(get("/api/logs/{id}", executionId))
                .andExpect(status().isOk()); // The controller returns an emitter with error
        
        verify(gatlingService).getTestExecution(executionId);
        verify(logStreamService, never()).createLogEmitter(any());
    }
}