package pl.kul.fraud_insight_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.kul.fraud_insight_service.model.kafka.QueueEntry;
import pl.kul.fraud_insight_service.model.kafka.QueueJoinedEvent;
import pl.kul.fraud_insight_service.repository.QueueEntryRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FraudInsightService {
    private final QueueEntryRepository repository;

    public Optional<QueueEntry> saveQueueJoinEvent(QueueJoinedEvent event) {
        if (repository.existsById(event.kafkaEventUUID())) {
            return Optional.empty();
        }

        return Optional.of(repository.save(new QueueEntry(event)));
    }
}
