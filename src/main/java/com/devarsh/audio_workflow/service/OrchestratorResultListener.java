package com.devarsh.audio_workflow.service;

import com.devarsh.audio_workflow.messaging.dto.TaskResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrchestratorResultListener {
    private final OrchestratorService orchestratorService;

    @RabbitListener(queues = "q.results", containerFactory = "serializedResultListenerFactory")
    public void onTaskResult(TaskResultMessage result) {
        log.info("Received result for task {} success={}", result.taskId(), result.success());
        orchestratorService.handleTaskResult(result);
    }
}
