package uz.demo.dockerlab.saga544;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_stock_544")
public class InventoryStock544 {
    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private int available;

    @Version
    @Column(nullable = false)
    private Long version;

    protected InventoryStock544() {}

    public Long getProductId() { return productId; }
    public int getAvailable() { return available; }

    public boolean reserveOne() {
        if (available <= 0) return false;
        available--;
        return true;
    }
}
