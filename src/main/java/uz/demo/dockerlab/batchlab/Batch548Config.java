package uz.demo.dockerlab.batchlab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
@ConditionalOnProperty(
        prefix = "lab.batch",
        name = "enabled",
        havingValue = "true"
)
public class Batch548Config {

    private static final Logger log =
            LoggerFactory.getLogger(Batch548Config.class);

    // 1. ItemReader
    @Bean
    public ItemReader<String> productNameReader() {

        return new ListItemReader<>(
                List.of(
                        "  laptop ",
                        "mouse",
                        " keyboard ",
                        "monitor",
                        "  printer",
                        "webcam ",
                        " headset "
                )
        );
    }

    // 2. ItemProcessor
    @Bean
    public ItemProcessor<String, String> productNameProcessor() {
        return item -> item.trim().toUpperCase();
    }

    // 3. ItemWriter
    @Bean
    public ItemWriter<String> productNameWriter() {
        return chunk -> {

            log.info(
                    "Writing chunk with {} items",
                    chunk.size()
            );

            for (String item : chunk) {
                log.info("Processed product: {}", item);
            }
        };
    }

    // 4. Step
    @Bean
    public Step productNameStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<String> productNameReader,
            ItemProcessor<String, String> productNameProcessor,
            ItemWriter<String> productNameWriter
    ) {

        return new StepBuilder(
                "productNameStep548",
                jobRepository
        )
                .<String, String>chunk(3)
                .transactionManager(transactionManager)
                .reader(productNameReader)
                .processor(productNameProcessor)
                .writer(productNameWriter)
                .build();
    }

    // 5. Job
    @Bean
    public Job productNameJob(
            JobRepository jobRepository,
            Step productNameStep
    ) {

        return new JobBuilder(
                "productNameJob548",
                jobRepository
        )
                .start(productNameStep)
                .build();
    }
}