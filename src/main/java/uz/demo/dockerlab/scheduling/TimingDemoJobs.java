
package uz.demo.dockerlab.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "lab.jobs.timing-demo-enabled",
        havingValue = "true"
)
public class TimingDemoJobs {

    private static final Logger log =
            LoggerFactory.getLogger(TimingDemoJobs.class);

    @Scheduled(
            initialDelay = 3000,
            fixedRate = 5000
    )
    public void fixedRateJob() {

        runJob("FIXED RATE");
    }

    @Scheduled(
            initialDelay = 3000,
            fixedDelay = 5000
    )
    public void fixedDelayJob() {

        runJob("FIXED DELAY");
    }

    private void runJob(String name) {

        long start = System.nanoTime();

        log.info(
                "{} START: thread={}",
                name,
                Thread.currentThread().getName()
        );

        try {
            Thread.sleep(2000);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        log.info(
                "{} FINISH: durationMs={}",
                name,
                (System.nanoTime() - start) / 1_000_000
        );
    }
}
