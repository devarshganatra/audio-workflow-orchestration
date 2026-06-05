package com.devarsh.audio_workflow.api;

import com.devarsh.audio_workflow.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final RabbitTemplate rabbitTemplate;

    public TestController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping("/test")
    public String test() {

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                "validate",
                "Hello RabbitMQ"
        );

        return "Message Sent";
    }
}