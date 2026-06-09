package com.devarsh.audio_workflow.messaging.dto;

import java.util.List;

public record GroqChatResponse(List<Choice> choices) {
    public record Choice(
            Message message
    ) {}

    public record Message(
            String role,
            String content
    ) {}
}
