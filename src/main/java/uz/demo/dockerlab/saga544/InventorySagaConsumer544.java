package uz.demo.dockerlab.saga544;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import uz.demo.dockerlab.kafkalab.OrderCreatedMessage;

@Component
@ConditionalOnProperty(prefix = "lab.saga544", name = "enabled", havingValue = "true")
public class InventorySagaConsumer544 {
    private static final Logger log = LoggerFactory.getLogger(InventorySagaConsumer544.class);
    private final InventorySagaStep544 steps;

    public InventorySagaConsumer544(InventorySagaStep544 steps) {
        this.steps = steps;
    }

    @KafkaListener(
            id = "saga544InventoryListener",
            topics = "outbox.orders.created",
            groupId = "saga544-inventory-lab",
            concurrency = "1")
    public void listen(ConsumerRecord<String, OrderCreatedMessage> record) {
        var event = record.value();
        var result = steps.reserve(event);     // TX-A commit
        steps.finishOrder(event, result);       // TX-B commit

        log.info("SAGA544 eventId={} orderId={} decision={} duplicate={} offset={}",
                event.eventId(), event.orderId(), result.decision(),
                result.duplicate(), record.offset());
    }
}
