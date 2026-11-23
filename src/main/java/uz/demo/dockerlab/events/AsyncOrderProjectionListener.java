
package uz.demo.dockerlab.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class AsyncOrderProjectionListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    AsyncOrderProjectionListener.class
            );

    private final ConcurrentHashMap<Long, ProjectionView> views =
            new ConcurrentHashMap<>();

    @Async("orderProjectionExecutor")
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {

        log.info(
                "PROJECTION START: orderId={}, thread={}, requestId={}",
                event.orderId(),
                Thread.currentThread().getName(),
                event.requestId()
        );

        try {
            // Ataylab sekin processing
            TimeUnit.SECONDS.sleep(5);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            log.warn(
                    "PROJECTION INTERRUPTED: orderId={}",
                    event.orderId()
            );

            return;
        }

        ProjectionView projection = new ProjectionView(
                event.orderId(),
                event.amount(),
                "READY"
        );

        views.put(
                event.orderId(),
                projection
        );

        log.info(
                "PROJECTION READY: orderId={}, thread={}",
                event.orderId(),
                Thread.currentThread().getName()
        );
    }

    public ProjectionView find(long orderId) {

        return views.getOrDefault(
                orderId,
                new ProjectionView(
                        orderId,
                        null,
                        "PENDING"
                )
        );
    }

    public record ProjectionView(
            long orderId,
            Long amount,
            String state
    ) {}
}
