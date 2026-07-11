package com.devarsh.audio_workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock implementation of GroqTranscriptionService for load testing.
 * Returns realistic fake transcription data after a simulated latency delay.
 * Activated only when the 'loadtest' Spring profile is active.
 */
@Service
@Primary
@Profile("loadtest")
@Slf4j
public class MockGroqTranscriptionService extends GroqTranscriptionService {

    public MockGroqTranscriptionService() {
        super(null, null);
    }

    @Override
    public String transcribe(byte[] audioBytes, String filename) {
        int latencyMs = ThreadLocalRandom.current().nextInt(200, 800);
        log.info("[MOCK] Simulating transcription for {} with {}ms latency", filename, latencyMs);

        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return """
                This is a simulated transcription generated during load testing.
                The audio file %s was processed by the mock transcription service.
                In a production environment, this would contain the actual speech-to-text output
                from the Groq Whisper API. The distributed pipeline, including RabbitMQ message
                dispatch, Redis idempotency checks, PostgreSQL state transitions, and MinIO
                blob storage, is fully exercised during this test run.
                """.formatted(filename);
    }
}
