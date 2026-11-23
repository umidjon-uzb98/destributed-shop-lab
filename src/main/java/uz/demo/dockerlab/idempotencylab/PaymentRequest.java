package uz.demo.dockerlab.idempotencylab;

public record PaymentRequest(
        Long walletId,
        Long amountUzs
) {}