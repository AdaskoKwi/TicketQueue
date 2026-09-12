package pl.kul.queue_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import pl.kul.queue_service.model.QueuePosition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {
    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;
    @Mock
    private ReactiveZSetOperations<String, String> zSetOps;
    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    @InjectMocks
    private QueueService queueService;

    private static final String EVENT_ID = "concert-2026";
    private static final String USER_ID = "user-1";
    private static final String USER_ID_2 = "user-2";

    @BeforeEach
    void setup() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    public void joinQueue_should_return_true_when_joining() {
        // given
        when(zSetOps.add(anyString(), eq(USER_ID), anyDouble())).thenReturn(Mono.just(true));
        when(zSetOps.rank(anyString(), eq(USER_ID))).thenReturn(Mono.just(0L));

        // when & then
        StepVerifier.create(queueService.joinQueue(EVENT_ID, USER_ID))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    public void leaveQueue_should_1L_when_user_was_in_queue() {
        // given
        when(zSetOps.remove(anyString(), eq(USER_ID))).thenReturn(Mono.just(1L));

        // when & then
        StepVerifier.create(queueService.leaveQueue(EVENT_ID, USER_ID))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    public void leaveQueue_should_return_1L_when_user_was_not_in_queue() {
        // given
        when(zSetOps.remove(anyString(), eq(USER_ID))).thenReturn(Mono.just(0L));

        // when & then
        StepVerifier.create(queueService.leaveQueue(EVENT_ID, USER_ID))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    public void getQueueSize_should_return_1L_with_one_user_in_queue() {
        // given
        when(zSetOps.size(anyString())).thenReturn(Mono.just(1L));

        // when & then
        StepVerifier.create(queueService.getQueueSize(EVENT_ID))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    public void getQueueSize_should_return_0L_with_empty_queue() {
        // given
        when(zSetOps.size(anyString())).thenReturn(Mono.just(0L));

        // when & then
        StepVerifier.create(queueService.getQueueSize(EVENT_ID))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    public void getPosition_should_return_1L_when_user_is_first_in_queue() {
        // given
        when(zSetOps.rank(anyString(), eq(USER_ID))).thenReturn(Mono.just(0L));

        // when & then
        StepVerifier.create(queueService.getPosition(EVENT_ID, USER_ID))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    public void getPosition_should_return_minus_1L_when_user_is_not_in_queue() {
        // given
        when(zSetOps.rank(anyString(), eq(USER_ID))).thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(queueService.getPosition(EVENT_ID, USER_ID))
                .expectNext(-1L)
                .verifyComplete();
    }

    @Test
    public void isRateLimited_should_return_false_when_user_is_not_limited() {
        // given
        when(valueOps.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(3)))).thenReturn(Mono.just(true));

        // when & then
        StepVerifier.create(queueService.isRateLimited(EVENT_ID, USER_ID))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    public void isRateLimited_should_return_true_when_user_is_limited() {
        // given
        when(valueOps.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(3)))).thenReturn(Mono.just(false));

        // when & then
        StepVerifier.create(queueService.isRateLimited(EVENT_ID, USER_ID))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void releaseNext_should_release_1_user() {
        // given
        int COUNT = 1;
        when(zSetOps.range(anyString(), eq(Range.closed(0L, (long) COUNT - 1)))).thenReturn(Flux.just(USER_ID));
        when(zSetOps.remove(anyString(), eq(USER_ID))).thenReturn(Mono.just(1L));

        // when & then
        StepVerifier.create(queueService.releaseNext(EVENT_ID, COUNT))
                .expectNext(USER_ID)
                .verifyComplete();
    }

    @Test
    public void releaseNext_should_release_2_users() {
        // given
        int COUNT = 2;
        when(zSetOps.range(anyString(), eq(Range.closed(0L, (long) COUNT - 1)))).thenReturn(Flux.just(USER_ID, USER_ID_2));

        when(zSetOps.remove(anyString(), eq(USER_ID))).thenReturn(Mono.just(1L));
        when(zSetOps.remove(anyString(), eq(USER_ID_2))).thenReturn(Mono.just(1L));

        // when & then
        StepVerifier.create(queueService.releaseNext(EVENT_ID, COUNT))
                .expectNext(USER_ID)
                .expectNext(USER_ID_2)
                .verifyComplete();
    }

    @Test
    public void releaseNext_should_not_release_when_queue_is_empty() {
        // given
        int COUNT = 2;
        when(zSetOps.range(anyString(), eq(Range.closed(0L, (long) COUNT - 1)))).thenReturn(Flux.empty());

        // when & then
        StepVerifier.create(queueService.releaseNext(EVENT_ID, COUNT))
                .expectComplete()
                .verify();
    }

    @Test
    public void streamPositions_should_stream_users_position_when_in_queue() {
        // given
        when(zSetOps.range(anyString(), eq(Range.unbounded()))).thenReturn(Flux.just(USER_ID));
        when(zSetOps.size(anyString())).thenReturn(Mono.just(1L));

        // when & then
        StepVerifier.create(queueService.streamPositions(EVENT_ID))
                .expectNext(new QueuePosition(USER_ID, 1, 1))
                .verifyComplete();
    }

    @Test
    public void streamPositions_should_stream_two_users_positions_when_they_are_in_queue() {
        // given
        when(zSetOps.range(anyString(), eq(Range.unbounded()))).thenReturn(Flux.just(USER_ID, USER_ID_2));
        when(zSetOps.size(anyString())).thenReturn(Mono.just(2L));

        // when & then
        StepVerifier.create(queueService.streamPositions(EVENT_ID))
                .expectNext(new QueuePosition(USER_ID, 1, 2))
                .expectNext(new QueuePosition(USER_ID_2, 2, 2))
                .verifyComplete();
    }

    @Test
    public void streamPositions_should_stream_nothing_when_queue_is_empty() {
        // given
        when(zSetOps.range(anyString(), eq(Range.unbounded()))).thenReturn(Flux.empty());
        when(zSetOps.size(anyString())).thenReturn(Mono.just(0L));

        // when & then
        StepVerifier.create(queueService.streamPositions(EVENT_ID))
                .expectComplete()
                .verify();
    }
}