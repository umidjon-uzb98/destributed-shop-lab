
package uz.demo.dockerlab.events;

import org.slf4j.MDC;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/event-lab/orders")
public class EventLabOrderController {

    private final EventLabOrderService orderService;

    public EventLabOrderController(
            EventLabOrderService orderService
    ) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<EventLabOrderService.OrderView> create(
            @RequestBody CreateOrderRequest request
    ) {

        if (request.customerEmail() == null
                || request.customerEmail().isBlank()
                || request.amount() == null
                || request.amount() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order request"
            );
        }

        String requestId = MDC.get("requestId");

        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        EventLabOrderService.OrderView result =
                orderService.createOrder(
                        request.customerEmail(),
                        request.amount(),
                        requestId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }

    @GetMapping("/{id}")
    public EventLabOrderService.OrderView find(
            @PathVariable long id
    ) {

        return orderService.findOrder(id);
    }

    public record CreateOrderRequest(
            String customerEmail,
            Long amount
    ) {}
}
