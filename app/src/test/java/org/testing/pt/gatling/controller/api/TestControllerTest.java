package org.testing.pt.gatling.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestParameters;
import org.testing.pt.gatling.model.TestStatus;
import org.testing.pt.gatling.service.GatlingService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestController API endpoints.
 */
@WebMvcTest(TestController.class)
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GatlingService gatlingService;

    private TestExecution sampleTest;
    private List<TestExecution> sampleTests;

    @BeforeEach
    void setUp() {
        // Create sample test data
        sampleTest = new TestExecution("Sample Test", "com.example.SampleTest", new TestParameters());
        sampleTest.setId("test-123");
        sampleTest.setStatus(TestStatus.COMPLETED);
        sampleTest.setStartTime(LocalDateTime.now().minusHours(1));
        sampleTest.setEndTime(LocalDateTime.now());

        TestExecution test2 = new TestExecution("Sample Test 2", "com.example.SampleTest2", new TestParameters());
        test2.setId("test-456");
        test2.setStatus(TestStatus.RUNNING);
        test2.setStartTime(LocalDateTime.now().minusMinutes(30));

        sampleTests = Arrays.asList(sampleTest, test2);
    }

    @Test
    void testGetAllTestsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/tests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testGetAllTestsWithAuth() throws Exception {
        // Arrange
        when(gatlingService.getAllTestExecutions()).thenReturn(sampleTests);

        // Act & Assert
        mockMvc.perform(get("/api/tests")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("test-123"))
                .andExpect(jsonPath("$[1].id").value("test-456"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testGetTestById() throws Exception {
        // Arrange
        when(gatlingService.getTestExecution("test-123")).thenReturn(sampleTest);

        // Act & Assert
        mockMvc.perform(get("/api/tests/test-123")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("test-123"))
                .andExpect(jsonPath("$.description").value("Sample Test"))
                .andExpect(jsonPath("$.testClass").value("com.example.SampleTest"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testGetTestByIdNotFound() throws Exception{
        // Arrange
        when(gatlingService.getTestExecution("non-existent")).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/tests/non-existent")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testStopTest() throws Exception {
        // Arrange
        when(gatlingService.stopTest("test-123")).thenReturn(true);

        // Act & Assert - API endpoints have CSRF disabled
        mockMvc.perform(post("/api/tests/test-123/stop")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testStopTestFailed() throws Exception {
        // Arrange
        when(gatlingService.stopTest("test-123")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/tests/test-123/stop")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testDeleteTest() throws Exception {
        // Arrange
        when(gatlingService.deleteTestExecution("test-123")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/tests/test-123")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testDeleteTestNotFound() throws Exception {
        // Arrange
        when(gatlingService.deleteTestExecution("non-existent")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/tests/non-existent")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testRunTestWithParameters() throws Exception {
        // Arrange
        TestParameters params = new TestParameters();
        params.addParameter("users", "50");
        params.addParameter("duration", "120");

        TestExecution newTest = new TestExecution("New Test", "com.example.NewTest", params);
        newTest.setId("test-new");
        newTest.setStatus(TestStatus.RUNNING);

        when(gatlingService.runTest(anyString(), anyString(), anyString(), anyString(), any(TestParameters.class)))
                .thenReturn(newTest);

        // Act & Assert
        mockMvc.perform(post("/api/tests/run")
                        .with(httpBasic("testuser", "testpass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-new"))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }
}
