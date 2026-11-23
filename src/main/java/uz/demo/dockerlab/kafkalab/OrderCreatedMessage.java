package uz.demo.dockerlab.kafkalab;

public record OrderCreatedMessage(
        String eventId,
        long orderId,
        String customerEmail,
        long amount,
        long occurredAtEpochMs
) {}