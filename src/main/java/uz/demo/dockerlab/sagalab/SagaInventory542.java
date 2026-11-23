package uz.demo.dockerlab.sagalab;

import jakarta.persistence.*;

@Entity
@Table(name = "saga_inventory_542")
public class SagaInventory542 {
    @Id @Column(name = "product_id")
    private Long productId;
    @Column(nullable = false)
    private int available;
    @Version private Long version;

    protected SagaInventory542() {}
    public int getAvailable() { return available; }
    public boolean reserveOne() {
        if (available <= 0) return false;
        available--;
        return true;
    }
}
