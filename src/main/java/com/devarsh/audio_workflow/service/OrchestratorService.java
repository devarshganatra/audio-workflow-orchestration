package com.devarsh.audio_workflow.service;

import com.devarsh.audio_workflow.domain.Task;
import com.devarsh.audio_workflow.domain.TaskStatus;
import com.devarsh.audio_workflow.domain.TaskType;
import com.devarsh.audio_workflow.domain.Workflow;
import com.devarsh.audio_workflow.exception.TaskNotFoundException;
import com.devarsh.audio_workflow.messaging.dto.TaskMessage;
import com.devarsh.audio_workflow.messaging.dto.TaskResultMessage;
import com.devarsh.audio_workflow.repository.TaskRepository;
import com.devarsh.audio_workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorService {
    private final RabbitTemplate rabbitTemplate;

    private final WorkflowRepository workflowRepository;

    private final TaskRepository taskRepository;

    private final WorkflowStateService workflowStateService;

    public void dispatchTask(Task task, Map<String, String> context) {
        task.setInputContext(context);
        taskRepository.save(task);

        Workflow workflow = task.getWorkflow();
        TaskMessage message =
                new TaskMessage(
                        task.getId(),
                        workflow.getId(),
                        task.getTaskType().name(),
                        workflow.getAudioFileKey(),
                        context
                );

        String routingKey = switch (task.getTaskType()) {
            case VALIDATE         -> "validate";
            case TRANSCRIBE       -> "transcribe";
            case SUMMARIZE        -> "summarize";
            case EXTRACT_KEYWORDS -> "keywords";
            case PUBLISH          -> "publish";
        };

        log.info("Dispatching task {} type={} routingKey={}", task.getId(), task.getTaskType(), routingKey);
        rabbitTemplate.convertAndSend(
                "workflow.exchange",
                routingKey,
                message
        );
    }

    @Transactional
    public void handleTaskResult(TaskResultMessage result) {

        Task task = taskRepository.findById(result.taskId())
                .orElseThrow(() -> new TaskNotFoundException("Task not found: " + result.taskId()));

        Workflow workflow = workflowRepository.findById(task.getWorkflow().getId())
                .orElseThrow();

        if (!result.success()) {
            handleTaskFailure(task, workflow, result.errorMessage());
            return;
        }
        workflow.getMetadata()
                .putAll(result.output());

        workflowRepository.save(workflow);

        workflowStateService.transitionTask(task.getId(), TaskStatus.COMPLETED,
                "orchestrator", "Task completed");

        scheduleNextTask(task, workflow);
    }

    private void handleTaskFailure(Task task, Workflow workflow, String errorMessage) {
        int newRetryCount = task.getRetryCount() + 1;
        task.setRetryCount(newRetryCount);
        taskRepository.save(task);

        if (newRetryCount >= task.getMaxRetries()) {
            workflowStateService.transitionTask(task.getId(), TaskStatus.DEAD,
                    "orchestrator", "Max retries exceeded: " + errorMessage);
            workflowStateService.failWorkflow(workflow.getId(),
                    "Task " + task.getTaskType() + " exceeded max retries");
            log.warn("Task {} DEAD after {} attempts", task.getId(), newRetryCount);
        } else {
            long backoffSeconds = Math.min(10L * (1L << newRetryCount), 600L);
            task.setNextRunAt(Instant.now().plusSeconds(backoffSeconds));
            workflowStateService.transitionTask(task.getId(), TaskStatus.PENDING,
                    "orchestrator", "Retrying after " + backoffSeconds + "s: " + errorMessage);
            log.info("Task {} will retry in {}s (attempt {}/{})",
                    task.getId(), backoffSeconds, newRetryCount, task.getMaxRetries());
        }
    }

    private void scheduleNextTask(Task completedTask, Workflow workflow) {

        Map<String, String> workflowContext =
                workflow.getMetadata()
                        .entrySet()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> String.valueOf(
                                                entry.getValue()
                                        )
                                )
                        );

        switch (completedTask.getTaskType()) {

            case VALIDATE -> {
                Task next = taskRepository.saveAndFlush(Task.create(workflow, TaskType.TRANSCRIBE));
                dispatchTask(next, workflowContext);
            }

            case TRANSCRIBE -> {
                Task summarize = taskRepository.saveAndFlush(Task.create(workflow, TaskType.SUMMARIZE));
                Task keywords  = taskRepository.saveAndFlush(Task.create(workflow, TaskType.EXTRACT_KEYWORDS));
                dispatchTask(summarize, workflowContext);
                dispatchTask(keywords, workflowContext);
            }

            case SUMMARIZE, EXTRACT_KEYWORDS -> {
                Workflow lockedWorkflow = workflowRepository.findByIdForUpdate(workflow.getId())
                        .orElseThrow();

                long incomplete = taskRepository.countIncompleteFanOutTasks(lockedWorkflow.getId());

                if (incomplete == 0) {
                    // Re-build context from the locked workflow so it includes
                    // ALL fan-out outputs (summaryKey + keywordsKey)
                    Map<String, String> freshContext =
                            lockedWorkflow.getMetadata()
                                    .entrySet()
                                    .stream()
                                    .collect(
                                            Collectors.toMap(
                                                    Map.Entry::getKey,
                                                    entry -> String.valueOf(
                                                            entry.getValue()
                                                    )
                                            )
                                    );
                    Task publish = taskRepository.saveAndFlush(Task.create(lockedWorkflow, TaskType.PUBLISH));
                    dispatchTask(publish, freshContext);
                }
            }

            case PUBLISH -> workflowStateService.completeWorkflow(workflow.getId());
        }
    }

}
