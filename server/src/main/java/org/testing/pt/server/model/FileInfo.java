package org.testing.pt.server.model;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Represents information about a file in the simulations directory.
 */
public class FileInfo {
    
    private String name;
//    private String path;
//    private long size;
//    private Instant lastModified;
    
    public FileInfo() {
        // Default constructor for JSON serialization
    }
    
    public FileInfo(Path path) {
        this.name = path.getFileName().toString();
//        this.path = path.toString();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
//    public String getPath() {
//        return path;
//    }
//
//    public void setPath(String path) {
//        this.path = path;
//    }
//
//    public long getSize() {
//        return size;
//    }
//
//    public void setSize(long size) {
//        this.size = size;
//    }
//
//    public Instant getLastModified() {
//        return lastModified;
//    }
//
//    public void setLastModified(Instant lastModified) {
//        this.lastModified = lastModified;
//    }
}