package com.devarsh.audio_workflow.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long workflowId;

    @Column(nullable = false, length = 50)
    private String taskType;

    @Column(nullable = false)
    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> output;

    @Column(nullable = false)
    private boolean published = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public static OutboxEvent success(Long taskId, Long workflowId, String taskType, Map<String, String> output) {
        OutboxEvent e = new OutboxEvent();
        e.taskId = taskId;
        e.workflowId = workflowId;
        e.taskType = taskType;
        e.success = true;
        e.output = output;
        return e;
    }

    public static OutboxEvent failure(Long taskId, Long workflowId, String taskType, String errorMessage) {
        OutboxEvent e = new OutboxEvent();
        e.taskId = taskId;
        e.workflowId = workflowId;
        e.taskType = taskType;
        e.success = false;
        e.errorMessage = errorMessage;
        return e;
    }
}
