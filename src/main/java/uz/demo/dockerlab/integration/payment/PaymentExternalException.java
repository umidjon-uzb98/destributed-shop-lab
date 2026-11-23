
package uz.demo.dockerlab.integration.payment;

public class PaymentExternalException
        extends RuntimeException {

    public enum Kind {
        NOT_FOUND,
        INVALID_REQUEST,
        UPSTREAM_REJECTED,
        UPSTREAM_FAILURE,
        UNAVAILABLE,
        TIMEOUT,
        INVALID_RESPONSE,
        CIRCUIT_OPEN,
        BULKHEAD_FULL,
        RATE_LIMITED
    }

    private final Kind kind;
    private final String requestId;
    private final Integer upstreamStatus;

    public PaymentExternalException(
            Kind kind,
            String requestId,
            Integer upstreamStatus,
            Throwable cause
    ) {
        super(kind.name(), cause);

        this.kind = kind;
        this.requestId = requestId;
        this.upstreamStatus = upstreamStatus;
    }

    public Kind getKind() {
        return kind;
    }

    public String getRequestId() {
        return requestId;
    }

    public Integer getUpstreamStatus() {
        return upstreamStatus;
    }
}
