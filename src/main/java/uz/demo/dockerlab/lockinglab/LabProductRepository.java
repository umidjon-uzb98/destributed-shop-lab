
package uz.demo.dockerlab.lockinglab;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LabProductRepository
        extends JpaRepository<LabProduct, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM LabProduct p
            WHERE p.id = :id
            """)
    Optional<LabProduct> findByIdForUpdate(
            @Param("id") Long id
    );
}
