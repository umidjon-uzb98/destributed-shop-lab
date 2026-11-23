
package uz.demo.dockerlab.analyticslab;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderAnalyticsRepository
        extends JpaRepository<OrderAnalyticsSummary, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM OrderAnalyticsSummary a
            WHERE a.id = :id
            """)
    Optional<OrderAnalyticsSummary> findForUpdate(
            @Param("id") Long id
    );
}
