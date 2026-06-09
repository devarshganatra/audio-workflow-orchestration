package com.devarsh.audio_workflow;

import com.devarsh.audio_workflow.service.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class IdempotencyServiceTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Test
    void shouldMarkTaskAsProcessed() {
        Long taskId = 123L;

        assertFalse(idempotencyService.isProcessed(taskId));

        idempotencyService.markProcessed(taskId);

        assertTrue(idempotencyService.isProcessed(taskId));
    }
}