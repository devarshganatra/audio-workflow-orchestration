package com.devarsh.audio_workflow.worker;

import com.devarsh.audio_workflow.domain.OutboxEvent;
import com.devarsh.audio_workflow.messaging.dto.TaskMessage;
import com.devarsh.audio_workflow.service.IdempotencyService;
import com.devarsh.audio_workflow.service.WorkflowStateService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractTaskWorker {

    protected final WorkflowStateService workflowStateService;
    protected final IdempotencyService idempotencyService;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(5);

    protected AbstractTaskWorker(
            WorkflowStateService workflowStateService,
            IdempotencyService idempotencyService
    ) {
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
        log.info(
                "Worker {} processing taskId={} workflowId={}",
                workerId,
                message.taskId(),
                message.workflowId()
        );

        workflowStateService.markTaskInProgress(
                message.taskId(),
                workerId
        );
        ScheduledFuture<?> heartbeat =
                heartbeatExecutor.scheduleAtFixedRate(
                        () -> workflowStateService.recordHeartbeat(
                                message.taskId()
                        ),
                        0,
                        30,
                        TimeUnit.SECONDS
                );
        try {

            Map<String, String> output =
                    process(message);

            idempotencyService.markProcessed(message.taskId());

            workflowStateService.saveOutboxEvent(
                    OutboxEvent.success(
                            message.taskId(),
                            message.workflowId(),
                            message.taskType(),
                            output
                    )
            );

        } catch (Exception e) {
            log.error("Worker {} failed task {}", workerId, message.taskId(), e);
            workflowStateService.saveOutboxEvent(
                    OutboxEvent.failure(
                            message.taskId(),
                            message.workflowId(),
                            message.taskType(),
                            e.getMessage()
                    )
            );
        } finally {
            heartbeat.cancel(false);
        }
    }

    protected abstract Map<String, String> process(TaskMessage message) throws Exception;
}