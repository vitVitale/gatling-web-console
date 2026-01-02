package org.testing.pt.gatling.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.testing.pt.gatling.model.TestExecution;
import org.testing.pt.gatling.model.TestStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for TestExecution entity.
 */
@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, String> {

    /**
     * Find test executions by status.
     *
     * @param status the test status
     * @return list of test executions with the given status
     */
    List<TestExecution> findByStatus(TestStatus status);

    /**
     * Find test executions by status ordered by start time descending.
     *
     * @param status the test status
     * @return list of test executions with the given status
     */
    List<TestExecution> findByStatusOrderByStartTimeDesc(TestStatus status);

    /**
     * Find all test executions ordered by start time descending.
     *
     * @return list of all test executions
     */
    List<TestExecution> findAllByOrderByStartTimeDesc();

    /**
     * Find test executions started after a given time.
     *
     * @param startTime the start time
     * @return list of test executions started after the given time
     */
    List<TestExecution> findByStartTimeAfter(LocalDateTime startTime);

    /**
     * Count test executions by status.
     *
     * @param status the test status
     * @return count of test executions with the given status
     */
    long countByStatus(TestStatus status);
    
    /**
     * Find test executions that are completed, failed, or stopped and started before a given time.
     *
     * @param cutoffTime the cutoff time
     * @return list of old test executions
     */
    @Query("SELECT t FROM TestExecution t WHERE t.status IN ('COMPLETED', 'FAILED', 'STOPPED') AND t.startTime < :cutoffTime")
    List<TestExecution> findOldExecutions(@Param("cutoffTime") LocalDateTime cutoffTime);
}
