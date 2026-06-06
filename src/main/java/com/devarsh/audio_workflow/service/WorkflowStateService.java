package com.devarsh.audio_workflow.service;

import com.devarsh.audio_workflow.domain.*;
import com.devarsh.audio_workflow.exception.TaskNotFoundException;
import com.devarsh.audio_workflow.exception.WorkflowNotFoundException;
import com.devarsh.audio_workflow.repository.TaskHistoryRepository;
import com.devarsh.audio_workflow.repository.TaskRepository;
import com.devarsh.audio_workflow.repository.WorkflowRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkflowStateService {
    private final WorkflowRepository workflowRepository;
    private final TaskRepository taskRepository;
    private final TaskHistoryRepository taskHistoryRepository;

    public Workflow createWorkflow(String audioFileKey){
        Workflow workflow=new Workflow();
        workflow.setAudioFileKey(audioFileKey);
        workflow.setStatus(WorkflowStatus.RUNNING);
        workflow=workflowRepository.save(workflow);

        Task task=Task.create(workflow,TaskType.VALIDATE);
        task=taskRepository.save(task);

        appendHistory(
                task,
                null,
                TaskStatus.PENDING.name(),
                "system",
                "Workflow created"
        );

        return workflow;
    }

    public List<TaskHistory> getTimeline(
            UUID workflowId
    ) {

        Workflow workflow =
                getWorkflow(workflowId);

        return taskHistoryRepository
                .findByWorkflowIdOrderByOccurredAtAsc(
                        workflow.getId()
                );
    }

    public void transitionTask(
            Long taskId,
            TaskStatus newStatus,
            String workerId,
            String message) {

        Task task=taskRepository.findById(taskId).orElseThrow(()->new TaskNotFoundException("Task not found"+taskId));
        String oldStatus=task.getStatus().name();
        task.setStatus(newStatus);
        taskRepository.save(task);

        appendHistory(
                task,
                oldStatus,
                newStatus.name(),
                workerId,
                message
        );
        //throw new UnsupportedOperationException();
    }
    public void markTaskInProgress(
            Long taskId,
            String workerId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new TaskNotFoundException(
                                "Task not found: " + taskId
                        ));

        String oldStatus =
                task.getStatus().name();

        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setLockedBy(workerId);
        task.setHeartbeatAt(Instant.now());

        taskRepository.save(task);

        appendHistory(
                task,
                oldStatus,
                TaskStatus.IN_PROGRESS.name(),
                workerId,
                "Task started"
        );
        //throw new UnsupportedOperationException();
    }

    public void recordHeartbeat(Long taskId) {
        taskRepository.updateHeartbeat(
                taskId,
                Instant.now()
        );
     //   throw new UnsupportedOperationException();
    }
    public List<Task> getTasks(
            UUID workflowId
    ) {

        Workflow workflow =
                getWorkflow(workflowId);

        return taskRepository.findByWorkflowId(
                workflow.getId()
        );
    }


    public void failWorkflow(
            Long workflowId,
            String reason) {
        Workflow workflow = workflowRepository
                .findById(workflowId)
                .orElseThrow(() ->
                        new WorkflowNotFoundException(
                                "Workflow not found: "
                                        + workflowId
                        ));

        workflow.setStatus(
                WorkflowStatus.FAILED
        );

        workflowRepository.save(workflow);
      //  throw new UnsupportedOperationException();
    }

    public void completeWorkflow(Long workflowId) {

        Workflow workflow = workflowRepository
                .findById(workflowId)
                .orElseThrow(() ->
                        new WorkflowNotFoundException(
                                "Workflow not found: "
                                        + workflowId
                        ));

        workflow.setStatus(
                WorkflowStatus.COMPLETED
        );

        workflowRepository.save(workflow);
        //throw new UnsupportedOperationException();
    }
    public Workflow getWorkflow(UUID workflowId) {

        return workflowRepository
                .findByExternalId(workflowId)
                .orElseThrow(() ->
                        new WorkflowNotFoundException(
                                "Workflow not found: " + workflowId
                        )
                );
    }
//this is a helper function
    private void appendHistory(
            Task task,
            String oldStatus,
            String newStatus,
            String workerId,
            String message) {

        TaskHistory history =
                TaskHistory.of(
                        task,
                        oldStatus,
                        newStatus,
                        workerId,
                        message
                );

        taskHistoryRepository.save(history);
    }
}
