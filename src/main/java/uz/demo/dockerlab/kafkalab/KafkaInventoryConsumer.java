
package uz.demo.dockerlab.kafkalab;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaInventoryConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    KafkaInventoryConsumer.class
            );

    @KafkaListener(
            id = "inventory535Consumer",
            topics = KafkaTopics.ORDERS_CREATED,
            groupId = "inventory-535-lab",
            clientIdPrefix = "inventory-535",
            concurrency = "${lab.kafka.inventory.concurrency:1}"
    )
    public void reserveStock(
            ConsumerRecord<String, OrderCreatedMessage> record
    ) {

        OrderCreatedMessage message = record.value();

        log.info(
                "INVENTORY RECEIVED: eventId={}, orderId={}, key={}, partition={}, offset={}, thread={}",
                message.eventId(),
                message.orderId(),
                record.key(),
                record.partition(),
                record.offset(),
                Thread.currentThread().getName()
        );

        // Laboratoriya: real database update qilmaymiz
        log.info(
                "INVENTORY RESERVED (SIMULATED): orderId={}, amount={}",
                message.orderId(),
                message.amount()
        );
    }
}
