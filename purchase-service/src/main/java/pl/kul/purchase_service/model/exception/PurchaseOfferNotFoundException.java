package pl.kul.purchase_service.model.exception;

public class PurchaseOfferNotFoundException extends RuntimeException {
    public PurchaseOfferNotFoundException(String eventId, String userId) {
        super("Purchase offer for event " + eventId + " for " + userId + " does not exist");
    }
}
