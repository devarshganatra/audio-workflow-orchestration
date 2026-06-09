package com.devarsh.audio_workflow.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.IdGeneratorType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name="workflow_id",nullable = false)
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 50)
    private TaskType taskType;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 50)
    private TaskStatus status;

    @Column(nullable = false)
    private int retryCount=0;
    @Column(nullable = false)
    private int maxRetries=3;

    @Column
    private Instant nextRunAt;

    @Column(length = 100)
    private String lockedBy;

    @Column
    private Instant heartbeatAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> inputContext = new HashMap<>();


    @Column(nullable = false,updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();

        if (status == null) {
            status = TaskStatus.PENDING;
        }
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }

    public static Task create(Workflow workflow,TaskType type){
        Task task=new Task();
        task.setWorkflow(workflow);
        task.setTaskType(type);
        task.setStatus(TaskStatus.PENDING);
        task.setMaxRetries(
                type == TaskType.TRANSCRIBE
                        ? 5
                        : 3
        );
        return task;
    }

}
