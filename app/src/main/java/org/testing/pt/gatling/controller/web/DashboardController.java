package org.testing.pt.gatling.controller.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestParameters;
import org.testing.pt.gatling.service.GatlingService;
import org.testing.pt.gatling.repository.TestExecutionRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Controller for handling dashboard-related requests.
 */
@Controller
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private GatlingService gatlingService;
    
    @Autowired
    private TestExecutionRepository testExecutionRepository;
    
    @Value("${app.history.retention-days:30}")
    private int retentionDays;

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
        model.addAttribute("requestURI", "/dashboard");
        
        // Calculate statistics
        long totalTests = testExecutionRepository.count();
        long successfulTests = testExecutionRepository.countByStatus(org.testing.pt.gatling.model.TestStatus.COMPLETED);
        long failedTests = testExecutionRepository.countByStatus(org.testing.pt.gatling.model.TestStatus.FAILED);
        long runningTests = testExecutionRepository.countByStatus(org.testing.pt.gatling.model.TestStatus.RUNNING);
        
        model.addAttribute("totalTests", totalTests);
        model.addAttribute("successfulTests", successfulTests);
        model.addAttribute("failedTests", failedTests);
        model.addAttribute("runningTests", runningTests);

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

        // Get available JAR files from simulations directory
        model.addAttribute("jarFiles", gatlingService.getAvailableJarFiles());

        // Get recent tests for the sidebar
        List<TestExecution> recentTests = gatlingService.getAllTestExecutions();
        model.addAttribute("recentTests", recentTests);

        return "run-test";
    }

    /**
     * Handles the form submission for running a test.
     *
     * @param jarFile the selected JAR file name
     * @param simulationClass the simulation class to run
     * @param engineClass the engine class to use
     * @param description the test description (optional)
     * @param paramKeys array of parameter keys
     * @param paramValues array of parameter values
     * @param redirectAttributes attributes for the redirect
     * @return a redirect to the results page
     */
    @PostMapping("/run-test")
    public String handleRunTest(
            @RequestParam("jarFile") String jarFile,
            @RequestParam("simulationClass") String simulationClass,
            @RequestParam("engineClass") String engineClass,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "paramKeys[]", required = false) String[] paramKeys,
            @RequestParam(value = "paramValues[]", required = false) String[] paramValues,
            RedirectAttributes redirectAttributes) {

        try {
            // Build parameters map
            TestParameters parameters = new TestParameters();
            if (paramKeys != null && paramValues != null && paramKeys.length == paramValues.length) {
                for (int i = 0; i < paramKeys.length; i++) {
                    if (paramKeys[i] != null && !paramKeys[i].trim().isEmpty()) {
                        parameters.addParameter(paramKeys[i].trim(), paramValues[i]);
                    }
                }
            }

            // Run the test
            TestExecution execution = gatlingService.runTest(
                jarFile,
                simulationClass,
                engineClass,
                description,
                parameters
            );

            redirectAttributes.addFlashAttribute("successMessage",
                "Test started successfully! Execution ID: " + execution.getId());
            return "redirect:/results/" + execution.getId();
        } catch (IllegalArgumentException e) {
            logger.error("Error running test", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
            return "redirect:/run-test";
        } catch (Exception e) {
            logger.error("Unexpected error running test", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unexpected error: " + e.getMessage());
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
    
    /**
     * Downloads the test report as a ZIP file.
     *
     * @param id the test execution ID
     * @return the ZIP file containing the test report
     */
    @GetMapping("/results/{id}/download")
    public ResponseEntity<Resource> downloadReport(@PathVariable String id) {
        TestExecution test = gatlingService.getTestExecution(id);
        
        if (test == null) {
            logger.error("Test execution not found: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        if (test.getResultPath() == null || test.getResultPath().isEmpty()) {
            logger.error("Test has no result path: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        try {
            Path resultPath = Paths.get(test.getResultPath());
            if (!Files.exists(resultPath) || !Files.isDirectory(resultPath)) {
                logger.error("Result directory not found: {}", resultPath);
                return ResponseEntity.notFound().build();
            }
            
            // Create a temporary ZIP file
            File tempZip = Files.createTempFile("gatling-report-" + id, ".zip").toFile();
            tempZip.deleteOnExit();
            
            // Zip the result directory
            zipDirectory(resultPath.toFile(), tempZip);
            
            Resource resource = new FileSystemResource(tempZip);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"gatling-report-" + id + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(tempZip.length())
                    .body(resource);
                    
        } catch (IOException e) {
            logger.error("Error creating ZIP file for test: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Zips a directory into a ZIP file.
     *
     * @param sourceDir the source directory to zip
     * @param zipFile the target ZIP file
     * @throws IOException if an I/O error occurs
     */
    private void zipDirectory(File sourceDir, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            zipDirectoryRecursive(sourceDir, sourceDir.getName(), zos);
        }
    }
    
    /**
     * Recursively zips a directory.
     *
     * @param fileToZip the file or directory to zip
     * @param fileName the name to use in the ZIP file
     * @param zos the ZipOutputStream
     * @throws IOException if an I/O error occurs
     */
    private void zipDirectoryRecursive(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zos.putNextEntry(new ZipEntry(fileName));
            } else {
                zos.putNextEntry(new ZipEntry(fileName + "/"));
            }
            zos.closeEntry();
            
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipDirectoryRecursive(childFile, fileName + "/" + childFile.getName(), zos);
                }
            }
        } else {
            try (FileInputStream fis = new FileInputStream(fileToZip)) {
                ZipEntry zipEntry = new ZipEntry(fileName);
                zos.putNextEntry(zipEntry);
                
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) >= 0) {
                    zos.write(buffer, 0, length);
                }
                
                zos.closeEntry();
            }
        }
    }
    
    /**
     * Clears old test history based on retention policy.
     *
     * @param redirectAttributes attributes for the redirect
     * @return redirect to results page
     */
    @PostMapping("/results/clear-history")
    public String clearHistory(RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            List<TestExecution> oldExecutions = testExecutionRepository.findOldExecutions(cutoffTime);
            
            int deletedCount = 0;
            for (TestExecution execution : oldExecutions) {
                boolean deleted = gatlingService.deleteTestExecution(execution.getId());
                if (deleted) {
                    deletedCount++;
                }
            }
            
            logger.info("Cleared {} old test executions (retention: {} days)", deletedCount, retentionDays);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Cleared " + deletedCount + " test(s) older than " + retentionDays + " days");
        } catch (Exception e) {
            logger.error("Error clearing history", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to clear history: " + e.getMessage());
        }
        
        return "redirect:/results";
    }
    
    /**
     * Exports test results to CSV format.
     *
     * @return CSV file with test results
     */
    @GetMapping("/results/export")
    public ResponseEntity<Resource> exportResults() {
        try {
            List<TestExecution> tests = gatlingService.getAllTestExecutions();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
            
            // CSV Header
            writer.write("ID,Description,Test Class,Start Time,End Time,Duration (seconds),Status\n");
            
            // CSV Data
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (TestExecution test : tests) {
                writer.write(escapeCsv(test.getId()));
                writer.write(",");
                writer.write(escapeCsv(test.getDescription()));
                writer.write(",");
                writer.write(escapeCsv(test.getTestClass()));
                writer.write(",");
                writer.write(test.getStartTime() != null ? formatter.format(test.getStartTime()) : "");
                writer.write(",");
                writer.write(test.getEndTime() != null ? formatter.format(test.getEndTime()) : "");
                writer.write(",");
                writer.write(String.valueOf(test.getDurationInSeconds()));
                writer.write(",");
                writer.write(test.getStatus() != null ? test.getStatus().name() : "");
                writer.write("\n");
            }
            
            writer.flush();
            writer.close();
            
            byte[] csvBytes = baos.toByteArray();
            Resource resource = new org.springframework.core.io.ByteArrayResource(csvBytes);
            
            String filename = "gatling-test-results-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(csvBytes.length)
                    .body(resource);
                    
        } catch (IOException e) {
            logger.error("Error exporting results to CSV", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Escapes a CSV field value.
     *
     * @param value the value to escape
     * @return the escaped value
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}