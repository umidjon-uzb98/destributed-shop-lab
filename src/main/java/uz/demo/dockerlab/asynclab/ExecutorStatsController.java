
package uz.demo.dockerlab.asynclab;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecutorStatsController {

    private final ThreadPoolTaskExecutor executor;

    public ExecutorStatsController(
            @Qualifier("orderEmailExecutor")
            ThreadPoolTaskExecutor executor
    ) {
        this.executor = executor;
    }

    @GetMapping("/api/async-lab/executor/stats")
    public PoolStats stats() {

        var pool = executor.getThreadPoolExecutor();

        return new PoolStats(
                pool.getCorePoolSize(),
                pool.getMaximumPoolSize(),
                pool.getPoolSize(),
                pool.getActiveCount(),
                pool.getQueue().size(),
                pool.getQueue().remainingCapacity(),
                pool.getCompletedTaskCount(),
                pool.getLargestPoolSize(),
                pool.isShutdown()
        );
    }

    public record PoolStats(
            int corePoolSize,
            int maximumPoolSize,
            int poolSize,
            int activeCount,
            int queueSize,
            int queueRemainingCapacity,
            long completedTasks,
            int largestPoolSize,
            boolean shutdown
    ) {}
}
