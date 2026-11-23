package uz.demo.dockerlab.saga544;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface InventoryStockRepository544
        extends JpaRepository<InventoryStock544, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from InventoryStock544 s where s.productId = :productId")
    Optional<InventoryStock544> findForUpdate(@Param("productId") Long productId);
}
