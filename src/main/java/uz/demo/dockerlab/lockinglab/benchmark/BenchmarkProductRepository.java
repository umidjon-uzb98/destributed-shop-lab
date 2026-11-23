
package uz.demo.dockerlab.lockinglab.benchmark;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BenchmarkProductRepository
        extends JpaRepository<BenchmarkProduct, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM BenchmarkProduct p
            WHERE p.id = :id
            """)
    Optional<BenchmarkProduct> findForUpdate(
            @Param("id") Long id
    );
}
