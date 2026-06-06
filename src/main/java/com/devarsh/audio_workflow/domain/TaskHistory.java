package com.devarsh.audio_workflow.domain;

import jakarta.persistence.*;
import jdk.jfr.ContentType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.print.attribute.standard.MediaSize;
import java.time.Instant;

@Entity
@Table(name = "task_history")
@Getter
@NoArgsConstructor
public class TaskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name="task_id",nullable = false)
    private Task task;

    @Column(nullable = false)
    private Long workflowId;  //here we could have used the other id but this saves a join operation

    @Column(length = 50)
    private String taskType;

    @Column(length = 30)
    private String oldStatus;

    @Column(nullable = false,length = 30)
    private String newStatus;


    @Column(length = 100)
    private String workerId;

    @Column(columnDefinition="TEXT")
    private String message;

    @Column(nullable = false,updatable = false)
    private Instant occurredAt;

    @PrePersist
    private void prePersist(){
        occurredAt=Instant.now();
    }

    public static TaskHistory of(
            Task task,
            String oldStatus,
            String newStatus,
            String workerId,
            String message
    ) {
        TaskHistory history = new TaskHistory();

        history.task = task;
        history.workflowId = task.getWorkflow().getId();
        history.taskType = task.getTaskType().name();

        history.oldStatus = oldStatus;
        history.newStatus = newStatus;

        history.workerId = workerId;
        history.message = message;

        return history;
    }
}
