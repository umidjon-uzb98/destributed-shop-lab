package uz.demo.dockerlab.sagalab;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "saga_payment_542")
public class SagaPayment542 {
    public enum Status { CHARGED, REFUNDED }
    @Id @Column(name = "order_id")
    private UUID orderId;
    @Column(name = "amount_uzs", nullable = false)
    private long amountUzs;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status;

    protected SagaPayment542() {}
    public SagaPayment542(UUID orderId, long amountUzs) {
        this.orderId = orderId;
        this.amountUzs = amountUzs;
        this.status = Status.CHARGED;
    }
    public long getAmountUzs() { return amountUzs; }
    public Status getStatus() { return status; }
    public void markRefunded() {
        if (status != Status.CHARGED) throw new IllegalStateException("Payment already refunded");
        status = Status.REFUNDED;
    }
}
