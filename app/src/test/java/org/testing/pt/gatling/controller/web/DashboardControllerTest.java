package org.testing.pt.gatling.controller.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;
import org.testing.pt.gatling.model.FileInfo;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestStatus;
import org.testing.pt.gatling.service.FileScanner;
import org.testing.pt.gatling.service.GatlingService;
import org.testing.pt.gatling.model.TestParameters;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for the DashboardController class.
 */
@ExtendWith(MockitoExtension.class)
public class DashboardControllerTest {
    
    @Mock
    private GatlingService gatlingService;
    
    @Mock
    private FileScanner fileScanner;
    
    @Mock
    private Model model;
    
    private DashboardController dashboardController;
    private MockMvc mockMvc;
    
    @BeforeEach
    public void setUp() {
        dashboardController = new DashboardController();
        ReflectionTestUtils.setField(dashboardController, "gatlingService", gatlingService);
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
    }
    
    // Note: These tests would require full Spring context with Thymeleaf templates
    // For unit testing, we focus on the controller logic without template rendering
    
    @Test
    public void testDashboardWithModel() {
        List<TestExecution> executions = Arrays.asList(
            createTestExecution("1", "test1.jar", TestStatus.COMPLETED)
        );
        
        when(gatlingService.getAllTestExecutions()).thenReturn(executions);
        
        String viewName = dashboardController.dashboard(model);
        
        assertEquals("dashboard", viewName);
        verify(model).addAttribute("recentTests", executions);
        verify(gatlingService).getAllTestExecutions();
    }
    
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