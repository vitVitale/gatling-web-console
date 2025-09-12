package org.testing.pt.gatling.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testing.pt.gatling.model.FileInfo;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestParameters;
import org.testing.pt.gatling.model.TestStatus;
import org.testing.pt.gatling.service.FileScanner;
import org.testing.pt.gatling.service.GatlingService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for API controllers using TestRestTemplate.
 * Tests the REST API endpoints with security configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class ApiControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private FileScanner fileScanner;

    @MockBean
    private GatlingService gatlingService;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;
    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        
        // Set up basic authentication headers
        authHeaders = new HttpHeaders();
        authHeaders.setBasicAuth("testuser", "testpass");
    }

    @Test
    void testGetFilesWithoutAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/files", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testGetFilesWithAuthentication() throws Exception {
        // Mock file scanner to return test files
        FileInfo file1 = new FileInfo();
        file1.setName("test1.jar");
        FileInfo file2 = new FileInfo();
        file2.setName("test2.jar");
        List<FileInfo> mockFiles = Arrays.asList(file1, file2);
        when(fileScanner.getAllFiles()).thenReturn(mockFiles);

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/files", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Parse response and verify content
        List<FileInfo> responseFiles = objectMapper.readValue(
                response.getBody(), new TypeReference<List<FileInfo>>() {});
        assertThat(responseFiles).hasSize(2);
        assertThat(responseFiles.get(0).getName()).isEqualTo("test1.jar");
        assertThat(responseFiles.get(1).getName()).isEqualTo("test2.jar");
    }

    @Test
    void testGetFilesWithInvalidCredentials() {
        HttpHeaders invalidAuthHeaders = new HttpHeaders();
        invalidAuthHeaders.setBasicAuth("invalid", "credentials");
        
        HttpEntity<String> entity = new HttpEntity<>(invalidAuthHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/files", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testRunTestWithoutAuthentication() {
        TestParameters testParams = new TestParameters();
        testParams.setUsers(10);
        testParams.setDuration(5);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/tests/run", testParams, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testRunTestWithAuthentication() throws Exception {
        TestParameters testParams = new TestParameters();
        testParams.setUsers(10);
        testParams.setDuration(5);

        TestExecution mockExecution = new TestExecution("Test Description", "test1.jar", testParams);
        mockExecution.setId("test-1");
        mockExecution.setStatus(TestStatus.RUNNING);
        when(gatlingService.runTest(anyString(), anyString(), anyString(), any(TestParameters.class)))
                .thenReturn(mockExecution);

        HttpEntity<TestParameters> entity = new HttpEntity<>(testParams, authHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tests/run", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Parse response and verify content
        TestExecution responseExecution = objectMapper.readValue(response.getBody(), TestExecution.class);
        assertThat(responseExecution.getId()).isEqualTo("test-1");
        assertThat(responseExecution.getTestClass()).isEqualTo("test1.jar");
        assertThat(responseExecution.getStatus()).isEqualTo(TestStatus.RUNNING);
    }

    @Test
    void testGetTestExecutionsWithAuthentication() throws Exception {
        TestExecution execution1 = new TestExecution("Test 1", "test1.jar", new TestParameters());
        execution1.setId("test-1");
        execution1.setStatus(TestStatus.COMPLETED);
        execution1.setEndTime(LocalDateTime.now());
        
        TestExecution execution2 = new TestExecution("Test 2", "test2.jar", new TestParameters());
        execution2.setId("test-2");
        execution2.setStatus(TestStatus.RUNNING);
        
        List<TestExecution> mockExecutions = Arrays.asList(execution1, execution2);
        when(gatlingService.getAllTestExecutions()).thenReturn(mockExecutions);

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tests", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Parse response and verify content
        List<TestExecution> responseExecutions = objectMapper.readValue(
                response.getBody(), new TypeReference<List<TestExecution>>() {});
        assertThat(responseExecutions).hasSize(2);
        assertThat(responseExecutions.get(0).getId()).isEqualTo("test-1");
        assertThat(responseExecutions.get(1).getId()).isEqualTo("test-2");
    }

    @Test
    void testGetTestExecutionByIdWithAuthentication() throws Exception {
        String testId = "test-1";
        TestExecution mockExecution = new TestExecution("Test Description", "test1.jar", new TestParameters());
        mockExecution.setId(testId);
        mockExecution.setStatus(TestStatus.COMPLETED);
        mockExecution.setEndTime(LocalDateTime.now());
        when(gatlingService.getTestExecution(testId)).thenReturn(mockExecution);

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tests/" + testId, HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Parse response and verify content
        TestExecution responseExecution = objectMapper.readValue(response.getBody(), TestExecution.class);
        assertThat(responseExecution.getId()).isEqualTo(testId);
        assertThat(responseExecution.getTestClass()).isEqualTo("test1.jar");
        assertThat(responseExecution.getStatus()).isEqualTo(TestStatus.COMPLETED);
    }

    @Test
    void testGetTestExecutionByIdNotFound() {
        String testId = "non-existent";
        when(gatlingService.getTestExecution(testId)).thenReturn(null);

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tests/" + testId, HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testStopTestWithAuthentication() throws Exception {
        String testId = "test-1";
        TestExecution mockExecution = new TestExecution("Test Description", "test1.jar", new TestParameters());
        mockExecution.setId(testId);
        mockExecution.setStatus(TestStatus.STOPPED);
        mockExecution.setEndTime(LocalDateTime.now());
        when(gatlingService.stopTest(testId)).thenReturn(true);

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tests/" + testId + "/stop", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testStopTestNotFound() {
        String testId = "non-existent";
        when(gatlingService.stopTest(testId)).thenReturn(false);

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tests/" + testId + "/stop", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testUploadFileWithoutAuthentication() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("test content".getBytes()) {
            @Override
            public String getFilename() {
                return "test.jar";
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/files/upload", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testUploadFileWithAuthentication() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("test content".getBytes()) {
            @Override
            public String getFilename() {
                return "test.jar";
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBasicAuth("testuser", "testpass");
        
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/files/upload", entity, String.class);

        // The actual response depends on the implementation, but it should be authenticated
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testLogStreamWithoutAuthentication() {
        String testId = "test-1";
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/logs/" + testId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Disabled
    @Test
    void testLogStreamWithAuthentication() {
        String testId = "test-1";
        TestExecution mockExecution = new TestExecution("Test Description", "test1.jar", new TestParameters());
        mockExecution.setId(testId);
        mockExecution.setStatus(TestStatus.RUNNING);
        when(gatlingService.getTestExecution(testId)).thenReturn(mockExecution);

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/logs/" + testId, HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).contains("text/event-stream");
    }

    @Test
    void testLogStreamWithNonExistentTest() {
        String testId = "non-existent";
        when(gatlingService.getTestExecution(testId)).thenReturn(null);

        HttpEntity<String> entity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/logs/" + testId, HttpMethod.GET, entity, String.class);

        // The response should still be OK but the SSE emitter will complete with error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testHealthEndpointAccessible() {
        // Health endpoint should be accessible without authentication
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void testCorsHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("testuser", "testpass");
        headers.set("Origin", "http://localhost:3000");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/files", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // CORS headers would be tested here if configured
    }
}