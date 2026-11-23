
package uz.demo.dockerlab.analyticslab;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import uz.demo.dockerlab.kafkalab.OrderCreatedMessage;

@Component
@ConditionalOnProperty(
        prefix = "lab.kafka",
        name = "idempotent-enabled",
        havingValue = "true"
)
public class OrderAnalyticsConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(OrderAnalyticsConsumer.class);

    private final OrderAnalyticsProcessor processor;

    public OrderAnalyticsConsumer(
            OrderAnalyticsProcessor processor
    ) {
        this.processor = processor;
    }

    @KafkaListener(
            id = "analytics540Listener",
            topics = "orders.created",
            groupId = "order-analytics-540-lab",
            concurrency = "1"
    )
    public void listen(
            ConsumerRecord<String, OrderCreatedMessage> record
    ) {

        OrderCreatedMessage event = record.value();

        log.info(
                "ANALYTICS RECEIVED: eventId={}, orderId={}, partition={}, offset={}",
                event.eventId(),
                event.orderId(),
                record.partition(),
                record.offset()
        );

        OrderAnalyticsProcessor.ProcessingOutcome result =
                processor.process(event);

        log.info(
                "ANALYTICS RESULT: eventId={}, result={}",
                event.eventId(),
                result
        );

        // FAQAT LAB: DB commit bo'lgach,
        // offset commitdan oldingi failure'ni simulate qilamiz.
        if (result == OrderAnalyticsProcessor.ProcessingOutcome.APPLIED
                && "simulate-after-db-commit@example.test"
                .equals(event.customerEmail())) {

            throw new IllegalStateException(
                    "LAB: simulated failure after DB commit"
            );
        }
    }
}
