
package uz.demo.dockerlab.outboxlab;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OutboxOrderService543 {

    private final OutboxOrderRepository543 orders;
    private final OutboxEventRepository543 events;

    public OutboxOrderService543(
            OutboxOrderRepository543 orders,
            OutboxEventRepository543 events
    ) {
        this.orders = orders;
        this.events = events;
    }

    @Transactional
    public CreatedOrder create(
            String customerEmail,
            long amountUzs
    ) {
        if (customerEmail == null
                || customerEmail.isBlank()
                || customerEmail.length() > 200
                || amountUzs <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order request"
            );
        }

        // 1. Order saqlanadi
        OutboxOrder543 order = orders.saveAndFlush(
                new OutboxOrder543(customerEmail, amountUzs)
        );

        // 2. Kafka eventi DB ga saqlanadi
        OutboxEvent543 event = events.saveAndFlush(
                new OutboxEvent543(
                        order.getId(),
                        customerEmail,
                        amountUzs
                )
        );

        // 3. Method tugaganda transaction COMMIT
        return new CreatedOrder(
                order.getId(),
                event.getEventId()
        );
    }

    public record CreatedOrder(
            Long orderId,
            String eventId
    ) {}
}
