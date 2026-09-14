package pl.kul.purchase_service.data;

import pl.kul.purchase_service.model.PurchaseOffer;
import pl.kul.purchase_service.model.kafka.SlotOfferedEvent;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class PurchaseOfferTestData {
    public static final UUID KAFKA_EVENT_UUID = UUID.fromString("078768cb-8045-4e50-8033-e4caa74e84a4");
    public static final String EVENT_ID = "concert-2026";
    public static final String USER_ID = "user-1";

    public static final SlotOfferedEvent VALID_SLOT_OFFERED_EVENT = new SlotOfferedEvent(
            KAFKA_EVENT_UUID,
            EVENT_ID,
            USER_ID,
            Timestamp.from(Instant.now())
    );

    public static final SlotOfferedEvent EXPIRED_SLOT_OFFERED_EVENT = new SlotOfferedEvent(
            KAFKA_EVENT_UUID,
            EVENT_ID,
            USER_ID,
            Timestamp.valueOf(LocalDate.now().atTime(LocalTime.of(15, 0)))
    );

    public static final PurchaseOffer VALID_PURCHASE_OFFER = new PurchaseOffer(VALID_SLOT_OFFERED_EVENT);
    public static final PurchaseOffer EXPIRED_PURCHASE_OFFER = new PurchaseOffer(EXPIRED_SLOT_OFFERED_EVENT);
}
