package pl.kul.queue_service.controller;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pl.kul.queue_service.service.QueueService;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.mockito.Mockito.when;

@WebMvcTest(QueueController.class)
class QueueControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private QueueService queueService;
    @Autowired
    private ObjectMapper objectMapper;

    private final static String BASE_URL = "/events/{eventId}/queue";
    private final static String EVENT_ID = "concert-2026";
    private final static String USER_ID = "user-1";

    private final static Long CHECKED_POSITION_NOT_IN_QUEUE = -1L;
    private final static Long USER_DELETED = 1L;
    private final static Long USER_NOT_DELETED = 0L;

    @Test
    public void join_should_return_200_when_user_is_not_limited() throws Exception {
        // given
        when(queueService.isRateLimited(EVENT_ID, USER_ID)).thenReturn(Mono.just(false));
        when(queueService.joinQueue(EVENT_ID, USER_ID)).thenReturn(Mono.just(1L));
        when(queueService.getQueueSize(EVENT_ID)).thenReturn(Mono.just(1L));

        // when && then
        performAsync(post(BASE_URL + "/join", EVENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinRequest(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(1L))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    public void join_should_return_429_when_user_is_rate_limited() throws Exception {
        // given
        when(queueService.isRateLimited(EVENT_ID, USER_ID)).thenReturn(Mono.just(true));

        // when & then
        performAsync(post(BASE_URL + "/join", EVENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JoinRequest(USER_ID))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    public void position_should_return_200_when_user_is_in_queue() throws Exception {
        // given
        when(queueService.getPosition(EVENT_ID, USER_ID)).thenReturn(Mono.just(1L));
        when(queueService.getQueueSize(EVENT_ID)).thenReturn(Mono.just(1L));

        // when & then
        performAsync(get(BASE_URL + "/position/{userId}", EVENT_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(1L))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.totalInQueue").value(1L));
    }

    @Test
    public void position_should_return_200_when_user_is_second_in_queue() throws Exception {
        // given
        when(queueService.getPosition(EVENT_ID, USER_ID)).thenReturn(Mono.just(2L));
        when(queueService.getQueueSize(EVENT_ID)).thenReturn(Mono.just(2L));

        // when & then
        performAsync(get(BASE_URL + "/position/{userId}", EVENT_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(2L))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.totalInQueue").value(2L));
    }

    @Test
    public void position_should_return_404_when_user_is_not_in_queue() throws Exception {
        // given
        when(queueService.getPosition(EVENT_ID, USER_ID)).thenReturn(Mono.just(CHECKED_POSITION_NOT_IN_QUEUE));

        // when & then
        performAsync(get(BASE_URL + "/position/{userId}", EVENT_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void leave_should_return_204_when_user_is_in_queue() throws Exception {
        // given
        when(queueService.leaveQueue(EVENT_ID, USER_ID)).thenReturn(Mono.just(USER_DELETED));

        // when & then
        performAsync(delete(BASE_URL + "/leave/{userId}", EVENT_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void leave_should_return_404_when_user_is_not_in_queue() throws Exception {
        // given
        when(queueService.leaveQueue(EVENT_ID, USER_ID)).thenReturn(Mono.just(USER_NOT_DELETED));

        // when & then
        performAsync(delete(BASE_URL + "/leave/{userId}", EVENT_ID, USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private ResultActions performAsync(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request)
                .andExpect(request().asyncStarted())
                .andReturn();

        return mockMvc.perform(asyncDispatch(result));
    }
}