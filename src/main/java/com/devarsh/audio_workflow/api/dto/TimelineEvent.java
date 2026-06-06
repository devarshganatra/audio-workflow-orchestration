package com.devarsh.audio_workflow.api.dto;

import java.time.Instant;

public record TimelineEvent(Long taskId,
                            String taskType,
                            String oldStatus,
                            String newStatus,
                            String workerId,
                            String message,
                            Instant occurredAt) {
}
