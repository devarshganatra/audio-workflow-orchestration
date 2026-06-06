package com.devarsh.audio_workflow.api.dto;

import java.time.Instant;

public record TaskResponse(Long taskId,
                           String taskType,
                           String status,
                           int retryCount,
                           String errorMessage,
                           Instant createdAt) {
}
