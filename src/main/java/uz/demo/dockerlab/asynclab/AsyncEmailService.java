
package uz.demo.dockerlab.asynclab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncEmailService {

    private static final Logger log =
            LoggerFactory.getLogger(AsyncEmailService.class);

    private final EmailSimulator emailSimulator;

    public AsyncEmailService(
            EmailSimulator emailSimulator
    ) {
        this.emailSimulator = emailSimulator;
    }

    @Async("orderEmailExecutor")
    public void sendConfirmation(String orderId) {

        try {
            emailSimulator.sendConfirmation(orderId);

        } catch (RuntimeException e) {

            log.error(
                    "ASYNC EMAIL FAILED: orderId={}",
                    orderId,
                    e
            );

            throw e;
        }
    }
}
