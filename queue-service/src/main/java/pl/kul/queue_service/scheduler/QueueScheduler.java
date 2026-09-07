package pl.kul.queue_service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.kul.queue_service.service.QueueService;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueueScheduler {
    private final QueueService queueService;
    private static final int SLOTS_PER_TICK = 5;

    @Scheduled(fixedRate = 5000)
    public void releaseSlots() {
        queueService.releaseNext("concert-2026", SLOTS_PER_TICK)
                .doOnNext(userId -> log.info("User {} got a slot, removed from queue", userId))
                .subscribe();
    }
}
