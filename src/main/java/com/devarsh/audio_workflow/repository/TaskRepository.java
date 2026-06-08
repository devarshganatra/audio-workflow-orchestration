package com.devarsh.audio_workflow.repository;

import com.devarsh.audio_workflow.domain.Task;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task,Long> {
    List<Task> findByWorkflowId(Long workflowId);

    @Query("""
            SELECT t from Task t
            WHERE t.workflow.id=:wid
            AND t.status <> com.devarsh.audio_workflow.domain.TaskStatus.COMPLETED
            """)
    List<Task> findIncompleteTasks(@Param("wid") Long workflowId);
    @Query("""
            SELECT t
            FROM Task t
            WHERE t.status = com.devarsh.audio_workflow.domain.TaskStatus.IN_PROGRESS
            AND t.heartbeatAt < :cutoff
            """)
    List<Task> findStaleTasks(
            @Param("cutoff") Instant cutoff
    );

    @Modifying
    @Query("""
            UPDATE Task t
            SET t.heartbeatAt = :now
            WHERE t.id = :id
            """)
    void updateHeartbeat(
            @Param("id") Long id,
            @Param("now") Instant now
    );


    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.workflow.id = :workflowId
            AND t.taskType IN (
                com.devarsh.audio_workflow.domain.TaskType.SUMMARIZE,
                com.devarsh.audio_workflow.domain.TaskType.EXTRACT_KEYWORDS
            )
            AND t.status <> com.devarsh.audio_workflow.domain.TaskStatus.COMPLETED
            """)
    long countIncompleteFanOutTasks(@Param("workflowId") Long workflowId);

}
