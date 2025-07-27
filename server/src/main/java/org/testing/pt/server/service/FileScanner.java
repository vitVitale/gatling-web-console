package org.testing.pt.server.service;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.testing.pt.server.model.FileInfo;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for scanning a directory for files.
 */
@Service
public class FileScanner {

    private static final Logger logger = LoggerFactory.getLogger(FileScanner.class);
    
    @Value("${simulations.folder:/opt/gatling/simulations}")
    private String simulationsFolder;

    @Getter
    private final List<FileInfo> files = new CopyOnWriteArrayList<>();
    
    /**
     * Sets the simulations folder path.
     * This method is primarily used for testing.
     * 
     * @param simulationsFolder the path to the simulations folder
     */
    public void setSimulationsFolder(String simulationsFolder) {
        this.simulationsFolder = simulationsFolder;
    }
    
    /**
     * Initializes the service by scanning the simulations folder.
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing FileScanner with simulations folder: {}", simulationsFolder);
        scanFolder();
    }
    
    /**
     * Scans the simulations folder every 20 seconds.
     */
    @Scheduled(fixedRate = 5000) // 20 seconds
    public void scanFolder() {
        Path folder = Paths.get(simulationsFolder);
        
        // Check if the folder exists
        if (!Files.exists(folder)) {
            logger.warn("Simulations folder does not exist: {}", simulationsFolder);
            try {
                // Create the folder if it doesn't exist
                Files.createDirectories(folder);
                logger.info("Created simulations folder: {}", simulationsFolder);
            } catch (IOException e) {
                logger.error("Failed to create simulations folder: {}", simulationsFolder, e);
                return;
            }
        }
        
        // Check if it's a directory
        if (!Files.isDirectory(folder)) {
            logger.error("Simulations folder is not a directory: {}", simulationsFolder);
            return;
        }
        
        try {
            // Scan the folder for files (not subdirectories)
            List<FileInfo> scannedFiles = new ArrayList<>();
            try (Stream<Path> paths = Files.list(folder)) {
                scannedFiles = paths
                        .filter(Files::isRegularFile)
                        .map(this::createFileInfo)
                        .collect(Collectors.toList());
            }
            
            // Update the files list
            files.clear();
            files.addAll(scannedFiles);
            
            logger.debug("Scanned simulations folder: {} files found", files.size());
        } catch (IOException e) {
            logger.error("Error scanning simulations folder: {}", simulationsFolder, e);
        }
    }
    
    /**
     * Creates a FileInfo object from a Path.
     * 
     * @param path the path to the file
     * @return a FileInfo object
     */
    private FileInfo createFileInfo(Path path) {
        FileInfo fileInfo = new FileInfo(path);
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
//            fileInfo.setSize(attrs.size());
//            fileInfo.setLastModified(attrs.lastModifiedTime().toInstant());
        } catch (IOException e) {
            logger.error("Error reading file attributes: {}", path, e);
        }
        return fileInfo;
    }
    
    /**
     * Gets all files in the simulations folder.
     * 
     * @return a list of FileInfo objects
     */
    public List<FileInfo> getAllFiles() {
        return new ArrayList<>(files);
    }
}