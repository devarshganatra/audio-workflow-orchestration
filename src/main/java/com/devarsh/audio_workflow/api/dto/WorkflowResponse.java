package com.devarsh.audio_workflow.api.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkflowResponse(UUID workflowId,
                               String status,
                               String audioFileKey,
                               Instant createdAt,
                               Instant updatedAt
){};
