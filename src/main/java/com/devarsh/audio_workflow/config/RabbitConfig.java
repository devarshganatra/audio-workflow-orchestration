package com.devarsh.audio_workflow.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "workflow.exchange";

    public static final String VALIDATE_QUEUE = "q.validate";
    public static final String TRANSCRIBE_QUEUE = "q.transcribe";
    public static final String SUMMARIZE_QUEUE = "q.summarize";
    public static final String KEYWORDS_QUEUE = "q.keywords";
    public static final String PUBLISH_QUEUE = "q.publish";
    public static final String RESULTS_QUEUE = "q.results";

    @Bean
    public DirectExchange workflowExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue validateQueue() {
        return new Queue(VALIDATE_QUEUE);
    }

    @Bean
    public Queue transcribeQueue() {
        return new Queue(TRANSCRIBE_QUEUE);
    }

    @Bean
    public Queue summarizeQueue() {
        return new Queue(SUMMARIZE_QUEUE);
    }

    @Bean
    public Queue keywordsQueue() {
        return new Queue(KEYWORDS_QUEUE);
    }

    @Bean
    public Queue publishQueue() {
        return new Queue(PUBLISH_QUEUE);
    }

    @Bean
    public Queue resultsQueue() {
        return new Queue(RESULTS_QUEUE);
    }

    @Bean
    public Binding validateBinding() {
        return BindingBuilder.bind(validateQueue())
                .to(workflowExchange())
                .with("validate");
    }

    @Bean
    public Binding transcribeBinding() {
        return BindingBuilder.bind(transcribeQueue())
                .to(workflowExchange())
                .with("transcribe");
    }

    @Bean
    public Binding summarizeBinding() {
        return BindingBuilder.bind(summarizeQueue())
                .to(workflowExchange())
                .with("summarize");
    }

    @Bean
    public Binding keywordsBinding() {
        return BindingBuilder.bind(keywordsQueue())
                .to(workflowExchange())
                .with("keywords");
    }

    @Bean
    public Binding publishBinding() {
        return BindingBuilder.bind(publishQueue())
                .to(workflowExchange())
                .with("publish");
    }

    @Bean
    public Binding resultsBinding() {
        return BindingBuilder.bind(resultsQueue())
                .to(workflowExchange())
                .with("results");
    }

    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter){
        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(messageConverter);

        return rabbitTemplate;
    }

    // concurrency=1 so orchestrator result handling is strictly serialized
    @Bean
    public SimpleRabbitListenerContainerFactory serializedResultListenerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        return factory;
    }
}
