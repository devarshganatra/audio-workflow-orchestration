package com.devarsh.audio_workflow.worker;

import com.devarsh.audio_workflow.messaging.dto.TaskMessage;
import com.devarsh.audio_workflow.messaging.dto.TaskResultMessage;
import com.devarsh.audio_workflow.service.WorkflowStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PublishWorker {

    private final RabbitTemplate rabbitTemplate;
    private final WorkflowStateService workflowStateService;

    @RabbitListener(queues = "q.publish")
    public void handle(TaskMessage message) {
        log.info("PublishWorker received task {}", message.taskId());
        workflowStateService.markTaskInProgress(message.taskId(), "worker.publish");

        try {
            Thread.sleep(500);
            rabbitTemplate.convertAndSend("workflow.exchange", "results",
                    new TaskResultMessage(message.taskId(), message.workflowId(),
                            message.taskType(), true, null, Map.of("published", "true")));
            log.info("PublishWorker completed task {}", message.taskId());
        } catch (Exception e) {
            log.error("PublishWorker failed task {}", message.taskId(), e);
            rabbitTemplate.convertAndSend("workflow.exchange", "results",
                    new TaskResultMessage(message.taskId(), message.workflowId(),
                            message.taskType(), false, e.getMessage(), Map.of()));
        }
    }
}
