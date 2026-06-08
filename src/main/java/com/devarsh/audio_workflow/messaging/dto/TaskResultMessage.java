package com.devarsh.audio_workflow.messaging.dto;

import java.io.Serializable;
import java.util.Map;

public record TaskResultMessage(
        Long taskId,
        Long workflowId,
        String taskType,
        boolean success,
        String errorMessage,
        Map<String, String> output
) implements Serializable {
}