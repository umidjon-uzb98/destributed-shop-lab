package uz.demo.dockerlab.sagalab;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class SagaSteps542 {
    private final SagaOrderRepository542 orders;
    private final SagaPaymentRepository542 payments;
    private final SagaWalletRepository542 wallets;
    private final SagaInventoryRepository542 inventories;
    private final SagaReservationRepository542 reservations;

    public SagaSteps542(SagaOrderRepository542 orders, SagaPaymentRepository542 payments,
                        SagaWalletRepository542 wallets, SagaInventoryRepository542 inventories,
                        SagaReservationRepository542 reservations) {
        this.orders = orders;
        this.payments = payments;
        this.wallets = wallets;
        this.inventories = inventories;
        this.reservations = reservations;
    }

    // T1: standalone local transaction; the client supplies stable orderId.
    @Transactional
    public void ensureOrder(UUID orderId, long productId, long amountUzs) {
        var existing = orders.findById(orderId);
        if (existing.isPresent()) {
            var o = existing.get();
            if (o.getProductId() != productId || o.getAmountUzs() != amountUzs)
                throw new IllegalArgumentException("orderId reused with different request");
            return;
        }
        orders.saveAndFlush(new SagaOrder542(orderId, productId, amountUzs));
    }

    // T2: charge + payment marker are atomically committed in this lab database.
    @Transactional
    public boolean charge(UUID orderId, long amountUzs) {
        SagaWallet542 wallet = wallets.findForUpdate(1L).orElseThrow();
        var existing = payments.findById(orderId);
        if (existing.isPresent()) {
            var p = existing.get();
            if (p.getAmountUzs() != amountUzs || p.getStatus() == SagaPayment542.Status.REFUNDED)
                throw new IllegalStateException("Conflicting or already refunded payment");
            return true; // already CHARGED, no second debit
        }
        if (!wallet.debit(amountUzs)) return false; // confirmed insufficient funds
        payments.saveAndFlush(new SagaPayment542(orderId, amountUzs));
        wallets.flush();
        return true;
    }

    // T3: one reservation per order. Locking serializes concurrent reservations for the same product.
    @Transactional
    public boolean reserve(UUID orderId, long productId) {
        SagaInventory542 stock = inventories.findForUpdate(productId).orElseThrow();
        var old = reservations.findById(orderId);
        if (old.isPresent()) {
            if (old.get().getProductId() != productId)
                throw new IllegalStateException("Reservation reused for another product");
            return true; // already reserved, no second decrement
        }
        if (!stock.reserveOne()) return false; // confirmed OUT_OF_STOCK
        reservations.saveAndFlush(new SagaReservation542(orderId, productId));
        inventories.flush();
        return true;
    }

    // C2: refund is a NEW transaction, not a rollback of T2.
    @Transactional
    public void refund(UUID orderId, boolean simulateRefundFailure) {
        SagaWallet542 wallet = wallets.findForUpdate(1L).orElseThrow();
        SagaPayment542 payment = payments.findById(orderId).orElseThrow();
        if (payment.getStatus() == SagaPayment542.Status.REFUNDED) return;
        if (simulateRefundFailure)
            throw new IllegalStateException("LAB: Refund provider unavailable");
        wallet.refund(payment.getAmountUzs());
        payment.markRefunded();
        payments.flush();
        wallets.flush();
    }

    // Independent transaction for a visible persisted saga state transition.
    @Transactional
    public void move(UUID orderId, SagaOrder542.Status next) {
        SagaOrder542 order = orders.findById(orderId).orElseThrow();
        order.moveTo(next);
        orders.saveAndFlush(order);
    }

    @Transactional(readOnly = true)
    public SagaView542 state(UUID orderId) {
        SagaOrder542 order = orders.findById(orderId).orElseThrow();
        String payment = payments.findById(orderId)
                .map(p -> p.getStatus().name()).orElse("NOT_CHARGED");
        boolean reserved = reservations.existsById(orderId);
        long balance = wallets.findById(1L).orElseThrow().getBalanceUzs();
        int stock = inventories.findById(order.getProductId()).orElseThrow().getAvailable();
        return new SagaView542(orderId, order.getStatus().name(), payment,
                reserved, balance, stock);
    }

    public record SagaView542(UUID orderId, String orderStatus, String paymentStatus,
                              boolean inventoryReserved, long walletBalanceUzs,
                              int availableStock) {}
}
