
package uz.demo.dockerlab.outboxlab;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outbox-lab")
@ConditionalOnProperty(
        prefix = "lab.outbox",
        name = "enabled",
        havingValue = "true"
)
public class OutboxController543 {

    private final OutboxOrderService543 service;
    private final OutboxEventRepository543 events;

    public OutboxController543(
            OutboxOrderService543 service,
            OutboxEventRepository543 events
    ) {
        this.service = service;
        this.events = events;
    }

    @PostMapping("/orders")
    public ResponseEntity<OutboxOrderService543.CreatedOrder> create(
            @RequestBody CreateRequest request
    ) {

        var result = service.create(
                request.customerEmail(),
                request.amountUzs()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }

    @GetMapping("/events/{eventId}")
    public EventStatus status(@PathVariable String eventId) {

        var event = events.findById(eventId)
                .orElseThrow();

        return new EventStatus(
                event.getEventId(),
                event.getStatus()
        );
    }

    public record CreateRequest(
            String customerEmail,
            long amountUzs
    ) {}

    public record EventStatus(
            String eventId,
            String status
    ) {}
}
