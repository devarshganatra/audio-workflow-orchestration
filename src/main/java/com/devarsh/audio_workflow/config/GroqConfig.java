package com.devarsh.audio_workflow.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class GroqConfig {
    private final GroqProperties groqProperties;

    @Bean
    public RestClient groqRestClient(){
        return RestClient.builder().baseUrl(groqProperties.baseUrl())
                .defaultHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + groqProperties.apiKey()
        ).build();
    }
}
