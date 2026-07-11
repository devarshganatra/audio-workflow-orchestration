package com.devarsh.audio_workflow.repository;

import com.devarsh.audio_workflow.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByPublishedFalseOrderByCreatedAtAsc();
}
