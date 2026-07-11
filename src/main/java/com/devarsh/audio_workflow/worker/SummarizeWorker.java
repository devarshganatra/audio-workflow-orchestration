package com.devarsh.audio_workflow.worker;

import com.devarsh.audio_workflow.domain.Workflow;
import com.devarsh.audio_workflow.messaging.dto.TaskMessage;
import com.devarsh.audio_workflow.messaging.dto.TaskResultMessage;
import com.devarsh.audio_workflow.repository.WorkflowRepository;
import com.devarsh.audio_workflow.service.GroqChatService;
import com.devarsh.audio_workflow.service.IdempotencyService;
import com.devarsh.audio_workflow.service.MinioStorageService;
import com.devarsh.audio_workflow.service.WorkflowStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;

@Component
@Slf4j
public class SummarizeWorker extends AbstractTaskWorker {
    private final WorkflowRepository workflowRepository;
    private final MinioStorageService minioStorageService;
    private final GroqChatService groqChatService;
    public SummarizeWorker(
            WorkflowStateService workflowStateService,
            IdempotencyService idempotencyService,
            WorkflowRepository workflowRepository,
            MinioStorageService minioStorageService,
            GroqChatService groqChatService
    ) {
        super(workflowStateService, idempotencyService);
        this.workflowRepository = workflowRepository;
        this.minioStorageService = minioStorageService;
        this.groqChatService = groqChatService;
    }

    @RabbitListener(queues = "q.summarize",containerFactory = "workerListenerFactory")
    public void handle(TaskMessage message) {

        log.info(
                "SummarizeWorker received task {}",
                message.taskId()
        );

        execute(
                message,
                "worker.summarize"
        );
    }

    @Override
    protected Map<String, String> process(
            TaskMessage message
    ) throws Exception {

        Workflow workflow =
                workflowRepository.findById(
                        message.workflowId()
                ).orElseThrow(
                        () -> new IllegalArgumentException(
                                "Workflow not found"
                        )
                );

        String transcriptKey =
                message.context()
                        .get("transcriptKey");

        if (transcriptKey == null) {
            throw new IllegalArgumentException(
                    "transcriptKey missing"
            );
        }

        String transcript;

        try (InputStream inputStream =
                     minioStorageService.downloadFile(
                             transcriptKey
                     )) {

            transcript =
                    new String(
                            inputStream.readAllBytes()
                    );
        }

        String summary =
                groqChatService.summarize(
                        transcript
                );

        String summaryKey =
                "workflow-" +
                        workflow.getId() +
                        "/summary.txt";

        minioStorageService.uploadBytes(
                summary.getBytes(),
                summaryKey,
                "text/plain"
        );

        return Map.of(
                "summaryKey",
                summaryKey
        );
    }
}