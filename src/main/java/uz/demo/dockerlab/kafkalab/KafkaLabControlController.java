
package uz.demo.dockerlab.kafkalab;

import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;

import org.springframework.http.HttpStatus;

import org.springframework.kafka.config
        .KafkaListenerEndpointRegistry;

import org.springframework.kafka.listener
        .MessageListenerContainer;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/kafka-lab/lab")
@ConditionalOnProperty(
        prefix = "lab.kafka",
        name = "controls-enabled",
        havingValue = "true"
)
public class KafkaLabControlController {

    private static final String LISTENER_ID =
            "inventory535Consumer";

    private final KafkaListenerEndpointRegistry registry;

    public KafkaLabControlController(
            KafkaListenerEndpointRegistry registry
    ) {
        this.registry = registry;
    }

    private MessageListenerContainer inventoryContainer() {

        MessageListenerContainer container =
                registry.getListenerContainer(
                        LISTENER_ID
                );

        if (container == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Inventory Kafka listener not found"
            );
        }

        return container;
    }

    @GetMapping("/inventory/status")
    public ConsumerStatus status() {

        return new ConsumerStatus(
                LISTENER_ID,
                inventoryContainer().isRunning()
        );
    }

    @PostMapping("/inventory/stop")
    public ConsumerStatus stop() {

        var container = inventoryContainer();

        container.stop();

        return new ConsumerStatus(
                LISTENER_ID,
                container.isRunning()
        );
    }

    @PostMapping("/inventory/start")
    public ConsumerStatus start() {

        var container = inventoryContainer();

        container.start();

        return new ConsumerStatus(
                LISTENER_ID,
                container.isRunning()
        );
    }

    public record ConsumerStatus(
            String listenerId,
            boolean running
    ) {}
}
