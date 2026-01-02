package org.testing.pt.gatling.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.testing.pt.gatling.model.FileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileScanner.
 */
class FileScannerTest {

    private FileScanner fileScanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileScanner = new FileScanner();
        ReflectionTestUtils.setField(fileScanner, "simulationsFolder", tempDir.toString());
    }

    @Test
    void testScanEmptyFolder() {
        // Act
        fileScanner.scanFolder();
        List<FileInfo> files = fileScanner.getAllFiles();

        // Assert
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    void testScanFolderWithFiles() throws IOException {
        // Arrange - Create test files
        Files.createFile(tempDir.resolve("test1.jar"));
        Files.createFile(tempDir.resolve("test2.jar"));
        Files.createFile(tempDir.resolve("test3.txt"));

        // Act
        fileScanner.scanFolder();
        List<FileInfo> files = fileScanner.getAllFiles();

        // Assert
        assertNotNull(files);
        assertEquals(3, files.size());
    }

    @Test
    void testScanFolderIgnoresSubdirectories() throws IOException {
        // Arrange - Create files and subdirectories
        Files.createFile(tempDir.resolve("root.jar"));
        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        Files.createFile(subDir.resolve("nested.jar"));

        // Act
        fileScanner.scanFolder();
        List<FileInfo> files = fileScanner.getAllFiles();

        // Assert
        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals("root.jar", files.get(0).getName());
    }

    @Test
    void testScanNonExistentFolderCreatesIt() {
        // Arrange
        Path nonExistent = tempDir.resolve("non-existent");
        ReflectionTestUtils.setField(fileScanner, "simulationsFolder", nonExistent.toString());

        // Act
        fileScanner.scanFolder();

        // Assert
        assertTrue(Files.exists(nonExistent));
        assertTrue(Files.isDirectory(nonExistent));
    }

    @Test
    void testGetAllFilesReturnsCopy() throws IOException {
        // Arrange
        Files.createFile(tempDir.resolve("test.jar"));
        fileScanner.scanFolder();

        // Act
        List<FileInfo> files1 = fileScanner.getAllFiles();
        List<FileInfo> files2 = fileScanner.getAllFiles();

        // Assert
        assertNotSame(files1, files2);
        assertEquals(files1.size(), files2.size());
    }

    @Test
    void testScanFolderUpdatesFileList() throws IOException {
        // Arrange - Initial scan
        Files.createFile(tempDir.resolve("file1.jar"));
        fileScanner.scanFolder();
        assertEquals(1, fileScanner.getAllFiles().size());

        // Act - Add more files and rescan
        Files.createFile(tempDir.resolve("file2.jar"));
        fileScanner.scanFolder();

        // Assert
        assertEquals(2, fileScanner.getAllFiles().size());
    }

    @Test
    void testInit() {
        // Act
        fileScanner.init();
        List<FileInfo> files = fileScanner.getAllFiles();

        // Assert - init should call scanFolder
        assertNotNull(files);
    }
}
