
package uz.demo.dockerlab.analyticslab;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "order_analytics_summary_540")
public class OrderAnalyticsSummary {

    @Id
    private Long id;

    @Column(name = "processed_orders", nullable = false)
    private long processedOrders;

    @Column(name = "total_amount_uzs", nullable = false)
    private long totalAmountUzs;

    protected OrderAnalyticsSummary() {
    }

    public void recordOrder(long amountUzs) {

        if (amountUzs <= 0) {
            throw new IllegalArgumentException(
                    "Order amount must be positive"
            );
        }

        this.processedOrders =
                Math.addExact(processedOrders, 1L);

        this.totalAmountUzs =
                Math.addExact(totalAmountUzs, amountUzs);
    }
}
