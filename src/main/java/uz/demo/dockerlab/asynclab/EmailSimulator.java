package uz.demo.dockerlab.asynclab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class EmailSimulator {

    private static final Logger log =
            LoggerFactory.getLogger(EmailSimulator.class);

    public void sendConfirmation(String orderId) {
        log.info(
                "THREAD TYPE: virtual={}",
                Thread.currentThread().isVirtual()
        );
        log.info(
                "EMAIL CONTEXT: requestId={}",
                MDC.get("requestId")
        );
        log.info(
                "EMAIL START: orderId={}, thread={}",
                orderId,
                Thread.currentThread().getName()
        );

        try {
            Thread.sleep(2000);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Email interrupted", e
            );
        }

        log.info(
                "EMAIL FINISHED: orderId={}, thread={}",
                orderId,
                Thread.currentThread().getName()
        );
    }
}
