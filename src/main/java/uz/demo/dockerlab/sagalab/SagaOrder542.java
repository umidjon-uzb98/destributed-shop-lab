package uz.demo.dockerlab.sagalab;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "saga_order_542")
public class SagaOrder542 {
    public enum Status { PENDING, PAID, CONFIRMED, COMPENSATING, REFUND_PENDING, CANCELLED }

    @Id @Column(name = "order_id")
    private UUID orderId;
    @Column(name = "product_id", nullable = false)
    private long productId;
    @Column(name = "amount_uzs", nullable = false)
    private long amountUzs;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Version
    private Long version;

    protected SagaOrder542() {}
    public SagaOrder542(UUID orderId, long productId, long amountUzs) {
        this.orderId = orderId;
        this.productId = productId;
        this.amountUzs = amountUzs;
        this.status = Status.PENDING;
    }
    public UUID getOrderId() { return orderId; }
    public long getProductId() { return productId; }
    public long getAmountUzs() { return amountUzs; }
    public Status getStatus() { return status; }

    public void moveTo(Status next) {
        if (status == next) return;
        boolean allowed = switch (status) {
            case PENDING -> next == Status.PAID || next == Status.CANCELLED;
            case PAID -> next == Status.CONFIRMED || next == Status.COMPENSATING;
            case COMPENSATING -> next == Status.REFUND_PENDING || next == Status.CANCELLED;
            case REFUND_PENDING -> next == Status.CANCELLED;
            case CONFIRMED, CANCELLED -> false;
        };
        if (!allowed) throw new IllegalStateException("Invalid Saga transition: " + status + " -> " + next);
        status = next;
    }
}
