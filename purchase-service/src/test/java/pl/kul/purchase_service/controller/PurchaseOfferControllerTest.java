package pl.kul.purchase_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.kul.purchase_service.model.PurchaseStatus;
import pl.kul.purchase_service.model.exception.PurchaseOfferExpiredException;
import pl.kul.purchase_service.model.exception.PurchaseOfferNotFoundException;
import pl.kul.purchase_service.service.PurchaseOfferService;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static pl.kul.purchase_service.data.PurchaseOfferTestData.*;


@WebMvcTest(PurchaseOfferController.class)
class PurchaseOfferControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PurchaseOfferService offerService;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/events/{eventId}/purchases";
    private static final String EVENT_ID = "concert-2026";
    private static final String USER_ID = "user-1";

    @Test
    public void completePurchase_should_return_200_when_purchase_completed_successfully() throws Exception {
        // given
        when(offerService.completePurchase(EVENT_ID, USER_ID)).thenReturn(getCompletedOffer());

        // when & then
        mockMvc.perform(post(BASE_URL + "/{userId}/complete", EVENT_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.eventId").value(EVENT_ID))
                .andExpect(jsonPath("$.status").value(PurchaseStatus.COMPLETED.toString()));
    }

    @Test
    public void completePurchase_should_return_404_after_throwing_purchase_offer_not_found_exception() throws Exception{
        // given
        when(offerService.completePurchase(EVENT_ID, USER_ID))
                .thenThrow(new PurchaseOfferNotFoundException(EVENT_ID, USER_ID));

        // when & then
        mockMvc.perform(post(BASE_URL + "/{userId}/complete", EVENT_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Purchase offer for event " + EVENT_ID + " for " + USER_ID + " does not exist"));
    }

    @Test
    public void completePurchase_should_return_410_after_throwing_purchase_expired_exception() throws Exception {
        // given
        Instant expiredAt = Instant.now();
        when(offerService.completePurchase(EVENT_ID, USER_ID))
                .thenThrow(new PurchaseOfferExpiredException(EVENT_ID, USER_ID, expiredAt));

        // when & then
        mockMvc.perform(post(BASE_URL + "/{userId}/complete", EVENT_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGone())
                .andExpect(content().string("Purchase offer for " + EVENT_ID + " for " + USER_ID + " has expired at: " + expiredAt));
    }
}