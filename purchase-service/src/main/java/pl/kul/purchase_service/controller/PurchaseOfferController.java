package pl.kul.purchase_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.kul.purchase_service.model.PurchaseOffer;
import pl.kul.purchase_service.service.PurchaseOfferService;

@RestController
@RequestMapping("/events/{eventId}/purchases")
@RequiredArgsConstructor
public class PurchaseOfferController {
    private final PurchaseOfferService offerService;

    @PostMapping("/{userId}/complete")
    public ResponseEntity<?> completePurchase(
            @PathVariable String eventId,
            @PathVariable String userId
    ) {
        PurchaseOffer completedOffer = offerService.completePurchase(eventId, userId);

        return ResponseEntity
                .ok()
                .body(completedOffer);
    }
}
