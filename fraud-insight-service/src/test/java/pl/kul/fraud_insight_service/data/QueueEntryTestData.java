package pl.kul.fraud_insight_service.data;

import pl.kul.fraud_insight_service.model.kafka.QueueEntry;
import pl.kul.fraud_insight_service.model.kafka.QueueJoinedEvent;

import java.time.Instant;
import java.util.UUID;

public class QueueEntryTestData {
    public static final UUID KAFKA_EVENT_UUID = UUID.fromString("078768cb-8045-4e50-8033-e4caa74e84a4");
    public static final UUID KAFKA_EVENT_UUID_TWO= UUID.fromString("078768cb-8045-4e50-8033-e4caa74e84a5");
    public static final String EVENT_ID = "concert-2026";
    public static final String USER_ID = "user-1";
    public static final String USER_ID_TWO = "user-2";

    public static final QueueJoinedEvent VALID_QUEUE_JOINED_EVENT = new QueueJoinedEvent(
            KAFKA_EVENT_UUID,
            EVENT_ID,
            USER_ID,
            Instant.now()
    );

    public static final QueueJoinedEvent VALID_QUEUE_JOINED_EVENT_TWO = new QueueJoinedEvent(
            KAFKA_EVENT_UUID_TWO,
            EVENT_ID,
            USER_ID_TWO,
            Instant.now()
    );

    public static final QueueEntry VALID_QUEUE_ENTRY = new QueueEntry(VALID_QUEUE_JOINED_EVENT);
    public static final QueueEntry VALID_QUEUE_ENTRY_TWO = new QueueEntry(VALID_QUEUE_JOINED_EVENT_TWO);
}
