package pl.kul.purchase_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kul.purchase_service.model.PurchaseOffer;
import pl.kul.purchase_service.model.PurchaseStatus;
import pl.kul.purchase_service.model.exception.PurchaseOfferExpiredException;
import pl.kul.purchase_service.model.exception.PurchaseOfferNotFoundException;
import pl.kul.purchase_service.model.kafka.SlotOfferedEvent;
import pl.kul.purchase_service.repository.PurchaseOfferRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PurchaseOfferService {
    private final PurchaseOfferRepository repository;

    @Transactional
    public Optional<PurchaseOffer> createFromEvent(SlotOfferedEvent event) {
        if (repository.existsById(event.kafkaEventUUID())) {
            return Optional.empty();
        }

        if (repository.existsByEventIdAndUserIdAndStatus(event.eventId(), event. userId(), PurchaseStatus.OFFERED)) {
            return Optional.empty();
        }

        return Optional.of(repository.save(new PurchaseOffer(event)));
    }

    @Transactional(noRollbackFor = PurchaseOfferExpiredException.class)
    public PurchaseOffer completePurchase(String eventId, String userId) {
        PurchaseOffer offer = repository
                .findByEventIdAndUserIdAndStatus(eventId, userId, PurchaseStatus.OFFERED)
                .orElseThrow(() -> new PurchaseOfferNotFoundException(eventId, userId));

        if (offer.isExpired()) {
            offer.setStatus(PurchaseStatus.EXPIRED);
            repository.save(offer);
            throw new PurchaseOfferExpiredException(eventId, userId, offer.getExpiresAt());
        }

        offer.setStatus(PurchaseStatus.COMPLETED);
        return repository.save(offer);
    }
}
