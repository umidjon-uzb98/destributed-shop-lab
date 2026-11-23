package uz.demo.dockerlab.rabbitlab;

public record OrderCreatedMessage(
        String eventId,
        long orderId,
        String customerEmail,
        long amount,
        long occurredAtEpochMs
) {}