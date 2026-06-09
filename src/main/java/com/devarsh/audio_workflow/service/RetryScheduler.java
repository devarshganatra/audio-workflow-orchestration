package com.devarsh.audio_workflow.service;

import com.devarsh.audio_workflow.domain.Task;
import com.devarsh.audio_workflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryScheduler {

    private final TaskRepository taskRepository;
    private final OrchestratorService orchestratorService;
    private final MetricsService metricsService;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void retryPendingTasks() {
        Instant now = Instant.now();
        List<Task> pendingTasks = taskRepository.findTasksReadyForRetry(now);

        for (Task task : pendingTasks) {
            log.info("Retrying task {} type={} (attempt {}/{})",
                    task.getId(), task.getTaskType(),
                    task.getRetryCount(), task.getMaxRetries());

            task.setNextRunAt(null);
            taskRepository.save(task);

            orchestratorService.dispatchTask(task, task.getInputContext());
            metricsService.incrementRetry(task.getTaskType().name());
        }
    }
}
