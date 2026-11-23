package uz.demo.dockerlab.sagalab;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;

@RestController
@RequestMapping("/api/saga-lab")
@ConditionalOnProperty(prefix = "lab.saga", name = "enabled", havingValue = "true")
public class SagaController542 {
    private final CheckoutSagaOrchestrator542 orchestrator;
    private final SagaSteps542 steps;

    public SagaController542(CheckoutSagaOrchestrator542 orchestrator, SagaSteps542 steps) {
        this.orchestrator = orchestrator;
        this.steps = steps;
    }

    @PostMapping("/checkout")
    public SagaSteps542.SagaView542 checkout(@RequestBody CheckoutRequest request) {
        if (request == null || request.orderId() == null || request.productId() == null
                || request.amountUzs() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing fields");
        try {
            return orchestrator.checkout(request.orderId(), request.productId(),
                    request.amountUzs(), Boolean.TRUE.equals(request.simulateRefundFailure()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/orders/{id}")
    public SagaSteps542.SagaView542 get(@PathVariable UUID id) {
        return steps.state(id);
    }

    @PostMapping("/orders/{id}/recover")
    public SagaSteps542.SagaView542 recover(@PathVariable UUID id) {
        try {
            return orchestrator.recover(id);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }

    public record CheckoutRequest(UUID orderId, Long productId, Long amountUzs,
                                  Boolean simulateRefundFailure) {}
}
