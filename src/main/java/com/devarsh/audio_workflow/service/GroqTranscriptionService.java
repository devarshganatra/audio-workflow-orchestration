package com.devarsh.audio_workflow.service;

import com.devarsh.audio_workflow.config.GroqProperties;
import com.devarsh.audio_workflow.messaging.dto.GroqTranscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class GroqTranscriptionService {
    private final RestClient groqRestClient;
    private final GroqProperties groqProperties;

    public String transcribe(byte[] audioBytes,String filename){
        ByteArrayResource resource =
                new ByteArrayResource(audioBytes) {

                    @Override
                    public String getFilename() {
                        return filename;
                    }
                };
        MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add(
                "file",
                resource
        );

        body.add(
                "model",
                groqProperties.model()
        );
        GroqTranscriptionResponse response =
                groqRestClient.post()
                        .uri("/audio/transcriptions")
                        .contentType(
                                MediaType.MULTIPART_FORM_DATA
                        )
                        .body(body)
                        .retrieve()
                        .body(
                                GroqTranscriptionResponse.class
                        );

        if (response == null) {
            throw new RuntimeException(
                    "Groq returned null response"
            );
        }

        return response.text();

    }
}
