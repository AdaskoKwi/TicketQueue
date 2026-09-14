package pl.kul.purchase_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import pl.kul.purchase_service.model.kafka.SlotOfferedEvent;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
public class PurchaseOffer {
    @Id
    private UUID kafkaEventUUID;
    private String eventId;
    private String userId;
    @Enumerated(EnumType.STRING)
    private PurchaseStatus status;
    private Instant expiresAt;

    private static final long PURCHASE_OFFER_TIME_WINDOW_IN_SECONDS = 300;

    public PurchaseOffer(SlotOfferedEvent event) {
        this.kafkaEventUUID = event.kafkaEventUUID();
        this.eventId = event.eventId();
        this.userId = event.userId();
        this.status = PurchaseStatus.OFFERED;
        this.expiresAt = event.offeredAt().toInstant().plusSeconds(PURCHASE_OFFER_TIME_WINDOW_IN_SECONDS);
    }

    protected PurchaseOffer() {
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
}
