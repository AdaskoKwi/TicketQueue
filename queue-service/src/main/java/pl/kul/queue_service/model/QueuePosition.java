package pl.kul.queue_service.model;

public record QueuePosition(
        String userId,
        long position,
        long totalInQueue
) {
}
