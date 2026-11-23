
package uz.demo.dockerlab.lockinglab.benchmark;

import jakarta.persistence.OptimisticLockException;

import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
@RequestMapping("/api/locking-benchmark")
@ConditionalOnProperty(
        prefix = "lab.locking",
        name = "enabled",
        havingValue = "true"
)
public class BenchmarkController {

    private final BenchmarkStockService service;

    public BenchmarkController(BenchmarkStockService service) {
        this.service = service;
    }

    @PostMapping("/reset")
    public BenchmarkStockService.StockState reset() {
        return service.reset();
    }

    @GetMapping("/stock")
    public BenchmarkStockService.StockState stock() {
        return service.currentStock();
    }

    @PostMapping("/buy/{strategy}")
    public ResponseEntity<BenchmarkStockService.BuyResult> buy(
            @PathVariable String strategy
    ) {

        String mode = strategy.toLowerCase(Locale.ROOT);

        try {

            BenchmarkStockService.BuyResult result =
                    switch (mode) {

                        case "unsafe" -> service.buyUnsafe();

                        case "optimistic" -> service.buyOptimistic();

                        case "pessimistic" -> service.buyPessimistic();

                        default -> throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Unknown strategy"
                        );
                    };

            if ("SOLD_OUT".equals(result.status())) {

                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(result);
            }

            return ResponseEntity.ok(result);

        } catch (
                OptimisticLockingFailureException |
                OptimisticLockException ex
        ) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(
                            new BenchmarkStockService.BuyResult(
                                    mode,
                                    "CONFLICT",
                                    null
                            )
                    );
        }
    }
}
