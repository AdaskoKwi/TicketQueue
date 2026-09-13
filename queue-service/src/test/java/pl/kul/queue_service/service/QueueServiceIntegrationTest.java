package pl.kul.queue_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.Duration;

@SpringBootTest
@Testcontainers
public class QueueServiceIntegrationTest {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private QueueService queueService;
    @Autowired
    private ReactiveRedisConnectionFactory connectionFactory;

    private static final String EVENT_ID = "concert-2026";
    private static final String USER_ID = "user-1";
    private static final String USER_ID_2 = "user-2";
    private static final Long USER_LEFT_QUEUE = 1L;
    private static final Long USER_NOT_IN_QUEUE = -1L;

    @BeforeEach
    void setup() {
        connectionFactory.getReactiveConnection()
                .serverCommands()
                .flushDb()
                .block();
    }

    @Test
    public void join_two_users_should_have_incremented_positions() {
        StepVerifier.create(queueService.joinQueue(EVENT_ID, USER_ID))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(queueService.joinQueue(EVENT_ID, USER_ID_2))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    public void leaveQueue_user_should_be_deleted_from_redis_and_position_equal_to_minus_1L() {
        // given
        queueService.joinQueue(EVENT_ID, USER_ID).block();

        // when & then
        StepVerifier.create(queueService.leaveQueue(EVENT_ID, USER_ID))
                .expectNext(USER_LEFT_QUEUE)
                .verifyComplete();

        StepVerifier.create(queueService.getPosition(EVENT_ID, USER_ID))
                .expectNext(USER_NOT_IN_QUEUE)
                .verifyComplete();
    }

    @Test
    public void isRateLimited_should_return_true_after_limiting_added_user() {
        // given
        queueService.isRateLimited(EVENT_ID, USER_ID).block();

        // when & then
        StepVerifier.create(queueService.isRateLimited(EVENT_ID, USER_ID))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void isRateLimited_should_return_false_after_3_seconds() throws Exception {
        // given
        queueService.isRateLimited(EVENT_ID, USER_ID).block();

        Thread.sleep(Duration.ofMillis(3500));

        // when & then
        StepVerifier.create(queueService.isRateLimited(EVENT_ID, USER_ID))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    public void releaseNext_should_release_exactly_one_user() {
        // given
        queueService.joinQueue(EVENT_ID, USER_ID).block();
        queueService.joinQueue(EVENT_ID, USER_ID_2).block();

        // when && then
        StepVerifier.create(queueService.releaseNext(EVENT_ID, 1))
                .expectNext(USER_ID)
                .verifyComplete();
    }

    @Test
    public void releaseNext_should_release_exactly_two_users() {
        // given
        queueService.joinQueue(EVENT_ID, USER_ID).block();
        queueService.joinQueue(EVENT_ID, USER_ID_2).block();

        // when & then
        StepVerifier.create(queueService.releaseNext(EVENT_ID, 2))
                .expectNext(USER_ID)
                .expectNext(USER_ID_2)
                .verifyComplete();
    }
}
