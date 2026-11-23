
package uz.demo.dockerlab.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EventLabAuditListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    EventLabAuditListener.class
            );

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {

        log.info(
                "AUDIT: orderCreated, orderId={}, amount={}",
                event.orderId(),
                event.amount()
        );
    }
}
