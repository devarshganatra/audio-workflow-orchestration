package com.devarsh.audio_workflow.repository;

import com.devarsh.audio_workflow.domain.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory,Long> {
    List<TaskHistory> findByWorkflowIdOrderByOccurredAtAsc(Long workflowid);
}
