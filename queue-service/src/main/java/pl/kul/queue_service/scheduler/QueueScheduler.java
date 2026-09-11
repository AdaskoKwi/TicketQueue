package pl.kul.queue_service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.kul.queue_service.service.QueueService;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueueScheduler {
    private final QueueService queueService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int SLOTS_PER_TICK = 1;
    private static final String EVENT_ID = "concert-2026";

    @Scheduled(fixedRate = 5000)
    public void releaseSlots() {
        queueService.releaseNext(EVENT_ID, SLOTS_PER_TICK)
                .doOnNext(userId -> log.info("User {} got a slot, removed from queue", userId))
                .subscribe();
    }

    @Scheduled(fixedRate = 2000)
    public void streamPositions() {
        queueService.streamPositions(EVENT_ID)
                .doOnNext(position -> messagingTemplate.convertAndSend(
                        "/topic/queue/" + EVENT_ID + "/" + position.userId(),
                        position)
                )
                .doOnNext(position -> log.info(
                        "User {} got his position sent to him",
                        position.userId())
                )
                .subscribe();
    }
}
