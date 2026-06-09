package com.devarsh.audio_workflow.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final MeterRegistry registry;
    private final Counter workflowCompletedCounter;
    private final Counter workflowFailedCounter;
    private final Timer transcriptionTimer;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;

        this.workflowCompletedCounter = Counter.builder("workflow_completed_total")
                .description("Total workflows completed successfully")
                .register(registry);

        this.workflowFailedCounter = Counter.builder("workflow_failed_total")
                .description("Total workflows failed")
                .register(registry);

        this.transcriptionTimer = Timer.builder("transcription_duration_seconds")
                .description("Time taken to transcribe audio via Groq API")
                .register(registry);
    }

    public void incrementWorkflowCompleted() {
        workflowCompletedCounter.increment();
    }

    public void incrementWorkflowFailed() {
        workflowFailedCounter.increment();
    }

    public void incrementTaskFailed(String taskType) {
        Counter.builder("task_failed_total")
                .description("Total task failures")
                .tag("taskType", taskType)
                .register(registry)
                .increment();
    }

    public void incrementRetry(String taskType) {
        Counter.builder("retry_total")
                .description("Total task retries dispatched")
                .tag("taskType", taskType)
                .register(registry)
                .increment();
    }

    public Timer getTranscriptionTimer() {
        return transcriptionTimer;
    }
}
