package org.testing.pt.gatling.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;
import org.testing.pt.gatling.model.FileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the FileScanner service.
 */
public class FileScannerTest {
    
    private static final String TEST_DIR = "./test-simulations";
    private FileScanner fileScanner;
    
    @BeforeEach
    public void setUp() throws IOException {
        // Create a test directory
        Path testDir = Paths.get(TEST_DIR);
        if (Files.exists(testDir)) {
            FileSystemUtils.deleteRecursively(testDir);
        }
        Files.createDirectories(testDir);
        
        // Create some test files
        Files.createFile(testDir.resolve("test1.jar"));
        Files.createFile(testDir.resolve("test2.jar"));
        Files.createFile(testDir.resolve("test3.txt"));
        
        // Create a subdirectory (should be ignored)
        Path subDir = testDir.resolve("subdir");
        Files.createDirectories(subDir);
        Files.createFile(subDir.resolve("subfile.jar"));
        
        // Initialize the file scanner with the test directory
        fileScanner = new FileScanner();
        fileScanner.setSimulationsFolder(TEST_DIR);
    }
    
    @Test
    public void testScanFolder() {
        // Scan the folder
        fileScanner.scanFolder();
        
        // Get all files
        List<FileInfo> files = fileScanner.getAllFiles();
        
        // Verify the results
        assertNotNull(files);
        assertEquals(3, files.size(), "Should find 3 files in the test directory");
        
        // Verify file names
        assertTrue(files.stream().anyMatch(f -> f.getName().equals("test1.jar")), "Should find test1.jar");
        assertTrue(files.stream().anyMatch(f -> f.getName().equals("test2.jar")), "Should find test2.jar");
        assertTrue(files.stream().anyMatch(f -> f.getName().equals("test3.txt")), "Should find test3.txt");
        
        // Verify no subdirectory files
        assertFalse(files.stream().anyMatch(f -> f.getName().equals("subfile.jar")), "Should not find subfile.jar");
    }
}