package uz.demo.dockerlab.sagalab;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "saga_reservation_542")
public class SagaReservation542 {
    @Id @Column(name = "order_id")
    private UUID orderId;
    @Column(name = "product_id", nullable = false)
    private long productId;

    protected SagaReservation542() {}
    public SagaReservation542(UUID orderId, long productId) {
        this.orderId = orderId;
        this.productId = productId;
    }
    public long getProductId() { return productId; }
}
