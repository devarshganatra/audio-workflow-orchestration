package com.devarsh.audio_workflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String PREFIX = "task:processed:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public boolean isProcessed(Long taskId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(PREFIX + taskId)
        );
    }

    public void markProcessed(Long taskId) {
        redisTemplate.opsForValue().set(
                PREFIX + taskId,
                "done",
                TTL
        );
    }
}