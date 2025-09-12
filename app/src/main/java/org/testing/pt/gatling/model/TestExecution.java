package org.testing.pt.gatling.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a Gatling test execution.
 */
public class TestExecution {
    
    private String id;
    private String description;
    private String testClass;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private TestStatus status;
    private String resultPath;
    private TestParameters parameters;
    
    @JsonIgnore // Don't serialize the process
    private Process process;
    
    public TestExecution() {
        this.id = UUID.randomUUID().toString();
        this.startTime = LocalDateTime.now();
        this.status = TestStatus.PENDING;
    }
    
    public TestExecution(String description, String testClass, TestParameters parameters) {
        this();
        this.description = description;
        this.testClass = testClass;
        this.parameters = parameters;
    }
    
    // Getters and setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getTestClass() {
        return testClass;
    }
    
    public void setTestClass(String testClass) {
        this.testClass = testClass;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public TestStatus getStatus() {
        return status;
    }
    
    public void setStatus(TestStatus status) {
        this.status = status;
    }
    
    public String getResultPath() {
        return resultPath;
    }
    
    public void setResultPath(String resultPath) {
        this.resultPath = resultPath;
    }
    
    public TestParameters getParameters() {
        return parameters;
    }
    
    public void setParameters(TestParameters parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Gets the process object for this test execution.
     * 
     * @return the process object
     */
    public Process getProcess() {
        return process;
    }
    
    /**
     * Sets the process object for this test execution.
     * 
     * @param process the process object
     */
    public void setProcess(Process process) {
        this.process = process;
    }
    
    /**
     * Calculates the duration of the test in seconds.
     * 
     * @return the duration in seconds, or -1 if the test is still running
     */
    public long getDurationInSeconds() {
        if (endTime == null) {
            return -1;
        }
        return java.time.Duration.between(startTime, endTime).getSeconds();
    }
    
    /**
     * Gets the duration as a formatted string.
     * 
     * @return the duration as a string (e.g., "60s")
     */
    public String getDuration() {
        long seconds = getDurationInSeconds();
        if (seconds < 0) {
            return "Running";
        }
        return seconds + "s";
    }
}