package com.devarsh.audio_workflow.service;

import com.devarsh.audio_workflow.domain.OutboxEvent;
import com.devarsh.audio_workflow.messaging.dto.TaskResultMessage;
import com.devarsh.audio_workflow.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void relay() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByPublishedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : pending) {
            try {
                rabbitTemplate.convertAndSend(
                        "workflow.exchange",
                        "results",
                        new TaskResultMessage(
                                event.getTaskId(),
                                event.getWorkflowId(),
                                event.getTaskType(),
                                event.isSuccess(),
                                event.getErrorMessage(),
                                event.getOutput()
                        )
                );
                event.setPublished(true);
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Outbox relay failed for event id={}, will retry", event.getId(), e);
            }
        }
    }
}
