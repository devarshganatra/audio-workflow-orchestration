package com.devarsh.audio_workflow.api.dto;

public record AnalyticsResponse(
        double totalCompletedWorkflows,
        double totalFailedWorkflows,
        double totalTaskFailures,
        double totalRetries,
        double transcriptionAvgDurationSeconds,
        double transcriptionMaxDurationSeconds
) {}
