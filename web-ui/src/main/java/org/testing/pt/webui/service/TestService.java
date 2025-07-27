package org.testing.pt.webui.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.testing.pt.webui.model.TestExecution;
import org.testing.pt.webui.model.TestParameters;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Service for communicating with the server module's REST API.
 */
@Service
public class TestService {
    
    private static final Logger logger = LoggerFactory.getLogger(TestService.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${server.api.url}")
    private String serverApiUrl;
    
    public TestService() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Gets all test executions.
     * 
     * @return a list of all test executions
     */
    public List<TestExecution> getAllTests() {
        try {
            String url = serverApiUrl + "/tests";
            ResponseEntity<List<TestExecution>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<TestExecution>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error getting all tests", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Gets a test execution by ID.
     * 
     * @param id the test execution ID
     * @return the test execution, or null if not found
     */
    public TestExecution getTest(String id) {
        try {
            String url = serverApiUrl + "/tests/" + id;
            return restTemplate.getForObject(url, TestExecution.class);
        } catch (Exception e) {
            logger.error("Error getting test: {}", id, e);
            return null;
        }
    }
    
    /**
     * Uploads a test JAR file and runs the test.
     * 
     * @param file the JAR file to upload
     * @param testClass the test class to run (optional)
     * @param description the test description
     * @param parameters the test parameters
     * @return the test execution
     * @throws IOException if an I/O error occurs
     */
    public TestExecution uploadAndRunTest(MultipartFile file, String testClass, String description, TestParameters parameters) throws IOException {
        try {
            String url = serverApiUrl + "/tests/upload";
            
            // Create the request body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("testJar", fileResource);
            
            if (testClass != null && !testClass.isEmpty()) {
                body.add("testClass", testClass);
            }
            
            if (description != null && !description.isEmpty()) {
                body.add("description", description);
            }
            
            body.add("users", parameters.getUsers());
            body.add("rampUp", parameters.getRampUp());
            body.add("duration", parameters.getDuration());
            
            // Set the headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Create the request
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Send the request
            ResponseEntity<TestExecution> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    TestExecution.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error uploading and running test", e);
            throw new IOException("Error uploading and running test: " + e.getMessage(), e);
        }
    }
    
    /**
     * Stops a running test.
     * 
     * @param id the test execution ID
     * @return true if the test was stopped, false otherwise
     */
    public boolean stopTest(String id) {
        try {
            String url = serverApiUrl + "/tests/" + id + "/stop";
            
            // Set the headers for basic authentication
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth("admin", "password"); // Use the same credentials as in application.properties
            
            // Create the request
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // Send the request
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Error stopping test: {}", id, e);
            return false;
        }
    }
    
    /**
     * Deletes a test execution.
     * 
     * @param id the test execution ID
     * @return true if the test execution was deleted, false otherwise
     */
    public boolean deleteTest(String id) {
        try {
            String url = serverApiUrl + "/tests/" + id;
            
            // Set the headers for basic authentication
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth("admin", "password"); // Use the same credentials as in application.properties
            
            // Create the request
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            
            // Send the request
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    Void.class
            );
            
            return true;
        } catch (Exception e) {
            logger.error("Error deleting test: {}", id, e);
            return false;
        }
    }
    
    /**
     * Gets a stream of logs for a test execution.
     * 
     * @param id the test execution ID
     * @return the URL for the log stream
     */
    public String getLogStreamUrl(String id) {
        return serverApiUrl + "/logs/" + id;
    }
}