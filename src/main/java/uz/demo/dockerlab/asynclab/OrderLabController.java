
package uz.demo.dockerlab.asynclab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/async-lab/orders")
public class OrderLabController {

    private static final Logger log =
            LoggerFactory.getLogger(OrderLabController.class);

    private final OrderLabService service;

    public OrderLabController(OrderLabService service) {
        this.service = service;
    }

    @PostMapping("/sync")
    public OrderLabService.OrderResult createSync() {

        long start = System.nanoTime();

        var result = service.createSync();

        log.info(
                "SYNC CONTROLLER RETURN: durationMs={}, thread={}",
                (System.nanoTime() - start) / 1_000_000,
                Thread.currentThread().getName()
        );

        return result;
    }

    @PostMapping("/async")
    public OrderLabService.OrderResult createAsync() {

        long start = System.nanoTime();

        var result = service.createAsync();

        log.info(
                "ASYNC CONTROLLER RETURN: durationMs={}, thread={}",
                (System.nanoTime() - start) / 1_000_000,
                Thread.currentThread().getName()
        );

        return result;
    }
}
