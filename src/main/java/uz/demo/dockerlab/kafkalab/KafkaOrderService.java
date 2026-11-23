
package uz.demo.dockerlab.kafkalab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KafkaOrderService {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaOrderService.class);

    private final KafkaTemplate<String, OrderCreatedMessage>
            kafkaTemplate;

    private final AtomicLong sequence =
            new AtomicLong(3000);

    private final ConcurrentHashMap<Long, OrderView> orders =
            new ConcurrentHashMap<>();

    public KafkaOrderService(
            KafkaTemplate<String, OrderCreatedMessage>
                    kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CreateOrderResult createOrder(
            String email,
            long amount
    ) {

        // 1. Order yaratamiz
        long orderId = sequence.incrementAndGet();

        OrderView order = new OrderView(
                orderId,
                "CREATED"
        );

        // Faqat laboratoriya uchun in-memory storage
        orders.put(orderId, order);

        // 2. Kafka uchun event contract
        OrderCreatedMessage message =
                new OrderCreatedMessage(
                        UUID.randomUUID().toString(),
                        orderId,
                        email,
                        amount,
                        System.currentTimeMillis()
                );

        // 3. Record key
        String key = String.valueOf(orderId);

        log.info(
                "KAFKA PUBLISH START: eventId={}, orderId={}",
                message.eventId(),
                orderId
        );

        try {

            // 4. Kafka'ga record yuboramiz
            SendResult<String, OrderCreatedMessage> result =
                    kafkaTemplate.send(
                            KafkaTopics.ORDERS_CREATED,
                            key,
                            message
                    ).get(10, TimeUnit.SECONDS);

            // 5. Broker acknowledgment metadata
            var metadata = result.getRecordMetadata();

            log.info(
                    "KAFKA PUBLISH ACK: eventId={}, topic={}, partition={}, offset={}",
                    message.eventId(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset()
            );

            return new CreateOrderResult(
                    orderId,
                    "CREATED",
                    metadata.partition(),
                    metadata.offset()
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Kafka publish interrupted",
                    e
            );

        } catch (ExecutionException | TimeoutException e) {

            throw new IllegalStateException(
                    "Kafka publish failed or timed out",
                    e
            );
        }
    }

    public OrderView findOrder(long orderId) {
        return orders.get(orderId);
    }

    public record OrderView(
            long orderId,
            String status
    ) {}

    public record CreateOrderResult(
            long orderId,
            String status,
            int partition,
            long offset
    ) {}
}
