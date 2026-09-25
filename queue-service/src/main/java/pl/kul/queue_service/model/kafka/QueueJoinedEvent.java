package pl.kul.queue_service.model.kafka;

import java.time.Instant;
import java.util.UUID;

public record QueueJoinedEvent(
        UUID kafkaEventUUID,
        String eventId,
        String userId,
        Instant joinedAt
) {
}
