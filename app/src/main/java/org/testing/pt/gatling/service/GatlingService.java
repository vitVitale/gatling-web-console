package org.testing.pt.gatling.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.testing.pt.gatling.model.FileInfo;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestParameters;
import org.testing.pt.gatling.model.TestStatus;

import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for running Gatling tests.
 */
@Service
@Slf4j
public class GatlingService {

    @Value("${gatling.tests.dir:./gatling-tests}")
    private String testsDirectory;
    
    @Value("${gatling.results.dir:./gatling-results}")
    private String resultsDirectory;

    @Value("${gatling.jars.dir:/opt/gatling/jars}")
    private String gatlingJarsDirectory;
    
    @Autowired
    private FileScanner fileScanner;

    @Autowired
    private LogStreamService logStreamService;
    
    private final Map<String, TestExecution> testExecutions = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    /**
     * Initializes the service by creating the necessary directories.
     * This method is called automatically when the application starts.
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(testsDirectory));
            Files.createDirectories(Paths.get(resultsDirectory));
            log.info("Gatling directories initialized: tests={}, results={}", testsDirectory, resultsDirectory);
        } catch (IOException e) {
            log.error("Failed to create Gatling directories", e);
        }
    }
    
    /**
     * Uploads a test JAR file.
     * 
     * @param file the JAR file to upload
     * @return the path to the uploaded file
     * @throws IOException if an I/O error occurs
     */
    public String uploadTestJar(MultipartFile file) throws IOException {
        Path testJarPath = Paths.get(testsDirectory, file.getOriginalFilename());
        Files.copy(file.getInputStream(), testJarPath);
        log.info("Test JAR uploaded: {}", testJarPath);
        return testJarPath.toString();
    }
    
    /**
     * Runs a Gatling test.
     * 
     * @param testJarPath the path to the test JAR file
     * @param testClass the test class to run (optional)
     * @param description the test description
     * @param parameters the test parameters
     * @return the test execution
     * @throws IllegalArgumentException if the test JAR file is not in the designated folder
     */
    public TestExecution runTest(String testJarPath, String testClass, String description, TestParameters parameters) {
        // Validate that the JAR file is in the designated folder
        var isPresent = fileScanner.getAllFiles().stream()
                .map(FileInfo::getName)
                .anyMatch(name -> name.equals(testJarPath));

        if (!isPresent) {
            throw new IllegalArgumentException("Test JAR could not be found: " + testJarPath);
        }
        
        TestExecution execution = new TestExecution(description, testClass, parameters);
        testExecutions.put(execution.getId(), execution);
        
        executorService.submit(() -> {
            try {
                execution.setStatus(TestStatus.RUNNING);
                log.info("########  Gatling test execution started: {}  ######", execution.getId());
                
                // Create a directory for the test results
                Path testResultsDir = Paths.get(resultsDirectory, execution.getId());
                Files.createDirectories(testResultsDir);

                // Add test parameters
                String[] args = parameters.toGatlingArgs().split(" ");

                // Build the command to run the Gatling test
                List<String> command = new ArrayList<>();
                command.add("java");
                command.addAll(Arrays.asList(args));
                command.add("-Dgatling.core.outputDirectoryBaseName=" + execution.getId());
                // Add the Simulation class if specified
                if (Objects.nonNull(testClass) && !testClass.isEmpty()) command.add("-Dsimulation=" + testClass);
                command.add("-jar");
                command.add(Paths.get(gatlingJarsDirectory, testJarPath).toString());

                // Run the command
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.directory(new File(resultsDirectory));
                processBuilder.redirectErrorStream(true);
                
                Process process = processBuilder.start();
                execution.setProcess(process);

                // Send a log message indicating the test has started
                logStreamService.sendLogMessage(execution.getId(), "Test started: " + execution.getDescription());
                
                // Create a thread to read the process output and send it to the log stream
                Thread outputThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            logStreamService.sendLogMessage(execution.getId(), line);
                        }
                    } catch (IOException e) {
                        log.error("Error reading process output", e);
                    }
                });
                outputThread.start();

                // Create a thread to read the process error output and send it to the log stream
                Thread errorThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            logStreamService.sendLogMessage(execution.getId(), "ERROR: " + line);
                        }
                    } catch (IOException e) {
                        log.error("Error reading process error output", e);
                    }
                });
                errorThread.start();
                
                int exitCode = process.waitFor();
                
                // Wait for the output threads to finish
                outputThread.join();
                errorThread.join();
                
                // Update the test execution status
                if (exitCode == 0) {
                    execution.setStatus(TestStatus.COMPLETED);
                    logStreamService.sendLogMessage(execution.getId(), "Test completed successfully");
                } else {
                    execution.setStatus(TestStatus.FAILED);
                    logStreamService.sendLogMessage(execution.getId(), "Test failed with exit code: " + exitCode);
                }
                
                execution.setEndTime(LocalDateTime.now());
                execution.setResultPath(testResultsDir.toString());
                execution.setProcess(null); // Clear the process reference

                log.info("Test completed: {}, status: {}", execution.getId(), execution.getStatus());
            } catch (Exception e) {
                log.error("Error running test: {}", execution.getId(), e);
                execution.setStatus(TestStatus.FAILED);
                execution.setEndTime(LocalDateTime.now());
            }
        });
        
        return execution;
    }
    
    /**
     * Gets a test execution by ID.
     * 
     * @param id the test execution ID
     * @return the test execution, or null if not found
     */
    public TestExecution getTestExecution(String id) {
        return testExecutions.get(id);
    }
    
    /**
     * Gets all test executions.
     * 
     * @return a list of all test executions
     */
    public List<TestExecution> getAllTestExecutions() {
        return new ArrayList<>(testExecutions.values());
    }
    
    /**
     * Stops a running test.
     * 
     * @param id the test execution ID
     * @return true if the test was stopped, false otherwise
     */
    public boolean stopTest(String id) {
        TestExecution execution = testExecutions.get(id);
        if (execution != null && execution.getStatus() == TestStatus.RUNNING) {
            Process process = execution.getProcess();
            if (process != null && process.isAlive()) {
                log.info("Stopping test: {}", id);
                process.destroy();
                
                // Wait for the process to terminate
                try {
                    boolean terminated = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                    if (!terminated) {
                        // Force termination if it doesn't terminate gracefully
                        process.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while waiting for process to terminate", e);
                }
                
                execution.setStatus(TestStatus.STOPPED);
                execution.setEndTime(LocalDateTime.now());
                execution.setProcess(null);
                
                // Send a log message indicating the test was stopped
                logStreamService.sendLogMessage(id, "Test was manually stopped by user");

                log.info("Test stopped: {}", id);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Deletes a test execution and its results.
     * 
     * @param id the test execution ID
     * @return true if the test execution was deleted, false otherwise
     */
    public boolean deleteTestExecution(String id) {
        // Stop the test if it's running
        stopTest(id);
        
        TestExecution execution = testExecutions.remove(id);
        if (execution != null) {
            try {
                FileUtils.deleteDirectory(new File(execution.getResultPath()));
                log.info("Test execution deleted: {}", id);
                return true;
            } catch (IOException e) {
                log.error("Error deleting test results: {}", id, e);
            }
        }
        return false;
    }
}