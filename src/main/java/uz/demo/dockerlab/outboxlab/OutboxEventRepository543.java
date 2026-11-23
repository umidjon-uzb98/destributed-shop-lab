
package uz.demo.dockerlab.outboxlab;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository543
        extends JpaRepository<OutboxEvent543, String> {

    List<OutboxEvent543> findTop10ByStatusOrderByCreatedAtAsc(
            String status
    );

    @Modifying
    @Transactional
    @Query("""
            UPDATE OutboxEvent543 e
            SET e.status = 'SENT',
                e.sentAt = :sentAt
            WHERE e.eventId = :eventId
              AND e.status = 'PENDING'
            """)
    int markSent(
            @Param("eventId") String eventId,
            @Param("sentAt") Instant sentAt
    );
}
