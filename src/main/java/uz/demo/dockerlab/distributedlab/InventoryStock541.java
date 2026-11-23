
package uz.demo.dockerlab.distributedlab;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_stock_541")
public class InventoryStock541 {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private int available;

    @Version
    @Column(nullable = false)
    private Long version;

    protected InventoryStock541() {
    }

    public Long getProductId() {
        return productId;
    }

    public int getAvailable() {
        return available;
    }

    public void reserveOne() {

        if (available <= 0) {
            throw new IllegalStateException(
                    "OUT_OF_STOCK"
            );
        }

        available--;
    }
}
