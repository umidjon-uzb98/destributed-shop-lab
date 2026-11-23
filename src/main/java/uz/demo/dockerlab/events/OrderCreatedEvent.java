package uz.demo.dockerlab.events;

public record OrderCreatedEvent(
        long orderId,
        String customerEmail,
        long amount,
        String requestId
) {}
