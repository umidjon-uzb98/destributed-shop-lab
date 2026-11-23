
package uz.demo.dockerlab.analyticslab;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.demo.dockerlab.kafkalab.OrderCreatedMessage;

import java.util.List;

@Service
public class OrderAnalyticsProcessor {

    private static final String CONSUMER_NAME =
            "order-analytics-540";

    private final OrderAnalyticsRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public OrderAnalyticsProcessor(
            OrderAnalyticsRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public ProcessingOutcome process(
            OrderCreatedMessage event
    ) {

        validate(event);

        // 1. Event ID ni database'da atomik claim qilamiz
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO processed_message_540 (
                    consumer_name,
                    event_id,
                    order_id,
                    amount_uzs
                )
                VALUES (
                    :consumerName,
                    :eventId,
                    :orderId,
                    :amount
                )
                ON CONFLICT (consumer_name, event_id)
                DO NOTHING
                """)
                .setParameter("consumerName", CONSUMER_NAME)
                .setParameter("eventId", event.eventId())
                .setParameter("orderId", event.orderId())
                .setParameter("amount", event.amount())
                .executeUpdate();

        // 2. Duplicate event
        if (inserted == 0) {

            List<?> existing = entityManager.createNativeQuery("""
                    SELECT 1
                    FROM processed_message_540
                    WHERE consumer_name = :consumerName
                      AND event_id = :eventId
                      AND order_id = :orderId
                      AND amount_uzs = :amount
                    """)
                    .setParameter("consumerName", CONSUMER_NAME)
                    .setParameter("eventId", event.eventId())
                    .setParameter("orderId", event.orderId())
                    .setParameter("amount", event.amount())
                    .getResultList();

            if (existing.isEmpty()) {
                throw new IllegalArgumentException(
                        "Event ID reused with different order data"
                );
            }

            return ProcessingOutcome.DUPLICATE;
        }

        // 3. Yangi event: analytics aggregate'ni lock qilamiz
        OrderAnalyticsSummary analytics =
                repository.findForUpdate(1L)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Analytics summary not initialized"
                                )
                        );

        // 4. Biznes operatsiya
        analytics.recordOrder(event.amount());

        // 5. SQL update'ni yuboramiz
        repository.saveAndFlush(analytics);

        return ProcessingOutcome.APPLIED;
    }

    @Transactional(readOnly = true)
    public AnalyticsStats getStats() {

        OrderAnalyticsSummary analytics =
                repository.findById(1L)
                        .orElseThrow();

        return new AnalyticsStats(
                analytics.getProcessedOrders(),
                analytics.getTotalAmountUzs()
        );
    }

    private void validate(OrderCreatedMessage event) {

        if (event == null
                || event.eventId() == null
                || !event.eventId().matches(
                "[A-Za-z0-9._-]{1,120}"
        )
                || event.orderId() <= 0
                || event.amount() <= 0) {

            throw new IllegalArgumentException(
                    "Invalid OrderCreated event"
            );
        }
    }

    public enum ProcessingOutcome {
        APPLIED,
        DUPLICATE
    }

    public record AnalyticsStats(
            long processedOrders,
            long totalAmountUzs
    ) {}
}
