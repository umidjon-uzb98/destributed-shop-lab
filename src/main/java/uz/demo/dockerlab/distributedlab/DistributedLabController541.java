
package uz.demo.dockerlab.distributedlab;

import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/distributed-lab")
@ConditionalOnProperty(
        prefix = "lab.distributed",
        name = "enabled",
        havingValue = "true"
)
public class DistributedLabController541 {

    private final DistributedLabService541 service;

    public DistributedLabController541(
            DistributedLabService541 service
    ) {
        this.service = service;
    }

    @PostMapping("/orders")
    public ResponseEntity<DistributedLabService541.OrderView> create(
            @RequestBody CreateOrderRequest request
    ) {

        var result = service.createOrder(request.amountUzs());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result);
    }

    @GetMapping("/orders/{orderId}")
    public DistributedLabService541.OrderView getOrder(
            @PathVariable UUID orderId
    ) {
        return service.getOrder(orderId);
    }

    @PostMapping("/inventory/reserve")
    public DistributedLabService541.InventoryView reserve(
            @RequestBody ReserveRequest request
    ) {

        return service.reserve(
                request.orderId(),
                request.productId()
        );
    }

    @GetMapping("/inventory/stock/{productId}")
    public DistributedLabService541.InventoryView stock(
            @PathVariable Long productId
    ) {
        return service.getStock(productId);
    }

    public record CreateOrderRequest(
            long amountUzs
    ) {}

    public record ReserveRequest(
            UUID orderId,
            Long productId
    ) {}
}
