package pl.kul.purchase_service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.kul.purchase_service.model.PurchaseOffer;
import pl.kul.purchase_service.model.PurchaseStatus;
import pl.kul.purchase_service.service.PurchaseOfferService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseOfferScheduler {
    private final PurchaseOfferService offerService;

    @Scheduled(fixedRate = 600000)
    public void updateStatusOfExpiredOffers() {
        List<PurchaseOffer> offers = offerService.getAllOfferedPurchaseOffers();

        List<PurchaseOffer> expiredOffers = offers.stream()
                .filter(PurchaseOffer::isExpired)
                .peek(offer -> {
                    offer.setStatus(PurchaseStatus.EXPIRED);
                    log.info("Purchase offer expired for user {} for event {}", offer.getUserId(), offer.getEventId());
                })
                .toList();

        offerService.saveAll(expiredOffers);
    }
}
