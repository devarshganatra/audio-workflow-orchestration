package com.devarsh.audio_workflow.service;

import com.devarsh.audio_workflow.domain.Task;
import com.devarsh.audio_workflow.domain.TaskStatus;
import com.devarsh.audio_workflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class WatchdogScheduler {
    private final TaskRepository taskRepository;

    @Scheduled(
        fixedDelay=60000
    )
    @Transactional
    public void detectAndRepairStuckTasks(){
        List<Task> staleTasks =
                taskRepository.findStaleTasks(
                        Instant.now()
                                .minus(Duration.ofMinutes(2))
                );
        for(Task task:staleTasks){
            log.warn("Watchdog detected stuck task id={} workflow={}",
            task.getId(),task.getWorkflow().getId());
            task.setStatus(
                    TaskStatus.PENDING
            );

            task.setLockedBy(
                    null
            );

            task.setHeartbeatAt(
                    Instant.now()
            );

            task.setNextRunAt(
                    Instant.now()
            );

            taskRepository.save(task);

        }

    }



}
