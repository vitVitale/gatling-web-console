package org.testing.pt.webui.model;

/**
 * Represents the parameters for a Gatling test execution.
 */
public class TestParameters {
    
    private int users;
    private int rampUp;
    private int duration;
    
    public TestParameters() {
        // Default values
        this.users = 10;
        this.rampUp = 30;
        this.duration = 60;
    }
    
    public TestParameters(int users, int rampUp, int duration) {
        this.users = users;
        this.rampUp = rampUp;
        this.duration = duration;
    }
    
    // Getters and setters
    
    public int getUsers() {
        return users;
    }
    
    public void setUsers(int users) {
        this.users = users;
    }
    
    public int getRampUp() {
        return rampUp;
    }
    
    public void setRampUp(int rampUp) {
        this.rampUp = rampUp;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
}