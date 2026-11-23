
package uz.demo.dockerlab.integration.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PaymentClient {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentClient.class);

    private final RestClient restClient;

    public PaymentClient(
            @Qualifier("paymentRestClient")
            RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public PaymentResult findPayment(String id) {
        long start = System.nanoTime();
        log.info(
                "PAYMENT REQUEST START: id={}, thread={}",
                id,
                Thread.currentThread().getName()
        );

        try {

            return restClient.get()
                    .uri("/api/payments/{id}", id)
                    .retrieve()
                    .body(PaymentResult.class);

        } finally {

            log.info(
                    "PAYMENT REQUEST END: id={}, durationMs={}, thread={}",
                    id,
                    (System.nanoTime() - start) / 1_000_000,
                    Thread.currentThread().getName()
            );
        }
    }

    public record PaymentResult(
            String paymentId,
            String status
    ) {}
}
