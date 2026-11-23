
package uz.demo.dockerlab.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class EventLabOrderService {

    private static final Logger log =
            LoggerFactory.getLogger(EventLabOrderService.class);

    private final ApplicationEventPublisher publisher;

    private final AtomicLong sequence =
            new AtomicLong(1000);

    private final ConcurrentHashMap<Long, OrderView> orders =
            new ConcurrentHashMap<>();

    public EventLabOrderService(
            ApplicationEventPublisher publisher
    ) {
        this.publisher = publisher;
    }

    public OrderView createOrder(
            String customerEmail,
            long amount,
            String requestId
    ) {

        // 1. Order yaratamiz
        long orderId = sequence.incrementAndGet();

        OrderView order =
                new OrderView(orderId, "CREATED");

        // 2. Memory'ga saqlaymiz
        orders.put(orderId, order);

        log.info(
                "ORDER CREATED: id={}, thread={}",
                orderId,
                Thread.currentThread().getName()
        );

        // 3. Sodir bo'lgan event'ni yaratamiz
        OrderCreatedEvent event =
                new OrderCreatedEvent(
                        orderId,
                        customerEmail,
                        amount,
                        requestId
                );

        log.info(
                "BEFORE PUBLISH: orderId={}",
                orderId
        );

        // 4. Event e'lon qilinadi
        publisher.publishEvent(event);

        log.info(
                "AFTER PUBLISH: orderId={}",
                orderId
        );

        // 5. Natija qaytariladi
        return order;
    }

    public OrderView findOrder(long orderId) {

        OrderView order = orders.get(orderId);

        if (order == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Order not found"
            );
        }

        return order;
    }

    public record OrderView(
            long orderId,
            String status
    ) {}
}
