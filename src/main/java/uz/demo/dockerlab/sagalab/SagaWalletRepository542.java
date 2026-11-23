package uz.demo.dockerlab.sagalab;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SagaWalletRepository542 extends JpaRepository<SagaWallet542, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from SagaWallet542 w where w.id = :id")
    Optional<SagaWallet542> findForUpdate(@Param("id") Long id);
}
