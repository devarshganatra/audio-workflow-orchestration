package com.devarsh.audio_workflow.api.controller;

import com.devarsh.audio_workflow.api.dto.AnalyticsResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final MeterRegistry meterRegistry;

    @GetMapping
    public AnalyticsResponse getAnalytics() {
        return new AnalyticsResponse(
                getCounterValue("workflow_completed_total"),
                getCounterValue("workflow_failed_total"),
                getCounterValue("task_failed_total"),
                getCounterValue("retry_total"),
                getTimerMean("transcription_duration_seconds"),
                getTimerMax("transcription_duration_seconds")
        );
    }

    private double getCounterValue(String name) {
        try {
            return meterRegistry.get(name).counter().count();
        } catch (MeterNotFoundException e) {
            return 0.0;
        }
    }

    private double getTimerMean(String name) {
        try {
            return meterRegistry.get(name).timer().mean(TimeUnit.SECONDS);
        } catch (MeterNotFoundException e) {
            return 0.0;
        }
    }

    private double getTimerMax(String name) {
        try {
            return meterRegistry.get(name).timer().max(TimeUnit.SECONDS);
        } catch (MeterNotFoundException e) {
            return 0.0;
        }
    }
}
