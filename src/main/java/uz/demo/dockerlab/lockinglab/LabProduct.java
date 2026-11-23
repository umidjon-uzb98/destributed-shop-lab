
package uz.demo.dockerlab.lockinglab;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "optimistic_product_lab")
public class LabProduct {

    @Id
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int stock;

    @Version
    @Column(nullable = false)
    private Long version;

    protected LabProduct() {
    }

    public LabProduct(
            Long id,
            String name,
            int stock
    ) {
        this.id = id;
        this.name = name;
        this.stock = stock;
    }

    public void decreaseStock() {

        if (stock <= 0) {
            throw new IllegalStateException(
                    "Product out of stock"
            );
        }

        this.stock--;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
