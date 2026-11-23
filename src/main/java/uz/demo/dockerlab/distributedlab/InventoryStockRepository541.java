
package uz.demo.dockerlab.distributedlab;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryStockRepository541
        extends JpaRepository<InventoryStock541, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM InventoryStock541 s
            WHERE s.productId = :productId
            """)
    Optional<InventoryStock541> findForUpdate(
            @Param("productId") Long productId
    );
}
