package com.devarsh.audio_workflow.worker;

import com.devarsh.audio_workflow.domain.Workflow;
import com.devarsh.audio_workflow.messaging.dto.TaskMessage;
import com.devarsh.audio_workflow.repository.WorkflowRepository;
import com.devarsh.audio_workflow.service.IdempotencyService;
import com.devarsh.audio_workflow.service.MinioStorageService;
import com.devarsh.audio_workflow.service.WorkflowStateService;
import io.minio.StatObjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class ValidateWorker extends AbstractTaskWorker {

    private static final long MAX_FILE_SIZE =
            100L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(
                    "mp3",
                    "wav",
                    "m4a",
                    "flac"
            );

    private final WorkflowRepository workflowRepository;
    private final MinioStorageService minioStorageService;

    public ValidateWorker(
            WorkflowStateService workflowStateService,
            IdempotencyService idempotencyService,
            WorkflowRepository workflowRepository,
            MinioStorageService minioStorageService
    ) {
        super(workflowStateService, idempotencyService);
        this.workflowRepository = workflowRepository;
        this.minioStorageService = minioStorageService;
    }

    @RabbitListener(
            queues = "q.validate",
            containerFactory = "workerListenerFactory"
    )
    public void handle(TaskMessage message) {

        log.info(
                "ValidateWorker received task {}",
                message.taskId()
        );

        execute(
                message,
                "worker.validate"
        );
    }

    @Override
    protected Map<String, String> process(
            TaskMessage message
    ) {

        Workflow workflow =
                workflowRepository.findById(
                        message.workflowId()
                ).orElseThrow(
                        () -> new IllegalArgumentException(
                                "Workflow not found"
                        )
                );

        String audioKey =
                workflow.getAudioFileKey();

        if (audioKey == null || audioKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Audio file key missing"
            );
        }

        if (!minioStorageService.objectExists(audioKey)) {
            throw new IllegalArgumentException(
                    "Audio file not found in MinIO"
            );
        }

        String extension =
                getExtension(audioKey);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Unsupported audio format: "
                            + extension
            );
        }

        StatObjectResponse metadata =
                minioStorageService.getMetadata(
                        audioKey
                );

        if (metadata.size() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Audio file exceeds size limit"
            );
        }

        return Map.of(
                "validated",
                "true"
        );
    }

    private String getExtension(
            String filename
    ) {

        int idx =
                filename.lastIndexOf('.');

        if (idx == -1) {
            return "";
        }

        return filename
                .substring(idx + 1)
                .toLowerCase();
    }
}