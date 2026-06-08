package com.devarsh.audio_workflow.service;

import com.devarsh.audio_workflow.domain.Task;
import com.devarsh.audio_workflow.domain.TaskStatus;
import com.devarsh.audio_workflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitTestRunner implements CommandLineRunner {

    private final TaskRepository taskRepository;
    private final OrchestratorService orchestratorService;

    @Override
    @Transactional
    public void run(String... args) {
        // Only dispatch tasks that are still in PENDING state to avoid
        // re-processing stale tasks from previous runs that are already
        // completed or being processed by a worker.
        Task task = taskRepository.findAll()
                .stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .findFirst()
                .orElse(null);

        if (task == null) {
            log.info("No PENDING task found – skipping dispatch");
            return;
        }

        orchestratorService.dispatchTask(task, Map.of());
        log.info("Dispatched task id={} type={}", task.getId(), task.getTaskType());
    }
}