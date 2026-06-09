package com.devarsh.audio_workflow.worker;

import com.devarsh.audio_workflow.messaging.dto.TaskMessage;
import com.devarsh.audio_workflow.messaging.dto.TaskResultMessage;
import com.devarsh.audio_workflow.repository.WorkflowRepository;
import com.devarsh.audio_workflow.service.IdempotencyService;
import com.devarsh.audio_workflow.service.MinioStorageService;
import com.devarsh.audio_workflow.service.WorkflowStateService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

public abstract class AbstractTaskWorker {

    protected final RabbitTemplate rabbitTemplate;
    protected final WorkflowStateService workflowStateService;
    protected final IdempotencyService idempotencyService;


    protected AbstractTaskWorker(
            RabbitTemplate rabbitTemplate,
            WorkflowStateService workflowStateService,
            IdempotencyService idempotencyService
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.workflowStateService = workflowStateService;
        this.idempotencyService = idempotencyService;

    }

    protected void execute(
            TaskMessage message,
            String workerId
    ) {

        if (idempotencyService.isProcessed(
                message.taskId()
        )) {
            return;
        }

        workflowStateService.markTaskInProgress(
                message.taskId(),
                workerId
        );

        try {

            Map<String, String> output =
                    process(message);

            idempotencyService.markProcessed(
                    message.taskId()
            );

            publishSuccess(
                    message,
                    output
            );

        } catch (Exception e) {

            publishFailure(
                    message,
                    e
            );
        }
    }

    protected void publishSuccess(
            TaskMessage message,
            Map<String, String> output
    ) {

        rabbitTemplate.convertAndSend(
                "workflow.exchange",
                "results",
                new TaskResultMessage(
                        message.taskId(),
                        message.workflowId(),
                        message.taskType(),
                        true,
                        null,
                        output
                )
        );
    }

    protected void publishFailure(
            TaskMessage message,
            Exception e
    ) {

        rabbitTemplate.convertAndSend(
                "workflow.exchange",
                "results",
                new TaskResultMessage(
                        message.taskId(),
                        message.workflowId(),
                        message.taskType(),
                        false,
                        e.getMessage(),
                        Map.of()
                )
        );
    }

    protected abstract Map<String, String> process(
            TaskMessage message
    ) throws Exception;
}