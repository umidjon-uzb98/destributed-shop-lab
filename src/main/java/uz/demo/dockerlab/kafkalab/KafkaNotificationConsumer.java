
package uz.demo.dockerlab.kafkalab;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KafkaNotificationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    KafkaNotificationConsumer.class
            );

    // Faqat transient failure simulation uchun
    private final Set<String> failedOnce =
            ConcurrentHashMap.newKeySet();

    @KafkaListener(
            topics = KafkaTopics.ORDERS_CREATED
    )
    public void handle(
            ConsumerRecord<String, OrderCreatedMessage> record
    ) {

        OrderCreatedMessage message = record.value();

        log.info(
                "KAFKA RECEIVED: eventId={}, orderId={}, partition={}, offset={}",
                message.eventId(),
                message.orderId(),
                record.partition(),
                record.offset()
        );

        String email = message.customerEmail();

        // 1. Noto'g'ri biznes ma'lumotlari
        if (email == null || email.isBlank()) {

            throw new IllegalArgumentException(
                    "Customer email is required"
            );
        }

        // 2. Permanent validation failure simulation
        if (email.equals("blocked@example.test")) {

            throw new IllegalArgumentException(
                    "Customer blocked by business policy"
            );
        }

        // 3. Temporary failure: birinchi urinish xato
        if (email.equals("retry-once@example.test")
                && failedOnce.add(message.eventId())) {

            throw new IllegalStateException(
                    "Simulated temporary email provider timeout"
            );
        }

        // 4. Poison message: doim exception
        if (email.equals("always-fail@example.test")) {

            throw new IllegalStateException(
                    "Simulated persistent email provider failure"
            );
        }

        // 5. Muvaffaqiyatli processing
        failedOnce.remove(message.eventId());

        log.info(
                "KAFKA NOTIFICATION SENT (SIMULATED): eventId={}, orderId={}, email={}",
                message.eventId(),
                message.orderId(),
                email
        );
    }
}
