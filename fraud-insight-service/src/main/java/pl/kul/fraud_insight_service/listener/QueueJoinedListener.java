package pl.kul.fraud_insight_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.kul.fraud_insight_service.model.kafka.QueueJoinedEvent;
import pl.kul.fraud_insight_service.service.FraudInsightService;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueueJoinedListener {
    private final FraudInsightService fraudInsightService;

    private final static String QUEUE_JOINED_TOPIC = "queue-joined";

    @KafkaListener(topics = QUEUE_JOINED_TOPIC)
    public void listenForQueueJoins(QueueJoinedEvent event) {
        fraudInsightService.saveQueueJoinEvent(event)
                .ifPresentOrElse(entry -> log.info("Saved queue entry for user {} for event {}", entry.getUserId(), entry.getEventId()),
                        () -> log.info("Duplicate queue entry ignored: {}", event.kafkaEventUUID()));
    }
}
