
package uz.demo.dockerlab.outboxlab;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event_543")
public class OutboxEvent543 {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "amount_uzs", nullable = false)
    private long amountUzs;

    @Column(name = "occurred_at_epoch_ms", nullable = false)
    private long occurredAtEpochMs;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected OutboxEvent543() {
    }

    public OutboxEvent543(
            Long orderId,
            String customerEmail,
            long amountUzs
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.amountUzs = amountUzs;
        this.occurredAtEpochMs = System.currentTimeMillis();
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public String getEventId() { return eventId; }
    public Long getOrderId() { return orderId; }
    public String getCustomerEmail() { return customerEmail; }
    public long getAmountUzs() { return amountUzs; }
    public long getOccurredAtEpochMs() { return occurredAtEpochMs; }
    public String getStatus() { return status; }
}
