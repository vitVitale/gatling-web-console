package org.testing.pt.webui.model;

import java.time.LocalDateTime;

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
    
    public TestExecution() {
        // Default constructor for JSON deserialization
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
     * Gets the duration as a formatted string.
     * 
     * @return the duration as a string (e.g., "60s")
     */
    public String getDuration() {
        if (endTime == null) {
            return "Running";
        }
        long seconds = java.time.Duration.between(startTime, endTime).getSeconds();
        return seconds + "s";
    }
}