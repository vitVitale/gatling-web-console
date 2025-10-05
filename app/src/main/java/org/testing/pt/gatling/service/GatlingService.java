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
import org.testing.pt.gatling.repository.TestExecutionRepository;

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

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    // Keep running processes in memory (not persisted)
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();
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
     * Gets available JAR files from the simulations directory.
     *
     * @return list of available JAR files
     */
    public List<FileInfo> getAvailableJarFiles() {
        return fileScanner.getAllFiles().stream()
                .filter(file -> file.getName().endsWith(".jar"))
                .toList();
    }
    
    /**
     * Runs a Gatling test.
     *
     * @param jarFileName the name of the JAR file
     * @param simulationClass the simulation class to run
     * @param engineClass the engine class to use
     * @param description the test description
     * @param parameters the test parameters
     * @return the test execution
     * @throws IllegalArgumentException if the test JAR file is not in the designated folder
     */
    public TestExecution runTest(String jarFileName, String simulationClass, String engineClass,
                                  String description, TestParameters parameters) {
        // Validate that the JAR file is in the designated folder
        var isPresent = fileScanner.getAllFiles().stream()
                .map(FileInfo::getName)
                .anyMatch(name -> name.equals(jarFileName));

        if (!isPresent) {
            throw new IllegalArgumentException("Test JAR could not be found: " + jarFileName);
        }

        TestExecution execution = new TestExecution(description, simulationClass, parameters);
        execution = testExecutionRepository.save(execution);

        final String executionId = execution.getId();

        executorService.submit(() -> {
            TestExecution exec = testExecutionRepository.findById(executionId).orElseThrow();
            try {
                exec.setStatus(TestStatus.RUNNING);
                testExecutionRepository.save(exec);
                log.info("########  Gatling test execution started: {}  ######", exec.getId());

                // Create a directory for the test results
                Path testResultsDir = Paths.get(resultsDirectory, exec.getId());
                Files.createDirectories(testResultsDir);

                // Build the command to run the Gatling test
                List<String> command = new ArrayList<>();
                command.add("java");

                // Add custom parameters
                if (parameters != null) {
                    command.addAll(Arrays.asList(parameters.toJavaArgs()));
                }

                // Add Gatling output directory
                command.add("-Dgatling.core.outputDirectoryBaseName=" + exec.getId());

                // Add the Simulation class
                if (Objects.nonNull(simulationClass) && !simulationClass.isEmpty()) {
                    command.add("-Dsimulation=" + simulationClass);
                }

                // Add JAR file
                command.add("-jar");
                command.add(Paths.get(gatlingJarsDirectory, jarFileName).toString());

                // Run the command
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.directory(new File(resultsDirectory));
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();
                runningProcesses.put(exec.getId(), process);

                // Send a log message indicating the test has started
                logStreamService.sendLogMessage(exec.getId(), "Test started: " + exec.getDescription());
                
                // Create a thread to read the process output and send it to the log stream
                Thread outputThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            logStreamService.sendLogMessage(exec.getId(), line);
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
                            logStreamService.sendLogMessage(exec.getId(), "ERROR: " + line);
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
                    exec.setStatus(TestStatus.COMPLETED);
                    logStreamService.sendLogMessage(exec.getId(), "Test completed successfully");
                } else {
                    exec.setStatus(TestStatus.FAILED);
                    logStreamService.sendLogMessage(exec.getId(), "Test failed with exit code: " + exitCode);
                }

                exec.setEndTime(LocalDateTime.now());
                exec.setResultPath(testResultsDir.toString());
                runningProcesses.remove(exec.getId());
                testExecutionRepository.save(exec);

                log.info("Test completed: {}, status: {}", exec.getId(), exec.getStatus());
            } catch (Exception e) {
                log.error("Error running test: {}", exec.getId(), e);
                exec.setStatus(TestStatus.FAILED);
                exec.setEndTime(LocalDateTime.now());
                runningProcesses.remove(exec.getId());
                testExecutionRepository.save(exec);
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
        TestExecution execution = testExecutionRepository.findById(id).orElse(null);
        if (execution != null && runningProcesses.containsKey(id)) {
            execution.setProcess(runningProcesses.get(id));
        }
        return execution;
    }

    /**
     * Gets all test executions.
     *
     * @return a list of all test executions ordered by start time descending
     */
    public List<TestExecution> getAllTestExecutions() {
        List<TestExecution> executions = testExecutionRepository.findAllByOrderByStartTimeDesc();
        // Restore process references for running tests
        for (TestExecution execution : executions) {
            if (runningProcesses.containsKey(execution.getId())) {
                execution.setProcess(runningProcesses.get(execution.getId()));
            }
        }
        return executions;
    }
    
    /**
     * Stops a running test.
     *
     * @param id the test execution ID
     * @return true if the test was stopped, false otherwise
     */
    public boolean stopTest(String id) {
        TestExecution execution = testExecutionRepository.findById(id).orElse(null);
        if (execution != null && execution.getStatus() == TestStatus.RUNNING) {
            Process process = runningProcesses.get(id);
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
                runningProcesses.remove(id);
                testExecutionRepository.save(execution);

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

        TestExecution execution = testExecutionRepository.findById(id).orElse(null);
        if (execution != null) {
            try {
                if (execution.getResultPath() != null) {
                    FileUtils.deleteDirectory(new File(execution.getResultPath()));
                }
                testExecutionRepository.deleteById(id);
                log.info("Test execution deleted: {}", id);
                return true;
            } catch (IOException e) {
                log.error("Error deleting test results: {}", id, e);
            }
        }
        return false;
    }
}