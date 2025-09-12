package org.testing.pt.gatling.controller.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestParameters;
import org.testing.pt.gatling.service.GatlingService;

import java.io.IOException;
import java.util.List;

/**
 * Controller for handling dashboard-related requests.
 */
@Controller
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private GatlingService gatlingService;

    /**
     * Displays the main dashboard page.
     *
     * @param model the model to add attributes to
     * @return the name of the view to render
     */
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Gatling Web Console");
        
        // Get recent tests for the dashboard
        List<TestExecution> recentTests = gatlingService.getAllTestExecutions();
        model.addAttribute("recentTests", recentTests);
        
        return "dashboard";
    }

    /**
     * Displays the page for running tests.
     *
     * @param model the model to add attributes to
     * @return the name of the view to render
     */
    @GetMapping("/run-test")
    public String runTest(Model model) {
        model.addAttribute("pageTitle", "Run Gatling Test");
        
        // Get recent tests for the sidebar
        List<TestExecution> recentTests = gatlingService.getAllTestExecutions();
        model.addAttribute("recentTests", recentTests);
        
        return "run-test";
    }

    /**
     * Handles the form submission for running a test.
     *
     * @param testJar the JAR file to upload
     * @param testClass the test class to run (optional)
     * @param description the test description
     * @param users the number of users
     * @param rampUp the ramp-up period in seconds
     * @param duration the test duration in seconds
     * @param redirectAttributes attributes for the redirect
     * @return a redirect to the results page
     */
    @PostMapping("/run-test")
    public String handleRunTest(
            @RequestParam("testJar") MultipartFile testJar,
            @RequestParam(value = "testClass", required = false) String testClass,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "users", defaultValue = "10") int users,
            @RequestParam(value = "rampUp", defaultValue = "30") int rampUp,
            @RequestParam(value = "duration", defaultValue = "60") int duration,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Upload the test JAR file
            String testJarPath = gatlingService.uploadTestJar(testJar);
            
            // Create test parameters
            TestParameters parameters = new TestParameters(users, rampUp, duration);
            
            // Run the test using the uploaded JAR filename (not full path)
            TestExecution execution = gatlingService.runTest(testJar.getOriginalFilename(), testClass, description, parameters);
            
            redirectAttributes.addFlashAttribute("successMessage", "Test started successfully!");
            return "redirect:/results";
        } catch (IOException e) {
            logger.error("Error running test", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error running test: " + e.getMessage());
            return "redirect:/run-test";
        } catch (IllegalArgumentException e) {
            logger.error("Error running test", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error running test: " + e.getMessage());
            return "redirect:/run-test";
        }
    }

    /**
     * Displays the page for viewing test results.
     *
     * @param model the model to add attributes to
     * @return the name of the view to render
     */
    @GetMapping("/results")
    public String results(Model model) {
        model.addAttribute("pageTitle", "Test Results");
        
        // Get all tests for the results page
        List<TestExecution> tests = gatlingService.getAllTestExecutions();
        model.addAttribute("tests", tests);
        
        // Calculate statistics
        int totalTests = tests.size();
        int successfulTests = (int) tests.stream()
                .filter(test -> test.getStatus() != null && test.getStatus().name().equals("COMPLETED"))
                .count();
        int failedTests = (int) tests.stream()
                .filter(test -> test.getStatus() != null && test.getStatus().name().equals("FAILED"))
                .count();
        int stoppedTests = (int) tests.stream()
                .filter(test -> test.getStatus() != null && test.getStatus().name().equals("STOPPED"))
                .count();
        
        model.addAttribute("totalTests", totalTests);
        model.addAttribute("successfulTests", successfulTests);
        model.addAttribute("failedTests", failedTests);
        model.addAttribute("stoppedTests", stoppedTests);
        
        return "results";
    }
    
    /**
     * Displays the details of a specific test result.
     *
     * @param id the test execution ID
     * @param model the model to add attributes to
     * @return the name of the view to render
     */
    @GetMapping("/results/{id}")
    public String resultDetails(@PathVariable String id, Model model) {
        TestExecution test = gatlingService.getTestExecution(id);
        
        if (test == null) {
            return "redirect:/results";
        }
        
        model.addAttribute("pageTitle", "Test Result Details");
        model.addAttribute("test", test);
        
        return "result-details";
    }
    
    /**
     * Stops a running test.
     *
     * @param id the test execution ID
     * @param redirectAttributes attributes for the redirect
     * @return a redirect to the results page
     */
    @PostMapping("/results/{id}/stop")
    public String stopTest(@PathVariable String id, RedirectAttributes redirectAttributes) {
        boolean stopped = gatlingService.stopTest(id);
        
        if (stopped) {
            redirectAttributes.addFlashAttribute("successMessage", "Test stopped successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to stop test. It may have already completed or failed.");
        }
        
        return "redirect:/results/" + id;
    }
}