package org.testing.pt.gatling.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testing.pt.gatling.model.FileInfo;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestParameters;
import org.testing.pt.gatling.model.TestStatus;
import org.testing.pt.gatling.service.FileScanner;
import org.testing.pt.gatling.service.GatlingService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for web controllers using MockMvc.
 * Tests the web interface endpoints with security configuration.
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class WebControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private FileScanner fileScanner;

    @MockBean
    private GatlingService gatlingService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testLoginPageAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Gatling Web Console")));
    }

    @Test
    void testLoginPageWithError() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Invalid username or password."));
    }

    @Test
    void testLoginPageWithLogout() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("message", "You have been logged out successfully."));
    }

    @Test
    void testUnauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void testUnauthenticatedAccessToRunTestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/run-test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void testUnauthenticatedAccessToResultsRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/results"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testDashboardPageWithAuthentication() throws Exception {
        // Mock file scanner to return test files
        FileInfo file1 = new FileInfo();
        file1.setName("test1.jar");
        FileInfo file2 = new FileInfo();
        file2.setName("test2.jar");
        List<FileInfo> mockFiles = Arrays.asList(file1, file2);
        when(fileScanner.getAllFiles()).thenReturn(mockFiles);

        // Mock gatling service to return test executions
        TestExecution execution1 = new TestExecution("Test 1", "test1.jar", new TestParameters());
        execution1.setId("test-1");
        execution1.setStatus(TestStatus.COMPLETED);
        execution1.setEndTime(LocalDateTime.now());
        
        TestExecution execution2 = new TestExecution("Test 2", "test2.jar", new TestParameters());
        execution2.setId("test-2");
        execution2.setStatus(TestStatus.RUNNING);
        
        List<TestExecution> mockExecutions = Arrays.asList(execution1, execution2);
        when(gatlingService.getAllTestExecutions()).thenReturn(mockExecutions);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("files", mockFiles))
                .andExpect(model().attribute("executions", mockExecutions));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testRunTestPageWithAuthentication() throws Exception {
        // Mock file scanner to return test files
        FileInfo file1 = new FileInfo();
        file1.setName("test1.jar");
        List<FileInfo> mockFiles = Arrays.asList(file1);
        when(fileScanner.getAllFiles()).thenReturn(mockFiles);

        mockMvc.perform(get("/run-test"))
                .andExpect(status().isOk())
                .andExpect(view().name("run-test"))
                .andExpect(model().attribute("files", mockFiles));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testResultsPageWithAuthentication() throws Exception {
        // Mock gatling service to return test executions
        TestExecution execution1 = new TestExecution("Test 1", "test1.jar", new TestParameters());
        execution1.setId("test-1");
        execution1.setStatus(TestStatus.COMPLETED);
        execution1.setEndTime(LocalDateTime.now());
        List<TestExecution> mockExecutions = Arrays.asList(execution1);
        when(gatlingService.getAllTestExecutions()).thenReturn(mockExecutions);

        mockMvc.perform(get("/results"))
                .andExpect(status().isOk())
                .andExpect(view().name("results"))
                .andExpect(model().attribute("executions", mockExecutions));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testResultDetailsPageWithAuthentication() throws Exception {
        String testId = "test-1";
        TestExecution mockExecution = new TestExecution("Test Description", "test1.jar", new TestParameters());
        mockExecution.setId(testId);
        mockExecution.setStatus(TestStatus.COMPLETED);
        mockExecution.setEndTime(LocalDateTime.now());
        when(gatlingService.getTestExecution(testId)).thenReturn(mockExecution);

        mockMvc.perform(get("/results/" + testId))
                .andExpect(status().isOk())
                .andExpect(view().name("result-details"))
                .andExpect(model().attribute("execution", mockExecution));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testResultDetailsPageWithNonExistentTest() throws Exception {
        String testId = "non-existent";
        when(gatlingService.getTestExecution(testId)).thenReturn(null);

        mockMvc.perform(get("/results/" + testId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testRunTestSubmission() throws Exception {
        String testId = "test-1";
        TestExecution mockExecution = new TestExecution("Test Description", "test1.jar", new TestParameters());
        mockExecution.setId(testId);
        mockExecution.setStatus(TestStatus.RUNNING);
        when(gatlingService.runTest(eq("test1.jar"), anyString(), eq("Test Description"), any(TestParameters.class))).thenReturn(mockExecution);

        mockMvc.perform(post("/run-test")
                .with(csrf())
                .param("fileName", "test1.jar")
                .param("users", "10")
                .param("duration", "5")
                .param("description", "Test Description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/results/" + testId));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testRunTestSubmissionWithInvalidParameters() throws Exception {
        mockMvc.perform(post("/run-test")
                .with(csrf())
                .param("fileName", "")
                .param("users", "0")
                .param("duration", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testStaticResourcesAccessible() throws Exception {
        // Test that static resources like CSS and JS are accessible without authentication
        mockMvc.perform(get("/webjars/bootstrap/5.3.2/css/bootstrap.min.css"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testLogStreamEndpoint() throws Exception {
        String testId = "test-1";
        TestExecution mockExecution = new TestExecution("Test Description", "test1.jar", new TestParameters());
        mockExecution.setId(testId);
        mockExecution.setStatus(TestStatus.RUNNING);
        when(gatlingService.getTestExecution(testId)).thenReturn(mockExecution);

        mockMvc.perform(get("/logs/" + testId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/event-stream"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testLogStreamEndpointWithNonExistentTest() throws Exception {
        String testId = "non-existent";
        when(gatlingService.getTestExecution(testId)).thenReturn(null);

        mockMvc.perform(get("/logs/" + testId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/event-stream"));
    }
}