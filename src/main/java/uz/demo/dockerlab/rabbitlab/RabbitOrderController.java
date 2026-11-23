
package uz.demo.dockerlab.rabbitlab;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rabbit-lab/orders")
public class RabbitOrderController {

    private final RabbitOrderService orderService;

    public RabbitOrderController(
            RabbitOrderService orderService
    ) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<RabbitOrderService.OrderView> create(
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

        RabbitOrderService.OrderView result =
                orderService.createOrder(
                        request.customerEmail(),
                        request.amount()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }

    @GetMapping("/{id}")
    public RabbitOrderService.OrderView find(
            @PathVariable long id
    ) {

        RabbitOrderService.OrderView order =
                orderService.findOrder(id);

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
