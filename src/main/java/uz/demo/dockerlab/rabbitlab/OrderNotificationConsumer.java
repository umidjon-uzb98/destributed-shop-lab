
package uz.demo.dockerlab.rabbitlab;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;

import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class OrderNotificationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OrderNotificationConsumer.class
            );

    private static final int MAX_ATTEMPTS = 3;

    @RabbitListener(
            id = "notificationConsumer",
            queues = RabbitTopologyConfig.NOTIFICATION_QUEUE
    )
    public void onOrderCreated(
            @Payload OrderCreatedMessage message,

            @Header(AmqpHeaders.DELIVERY_TAG)
            long deliveryTag,

            @Header(
                    value = AmqpHeaders.REDELIVERED,
                    required = false
            )
            Boolean redelivered,

            Channel channel
    ) throws IOException {

        log.info(
                "RABBIT RECEIVED: eventId={}, orderId={}, tag={}, redelivered={}, thread={}",
                message.eventId(),
                message.orderId(),
                deliveryTag,
                redelivered,
                Thread.currentThread().getName()
        );

        // Invalid message: local retry foydasiz
        if (message.eventId() == null
                || message.eventId().isBlank()
                || message.customerEmail() == null
                || message.customerEmail().isBlank()) {

            log.warn(
                    "RABBIT INVALID MESSAGE: orderId={}",
                    message.orderId()
            );

            channel.basicNack(
                    deliveryTag,
                    false,
                    false
            );

            return;
        }

// Faqat laboratoriya uchun:
// birinchi delivery'da requeue qilamiz,
// redelivery'da esa normal processing davom etadi.
        if ("requeue-once@example.test".equals(
                message.customerEmail()
        ) && !Boolean.TRUE.equals(redelivered)) {

            log.warn(
                    "RABBIT REQUEUE ONCE: eventId={}",
                    message.eventId()
            );

            channel.basicNack(
                    deliveryTag,
                    false,
                    true
            );

            return;
        }

        try {

            // Biznes processing + bounded retry
            sendWithRetry(message);

        } catch (RuntimeException error) {

            log.error(
                    "RABBIT PROCESSING FAILED: eventId={}, orderId={}, attempts={}",
                    message.eventId(),
                    message.orderId(),
                    MAX_ATTEMPTS,
                    error
            );

            // DLX konfiguratsiyasi orqali DLQ'ga ketadi
            channel.basicNack(
                    deliveryTag,
                    false,
                    false
            );

            log.warn(
                    "RABBIT NACK: eventId={}, requeue=false",
                    message.eventId()
            );

            return;
        }

        // Faqat processing muvaffaqiyatli
        // tugagandan keyin ACK
        channel.basicAck(
                deliveryTag,
                false
        );

        log.info(
                "RABBIT ACK: eventId={}, orderId={}",
                message.eventId(),
                message.orderId()
        );
    }

    private void sendWithRetry(OrderCreatedMessage message) {

        for (int attempt = 1;
             attempt <= MAX_ATTEMPTS;
             attempt++) {

            try {

                log.info(
                        "NOTIFICATION ATTEMPT: eventId={}, attempt={}",
                        message.eventId(),
                        attempt
                );

                simulateEmail(message, attempt);

                // Success bo'lsa retry to'xtaydi
                return;

            } catch (IllegalStateException error) {

                if (attempt == MAX_ATTEMPTS) {
                    throw error;
                }

                long backoffMs = 200L * attempt;

                log.warn(
                        "NOTIFICATION RETRY: eventId={}, attempt={}, backoffMs={}",
                        message.eventId(),
                        attempt,
                        backoffMs
                );

                sleep(backoffMs);
            }
        }
    }

    private void simulateEmail(
            OrderCreatedMessage message,
            int attempt
    ) {

        String email = message.customerEmail();

        // Transient failure simulation:
        // faqat birinchi urinishda xato
        if (email.equals("retry-once@example.test")
                && attempt == 1) {

            throw new IllegalStateException(
                    "Simulated transient failure"
            );
        }

        // Poison message simulation:
        // har bir urinishda xato
        if (email.equals("always-fail@example.test")) {

            throw new IllegalStateException(
                    "Simulated permanent failure"
            );
        }

        // Consumer crash test uchun
        if (email.equals("slow@example.test")) {
            sleep(15000);
        } else {
            sleep(300);
        }

        // Haqiqiy email yuborilmaydi
        log.info(
                "NOTIFICATION SENT (SIMULATED): eventId={}, orderId={}, attempt={}",
                message.eventId(),
                message.orderId(),
                attempt
        );
    }

    private void sleep(long millis) {

        try {
            TimeUnit.MILLISECONDS.sleep(millis);

        } catch (InterruptedException error) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Notification processing interrupted",
                    error
            );
        }
    }
}
