
package uz.demo.dockerlab.lockinglab.benchmark;

import jakarta.persistence.*;

@Entity
@Table(name = "locking_benchmark_product")
public class BenchmarkProduct {

    @Id
    private Long id;

    @Column(nullable = false)
    private int stock;

    @Version
    @Column(nullable = false)
    private Long version;

    protected BenchmarkProduct() {
    }

    public Long getId() {
        return id;
    }

    public int getStock() {
        return stock;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isSoldOut() {
        return stock <= 0;
    }

    public void decreaseStock() {
        if (isSoldOut()) {
            throw new IllegalStateException(
                    "Product out of stock"
            );
        }

        stock--;
    }
}
