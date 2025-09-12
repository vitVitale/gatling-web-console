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

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for complete workflows including file upload and test execution.
 * Tests end-to-end scenarios that users would perform.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class WorkflowIntegrationTest {

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

    @Test
    void testCompleteFileUploadWorkflowViaApi() throws Exception {
        // Step 1: Upload a file via API
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("mock jar content".getBytes()) {
            @Override
            public String getFilename() {
                return "test-simulation.jar";
            }
        });

        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        uploadHeaders.setBasicAuth("testuser", "testpass");
        
        HttpEntity<MultiValueMap<String, Object>> uploadEntity = new HttpEntity<>(body, uploadHeaders);
        ResponseEntity<String> uploadResponse = restTemplate.postForEntity(
                baseUrl + "/api/files/upload", uploadEntity, String.class);

        // Upload should be successful (or at least authenticated)
        assertThat(uploadResponse.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);

        // Step 2: Verify file appears in file list
        FileInfo file1 = new FileInfo();
        file1.setName("test-simulation.jar");
        List<FileInfo> mockFiles = Arrays.asList(file1);
        when(fileScanner.getAllFiles()).thenReturn(mockFiles);

        HttpEntity<String> listEntity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> listResponse = restTemplate.exchange(
                baseUrl + "/api/files", HttpMethod.GET, listEntity, String.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<FileInfo> responseFiles = objectMapper.readValue(
                listResponse.getBody(), objectMapper.getTypeFactory().constructCollectionType(List.class, FileInfo.class));
        assertThat(responseFiles).hasSize(1);
        assertThat(responseFiles.get(0).getName()).isEqualTo("test-simulation.jar");
    }

    @Test
    void testCompleteTestExecutionWorkflowViaApi() throws Exception {
        // Step 1: Run a test via API
        TestParameters testParams = new TestParameters();
        testParams.setUsers(10);
        testParams.setDuration(5);

        String testId = "integration-test-1";
        TestExecution runningExecution = new TestExecution("Integration test execution", "test-simulation.jar", testParams);
        runningExecution.setId(testId);
        runningExecution.setStatus(TestStatus.RUNNING);
        when(gatlingService.runTest(anyString(), anyString(), anyString(), any(TestParameters.class)))
                .thenReturn(runningExecution);

        HttpEntity<TestParameters> runEntity = new HttpEntity<>(testParams, authHeaders);
        ResponseEntity<String> runResponse = restTemplate.exchange(
                baseUrl + "/api/tests/run", HttpMethod.POST, runEntity, String.class);

        assertThat(runResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestExecution responseExecution = objectMapper.readValue(runResponse.getBody(), TestExecution.class);
        assertThat(responseExecution.getId()).isEqualTo(testId);
        assertThat(responseExecution.getStatus()).isEqualTo(TestStatus.RUNNING);

        // Step 2: Check test status
        when(gatlingService.getTestExecution(testId)).thenReturn(runningExecution);

        HttpEntity<String> statusEntity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> statusResponse = restTemplate.exchange(
                baseUrl + "/api/tests/" + testId, HttpMethod.GET, statusEntity, String.class);

        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestExecution statusExecution = objectMapper.readValue(statusResponse.getBody(), TestExecution.class);
        assertThat(statusExecution.getStatus()).isEqualTo(TestStatus.RUNNING);

        // Step 3: Simulate test completion
        TestExecution completedExecution = new TestExecution("Integration test execution", "test-simulation.jar", testParams);
        completedExecution.setId(testId);
        completedExecution.setStatus(TestStatus.COMPLETED);
        completedExecution.setEndTime(LocalDateTime.now());
        when(gatlingService.getTestExecution(testId)).thenReturn(completedExecution);

        ResponseEntity<String> completedResponse = restTemplate.exchange(
                baseUrl + "/api/tests/" + testId, HttpMethod.GET, statusEntity, String.class);

        assertThat(completedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestExecution finalExecution = objectMapper.readValue(completedResponse.getBody(), TestExecution.class);
        assertThat(finalExecution.getStatus()).isEqualTo(TestStatus.COMPLETED);
        assertThat(finalExecution.getEndTime()).isNotNull();

        // Step 4: Verify test appears in execution list
        when(gatlingService.getAllTestExecutions()).thenReturn(Arrays.asList(completedExecution));

        ResponseEntity<String> listResponse = restTemplate.exchange(
                baseUrl + "/api/tests", HttpMethod.GET, statusEntity, String.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<TestExecution> executions = objectMapper.readValue(
                listResponse.getBody(), objectMapper.getTypeFactory().constructCollectionType(List.class, TestExecution.class));
        assertThat(executions).hasSize(1);
        assertThat(executions.get(0).getId()).isEqualTo(testId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testCompleteWebInterfaceWorkflow() throws Exception {
        // Step 1: Access dashboard and verify files are displayed
        FileInfo file1 = new FileInfo();
        file1.setName("test1.jar");
        FileInfo file2 = new FileInfo();
        file2.setName("test2.jar");
        List<FileInfo> mockFiles = Arrays.asList(file1, file2);
        when(fileScanner.getAllFiles()).thenReturn(mockFiles);
        when(gatlingService.getAllTestExecutions()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("files", mockFiles));

        // Step 2: Navigate to run-test page
        mockMvc.perform(get("/run-test"))
                .andExpect(status().isOk())
                .andExpect(view().name("run-test"))
                .andExpect(model().attribute("files", mockFiles));

        // Step 3: Submit test execution
        String testId = "web-test-1";
        TestExecution mockExecution = new TestExecution("Web interface test", "test1.jar", new TestParameters());
        mockExecution.setId(testId);
        mockExecution.setStatus(TestStatus.RUNNING);
        when(gatlingService.runTest(eq("test1.jar"), anyString(), eq("Web interface test"), any(TestParameters.class))).thenReturn(mockExecution);

        mockMvc.perform(post("/run-test")
                .with(csrf())
                .param("fileName", "test1.jar")
                .param("users", "10")
                .param("duration", "5")
                .param("description", "Web interface test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/results/" + testId));

        // Step 4: View test results
        when(gatlingService.getTestExecution(testId)).thenReturn(mockExecution);

        mockMvc.perform(get("/results/" + testId))
                .andExpect(status().isOk())
                .andExpect(view().name("result-details"))
                .andExpect(model().attribute("execution", mockExecution));

        // Step 5: View all results
        when(gatlingService.getAllTestExecutions()).thenReturn(Arrays.asList(mockExecution));

        mockMvc.perform(get("/results"))
                .andExpect(status().isOk())
                .andExpect(view().name("results"))
                .andExpect(model().attribute("executions", Arrays.asList(mockExecution)));
    }

    @Test
    void testLogStreamingWorkflow() throws Exception {
        String testId = "streaming-test-1";
        TestExecution mockExecution = new TestExecution("Streaming test", "test.jar", new TestParameters());
        mockExecution.setId(testId);
        mockExecution.setStatus(TestStatus.RUNNING);
        when(gatlingService.getTestExecution(testId)).thenReturn(mockExecution);

        // Test API log streaming
        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> apiResponse = restTemplate.exchange(
                baseUrl + "/api/logs/" + testId, HttpMethod.GET, entity, String.class);

        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(apiResponse.getHeaders().getContentType().toString()).contains("text/event-stream");

        // Test web log streaming (should also work with session auth)
        // Note: This would require a more complex setup with actual session authentication
        // For now, we'll test that the endpoint exists and responds correctly to basic auth
        ResponseEntity<String> webResponse = restTemplate.exchange(
                baseUrl + "/logs/" + testId, HttpMethod.GET, entity, String.class);

        assertThat(webResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(webResponse.getHeaders().getContentType().toString()).contains("text/event-stream");
    }

    @Test
    void testTestStopWorkflow() throws Exception {
        String testId = "stop-test-1";
        
        // Step 1: Start a test
        TestParameters testParams = new TestParameters();
        testParams.setUsers(10);
        testParams.setDuration(60); // Long duration so we can stop it
        
        TestExecution runningExecution = new TestExecution("Stop test", "test.jar", testParams);
        runningExecution.setId(testId);
        runningExecution.setStatus(TestStatus.RUNNING);
        when(gatlingService.runTest(anyString(), anyString(), anyString(), any(TestParameters.class)))
                .thenReturn(runningExecution);

        HttpEntity<TestParameters> runEntity = new HttpEntity<>(testParams, authHeaders);
        ResponseEntity<String> runResponse = restTemplate.exchange(
                baseUrl + "/api/tests/run", HttpMethod.POST, runEntity, String.class);

        assertThat(runResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 2: Stop the test
        TestExecution stoppedExecution = new TestExecution("Stop test", "test.jar", testParams);
        stoppedExecution.setId(testId);
        stoppedExecution.setStatus(TestStatus.STOPPED);
        stoppedExecution.setEndTime(LocalDateTime.now());
        when(gatlingService.stopTest(testId)).thenReturn(true);

        HttpEntity<String> stopEntity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> stopResponse = restTemplate.exchange(
                baseUrl + "/api/tests/" + testId + "/stop", HttpMethod.POST, stopEntity, String.class);

        assertThat(stopResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TestExecution responseExecution = objectMapper.readValue(stopResponse.getBody(), TestExecution.class);
        assertThat(responseExecution.getStatus()).isEqualTo(TestStatus.STOPPED);
        assertThat(responseExecution.getEndTime()).isNotNull();
    }

    @Test
    void testErrorHandlingWorkflow() throws Exception {
        // Test 1: Run test with non-existent file
        TestParameters invalidParams = new TestParameters();
        invalidParams.setUsers(10);
        invalidParams.setDuration(5);

        when(gatlingService.runTest(anyString(), anyString(), anyString(), any(TestParameters.class)))
                .thenThrow(new IllegalArgumentException("File not found"));

        HttpEntity<TestParameters> entity = new HttpEntity<>(invalidParams, authHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/tests/run", HttpMethod.POST, entity, String.class);

        // Should handle the error gracefully
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR);

        // Test 2: Get non-existent test execution
        when(gatlingService.getTestExecution("non-existent")).thenReturn(null);

        HttpEntity<String> getEntity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> getResponse = restTemplate.exchange(
                baseUrl + "/api/tests/non-existent", HttpMethod.GET, getEntity, String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Test 3: Stop non-existent test
        when(gatlingService.stopTest("non-existent")).thenReturn(null);

        ResponseEntity<String> stopResponse = restTemplate.exchange(
                baseUrl + "/api/tests/non-existent/stop", HttpMethod.POST, getEntity, String.class);

        assertThat(stopResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testConcurrentTestExecutions() throws Exception {
        // Simulate multiple concurrent test executions
        TestParameters params1 = new TestParameters();
        params1.setUsers(5);
        params1.setDuration(3);

        TestParameters params2 = new TestParameters();
        params2.setUsers(10);
        params2.setDuration(5);

        TestExecution execution1 = new TestExecution("Concurrent test 1", "test1.jar", params1);
        execution1.setId("concurrent-1");
        execution1.setStatus(TestStatus.RUNNING);
        
        TestExecution execution2 = new TestExecution("Concurrent test 2", "test2.jar", params2);
        execution2.setId("concurrent-2");
        execution2.setStatus(TestStatus.RUNNING);

        when(gatlingService.runTest(eq("test1.jar"), anyString(), isNull(), any(TestParameters.class))).thenReturn(execution1);
        when(gatlingService.runTest(eq("test2.jar"), anyString(), isNull(), any(TestParameters.class))).thenReturn(execution2);

        // Start both tests
        HttpEntity<TestParameters> entity1 = new HttpEntity<>(params1, authHeaders);
        HttpEntity<TestParameters> entity2 = new HttpEntity<>(params2, authHeaders);

        CompletableFuture<ResponseEntity<String>> future1 = CompletableFuture.supplyAsync(() ->
                restTemplate.exchange(baseUrl + "/api/tests/run", HttpMethod.POST, entity1, String.class));

        CompletableFuture<ResponseEntity<String>> future2 = CompletableFuture.supplyAsync(() ->
                restTemplate.exchange(baseUrl + "/api/tests/run", HttpMethod.POST, entity2, String.class));

        // Wait for both to complete
        ResponseEntity<String> response1 = future1.get();
        ResponseEntity<String> response2 = future2.get();

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify both executions are tracked
        when(gatlingService.getAllTestExecutions()).thenReturn(Arrays.asList(execution1, execution2));

        HttpEntity<String> listEntity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> listResponse = restTemplate.exchange(
                baseUrl + "/api/tests", HttpMethod.GET, listEntity, String.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<TestExecution> executions = objectMapper.readValue(
                listResponse.getBody(), objectMapper.getTypeFactory().constructCollectionType(List.class, TestExecution.class));
        assertThat(executions).hasSize(2);
    }

    @Test
    void testFileManagementWorkflow() throws Exception {
        // Step 1: Initially no files
        when(fileScanner.getAllFiles()).thenReturn(Arrays.asList());

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> emptyResponse = restTemplate.exchange(
                baseUrl + "/api/files", HttpMethod.GET, entity, String.class);

        assertThat(emptyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<FileInfo> emptyFiles = objectMapper.readValue(
                emptyResponse.getBody(), objectMapper.getTypeFactory().constructCollectionType(List.class, FileInfo.class));
        assertThat(emptyFiles).isEmpty();

        // Step 2: Files appear after upload/scanning
        FileInfo uploadedFile = new FileInfo();
        uploadedFile.setName("uploaded.jar");
        List<FileInfo> newFiles = Arrays.asList(uploadedFile);
        when(fileScanner.getAllFiles()).thenReturn(newFiles);

        ResponseEntity<String> updatedResponse = restTemplate.exchange(
                baseUrl + "/api/files", HttpMethod.GET, entity, String.class);

        assertThat(updatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<FileInfo> updatedFiles = objectMapper.readValue(
                updatedResponse.getBody(), objectMapper.getTypeFactory().constructCollectionType(List.class, FileInfo.class));
        assertThat(updatedFiles).hasSize(1);
        assertThat(updatedFiles.get(0).getName()).isEqualTo("uploaded.jar");
    }
}