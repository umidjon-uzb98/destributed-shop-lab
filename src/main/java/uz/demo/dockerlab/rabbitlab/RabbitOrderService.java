
package uz.demo.dockerlab.rabbitlab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RabbitOrderService {

    private static final Logger log =
            LoggerFactory.getLogger(RabbitOrderService.class);

    private final RabbitTemplate rabbitTemplate;

    private final AtomicLong sequence =
            new AtomicLong(2000);

    private final ConcurrentHashMap<Long, OrderView> orders =
            new ConcurrentHashMap<>();

    public RabbitOrderService(
            RabbitTemplate rabbitTemplate
    ) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public OrderView createOrder(
            String customerEmail,
            long amount
    ) {

        // 1. Order yaratish
        long orderId = sequence.incrementAndGet();

        OrderView order = new OrderView(
                orderId,
                "CREATED"
        );

        // 2. Lab uchun memory'ga saqlash
        orders.put(orderId, order);

        log.info(
                "RABBIT ORDER CREATED: orderId={}",
                orderId
        );

        // 3. Message yaratish
        OrderCreatedMessage message =
                new OrderCreatedMessage(
                        UUID.randomUUID().toString(),
                        orderId,
                        customerEmail,
                        amount,
                        System.currentTimeMillis()
                );

        // 4. RabbitMQ'ga publish qilish
        log.info(
                "RABBIT PUBLISH START: eventId={}, orderId={}",
                message.eventId(),
                message.orderId()
        );

        rabbitTemplate.convertAndSend(
                RabbitTopologyConfig.EXCHANGE,
                RabbitTopologyConfig.ROUTING_KEY,
                message
        );

        log.info(
                "RABBIT PUBLISH INVOKED: eventId={}, orderId={}",
                message.eventId(),
                message.orderId()
        );

        // 5. HTTP layer'ga javob uchun natija
        return order;
    }

    public OrderView findOrder(long orderId) {
        return orders.get(orderId);
    }

    public record OrderView(
            long orderId,
            String status
    ) {}
}
