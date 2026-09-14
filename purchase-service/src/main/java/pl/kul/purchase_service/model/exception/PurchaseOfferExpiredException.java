package pl.kul.purchase_service.model.exception;

import java.time.Instant;

public class PurchaseOfferExpiredException extends RuntimeException {
    public PurchaseOfferExpiredException(String eventId, String userId, Instant expiredAt) {

        super("Purchase offer for " + eventId + " for " + userId + " has expired at: " + expiredAt);
    }
}
