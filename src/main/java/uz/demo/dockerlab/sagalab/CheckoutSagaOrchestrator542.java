package uz.demo.dockerlab.sagalab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class CheckoutSagaOrchestrator542 {
    private static final Logger log = LoggerFactory.getLogger(CheckoutSagaOrchestrator542.class);
    private final SagaSteps542 steps;

    public CheckoutSagaOrchestrator542(SagaSteps542 steps) { this.steps = steps; }

    // Intentionally NO @Transactional here: each step has its own local commit.
    public SagaSteps542.SagaView542 checkout(UUID id, long productId,
                                              long amountUzs, boolean simulateRefundFailure) {
        if (id == null || productId != 1L || amountUzs <= 0 || amountUzs > 1_000_000L)
            throw new IllegalArgumentException("Invalid checkout request");

        steps.ensureOrder(id, productId, amountUzs);
        var state = steps.state(id);
        var status = SagaOrder542.Status.valueOf(state.orderStatus());

        if (status == SagaOrder542.Status.CONFIRMED || status == SagaOrder542.Status.CANCELLED)
            return state;
        if (status == SagaOrder542.Status.COMPENSATING || status == SagaOrder542.Status.REFUND_PENDING)
            return compensate(id, simulateRefundFailure);

        if (status == SagaOrder542.Status.PENDING) {
            boolean paid = steps.charge(id, amountUzs);
            if (!paid) {
                steps.move(id, SagaOrder542.Status.CANCELLED);
                return steps.state(id);
            }
            steps.move(id, SagaOrder542.Status.PAID);
        }

        // If a crash occurred after reserve commit but before CONFIRMED,
        // reserve() sees saga_reservation_542 and returns true without double-decrement.
        if (steps.reserve(id, productId)) {
            steps.move(id, SagaOrder542.Status.CONFIRMED);
            return steps.state(id);
        }

        // Confirmed OUT_OF_STOCK -> compensation, not a generic timeout.
        steps.move(id, SagaOrder542.Status.COMPENSATING);
        return compensate(id, simulateRefundFailure);
    }

    public SagaSteps542.SagaView542 recover(UUID id) {
        var state = steps.state(id);
        var status = SagaOrder542.Status.valueOf(state.orderStatus());
        if (status == SagaOrder542.Status.CANCELLED) return state; // repeat recovery is safe
        if (status != SagaOrder542.Status.REFUND_PENDING && status != SagaOrder542.Status.COMPENSATING)
            throw new IllegalStateException("Saga does not need refund recovery");
        return compensate(id, false);
    }

    private SagaSteps542.SagaView542 compensate(UUID id, boolean failRefund) {
        try {
            steps.refund(id, failRefund);
            steps.move(id, SagaOrder542.Status.CANCELLED);
        } catch (RuntimeException ex) {
            log.warn("Saga refund pending: orderId={}", id, ex);
            // This status transition is a separate transaction. If DB is unavailable,
            // even this may fail; a durable recovery worker is needed in production.
            steps.move(id, SagaOrder542.Status.REFUND_PENDING);
        }
        return steps.state(id);
    }
}
