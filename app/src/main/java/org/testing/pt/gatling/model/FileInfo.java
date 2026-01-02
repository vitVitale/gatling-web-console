package org.testing.pt.gatling.model;

import java.nio.file.Path;

/**
 * Represents information about a file in the simulations directory.
 */
public class FileInfo {
    
    private String name;
    
    public FileInfo() {
        // Default constructor for JSON serialization
    }
    
    public FileInfo(Path path) {
        this.name = path.getFileName().toString();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}