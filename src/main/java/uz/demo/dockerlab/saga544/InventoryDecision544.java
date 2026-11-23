package uz.demo.dockerlab.saga544;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_decision_544")
public class InventoryDecision544 {
    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(nullable = false, length = 16)
    private String decision;

    protected InventoryDecision544() {}

    public InventoryDecision544(Long orderId, String eventId, String decision) {
        this.orderId = orderId;
        this.eventId = eventId;
        this.decision = decision;
    }

    public Long getOrderId() { return orderId; }
    public String getEventId() { return eventId; }
    public String getDecision() { return decision; }
}
