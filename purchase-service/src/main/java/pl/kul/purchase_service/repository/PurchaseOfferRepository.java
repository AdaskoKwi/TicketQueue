package pl.kul.purchase_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.kul.purchase_service.model.PurchaseOffer;
import pl.kul.purchase_service.model.PurchaseStatus;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseOfferRepository extends JpaRepository<PurchaseOffer, UUID> {
    Optional<PurchaseOffer> findByEventIdAndUserIdAndStatus(String eventId, String userId, PurchaseStatus status);
    Boolean existsByEventIdAndUserIdAndStatus(String eventId, String userId, PurchaseStatus status);
}
