
package uz.demo.dockerlab.lockinglab;

import jakarta.persistence.OptimisticLockException;

import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;

import org.springframework.dao.OptimisticLockingFailureException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/locking-lab")
@ConditionalOnProperty(
        prefix = "lab.locking",
        name = "enabled",
        havingValue = "true"
)
public class OptimisticLockLabController {

    private final OptimisticLockLabService labService;
    private final StockTransactionWorker worker;

    public OptimisticLockLabController(
            OptimisticLockLabService labService,
            StockTransactionWorker worker
    ) {
        this.labService = labService;
        this.worker = worker;
    }

    @PostMapping("/reset")
    public OptimisticLockLabService.ProductState reset(
            @RequestParam(defaultValue = "10") int stock
    ) {

        if (stock < 0 || stock > 100000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid stock"
            );
        }

        return labService.reset(stock);
    }

    @GetMapping("/product")
    public OptimisticLockLabService.ProductState product() {
        return labService.currentState();
    }

    @PostMapping("/decrease")
    public StockTransactionWorker.AttemptInfo decrease() {
        return worker.decrease(1L, null);
    }

    @PostMapping("/race")
    public OptimisticLockLabService.RaceResult race() {
        return labService.runRace();
    }

    @ExceptionHandler({
            OptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<Map<String, String>> conflict(
            Exception exception
    ) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "OPTIMISTIC_LOCK_CONFLICT",
                        "message",
                        "Product was modified by another transaction"
                ));
    }
}
