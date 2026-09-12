package pl.kul.queue_service.model.kafka;

import java.sql.Timestamp;
import java.util.UUID;

public record SlotOfferedEvent(
        UUID kafkaEventUUID,
        String eventId,
        String userId,
        Timestamp offeredAt
) {
}
