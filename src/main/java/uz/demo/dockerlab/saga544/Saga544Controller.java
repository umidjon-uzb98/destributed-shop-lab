package uz.demo.dockerlab.saga544;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uz.demo.dockerlab.kafkalab.OrderCreatedMessage;
import uz.demo.dockerlab.outboxlab.OutboxEventRepository543;
import uz.demo.dockerlab.outboxlab.OutboxOrderRepository543;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/saga544")
@ConditionalOnProperty(prefix = "lab.saga544", name = "enabled", havingValue = "true")
public class Saga544Controller {
    private final OutboxOrderRepository543 orders;
    private final InventoryStockRepository544 stocks;
    private final InventoryDecisionRepository544 decisions;
    private final OutboxEventRepository543 events;
    private final KafkaTemplate<String, OrderCreatedMessage> kafka;
    private final KafkaListenerEndpointRegistry registry;

    public Saga544Controller(OutboxOrderRepository543 orders,
                            InventoryStockRepository544 stocks,
                            InventoryDecisionRepository544 decisions,
                            OutboxEventRepository543 events,
                            KafkaTemplate<String, OrderCreatedMessage> kafka,
                            KafkaListenerEndpointRegistry registry) {
        this.orders = orders;
        this.stocks = stocks;
        this.decisions = decisions;
        this.events = events;
        this.kafka = kafka;
        this.registry = registry;
    }

    @GetMapping("/orders/{orderId}")
    public OrderState order(@PathVariable Long orderId) {
        var order = orders.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return new OrderState(order.getId(), order.getStatus(), order.getAmountUzs());
    }

    @GetMapping("/stock")
    public StockState stock() {
        var stock = stocks.findById(1L).orElseThrow();
        return new StockState(stock.getProductId(), stock.getAvailable());
    }

    @GetMapping("/decisions/{orderId}")
    public DecisionState decision(@PathVariable Long orderId) {
        var saved = decisions.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Decision not found"));
        return new DecisionState(saved.getOrderId(), saved.getEventId(), saved.getDecision());
    }

    @PostMapping("/consumer/pause")
    public ListenerState pause() {
        container().pause();
        return state();
    }

    @PostMapping("/consumer/resume")
    public ListenerState resume() {
        container().resume();
        return state();
    }

    @GetMapping("/consumer/state")
    public ListenerState state() {
        var c = container();
        return new ListenerState(c.isPauseRequested(), c.isContainerPaused());
    }

    // LAB ONLY: aynan oldingi outbox eventi yana Kafka'ga yoziladi.
    @PostMapping("/republish/{eventId}")
    public ReplayReceipt republish(@PathVariable String eventId) throws Exception {
        var event = events.findById(eventId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        var message = new OrderCreatedMessage(
                event.getEventId(), event.getOrderId(), event.getCustomerEmail(),
                event.getAmountUzs(), event.getOccurredAtEpochMs());
        var result = kafka.send("outbox.orders.created",
                String.valueOf(event.getOrderId()), message).get(10, TimeUnit.SECONDS);
        return new ReplayReceipt(eventId, result.getRecordMetadata().offset());
    }

    private MessageListenerContainer container() {
        var c = registry.getListenerContainer("saga544InventoryListener");
        if (c == null) throw new IllegalStateException("Saga 544 listener is not registered");
        return c;
    }

    public record OrderState(Long orderId, String status, long amountUzs) {}
    public record StockState(Long productId, int available) {}
    public record DecisionState(Long orderId, String eventId, String decision) {}
    public record ListenerState(boolean pauseRequested, boolean paused) {}
    public record ReplayReceipt(String eventId, long kafkaOffset) {}
}
