package pl.kul.queue_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.kul.queue_service.model.QueuePosition;
import pl.kul.queue_service.service.QueueService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/events/{eventId}/queue")
@RequiredArgsConstructor
public class QueueController {
    private final QueueService queueService;
    private final static Long CHECKED_POSITION_NOT_IN_QUEUE = -1L;
    private final static Long USER_NOT_DELETED = 0L;

    @PostMapping("/join")
    public Mono<ResponseEntity<?>> join(
            @PathVariable String eventId,
            @RequestBody @Valid JoinRequest request) {
        return queueService.isRateLimited(eventId, request.userId())
                .flatMap(limited -> {
                    if (limited) {
                        return Mono.just(ResponseEntity.status(429).body("The user is rate limited."));
                    } else {
                        return queueService.joinQueue(eventId, request.userId())
                                .zipWith(queueService.getQueueSize(eventId))
                                .map(tuple -> new QueuePosition(request.userId(), tuple.getT1(), tuple.getT2()))
                                .map(ResponseEntity::ok);
                    }
                });
    }

    @GetMapping("/position/{userId}")
    public Mono<ResponseEntity<?>> position(
            @PathVariable String eventId,
            @PathVariable String userId) {
        return queueService.getPosition(eventId, userId)
                .flatMap(position -> {
                    if (position.equals(CHECKED_POSITION_NOT_IN_QUEUE)) {
                        return Mono.just(ResponseEntity.status(404).body("User " + userId + " is not in queue for " + eventId));
                    } else {
                        return Mono.just(position)
                                .zipWith(queueService.getQueueSize(eventId))
                                .map(tuple -> new QueuePosition(userId, tuple.getT1(), tuple.getT2()))
                                .map(ResponseEntity::ok);
                    }
                });
    }

    @DeleteMapping("/leave/{userId}")
    public Mono<ResponseEntity<?>> leave(
            @PathVariable String eventId,
            @PathVariable String userId) {
        return queueService.leaveQueue(eventId, userId)
                .flatMap(deleted -> {
                    if (deleted.equals(USER_NOT_DELETED)) {
                        return Mono.just(ResponseEntity.status(404).body("User " + userId + " is not in queue for " + eventId));
                    } else {
                        return Mono.just(deleted).map(removed -> ResponseEntity.noContent().build());
                    }
                });
    }
}

record JoinRequest(@NotBlank String userId) {}
