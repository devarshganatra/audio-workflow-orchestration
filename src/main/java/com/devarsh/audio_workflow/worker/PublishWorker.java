package com.devarsh.audio_workflow.worker;

import com.devarsh.audio_workflow.messaging.dto.TaskMessage;
import com.devarsh.audio_workflow.messaging.dto.TaskResultMessage;
import com.devarsh.audio_workflow.service.IdempotencyService;
import com.devarsh.audio_workflow.service.WorkflowStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class PublishWorker extends AbstractTaskWorker {

    public PublishWorker(
            RabbitTemplate rabbitTemplate,
            WorkflowStateService workflowStateService,
            IdempotencyService idempotencyService
    ) {
        super(
                rabbitTemplate,
                workflowStateService,
                idempotencyService
        );
    }

    @RabbitListener(queues = "q.publish",containerFactory = "workerListenerFactory")
    public void handle(TaskMessage message) {

        log.info(
                "PublishWorker received task {}",
                message.taskId()
        );

        execute(
                message,
                "worker.publish"
        );
    }

    @Override
    protected Map<String, String> process(
            TaskMessage message
    ) throws Exception {

        Thread.sleep(500);

        return Map.of(
                "published",
                "true"
        );
    }
}