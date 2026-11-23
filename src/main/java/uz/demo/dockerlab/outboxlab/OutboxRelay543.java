
package uz.demo.dockerlab.outboxlab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uz.demo.dockerlab.kafkalab.OrderCreatedMessage;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(
        prefix = "lab.outbox",
        name = "enabled",
        havingValue = "true"
)
public class OutboxRelay543 {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxRelay543.class);

    private static final String TOPIC =
            "outbox.orders.created";

    private final OutboxEventRepository543 events;

    private final KafkaTemplate<String, OrderCreatedMessage> kafka;

    public OutboxRelay543(
            OutboxEventRepository543 events,
            KafkaTemplate<String, OrderCreatedMessage> kafka
    ) {
        this.events = events;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${lab.outbox.poll-ms:3000}")
    public void publishPending() {

        var pending =
                events.findTop10ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent543 event : pending) {

            try {

                // Outbox row dan Kafka event yasaymiz
                OrderCreatedMessage message =
                        new OrderCreatedMessage(
                                event.getEventId(),
                                event.getOrderId(),
                                event.getCustomerEmail(),
                                event.getAmountUzs(),
                                event.getOccurredAtEpochMs()
                        );

                // Kafka acknowledgment kutamiz
                kafka.send(
                        TOPIC,
                        String.valueOf(event.getOrderId()),
                        message
                ).get(10, TimeUnit.SECONDS);

                // Faqat ACK dan keyin SENT
                events.markSent(
                        event.getEventId(),
                        Instant.now()
                );

                log.info(
                        "OUTBOX SENT eventId={}",
                        event.getEventId()
                );

            } catch (InterruptedException ex) {

                Thread.currentThread().interrupt();

                log.warn("Outbox publisher interrupted", ex);
                return;

            } catch (Exception ex) {

                log.warn(
                        "OUTBOX RETRY LATER eventId={}",
                        event.getEventId(),
                        ex
                );

                // Keyingi pollingda yana urinadi
                return;
            }
        }
    }
}
