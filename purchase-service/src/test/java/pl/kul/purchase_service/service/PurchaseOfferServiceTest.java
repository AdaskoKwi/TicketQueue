package pl.kul.purchase_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.kul.purchase_service.data.PurchaseOfferTestData;
import pl.kul.purchase_service.model.PurchaseOffer;
import pl.kul.purchase_service.model.PurchaseStatus;
import pl.kul.purchase_service.model.exception.PurchaseOfferExpiredException;
import pl.kul.purchase_service.model.exception.PurchaseOfferNotFoundException;
import pl.kul.purchase_service.repository.PurchaseOfferRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static pl.kul.purchase_service.data.PurchaseOfferTestData.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOfferServiceTest {
    @Mock
    private PurchaseOfferRepository repository;

    @InjectMocks
    private PurchaseOfferService offerService;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    public void createFromEvent_should_return_optional_containing_a_purchase_offer() {
        // given
        when(repository.existsById(KAFKA_EVENT_UUID)).thenReturn(false);
        when(repository.existsByEventIdAndUserIdAndStatus(
                EVENT_ID,
                USER_ID,
                PurchaseStatus.OFFERED)
        ).thenReturn(false);
        when(repository.save(any(PurchaseOffer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)
        );

        // when
        Optional<PurchaseOffer> result = offerService.createFromEvent(VALID_SLOT_OFFERED_EVENT);

        // then
        assertThat(result).isPresent();

        PurchaseOffer offer = result.get();

        assertThat(offer.getEventId()).isEqualTo(EVENT_ID);
        assertThat(offer.getUserId()).isEqualTo(USER_ID);
        assertThat(offer.getKafkaEventUUID()).isEqualTo(KAFKA_EVENT_UUID);
    }

    @Test
    public void createFromEvent_should_return_empty_optional_when_duplicate_slot_offered_event() {
        // given
        when(repository.existsById(KAFKA_EVENT_UUID)).thenReturn(true);

        // when
        Optional<PurchaseOffer> result = offerService.createFromEvent(VALID_SLOT_OFFERED_EVENT);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void createFromEvent_should_return_empty_optional_when_slot_offered_event_is_still_offered() {
        // given
        when(repository.existsById(KAFKA_EVENT_UUID)).thenReturn(false);
        when(repository.existsByEventIdAndUserIdAndStatus(EVENT_ID, USER_ID, PurchaseStatus.OFFERED)).thenReturn(true);

        // when
        Optional<PurchaseOffer> result = offerService.createFromEvent(VALID_SLOT_OFFERED_EVENT);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void completePurchase_should_return_offer_with_completed_status_before_expiration_time() {
        // given
        when(repository.findByEventIdAndUserIdAndStatus(
                EVENT_ID,
                USER_ID,
                PurchaseStatus.OFFERED)
        ).thenReturn(Optional.of(VALID_PURCHASE_OFFER));

        when(repository.save(any(PurchaseOffer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        PurchaseOffer result = offerService.completePurchase(EVENT_ID, USER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getKafkaEventUUID()).isEqualTo(KAFKA_EVENT_UUID);
        assertThat(result.getEventId()).isEqualTo(EVENT_ID);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getStatus()).isEqualTo(PurchaseStatus.COMPLETED);
    }

    @Test
    public void completePurchase_should_throw_PurchaseOfferNotFoundException_when_offer_not_found() {
        // given
        when(repository.findByEventIdAndUserIdAndStatus(
                EVENT_ID,
                USER_ID,
                PurchaseStatus.OFFERED)
        ).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> offerService.completePurchase(EVENT_ID, USER_ID))
                .isInstanceOf(PurchaseOfferNotFoundException.class)
                .hasMessage("Purchase offer for event " + EVENT_ID + " for " + USER_ID + " does not exist");
    }

    @Test
    public void completePurchase_should_throw_PurchaseOfferExpiredException_and_change_offer_status_when_expired() {
        // given
        when(repository.findByEventIdAndUserIdAndStatus(
                EVENT_ID,
                PurchaseOfferTestData.USER_ID,
                PurchaseStatus.OFFERED
        )).thenReturn(Optional.of(EXPIRED_PURCHASE_OFFER));

        // when & then
        assertThatThrownBy(() -> offerService.completePurchase(EVENT_ID, PurchaseOfferTestData.USER_ID))
                .isInstanceOf(PurchaseOfferExpiredException.class)
                .hasMessage("Purchase offer for " + EVENT_ID + " for " + PurchaseOfferTestData.USER_ID + " has expired at: " + EXPIRED_PURCHASE_OFFER.getExpiresAt());
        assertThat(EXPIRED_PURCHASE_OFFER.getStatus()).isEqualTo(PurchaseStatus.EXPIRED);
    }
}