package uz.demo.dockerlab.sagalab;

import jakarta.persistence.*;

@Entity
@Table(name = "saga_wallet_542")
public class SagaWallet542 {
    @Id private Long id;
    @Column(name = "balance_uzs", nullable = false)
    private long balanceUzs;
    @Version private Long version;

    protected SagaWallet542() {}
    public long getBalanceUzs() { return balanceUzs; }
    public boolean debit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (balanceUzs < amount) return false;
        balanceUzs -= amount;
        return true;
    }
    public void refund(long amount) { balanceUzs = Math.addExact(balanceUzs, amount); }
}
