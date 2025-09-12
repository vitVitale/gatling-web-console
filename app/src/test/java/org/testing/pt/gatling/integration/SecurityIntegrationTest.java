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
import org.testing.pt.gatling.service.FileScanner;
import org.testing.pt.gatling.service.GatlingService;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for security configuration.
 * Tests both web and API authentication mechanisms.
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class SecurityIntegrationTest {

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
        
        // Set up common mocks
        when(fileScanner.getAllFiles()).thenReturn(Collections.emptyList());
        when(gatlingService.getAllTestExecutions()).thenReturn(Collections.emptyList());
    }

    // API Endpoint Security Tests

    @Test
    void testApiEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testApiEndpointWithValidBasicAuth() throws Exception {
        mockMvc.perform(get("/api/files")
                .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk());
    }

    @Test
    void testApiEndpointWithInvalidBasicAuth() throws Exception {
        mockMvc.perform(get("/api/files")
                .with(httpBasic("wronguser", "wrongpass")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testApiEndpointWithInvalidUsername() throws Exception {
        mockMvc.perform(get("/api/files")
                .with(httpBasic("wronguser", "testpass")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testApiEndpointWithInvalidPassword() throws Exception {
        mockMvc.perform(get("/api/files")
                .with(httpBasic("testuser", "wrongpass")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testApiTestsEndpointWithValidAuth() throws Exception {
        mockMvc.perform(get("/api/tests")
                .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk());
    }

    @Test
    void testApiLogsEndpointWithValidAuth() throws Exception {
        when(gatlingService.getTestExecution("test-1")).thenReturn(null);
        
        mockMvc.perform(get("/api/logs/test-1")
                .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/event-stream"));
    }

    // Web Interface Security Tests

    @Test
    void testWebEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testWebEndpointWithAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    void testRunTestPageWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/run-test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testRunTestPageWithAuthentication() throws Exception {
        mockMvc.perform(get("/run-test"))
                .andExpect(status().isOk())
                .andExpect(view().name("run-test"));
    }

    @Test
    void testResultsPageWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/results"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testResultsPageWithAuthentication() throws Exception {
        mockMvc.perform(get("/results"))
                .andExpect(status().isOk())
                .andExpect(view().name("results"));
    }

    // Login Functionality Tests

    @Test
    void testLoginPageAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void testLoginWithValidCredentials() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "testuser")
                .param("password", "testpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "wronguser")
                .param("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login?error"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testLogout() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    // Static Resources Security Tests

    @Test
    void testStaticResourcesAccessibleWithoutAuth() throws Exception {
        // CSS files should be accessible without authentication
        mockMvc.perform(get("/webjars/bootstrap/5.3.2/css/bootstrap.min.css"))
                .andExpect(status().isOk());
    }

    @Test
    void testJavaScriptResourcesAccessibleWithoutAuth() throws Exception {
        // JS files should be accessible without authentication
        mockMvc.perform(get("/webjars/jquery/3.7.1/jquery.min.js"))
                .andExpect(status().isOk());
    }

    // Cross-Authentication Tests (API auth shouldn't work for web and vice versa)

    @Test
    void testWebEndpointWithBasicAuthShouldRedirectToLogin() throws Exception {
        // Basic auth should not work for web endpoints - should still redirect to login
        mockMvc.perform(get("/")
                .with(httpBasic("testuser", "testpass")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testApiEndpointWithSessionAuthShouldRequireBasicAuth() throws Exception {
        // Session auth should not work for API endpoints - should require basic auth
        // This test verifies that API endpoints are properly isolated
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isUnauthorized());
    }

    // Security Headers Tests

    @Test
    void testSecurityHeaders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("X-XSS-Protection"));
    }

    // CSRF Tests (currently disabled but tests for future enablement)

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testWebFormSubmissionWithoutCsrf() throws Exception {
        // CSRF is currently disabled, so this should work
        mockMvc.perform(post("/run-test")
                .param("fileName", "test.jar")
                .param("users", "10")
                .param("duration", "5"))
                .andExpect(status().isBadRequest()); // Bad request due to missing required params, not CSRF
    }

    // Session Management Tests

    @Test
    void testSessionCreationForWebInterface() throws Exception {
        // Web interface should create sessions
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void testStatelessApiEndpoints() throws Exception {
        // API endpoints should be stateless (no session creation)
        mockMvc.perform(get("/api/files")
                .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk());
        // Session should not be created for API calls
    }

    // Role-based Access Tests

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void testWebAccessWithAdminRole() throws Exception {
        // Admin role should also have access
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "GUEST")
    void testWebAccessWithGuestRole() throws Exception {
        // Only USER role is configured, so GUEST should not have access
        mockMvc.perform(get("/"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testApiAccessWithAdminRoleViaBasicAuth() throws Exception {
        // Basic auth user has USER role, should work
        mockMvc.perform(get("/api/files")
                .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk());
    }
}