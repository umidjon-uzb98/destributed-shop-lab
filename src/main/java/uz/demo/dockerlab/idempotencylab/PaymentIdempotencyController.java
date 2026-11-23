
package uz.demo.dockerlab.idempotencylab;

import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/idempotency-lab")
@ConditionalOnProperty(
        prefix = "lab.idempotency",
        name = "enabled",
        havingValue = "true"
)
public class PaymentIdempotencyController {

    private final PaymentIdempotencyService service;

    public PaymentIdempotencyController(
            PaymentIdempotencyService service
    ) {
        this.service = service;
    }

    @PostMapping("/payments")
    public ResponseEntity<String> pay(
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody PaymentRequest request
    ) {

        var result = service.pay(key, request);

        return ResponseEntity
                .status(result.httpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                        "Idempotency-Replayed",
                        String.valueOf(result.replayed())
                )
                .header("Cache-Control", "no-store")
                .body(result.body());
    }

    @GetMapping("/state")
    public PaymentIdempotencyService.WalletState state() {
        return service.state();
    }

    @PostMapping("/reset")
    public PaymentIdempotencyService.WalletState reset() {
        return service.reset();
    }
}
