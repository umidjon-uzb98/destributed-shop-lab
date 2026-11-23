
package uz.demo.dockerlab.integration.payment;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static uz.demo.dockerlab.integration.payment
        .PaymentExternalException.Kind.*;

@Component("paymentWebClientGateway")
public class PaymentWebClient {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentWebClient.class);

    private final WebClient webClient;
    private final CircuitBreaker paymentLookupBreaker;

    private final Bulkhead paymentLookupBulkhead;
    private final RateLimiter paymentLookupRateLimiter;
    private final TimeLimiter paymentLookupTimeLimiter;
    private final PaymentLabMetrics metrics;

    public PaymentWebClient(
            @Qualifier("paymentWebClient")
            WebClient webClient,

            @Qualifier("paymentLookupBreaker")
            CircuitBreaker paymentLookupBreaker,

            @Qualifier("paymentLookupBulkhead")
            Bulkhead paymentLookupBulkhead,

            @Qualifier("paymentLookupRateLimiter")
            RateLimiter paymentLookupRateLimiter,

            @Qualifier("paymentLookupTimeLimiter")
            TimeLimiter paymentLookupTimeLimiter,

            PaymentLabMetrics metrics
    ) {
        this.webClient = webClient;
        this.paymentLookupBreaker = paymentLookupBreaker;
        this.paymentLookupBulkhead = paymentLookupBulkhead;
        this.paymentLookupRateLimiter = paymentLookupRateLimiter;
        this.paymentLookupTimeLimiter = paymentLookupTimeLimiter;
        this.metrics = metrics;
    }




    public Mono<PaymentResult> findPayment(
            String paymentId,
            String requestId
    ) {

        Mono<PaymentResult> call = webClient.get()
                .uri("/api/payments/{id}", paymentId)
                .header("X-Request-ID", requestId)
                .retrieve()
                .bodyToMono(PaymentResult.class)

                .switchIfEmpty(Mono.error(
                        new PaymentExternalException(
                                INVALID_RESPONSE,
                                requestId,
                                null,
                                null
                        )
                ))

                .map(result ->
                        requireResult(result, requestId)
                );

        Mono<PaymentResult> pipeline = protect(call, requestId, true)

                .doOnSubscribe(subscription -> {
                    metrics.event("attempt");

                    log.info(
                            "PAYMENT ATTEMPT: id={}, requestId={}, thread={}",
                            paymentId,
                            requestId,
                            Thread.currentThread().getName()
                    );
                })

                // 517-dars: Retry
                .retryWhen(
                        lookupRetryPolicy(
                                paymentId,
                                requestId
                        )
                )

                // 519-dars: Overall deadline
                .transformDeferred(
                        TimeLimiterOperator.of(
                                paymentLookupTimeLimiter
                        )
                )

                .onErrorMap(
                        TimeoutException.class,
                        error -> new PaymentExternalException(
                                TIMEOUT,
                                requestId,
                                null,
                                error
                        )
                )

                // 518-dars: Circuit Breaker
                .transformDeferred(
                        CircuitBreakerOperator.of(
                                paymentLookupBreaker
                        )
                )

                .onErrorMap(
                        CallNotPermittedException.class,
                        error -> new PaymentExternalException(
                                CIRCUIT_OPEN,
                                requestId,
                                null,
                                error
                        )
                )

                // 519-dars: Concurrency limit
                .transformDeferred(
                        BulkheadOperator.of(
                                paymentLookupBulkhead
                        )
                )

                // 519-dars: Rate limit
                .transformDeferred(
                        RateLimiterOperator.of(
                                paymentLookupRateLimiter
                        )
                )
                // Local capacity errors
                .onErrorMap(
                        BulkheadFullException.class,
                        error -> new PaymentExternalException(
                                BULKHEAD_FULL,
                                requestId,
                                null,
                                error
                        )
                )
                .onErrorMap(
                        RequestNotPermitted.class,
                        error -> new PaymentExternalException(
                                RATE_LIMITED,
                                requestId,
                                null,
                                error
                        )
                )
                .doOnNext(result ->
                        log.info(
                                "PAYMENT GET SUCCESS: id={}, requestId={}",
                                result.paymentId(),
                                requestId
                        )
                )
                .doOnError(
                        PaymentExternalException.class,
                        error -> log.warn(
                                "PAYMENT FINAL FAILURE: id={}, kind={}, requestId={}",
                                paymentId,
                                error.getKind(),
                                requestId
                        )
                );


        return Mono.defer(() -> {
            long started = System.nanoTime();
            metrics.event("started");
            return pipeline
                    .doOnSuccess(result ->
                            metrics.event("success")
                    )
                    .doOnError(error ->
                            metrics.event("failure")
                    )
                    .doFinally(signal -> {

                        if (signal == reactor.core.publisher
                                .SignalType.CANCEL) {

                            metrics.event("cancelled");
                        }

                        metrics.recordDuration(started);
                    });
        });

    }




    public Mono<ResponseEntity<PaymentResult>> createPayment(
            PaymentRequest request,
            String requestId
    ) {

        Mono<ResponseEntity<PaymentResult>> call =
                webClient.post()
                        .uri("/api/payments")
                        .header("X-Request-ID", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .toEntity(PaymentResult.class)

                        .map(response -> {

                            requireResult(
                                    response.getBody(),
                                    requestId
                            );

                            return response;
                        });

        return protect(call, requestId, false);
    }

    private <T> Mono<T> protect(
            Mono<T> call,
            String requestId,
            boolean lookup
    ) {

        return call
                // Overall operation timeout
                .timeout(Duration.ofSeconds(3))

                // External exceptions -> application exception
                .onErrorMap(
                        this::isExternalError,
                        error -> translate(error,requestId,lookup)
                )
                .doOnError(
                        PaymentExternalException.class,
                        error -> log.warn(
                                "PAYMENT FAILURE: kind={}, upstreamStatus={}, requestId={}",
                                error.getKind(),
                                error.getUpstreamStatus(),
                                requestId
                        )
                );
    }

    private boolean isExternalError(Throwable error) {

        return error instanceof WebClientRequestException
                || error instanceof WebClientResponseException
                || error instanceof CodecException
                || error instanceof DataBufferLimitException
                || error instanceof IOException
                || isTimeout(error);
    }

    private PaymentExternalException translate(
            Throwable error,
            String requestId,
            boolean lookup
    ) {

        Integer upstreamStatus = null;

        if (error instanceof WebClientResponseException e) {
            upstreamStatus = e.getStatusCode().value();
        }

        // 1. Timeout may be wrapped in another exception
        if (isTimeout(error)) {

            return new PaymentExternalException(
                    TIMEOUT,
                    requestId,
                    upstreamStatus,
                    error
            );
        }

        // 2. HTTP response errors
        if (upstreamStatus != null) {

            PaymentExternalException.Kind kind;

            if (upstreamStatus == 404 && lookup) {

                kind = NOT_FOUND;

            } else if (
                    (upstreamStatus == 400
                            || upstreamStatus == 422)
                            && !lookup
            ) {

                // Lab POST contract: invalid caller input
                kind = INVALID_REQUEST;

            } else if (upstreamStatus == 429) {

                kind = UNAVAILABLE;

            } else if (upstreamStatus >= 500
                    && upstreamStatus < 600) {

                kind = UPSTREAM_FAILURE;

            } else if (upstreamStatus >= 400
                    && upstreamStatus < 500) {

                kind = UPSTREAM_REJECTED;

            } else {

                kind = INVALID_RESPONSE;
            }

            return new PaymentExternalException(
                    kind,
                    requestId,
                    upstreamStatus,
                    error
            );
        }

        // 3. Response decoding and size errors
        if (error instanceof CodecException
                || error instanceof DataBufferLimitException) {

            return new PaymentExternalException(
                    INVALID_RESPONSE,
                    requestId,
                    null,
                    error
            );
        }

        // 4. Network / connection errors
        return new PaymentExternalException(
                UNAVAILABLE,
                requestId,
                null,
                error
        );
    }

    private boolean isTimeout(Throwable error) {

        Throwable current = error;

        for (int depth = 0;
             current != null && depth < 16;
             depth++) {

            if (current instanceof TimeoutException
                    || current instanceof ReadTimeoutException
                    || current instanceof ConnectTimeoutException
                    || current instanceof SocketTimeoutException) {

                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private PaymentResult requireResult(
            PaymentResult result,
            String requestId
    ) {

        if (result == null
                || result.paymentId() == null
                || result.paymentId().isBlank()
                || result.status() == null
                || result.status().isBlank()) {

            throw new PaymentExternalException(
                    INVALID_RESPONSE,
                    requestId,
                    null,
                    null
            );
        }

        return result;
    }

    public record PaymentRequest(
            String orderId,
            Long amount
    ) {}

    public record PaymentResult(
            String paymentId,
            String status
    ) {}


    private boolean isRetryableLookup(Throwable error) {

        if (!(error instanceof PaymentExternalException e)) {
            return false;
        }

        Integer status = e.getUpstreamStatus();

        return switch (e.getKind()) {

            case TIMEOUT -> true;

            case UPSTREAM_FAILURE ->
                    status != null
                            && (status == 502
                            || status == 503
                            || status == 504);

            case UNAVAILABLE ->
                    status == null
                            && hasCause(error, ConnectException.class);

            default -> false;
        };
    }

    private boolean hasCause(
            Throwable error,
            Class<? extends Throwable> type
    ) {

        Throwable current = error;

        for (int depth = 0;
             current != null && depth < 16;
             depth++) {

            if (type.isInstance(current)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private RetryBackoffSpec lookupRetryPolicy(
            String paymentId,
            String requestId
    ) {

        return Retry.backoff(
                        2,
                        Duration.ofMillis(200)
                )

                .maxBackoff(Duration.ofSeconds(1))

                .jitter(0.5)

                .filter(this::isRetryableLookup)

                .doBeforeRetry(signal -> {
                    metrics.event("retry");
                    PaymentExternalException error =
                            (PaymentExternalException)
                                    signal.failure();

                    log.warn(
                            "PAYMENT RETRY: id={}, nextAttempt={}, kind={}, upstreamStatus={}, requestId={}",
                            paymentId,
                            signal.totalRetries() + 2,
                            error.getKind(),
                            error.getUpstreamStatus(),
                            requestId
                    );
                })

                .onRetryExhaustedThrow(
                        (spec, signal) -> {

                            log.warn(
                                    "PAYMENT RETRY EXHAUSTED: id={}, requestId={}",
                                    paymentId,
                                    requestId
                            );

                            return signal.failure();
                        }
                );
    }

}
