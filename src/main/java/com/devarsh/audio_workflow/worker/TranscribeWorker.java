import com.devarsh.audio_workflow.domain.Workflow;
import com.devarsh.audio_workflow.messaging.dto.TaskMessage;
import com.devarsh.audio_workflow.repository.WorkflowRepository;
import com.devarsh.audio_workflow.service.GroqTranscriptionService;
import com.devarsh.audio_workflow.service.IdempotencyService;
import com.devarsh.audio_workflow.service.MetricsService;
import com.devarsh.audio_workflow.service.MinioStorageService;
import com.devarsh.audio_workflow.service.WorkflowStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;

@Component
@Slf4j
public class TranscribeWorker extends AbstractTaskWorker {

    private final WorkflowRepository workflowRepository;
    private final MinioStorageService minioStorageService;
    private final GroqTranscriptionService groqTranscriptionService;
    private final MetricsService metricsService;

    public TranscribeWorker(
            WorkflowStateService workflowStateService,
            IdempotencyService idempotencyService,
            WorkflowRepository workflowRepository,
            MinioStorageService minioStorageService,
            GroqTranscriptionService groqTranscriptionService,
            MetricsService metricsService
    ) {
        super(workflowStateService, idempotencyService);

        this.workflowRepository = workflowRepository;
        this.minioStorageService = minioStorageService;
        this.groqTranscriptionService = groqTranscriptionService;
        this.metricsService = metricsService;
    }

    @RabbitListener(queues = "q.transcribe",containerFactory = "workerListenerFactory")
    public void handle(TaskMessage message) {

        log.info(
                "TranscribeWorker received task {}",
                message.taskId()
        );

        execute(
                message,
                "worker.transcribe"
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

        String audioKey =
                workflow.getAudioFileKey();

        try (InputStream inputStream =
                     minioStorageService.downloadFile(
                             audioKey
                     )) {

            byte[] audioBytes =
                    inputStream.readAllBytes();

            String transcript =
                    metricsService.getTranscriptionTimer().record(() ->
                            groqTranscriptionService.transcribe(
                                    audioBytes,
                                    audioKey
                            )
                    );

            String transcriptKey =
                    "workflow-" +
                            workflow.getId() +
                            "/transcript.txt";

            minioStorageService.uploadBytes(
                    transcript.getBytes(),
                    transcriptKey,
                    "text/plain"
            );

            return Map.of(
                    "transcriptKey",
                    transcriptKey
            );
        }
    }
}