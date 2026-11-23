
package uz.demo.dockerlab.analyticslab;

import org.apache.kafka.clients.producer.RecordMetadata;

import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;

import org.springframework.http.HttpStatus;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import uz.demo.dockerlab.kafkalab.OrderCreatedMessage;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/analytics-lab")
@ConditionalOnProperty(
        prefix = "lab.kafka",
        name = "idempotent-enabled",
        havingValue = "true"
)
public class AnalyticsLabController {

    private final KafkaTemplate<String, OrderCreatedMessage> kafkaTemplate;
    private final OrderAnalyticsProcessor processor;

    public AnalyticsLabController(
            KafkaTemplate<String, OrderCreatedMessage> kafkaTemplate,
            OrderAnalyticsProcessor processor
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.processor = processor;
    }

    @PostMapping("/publish")
    public PublishedEvent publish(
            @RequestBody OrderCreatedMessage event
    ) throws Exception {

        if (event == null
                || event.eventId() == null
                || event.eventId().isBlank()
                || event.orderId() <= 0
                || event.amount() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid event"
            );
        }

        SendResult<String, OrderCreatedMessage> result =
                kafkaTemplate.send(
                        "orders.created",
                        String.valueOf(event.orderId()),
                        event
                ).get(10, TimeUnit.SECONDS);

        RecordMetadata metadata =
                result.getRecordMetadata();

        return new PublishedEvent(
                event.eventId(),
                metadata.topic(),
                metadata.partition(),
                metadata.offset()
        );
    }

    @GetMapping("/stats")
    public OrderAnalyticsProcessor.AnalyticsStats stats() {
        return processor.getStats();
    }

    public record PublishedEvent(
            String eventId,
            String topic,
            int partition,
            long offset
    ) {}
}
