package pl.kul.fraud_insight_service.model.kafka;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
public class QueueEntry {
    @Id
    private UUID kafkaEventUUID;
    private String eventId;
    private String userId;
    private Instant joinedAt;

    public QueueEntry(QueueJoinedEvent event) {
        this.kafkaEventUUID = event.kafkaEventUUID();
        this.eventId = event.eventId();
        this.userId = event.userId();
        this.joinedAt = event.joinedAt();
    }

    protected QueueEntry() {}
}
