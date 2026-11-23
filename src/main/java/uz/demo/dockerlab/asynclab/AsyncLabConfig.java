
package uz.demo.dockerlab.asynclab;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncLabConfig {
    @Bean("orderEmailExecutor")
    public ThreadPoolTaskExecutor orderEmailExecutor() {
        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(3);
        executor.setKeepAliveSeconds(10);

        executor.setThreadNamePrefix("order-email-");

        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.AbortPolicy()
        );

        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.setAwaitTerminationSeconds(15);

        executor.setTaskDecorator(new MdcTaskDecorator());

        return executor;
    }

    @Bean("reportExecutor")
    public ThreadPoolTaskExecutor reportExecutor() {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(5);

        executor.setThreadNamePrefix("report-worker-");

        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.AbortPolicy()
        );

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);

        executor.setTaskDecorator(new MdcTaskDecorator());

        return executor;
    }

    @Bean(
            name = "virtualExecutor",
            destroyMethod = "close"
    )
    public SimpleAsyncTaskExecutor virtualExecutor() {

        SimpleAsyncTaskExecutor executor =
                new SimpleAsyncTaskExecutor("virtual-worker-");

        executor.setVirtualThreads(true);

        executor.setTaskDecorator(
                new MdcTaskDecorator()
        );

        executor.setTaskTerminationTimeout(5000);

        return executor;
    }


}
