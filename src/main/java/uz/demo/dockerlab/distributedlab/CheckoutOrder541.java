
package uz.demo.dockerlab.distributedlab;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "checkout_order_541")
public class CheckoutOrder541 {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "amount_uzs", nullable = false)
    private long amountUzs;

    @Column(nullable = false, length = 32)
    private String status;

    protected CheckoutOrder541() {
    }

    public CheckoutOrder541(UUID orderId, long amountUzs) {
        this.orderId = orderId;
        this.amountUzs = amountUzs;
        this.status = "PENDING";
    }

    public UUID getOrderId() {
        return orderId;
    }

    public long getAmountUzs() {
        return amountUzs;
    }

    public String getStatus() {
        return status;
    }
}
