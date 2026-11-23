package uz.demo.dockerlab.outboxlab;

import jakarta.persistence.*;

@Entity
@Table(name = "lab_order_543")
public class OutboxOrder543 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "amount_uzs", nullable = false)
    private long amountUzs;

    @Column(nullable = false, length = 20)
    private String status;

    protected OutboxOrder543() {}

    public OutboxOrder543(String customerEmail, long amountUzs) {
        this.customerEmail = customerEmail;
        this.amountUzs = amountUzs;
        this.status = "PENDING";
    }

    public Long getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public long getAmountUzs() { return amountUzs; }
    public String getStatus() { return status; }

    public void finishAs(String newStatus) {
        if (!"CONFIRMED".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
            throw new IllegalArgumentException("Invalid final order status");
        }
        if (!"PENDING".equals(status) && !newStatus.equals(status)) {
            throw new IllegalStateException("Conflicting order state: " + status);
        }
        this.status = newStatus;
    }
}
