package com.devarsh.audio_workflow.api.controller;

import com.devarsh.audio_workflow.messaging.dto.TaskResultMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestController {

    private final RabbitTemplate rabbitTemplate;

    @PostMapping("/result")
    public String sendResult() {

        TaskResultMessage result = new TaskResultMessage(
                1823L,  // ← use the actual task ID from your DB
                5L,
                "VALIDATE",
                true,
                null,
                Map.of()
        );

        rabbitTemplate.convertAndSend(
                "workflow.exchange",
                "results",
                result
        );

        return "sent";
    }
}