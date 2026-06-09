package com.devarsh.audio_workflow.api.dto;

public record WorkflowResultsResponse(
        String transcript,
        String summary,
        String keywords
) {}
