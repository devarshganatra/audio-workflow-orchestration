package com.devarsh.audio_workflow.service;

import com.devarsh.audio_workflow.config.GroqProperties;
import com.devarsh.audio_workflow.messaging.dto.GroqChatRequest;
import com.devarsh.audio_workflow.messaging.dto.GroqChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroqChatService {
    private final RestClient groqRestClient;
    private final GroqProperties groqProperties;

    public String summarize(String transcript){
        String summaryPrompt = """
        Summarize the following transcript.
        Produce a concise summary in bullet points.

        Transcript:
        %s
        """.formatted(transcript);

        return prompt(summaryPrompt);
    }
    private String prompt(
            String prompt
    ) {

        GroqChatRequest request =
                new GroqChatRequest(
                        groqProperties.chatModel(),
                        List.of(
                                new GroqChatRequest.Message(
                                        "user",
                                        prompt
                                )
                        )
                );

        GroqChatResponse response =
                groqRestClient.post()
                        .uri("/chat/completions")
                        .body(request)
                        .retrieve()
                        .body(GroqChatResponse.class);

        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()) {

            throw new RuntimeException(
                    "Groq returned empty response"
            );
        }

        return response
                .choices()
                .get(0)
                .message()
                .content();
    }
    public String extractKeywords(
            String transcript
    ) {

        String keywordPrompt = """
            Extract the most important keywords
            from the transcript.

            Return only a comma-separated list.

            Transcript:
            %s
            """.formatted(transcript);

        return prompt(keywordPrompt);
    }
}
