package uz.demo.dockerlab.saga544;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.demo.dockerlab.kafkalab.OrderCreatedMessage;
import uz.demo.dockerlab.outboxlab.OutboxOrder543;
import uz.demo.dockerlab.outboxlab.OutboxOrderRepository543;

import java.util.Objects;

@Service
public class InventorySagaStep544 {
    private final OutboxOrderRepository543 orders;
    private final InventoryStockRepository544 stocks;
    private final InventoryDecisionRepository544 decisions;

    public InventorySagaStep544(
            OutboxOrderRepository543 orders,
            InventoryStockRepository544 stocks,
            InventoryDecisionRepository544 decisions) {
        this.orders = orders;
        this.stocks = stocks;
        this.decisions = decisions;
    }

    // TX-A: reservation + idempotency decision atomik commit qilinadi.
    @Transactional
    public StepOutcome reserve(OrderCreatedMessage event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("Event ID is required");
        }

        OutboxOrder543 order = orders.findById(event.orderId()).orElseThrow();
        if (order.getAmountUzs() != event.amount()
                || !Objects.equals(order.getCustomerEmail(), event.customerEmail())) {
            throw new IllegalArgumentException("Event does not match order");
        }

        // Bitta product: stock row lock duplicate va turli orderlarni serialize qiladi.
        InventoryStock544 stock = stocks.findForUpdate(1L).orElseThrow();
        var previous = decisions.findById(event.orderId());

        if (previous.isPresent()) {
            InventoryDecision544 saved = previous.get();
            if (!saved.getEventId().equals(event.eventId())) {
                throw new IllegalArgumentException("Order reused with another event ID");
            }
            return new StepOutcome(saved.getDecision(), true);
        }

        boolean reserved = stock.reserveOne();
        String decision = reserved ? "RESERVED" : "REJECTED";
        decisions.saveAndFlush(new InventoryDecision544(
                event.orderId(), event.eventId(), decision));
        // JPA dirty checking stock UPDATE'ni TX-A commitda yozadi.
        return new StepOutcome(decision, false);
    }

    // TX-B: order yakuniy holatiga o'tadi; TX-A'dan MUSTAQIL transaction.
    @Transactional
    public void finishOrder(OrderCreatedMessage event, StepOutcome step) {
        OutboxOrder543 order = orders.findById(event.orderId()).orElseThrow();
        String desired = "RESERVED".equals(step.decision())
                ? "CONFIRMED" : "CANCELLED";
        order.finishAs(desired);
        orders.saveAndFlush(order);
    }

    public record StepOutcome(String decision, boolean duplicate) {}
}
