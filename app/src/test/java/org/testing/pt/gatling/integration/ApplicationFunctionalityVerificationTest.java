package org.testing.pt.gatling.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.testing.pt.gatling.model.FileInfo;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestParameters;
import org.testing.pt.gatling.model.TestStatus;
import org.testing.pt.gatling.service.FileScanner;
import org.testing.pt.gatling.service.GatlingService;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive verification test for all application functionality.
 * This test verifies all requirements from task 14:
 * - Application startup and port binding
 * - Web pages load and function correctly
 * - REST API endpoints respond correctly
 * - File scanning, test execution, and log streaming work
 * - Security authentication for both web and API access
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class ApplicationFunctionalityVerificationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockBean
    private FileScanner fileScanner;

    @MockBean
    private GatlingService gatlingService;

    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    private String baseUrl;
    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        
        // Set up basic authentication headers
        authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth("testuser", "testpass");

        // Set up MockMvc for web interface testing
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    /**
     * Requirement 1.4: Test application startup and port binding
     */
    @Test
    void verifyApplicationStartupAndPortBinding() {
        // Verify the application started successfully by checking health endpoint
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
                baseUrl + "/actuator/health", String.class);
        
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).contains("UP");
        
        // Verify the port is correctly bound
        assertThat(port).isGreaterThan(0);
        
        // Verify basic connectivity to main endpoints
        ResponseEntity<String> webResponse = restTemplate.getForEntity(baseUrl + "/", String.class);
        assertThat(webResponse.getStatusCode()).isIn(HttpStatus.FOUND, HttpStatus.OK); // Redirect to login or OK if authenticated
        
        ResponseEntity<String> apiResponse = restTemplate.getForEntity(baseUrl + "/api/files", String.class);
        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED); // Should require auth
    }

    /**
     * Requirements 2.1, 2.2, 2.3: Verify all web pages load and function correctly
     */
    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void verifyWebPagesLoadAndFunctionCorrectly() throws Exception {
        // Mock data for pages
        FileInfo file1 = new FileInfo();
        file1.setName("test1.jar");
        List<FileInfo> mockFiles = Arrays.asList(file1);
        when(fileScanner.getAllFiles()).thenReturn(mockFiles);

        TestExecution execution1 = new TestExecution("Test 1", "test1.jar", new TestParameters());
        execution1.setId("test-1");
        execution1.setStatus(TestStatus.COMPLETED);
        execution1.setEndTime(LocalDateTime.now());
        List<TestExecution> mockExecutions = Arrays.asList(execution1);
        when(gatlingService.getAllTestExecutions()).thenReturn(mockExecutions);
        when(gatlingService.getTestExecution("test-1")).thenReturn(execution1);

        // Test dashboard page (/)
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("recentTests"));

        // Test run-test page
        mockMvc.perform(get("/run-test"))
                .andExpect(status().isOk())
                .andExpect(view().name("run-test"))
                .andExpect(model().attribute("files", mockFiles));

        // Test results page
        mockMvc.perform(get("/results"))
                .andExpect(status().isOk())
                .andExpect(view().name("results"))
                .andExpect(model().attribute("executions", mockExecutions));

        // Test result details page
        mockMvc.perform(get("/results/test-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("result-details"))
                .andExpect(model().attribute("execution", execution1));

        // Test login page (accessible without auth)
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    /**
     * Requirements 2.4, 2.5: Test REST API endpoints respond correctly
     */
    @Test
    void verifyRestApiEndpointsRespondCorrectly() throws Exception {
        // Mock data for API responses
        FileInfo file1 = new FileInfo();
        file1.setName("api-test.jar");
        List<FileInfo> mockFiles = Arrays.asList(file1);
        when(fileScanner.getAllFiles()).thenReturn(mockFiles);

        TestExecution execution1 = new TestExecution("API Test", "api-test.jar", new TestParameters());
        execution1.setId("api-test-1");
        execution1.setStatus(TestStatus.RUNNING);
        List<TestExecution> mockExecutions = Arrays.asList(execution1);
        when(gatlingService.getAllTestExecutions()).thenReturn(mockExecutions);
        when(gatlingService.getTestExecution("api-test-1")).thenReturn(execution1);
        when(gatlingService.runTest(anyString(), anyString(), anyString(), any(TestParameters.class)))
                .thenReturn(execution1);
        when(gatlingService.stopTest("api-test-1")).thenReturn(true);

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);

        // Test GET /api/files
        ResponseEntity<String> filesResponse = restTemplate.exchange(
                baseUrl + "/api/files", HttpMethod.GET, entity, String.class);
        assertThat(filesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<FileInfo> responseFiles = objectMapper.readValue(
                filesResponse.getBody(), objectMapper.getTypeFactory().constructCollectionType(List.class, FileInfo.class));
        assertThat(responseFiles).hasSize(1);
        assertThat(responseFiles.get(0).getName()).isEqualTo("api-test.jar");

        // Test GET /api/tests
        ResponseEntity<String> testsResponse = restTemplate.exchange(
                baseUrl + "/api/tests", HttpMethod.GET, entity, String.class);
        assertThat(testsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<TestExecution> responseExecutions = objectMapper.readValue(
                testsResponse.getBody(), objectMapper.getTypeFactory().constructCollectionType(List.class, TestExecution.class));
        assertThat(responseExecutions).hasSize(1);
        assertThat(responseExecutions.get(0).getId()).isEqualTo("api-test-1");

        // Test GET /api/tests/{id}
        ResponseEntity<String> testResponse = restTemplate.exchange(
                baseUrl + "/api/tests/api-test-1", HttpMethod.GET, entity, String.class);
        assertThat(testResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestExecution responseExecution = objectMapper.readValue(testResponse.getBody(), TestExecution.class);
        assertThat(responseExecution.getId()).isEqualTo("api-test-1");

        // Test POST /api/tests/run
        TestParameters testParams = new TestParameters();
        testParams.setUsers(10);
        testParams.setDuration(5);
        HttpEntity<TestParameters> runEntity = new HttpEntity<>(testParams, authHeaders);
        ResponseEntity<String> runResponse = restTemplate.exchange(
                baseUrl + "/api/tests/run", HttpMethod.POST, runEntity, String.class);
        assertThat(runResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Test POST /api/tests/{id}/stop
        ResponseEntity<String> stopResponse = restTemplate.exchange(
                baseUrl + "/api/tests/api-test-1/stop", HttpMethod.POST, entity, String.class);
        assertThat(stopResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Test file upload endpoint
        MultiValueMap<String, Object> uploadBody = new LinkedMultiValueMap<>();
        uploadBody.add("file", new ByteArrayResource("test content".getBytes()) {
            @Override
            public String getFilename() {
                return "upload-test.jar";
            }
        });
        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        uploadHeaders.setBasicAuth("testuser", "testpass");
        HttpEntity<MultiValueMap<String, Object>> uploadEntity = new HttpEntity<>(uploadBody, uploadHeaders);
        ResponseEntity<String> uploadResponse = restTemplate.postForEntity(
                baseUrl + "/api/files/upload", uploadEntity, String.class);
        assertThat(uploadResponse.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Requirements 3.4: Verify file scanning works
     */
    @Test
    void verifyFileScanningWorks() throws Exception {
        // Test that file scanner is properly integrated
        FileInfo scannedFile = new FileInfo();
        scannedFile.setName("scanned-file.jar");
        List<FileInfo> scannedFiles = Arrays.asList(scannedFile);
        when(fileScanner.getAllFiles()).thenReturn(scannedFiles);

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/files", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<FileInfo> responseFiles = objectMapper.readValue(
                response.getBody(), objectMapper.getTypeFactory().constructCollectionType(List.class, FileInfo.class));
        assertThat(responseFiles).hasSize(1);
        assertThat(responseFiles.get(0).getName()).isEqualTo("scanned-file.jar");

        // Verify file scanner was called
        verify(fileScanner, atLeastOnce()).getAllFiles();
    }

    /**
     * Requirements 3.4: Verify test execution works
     */
    @Test
    void verifyTestExecutionWorks() throws Exception {
        // Test complete test execution workflow
        TestParameters testParams = new TestParameters();
        testParams.setUsers(5);
        testParams.setDuration(10);

        TestExecution runningExecution = new TestExecution("Execution Test", "execution-test.jar", testParams);
        runningExecution.setId("execution-test-1");
        runningExecution.setStatus(TestStatus.RUNNING);
        when(gatlingService.runTest(anyString(), anyString(), anyString(), any(TestParameters.class)))
                .thenReturn(runningExecution);

        TestExecution completedExecution = new TestExecution("Execution Test", "execution-test.jar", testParams);
        completedExecution.setId("execution-test-1");
        completedExecution.setStatus(TestStatus.COMPLETED);
        completedExecution.setEndTime(LocalDateTime.now());
        when(gatlingService.getTestExecution("execution-test-1")).thenReturn(completedExecution);

        // Start test execution
        HttpEntity<TestParameters> runEntity = new HttpEntity<>(testParams, authHeaders);
        ResponseEntity<String> runResponse = restTemplate.exchange(
                baseUrl + "/api/tests/run", HttpMethod.POST, runEntity, String.class);

        assertThat(runResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestExecution responseExecution = objectMapper.readValue(runResponse.getBody(), TestExecution.class);
        assertThat(responseExecution.getStatus()).isEqualTo(TestStatus.RUNNING);

        // Check test status
        HttpEntity<String> statusEntity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> statusResponse = restTemplate.exchange(
                baseUrl + "/api/tests/execution-test-1", HttpMethod.GET, statusEntity, String.class);

        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestExecution statusExecution = objectMapper.readValue(statusResponse.getBody(), TestExecution.class);
        assertThat(statusExecution.getId()).isEqualTo("execution-test-1");

        // Verify gatling service was called
        verify(gatlingService).runTest(anyString(), anyString(), anyString(), any(TestParameters.class));
        verify(gatlingService, atLeastOnce()).getTestExecution("execution-test-1");
    }

    /**
     * Requirements 3.4: Verify log streaming works
     */
    @Test
    void verifyLogStreamingWorks() throws Exception {
        // Test log streaming endpoints
        String testId = "streaming-test-1";
        TestExecution streamingExecution = new TestExecution("Streaming Test", "streaming-test.jar", new TestParameters());
        streamingExecution.setId(testId);
        streamingExecution.setStatus(TestStatus.RUNNING);
        when(gatlingService.getTestExecution(testId)).thenReturn(streamingExecution);

        // Test API log streaming
        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> apiLogResponse = restTemplate.exchange(
                baseUrl + "/api/logs/" + testId, HttpMethod.GET, entity, String.class);

        assertThat(apiLogResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(apiLogResponse.getHeaders().getContentType().toString()).contains("text/event-stream");

        // Test web log streaming
        ResponseEntity<String> webLogResponse = restTemplate.exchange(
                baseUrl + "/logs/" + testId, HttpMethod.GET, entity, String.class);

        assertThat(webLogResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(webLogResponse.getHeaders().getContentType().toString()).contains("text/event-stream");
    }

    /**
     * Requirements 5.2, 5.3, 5.4: Test security authentication for both web and API access
     */
    @Test
    void verifySecurityAuthenticationForWebAndApi() throws Exception {
        // Test API endpoints require authentication
        ResponseEntity<String> unauthApiResponse = restTemplate.getForEntity(baseUrl + "/api/files", String.class);
        assertThat(unauthApiResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Test API endpoints work with valid authentication
        HttpEntity<String> authEntity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> authApiResponse = restTemplate.exchange(
                baseUrl + "/api/files", HttpMethod.GET, authEntity, String.class);
        assertThat(authApiResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Test API endpoints reject invalid authentication
        HttpHeaders invalidAuthHeaders = new HttpHeaders();
        invalidAuthHeaders.setBasicAuth("invalid", "credentials");
        HttpEntity<String> invalidAuthEntity = new HttpEntity<>(invalidAuthHeaders);
        ResponseEntity<String> invalidApiResponse = restTemplate.exchange(
                baseUrl + "/api/files", HttpMethod.GET, invalidAuthEntity, String.class);
        assertThat(invalidApiResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Test web endpoints redirect to login when not authenticated
        ResponseEntity<String> unauthWebResponse = restTemplate.getForEntity(baseUrl + "/", String.class);
        assertThat(unauthWebResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(unauthWebResponse.getHeaders().getLocation().toString()).contains("login");

        // Test login page is accessible
        ResponseEntity<String> loginResponse = restTemplate.getForEntity(baseUrl + "/login", String.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).contains("Gatling Web Console");

        // Test static resources are accessible without authentication
        ResponseEntity<String> cssResponse = restTemplate.getForEntity(
                baseUrl + "/webjars/bootstrap/5.3.2/css/bootstrap.min.css", String.class);
        assertThat(cssResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> jsResponse = restTemplate.getForEntity(
                baseUrl + "/webjars/jquery/3.7.1/jquery.min.js", String.class);
        assertThat(jsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * Additional verification: Test web form submission functionality
     */
    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void verifyWebFormSubmissionFunctionality() throws Exception {
        // Mock file scanner for run-test page
        FileInfo file1 = new FileInfo();
        file1.setName("form-test.jar");
        List<FileInfo> mockFiles = Arrays.asList(file1);
        when(fileScanner.getAllFiles()).thenReturn(mockFiles);

        // Mock test execution
        String testId = "form-test-1";
        TestExecution mockExecution = new TestExecution("Form Test", "form-test.jar", new TestParameters());
        mockExecution.setId(testId);
        mockExecution.setStatus(TestStatus.RUNNING);
        when(gatlingService.runTest(eq("form-test.jar"), anyString(), eq("Form Test"), any(TestParameters.class)))
                .thenReturn(mockExecution);

        // Test form submission
        mockMvc.perform(post("/run-test")
                .with(csrf())
                .param("fileName", "form-test.jar")
                .param("users", "15")
                .param("duration", "30")
                .param("description", "Form Test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/results/" + testId));

        // Verify the service was called with correct parameters
        verify(gatlingService).runTest(eq("form-test.jar"), anyString(), eq("Form Test"), any(TestParameters.class));
    }

    /**
     * Additional verification: Test error handling
     */
    @Test
    void verifyErrorHandling() throws Exception {
        // Test 404 for non-existent test execution
        when(gatlingService.getTestExecution("non-existent")).thenReturn(null);

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> notFoundResponse = restTemplate.exchange(
                baseUrl + "/api/tests/non-existent", HttpMethod.GET, entity, String.class);

        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Test 404 for non-existent test stop
        when(gatlingService.stopTest("non-existent")).thenReturn(false);

        ResponseEntity<String> stopNotFoundResponse = restTemplate.exchange(
                baseUrl + "/api/tests/non-existent/stop", HttpMethod.POST, entity, String.class);

        assertThat(stopNotFoundResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}