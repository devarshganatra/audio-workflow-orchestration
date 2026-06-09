package com.devarsh.audio_workflow.api.controller;

import com.devarsh.audio_workflow.service.GroqTranscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class GroqTestController {

    private final GroqTranscriptionService service;

    @PostMapping("/transcribe")
    public String test(
            @RequestParam MultipartFile file
    ) throws Exception {

        return service.transcribe(
                file.getBytes(),
                file.getOriginalFilename()
        );
    }
}