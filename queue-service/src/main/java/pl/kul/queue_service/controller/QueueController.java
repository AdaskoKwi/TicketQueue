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

    @PostMapping("/join")
    public Mono<ResponseEntity<QueuePosition>> join(
            @PathVariable String eventId,
            @RequestBody @Valid JoinRequest request) {
        return queueService.joinQueue(eventId, request.userId())
                .zipWith(queueService.getQueueSize(eventId))
                .map(tuple -> new QueuePosition(request.userId(), tuple.getT1(), tuple.getT2()))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/position/{userId}")
    public Mono<ResponseEntity<QueuePosition>> position(
            @PathVariable String eventId,
            @PathVariable String userId) {
        return queueService.getPosition(eventId, userId)
                .zipWith(queueService.getQueueSize(eventId))
                .map(tuple -> new QueuePosition(userId, tuple.getT1(), tuple.getT2()))
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/leave/{userId}")
    public Mono<ResponseEntity<Void>> leave(
            @PathVariable String eventId,
            @PathVariable String userId) {
        return queueService.leaveQueue(eventId, userId)
                .map(removed -> ResponseEntity.noContent().build());
    }
}

record JoinRequest(@NotBlank String userId) {}
