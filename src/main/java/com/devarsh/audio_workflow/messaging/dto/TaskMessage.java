package com.devarsh.audio_workflow.messaging.dto;

import java.io.Serializable;
import java.util.Map;

public record TaskMessage(
        Long taskId,
        Long workflowId,
        String taskType,
        String audioFileKey,
        Map<String, String> context
) implements Serializable {
}