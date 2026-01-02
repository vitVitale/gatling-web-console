package org.testing.pt.gatling.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestParameters;
import org.testing.pt.gatling.model.TestStatus;
import org.testing.pt.gatling.repository.TestExecutionRepository;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GatlingService.
 */
class GatlingServiceTest {

    @Mock
    private TestExecutionRepository testExecutionRepository;

    @Mock
    private FileScanner fileScanner;

    @Mock
    private LogStreamService logStreamService;

    @InjectMocks
    private GatlingService gatlingService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set test directories using reflection
        ReflectionTestUtils.setField(gatlingService, "testsDirectory", tempDir.resolve("tests").toString());
        ReflectionTestUtils.setField(gatlingService, "resultsDirectory", tempDir.resolve("results").toString());
        ReflectionTestUtils.setField(gatlingService, "gatlingJarsDirectory", tempDir.resolve("jars").toString());
    }

    @Test
    void testGetAllTestExecutions() {
        // Arrange
        TestExecution test1 = new TestExecution();
        test1.setId("test-1");
        test1.setDescription("Test 1");
        
        TestExecution test2 = new TestExecution();
        test2.setId("test-2");
        test2.setDescription("Test 2");
        
        List<TestExecution> expectedTests = Arrays.asList(test1, test2);
        when(testExecutionRepository.findAllByOrderByStartTimeDesc()).thenReturn(expectedTests);

        // Act
        List<TestExecution> actualTests = gatlingService.getAllTestExecutions();

        // Assert
        assertEquals(2, actualTests.size());
        assertEquals("test-1", actualTests.get(0).getId());
        assertEquals("test-2", actualTests.get(1).getId());
        verify(testExecutionRepository, times(1)).findAllByOrderByStartTimeDesc();
    }

    @Test
    void testGetTestExecution() {
        // Arrange
        String testId = "test-123";
        TestExecution expectedTest = new TestExecution();
        expectedTest.setId(testId);
        expectedTest.setDescription("Test execution");
        
        when(testExecutionRepository.findById(testId)).thenReturn(Optional.of(expectedTest));

        // Act
        TestExecution actualTest = gatlingService.getTestExecution(testId);

        // Assert
        assertNotNull(actualTest);
        assertEquals(testId, actualTest.getId());
        assertEquals("Test execution", actualTest.getDescription());
        verify(testExecutionRepository, times(1)).findById(testId);
    }

    @Test
    void testGetTestExecutionNotFound() {
        // Arrange
        String testId = "non-existent";
        when(testExecutionRepository.findById(testId)).thenReturn(Optional.empty());

        // Act
        TestExecution actualTest = gatlingService.getTestExecution(testId);

        // Assert
        assertNull(actualTest);
        verify(testExecutionRepository, times(1)).findById(testId);
    }

    @Test
    void testDeleteTestExecution() {
        // Arrange
        String testId = "test-to-delete";
        TestExecution testExecution = new TestExecution();
        testExecution.setId(testId);
        testExecution.setStatus(TestStatus.COMPLETED);
        
        when(testExecutionRepository.findById(testId)).thenReturn(Optional.of(testExecution));
        doNothing().when(testExecutionRepository).delete(any(TestExecution.class));

        // Act
        boolean result = gatlingService.deleteTestExecution(testId);

        // Assert
        assertTrue(result);
        verify(testExecutionRepository, times(1)).findById(testId);
        verify(testExecutionRepository, times(1)).delete(testExecution);
    }

    @Test
    void testDeleteTestExecutionNotFound() {
        // Arrange
        String testId = "non-existent";
        when(testExecutionRepository.findById(testId)).thenReturn(Optional.empty());

        // Act
        boolean result = gatlingService.deleteTestExecution(testId);

        // Assert
        assertFalse(result);
        verify(testExecutionRepository, times(1)).findById(testId);
        verify(testExecutionRepository, never()).delete(any(TestExecution.class));
    }

    @Test
    void testGetAvailableJarFiles() {
        // Arrange
        org.testing.pt.gatling.model.FileInfo file1 = new org.testing.pt.gatling.model.FileInfo(Path.of("test1.jar"));
        org.testing.pt.gatling.model.FileInfo file2 = new org.testing.pt.gatling.model.FileInfo(Path.of("test2.jar"));
        List<org.testing.pt.gatling.model.FileInfo> expectedFiles = Arrays.asList(file1, file2);
        
        when(fileScanner.getAllFiles()).thenReturn(expectedFiles);

        // Act
        List<org.testing.pt.gatling.model.FileInfo> actualFiles = gatlingService.getAvailableJarFiles();

        // Assert
        assertEquals(2, actualFiles.size());
        verify(fileScanner, times(1)).getAllFiles();
    }

    @Test
    void testStopTestNotRunning() {
        // Arrange
        String testId = "not-running";

        // Act
        boolean result = gatlingService.stopTest(testId);

        // Assert
        assertFalse(result);
    }

    @Test
    void testCreateTestExecution() {
        // Arrange
        TestExecution testExecution = new TestExecution("Test", "TestClass", new TestParameters());
        when(testExecutionRepository.save(any(TestExecution.class))).thenReturn(testExecution);

        // Act
        TestExecution saved = testExecutionRepository.save(testExecution);

        // Assert
        assertNotNull(saved);
        assertEquals("Test", saved.getDescription());
        verify(testExecutionRepository, times(1)).save(any(TestExecution.class));
    }
}
