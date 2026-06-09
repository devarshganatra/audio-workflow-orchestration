package com.devarsh.audio_workflow.api.controller;

import com.devarsh.audio_workflow.service.GroqChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class GroqSummaryTestController {

    private final GroqChatService groqChatService;

    @PostMapping("/summary")
    public String summarize(
            @RequestBody String transcript
    ) {

        return groqChatService.summarize(
                transcript
        );
    }
    @PostMapping("/keywords")
    public String keywords(
            @RequestBody String transcript
    ) {

        return groqChatService.extractKeywords(
                transcript
        );
    }
}