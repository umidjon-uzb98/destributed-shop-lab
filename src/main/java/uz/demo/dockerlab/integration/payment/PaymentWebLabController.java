
package uz.demo.dockerlab.integration.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/integration-lab/webclient/payments")
public class PaymentWebLabController {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PaymentWebLabController.class
            );

    private final PaymentWebClient client;
    private final PaymentLabMetrics metrics;


    public PaymentWebLabController(
            PaymentWebClient client, PaymentLabMetrics metrics
    ) {
        this.client = client;
        this.metrics = metrics;
    }

    @GetMapping("/{id}")
    public Mono<PaymentWebClient.PaymentResult> findPayment(
            @PathVariable String id
    ) {

        String requestId = currentRequestId();

        log.info(
                "CONTROLLER: creating Mono, id={}, thread={}",
                id,
                Thread.currentThread().getName()
        );

        Mono<PaymentWebClient.PaymentResult> result =
                client.findPayment(id, requestId);

        log.info(
                "CONTROLLER: returning Mono, id={}",
                id
        );

        return result;
    }

    @PostMapping
    public Mono<ResponseEntity<PaymentWebClient.PaymentResult>>
    createPayment(@RequestBody PaymentWebClient.PaymentRequest request) {

        if (request.orderId() == null
                || request.orderId().isBlank()
                || request.amount() == null
                || request.amount() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment request"
            );
        }

        String requestId = currentRequestId();

        return client.createPayment(request, requestId)

                .map(response ->
                        ResponseEntity
                                .status(response.getStatusCode())
                                .body(response.getBody())
                );
    }

    @GetMapping("/lookup/{id}")
    public Mono<PaymentLookupResult> lookupPayment(
            @PathVariable String id
    ) {

        String requestId = currentRequestId();

        return client.findPayment(id, requestId)

                .map(payment ->
                        new PaymentLookupResult(
                                payment.paymentId(),
                                payment.status(),
                                true,
                                "PAYMENT_SERVICE"
                        )
                )

                .onErrorResume(
                        PaymentExternalException.class,
                        error -> {

                            return switch (error.getKind()) {

                                case TIMEOUT,
                                     UNAVAILABLE,
                                     UPSTREAM_FAILURE,
                                     CIRCUIT_OPEN,
                                     BULKHEAD_FULL -> {

                                    metrics.event("fallback");

                                    log.warn(
                                            "PAYMENT FALLBACK: id={}, kind={}, requestId={}",
                                            id,
                                            error.getKind(),
                                            requestId
                                    );

                                    yield Mono.just(
                                            new PaymentLookupResult(
                                                    id,
                                                    "UNKNOWN",
                                                    false,
                                                    "FALLBACK"
                                            )
                                    );
                                }

                                default -> Mono.error(error);
                            };
                        }
                );
    }

    public record PaymentLookupResult(
            String paymentId,
            String status,
            boolean available,
            String source
    ) {
    }


    private String currentRequestId() {

        String requestId = MDC.get("requestId");

        return requestId != null
                ? requestId
                : UUID.randomUUID().toString();
    }
}
