
package uz.demo.dockerlab.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OrderNotificationListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OrderNotificationListener.class
            );

    private final boolean simulateFailure;

    public OrderNotificationListener(
            @Value("${lab.events.fail-notification:false}")
            boolean simulateFailure
    ) {
        this.simulateFailure = simulateFailure;
    }

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {

        log.info(
                "NOTIFICATION START: orderId={}, thread={}, requestId={}",
                event.orderId(),
                Thread.currentThread().getName(),
                event.requestId()
        );

        // Lab uchun 700 ms ishlov berish vaqti
        try {
            TimeUnit.MILLISECONDS.sleep(700);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Notification processing interrupted",
                    e
            );
        }

        // Keyinroq failure simulation qilamiz
        if (simulateFailure) {
            throw new IllegalStateException(
                    "Simulated notification failure"
            );
        }

        // Haqiqiy email yuborilmaydi!
        log.info(
                "NOTIFICATION SENT (SIMULATED): orderId={}",
                event.orderId()
        );
    }
}
