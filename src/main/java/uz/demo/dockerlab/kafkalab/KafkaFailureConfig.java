
package uz.demo.dockerlab.kafkalab;

import org.apache.kafka.common.TopicPartition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;

@Configuration
public class KafkaFailureConfig {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaFailureConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, OrderCreatedMessage> kafkaTemplate
    ) {

        // 1. Xatoli record qayerga yuboriladi?
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        KafkaTopics.ORDERS_CREATED_DLT,
                                        record.partition()
                                )
                );

        // 2. DLT publish xatosini yashirmaslik
        recoverer.setFailIfSendResultIsError(true);

        recoverer.setWaitForSendResultTimeout(
                Duration.ofSeconds(10)
        );

        // 3. 1 initial attempt + 2 retry
        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recoverer,
                        new FixedBackOff(1000L, 2L)
                );

        // 4. Invalid data uchun retry foydasiz
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class
        );

        // 5. Retrylarni log orqali kuzatamiz
        errorHandler.setRetryListeners(
                (record, exception, deliveryAttempt) -> {

                    log.warn(
                            "KAFKA PROCESSING FAILED: topic={}, partition={}, offset={}, attempt={}",
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            deliveryAttempt
                    );
                }
        );

        return errorHandler;
    }
}
