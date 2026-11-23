
package uz.demo.dockerlab.asynclab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class OrderLabService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderLabService.class);

    private final EmailSimulator emailSimulator;
    private final AsyncEmailService asyncEmailService;

    public OrderLabService(
            EmailSimulator emailSimulator,
            AsyncEmailService asyncEmailService
    ) {
        this.emailSimulator = emailSimulator;
        this.asyncEmailService = asyncEmailService;
    }

    public OrderResult createSync() {

        String orderId = simulateOrderCreation();

        emailSimulator.sendConfirmation(orderId);

        return new OrderResult(
                orderId,
                "SIMULATED_ORDER_CREATED",
                "SIMULATED_EMAIL_SENT"
        );
    }

    public OrderResult createAsync() {

        String orderId = simulateOrderCreation();

        try {
            asyncEmailService.sendConfirmation(orderId);

        } catch (TaskRejectedException e) {

            log.warn("EMAIL EXECUTOR REJECTED: orderId={}",
                    orderId);

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Email executor is busy"
            );
        }

        log.info(
                "ASYNC TASK ACCEPTED: orderId={}, thread={}",
                orderId,
                Thread.currentThread().getName()
        );

        return new OrderResult(
                orderId,
                "SIMULATED_ORDER_CREATED",
                "EMAIL_TASK_ACCEPTED"
        );
    }

    private String simulateOrderCreation() {

        try {
            Thread.sleep(100);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Order creation interrupted", e
            );
        }

        String orderId = UUID.randomUUID().toString();

        log.info(
                "ORDER CREATED: orderId={}, thread={}",
                orderId,
                Thread.currentThread().getName()
        );

        return orderId;
    }

    public record OrderResult(
            String orderId,
            String orderStatus,
            String emailStatus
    ) {}
}
