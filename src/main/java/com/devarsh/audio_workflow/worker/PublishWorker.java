package com.devarsh.audio_workflow.worker;

import com.devarsh.audio_workflow.domain.Workflow;
import com.devarsh.audio_workflow.messaging.dto.TaskMessage;
import com.devarsh.audio_workflow.messaging.dto.TaskResultMessage;
import com.devarsh.audio_workflow.repository.WorkflowRepository;
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
public class PublishWorker extends AbstractTaskWorker {
    private final WorkflowRepository workflowRepository;
    private final MinioStorageService minioStorageService;

    public PublishWorker(
            WorkflowStateService workflowStateService,
            IdempotencyService idempotencyService,
            WorkflowRepository workflowRepository,
            MinioStorageService minioStorageService
    ) {
        super(workflowStateService, idempotencyService);
        this.workflowRepository = workflowRepository;
        this.minioStorageService = minioStorageService;
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

        Workflow workflow =
                workflowRepository.findById(
                        message.workflowId()
                ).orElseThrow(
                        () -> new IllegalArgumentException(
                                "Workflow not found"
                        )
                );

        String summaryKey =
                message.context().get("summaryKey");

        String keywordsKey =
                message.context().get("keywordsKey");
        String summary;

        try (InputStream inputStream =
                     minioStorageService.downloadFile(
                             summaryKey
                     )) {

            summary =
                    new String(
                            inputStream.readAllBytes()
                    );
        }
        String keywords;

        try (InputStream inputStream =
                     minioStorageService.downloadFile(
                             keywordsKey
                     )) {

            keywords =
                    new String(
                            inputStream.readAllBytes()
                    );
        }
        String report = """
        === AUDIO ANALYSIS REPORT ===

        SUMMARY
        -------
        %s

        KEYWORDS
        --------
        %s

        ARTIFACTS
        ---------
        Transcript: %s
        Summary: %s
        Keywords: %s
        """
                .formatted(
                        summary,
                        keywords,
                        message.context().get("transcriptKey"),
                        summaryKey,
                        keywordsKey
                );
        String reportKey =
                "workflow-" +
                        workflow.getId() +
                        "/final-report.txt";

        minioStorageService.uploadBytes(
                report.getBytes(),
                reportKey,
                "text/plain"
        );
        return Map.of(
                "reportKey",
                reportKey
        );
    }
}