package pl.kul.purchase_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.kul.purchase_service.model.kafka.SlotOfferedEvent;
import pl.kul.purchase_service.service.PurchaseOfferService;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseOfferListener {
    private final PurchaseOfferService purchaseOfferService;

    private static final String SLOT_OFFERED_TOPIC = "slot-offered";

    @KafkaListener(topics = SLOT_OFFERED_TOPIC)
    public void listenForOffers(SlotOfferedEvent event) {
        purchaseOfferService.createFromEvent(event)
                .ifPresentOrElse(
                        offer -> log.info("Created purchase offer for user {} on event {}", offer.getUserId(), offer.getEventId()),
                        () -> log.info("Duplicate SlotOfferedEvent ignored: {}", event.kafkaEventUUID()));
    }
}
