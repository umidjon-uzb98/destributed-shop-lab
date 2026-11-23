
package uz.demo.dockerlab.integration.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes = PaymentWebLabController.class
)
public class PaymentExternalErrorHandler {

    @ExceptionHandler(PaymentExternalException.class)
    public ResponseEntity<ApiError> handle(
            PaymentExternalException error
    ) {

        HttpStatus status = switch (error.getKind()) {

            case NOT_FOUND ->
                    HttpStatus.NOT_FOUND;

            case INVALID_REQUEST ->
                    HttpStatus.BAD_REQUEST;

            case UNAVAILABLE ->
                    HttpStatus.SERVICE_UNAVAILABLE;

            case TIMEOUT ->
                    HttpStatus.GATEWAY_TIMEOUT;

            case UPSTREAM_REJECTED,
                 UPSTREAM_FAILURE,
                 INVALID_RESPONSE ->
                    HttpStatus.BAD_GATEWAY;
            case CIRCUIT_OPEN ->
                    HttpStatus.SERVICE_UNAVAILABLE;
            case BULKHEAD_FULL ->
                    HttpStatus.SERVICE_UNAVAILABLE;

            case RATE_LIMITED ->
                    HttpStatus.TOO_MANY_REQUESTS;
        };

        String message = switch (error.getKind()) {

            case NOT_FOUND ->
                    "Payment not found";

            case INVALID_REQUEST ->
                    "Invalid payment request";

            case UNAVAILABLE ->
                    "Payment service temporarily unavailable";

            case TIMEOUT ->
                    "Payment service did not respond in time";

            case UPSTREAM_REJECTED ->
                    "Payment service rejected the request";

            case UPSTREAM_FAILURE ->
                    "Payment service returned an error";

            case INVALID_RESPONSE ->
                    "Invalid response from payment service";
            case CIRCUIT_OPEN ->
                    "Payment service temporarily unavailable";
            case BULKHEAD_FULL ->
                    "Payment lookup capacity is temporarily full";

            case RATE_LIMITED ->
                    "Too many payment lookup requests";
        };

        return ResponseEntity
                .status(status)
                .body(new ApiError(
                        "PAYMENT_" + error.getKind().name(),
                        message,
                        error.getRequestId()
                ));
    }

    public record ApiError(
            String code,
            String message,
            String requestId
    ) {}
}
