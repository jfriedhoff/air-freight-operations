package be.kdg.airfreight.loadplanning.infrastructure.messaging;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findBySentAtIsNullOrderBySequenceAsc();
}
