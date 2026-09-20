package pl.kul.fraud_insight_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.kul.fraud_insight_service.model.kafka.QueueEntry;

import java.util.Optional;
import java.util.UUID;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, UUID> {
}
