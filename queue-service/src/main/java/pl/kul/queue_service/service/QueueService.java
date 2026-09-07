package pl.kul.queue_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class QueueService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private String queueKey(String eventId) {
        return "queue:" + eventId;
    }

    public Mono<Long> joinQueue(String eventId, String userId) {
        double score = System.currentTimeMillis();
        return redisTemplate.opsForZSet()
                .add(queueKey(eventId), userId, score)
                .then(getPosition(eventId, userId));
    }

    public Mono<Long> getPosition(String eventId, String userId) {
        return redisTemplate.opsForZSet()
                .rank(queueKey(eventId), userId)
                .map(rank -> rank + 1)
                .defaultIfEmpty(-1L);
    }

    public Mono<Long> leaveQueue(String eventId, String userId) {
        return redisTemplate.opsForZSet()
                .remove(queueKey(eventId), userId);
    }

    public Mono<Long> getQueueSize(String eventId) {
        return redisTemplate.opsForZSet()
                .size(queueKey(eventId));
    }
}
