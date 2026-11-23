
package uz.demo.dockerlab.kafkalab;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/kafka-lab/orders")
public class KafkaOrderController {

    private final KafkaOrderService orderService;

    public KafkaOrderController(
            KafkaOrderService orderService
    ) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<KafkaOrderService.CreateOrderResult> create(
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

        var result = orderService.createOrder(
                request.customerEmail(),
                request.amount()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }

    @GetMapping("/{id}")
    public KafkaOrderService.OrderView find(
            @PathVariable long id
    ) {

        var order = orderService.findOrder(id);

        if (order == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Order not found"
            );
        }

        return order;
    }

    public record CreateOrderRequest(
            String customerEmail,
            Long amount
    ) {}
}
