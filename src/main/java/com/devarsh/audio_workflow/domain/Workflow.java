package com.devarsh.audio_workflow.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "workflows")
@Getter
@Setter
@NoArgsConstructor
public class Workflow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false,unique = true,updatable = false)
    private UUID externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;

    @Column(length = 500)
    private String audioFileKey;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata=new HashMap<>();

    @Column(nullable = false,updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    private void prePersist(){
        externalId=UUID.randomUUID();
        createdAt=Instant.now();
        updatedAt=Instant.now();

        if (status == null) {
            status=WorkflowStatus.PENDING;
        }
        if(metadata==null){
            metadata=new HashMap<>();
        }
    }
    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}


