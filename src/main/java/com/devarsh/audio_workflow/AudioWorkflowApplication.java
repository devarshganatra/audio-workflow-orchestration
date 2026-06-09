package com.devarsh.audio_workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@EnableJpaRepositories(basePackages = "com.devarsh.audio_workflow.repository")
public class AudioWorkflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(AudioWorkflowApplication.class, args);
	}

}
