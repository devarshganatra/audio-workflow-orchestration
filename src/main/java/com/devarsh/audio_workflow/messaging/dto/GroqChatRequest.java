package com.devarsh.audio_workflow.messaging.dto;

public record GroqChatRequest(String model, java.util.List<Message>messages) {
    public record Message(String role,String content){}
}
