package uz.demo.dockerlab.sagalab;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SagaInventoryRepository542 extends JpaRepository<SagaInventory542, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SagaInventory542 s where s.productId = :id")
    Optional<SagaInventory542> findForUpdate(@Param("id") Long id);
}
