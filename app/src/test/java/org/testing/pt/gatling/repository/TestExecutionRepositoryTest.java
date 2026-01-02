package org.testing.pt.gatling.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestParameters;
import org.testing.pt.gatling.model.TestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TestExecutionRepository.
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class TestExecutionRepositoryTest {

    @Autowired
    private TestExecutionRepository repository;

    private TestExecution completedTest;
    private TestExecution failedTest;
    private TestExecution runningTest;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // Create test data
        completedTest = new TestExecution("Completed Test", "com.example.Test1", new TestParameters());
        completedTest.setStatus(TestStatus.COMPLETED);
        completedTest.setStartTime(LocalDateTime.now().minusHours(2));
        completedTest.setEndTime(LocalDateTime.now().minusHours(1));
        repository.save(completedTest);

        failedTest = new TestExecution("Failed Test", "com.example.Test2", new TestParameters());
        failedTest.setStatus(TestStatus.FAILED);
        failedTest.setStartTime(LocalDateTime.now().minusHours(3));
        failedTest.setEndTime(LocalDateTime.now().minusHours(2));
        repository.save(failedTest);

        runningTest = new TestExecution("Running Test", "com.example.Test3", new TestParameters());
        runningTest.setStatus(TestStatus.RUNNING);
        runningTest.setStartTime(LocalDateTime.now().minusMinutes(30));
        repository.save(runningTest);
    }

    @Test
    void testSaveAndFindById() {
        // Arrange
        TestExecution newTest = new TestExecution("New Test", "com.example.NewTest", new TestParameters());
        
        // Act
        TestExecution saved = repository.save(newTest);
        Optional<TestExecution> found = repository.findById(saved.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals("New Test", found.get().getDescription());
        assertEquals("com.example.NewTest", found.get().getTestClass());
    }

    @Test
    void testFindByStatus() {
        // Act
        List<TestExecution> completed = repository.findByStatus(TestStatus.COMPLETED);
        List<TestExecution> failed = repository.findByStatus(TestStatus.FAILED);
        List<TestExecution> running = repository.findByStatus(TestStatus.RUNNING);

        // Assert
        assertEquals(1, completed.size());
        assertEquals("Completed Test", completed.get(0).getDescription());
        
        assertEquals(1, failed.size());
        assertEquals("Failed Test", failed.get(0).getDescription());
        
        assertEquals(1, running.size());
        assertEquals("Running Test", running.get(0).getDescription());
    }

    @Test
    void testFindByStatusOrderByStartTimeDesc() {
        // Arrange - Add another completed test
        TestExecution anotherCompleted = new TestExecution("Another Completed", "com.example.Test4", new TestParameters());
        anotherCompleted.setStatus(TestStatus.COMPLETED);
        anotherCompleted.setStartTime(LocalDateTime.now().minusHours(1));
        anotherCompleted.setEndTime(LocalDateTime.now());
        repository.save(anotherCompleted);

        // Act
        List<TestExecution> completedTests = repository.findByStatusOrderByStartTimeDesc(TestStatus.COMPLETED);

        // Assert
        assertEquals(2, completedTests.size());
        // Most recent should be first
        assertEquals("Another Completed", completedTests.get(0).getDescription());
        assertEquals("Completed Test", completedTests.get(1).getDescription());
    }

    @Test
    void testFindAllByOrderByStartTimeDesc() {
        // Act
        List<TestExecution> allTests = repository.findAllByOrderByStartTimeDesc();

        // Assert
        assertEquals(3, allTests.size());
        // Most recent (running) should be first
        assertEquals("Running Test", allTests.get(0).getDescription());
    }

    @Test
    void testFindByStartTimeAfter() {
        // Arrange
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1).minusMinutes(30);

        // Act
        List<TestExecution> recentTests = repository.findByStartTimeAfter(cutoff);

        // Assert
        assertEquals(1, recentTests.size());
        assertEquals("Running Test", recentTests.get(0).getDescription());
    }

    @Test
    void testCountByStatus() {
        // Act
        long completedCount = repository.countByStatus(TestStatus.COMPLETED);
        long failedCount = repository.countByStatus(TestStatus.FAILED);
        long runningCount = repository.countByStatus(TestStatus.RUNNING);
        long pendingCount = repository.countByStatus(TestStatus.PENDING);

        // Assert
        assertEquals(1, completedCount);
        assertEquals(1, failedCount);
        assertEquals(1, runningCount);
        assertEquals(0, pendingCount);
    }

    @Test
    void testFindOldExecutions() {
        // Arrange - Create an old test
        TestExecution oldTest = new TestExecution("Old Test", "com.example.OldTest", new TestParameters());
        oldTest.setStatus(TestStatus.COMPLETED);
        oldTest.setStartTime(LocalDateTime.now().minusDays(40));
        oldTest.setEndTime(LocalDateTime.now().minusDays(40).plusHours(1));
        repository.save(oldTest);

        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

        // Act
        List<TestExecution> oldExecutions = repository.findOldExecutions(cutoff);

        // Assert
        assertEquals(1, oldExecutions.size());
        assertEquals("Old Test", oldExecutions.get(0).getDescription());
    }

    @Test
    void testDeleteTest() {
        // Arrange
        String testId = completedTest.getId();

        // Act
        repository.delete(completedTest);
        Optional<TestExecution> found = repository.findById(testId);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void testCount() {
        // Act
        long count = repository.count();

        // Assert
        assertEquals(3, count);
    }

    @Test
    void testTestExecutionWithParameters() {
        // Arrange
        TestParameters params = new TestParameters();
        params.addParameter("users", "100");
        params.addParameter("duration", "300");
        
        TestExecution test = new TestExecution("Param Test", "com.example.ParamTest", params);
        test.setStatus(TestStatus.PENDING);

        // Act
        TestExecution saved = repository.save(test);
        
        // Clear the persistence context to force a reload
        repository.flush();
        Optional<TestExecution> found = repository.findById(saved.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Param Test", found.get().getDescription());
        assertNotNull(found.get().getParameters());
        // The parameters should be stored as JSON
        assertNotNull(found.get().getParameters().getParametersJson());
    }
}
