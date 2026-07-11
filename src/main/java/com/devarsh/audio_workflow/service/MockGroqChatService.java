package com.devarsh.audio_workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock implementation of GroqChatService for load testing.
 * Returns realistic fake summaries and keywords after a simulated latency delay.
 * Activated only when the 'loadtest' Spring profile is active.
 */
@Service
@Primary
@Profile("loadtest")
@Slf4j
public class MockGroqChatService extends GroqChatService {

    public MockGroqChatService() {
        super(null, null);
    }

    @Override
    public String summarize(String transcript) {
        int latencyMs = ThreadLocalRandom.current().nextInt(100, 500);
        log.info("[MOCK] Simulating summarization with {}ms latency", latencyMs);

        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return """
                - The audio discusses distributed systems architecture and event-driven processing.
                - Key topics include message brokers, state machines, and fault tolerance patterns.
                - The speaker emphasizes the importance of idempotent operations in distributed environments.
                - Retry mechanisms and dead-letter queues are highlighted as critical reliability patterns.
                """;
    }

    @Override
    public String extractKeywords(String transcript) {
        int latencyMs = ThreadLocalRandom.current().nextInt(50, 300);
        log.info("[MOCK] Simulating keyword extraction with {}ms latency", latencyMs);

        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "distributed systems, event-driven, RabbitMQ, state machine, fault tolerance, idempotency, orchestration, Spring Boot";
    }
}
