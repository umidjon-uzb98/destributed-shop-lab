
package uz.demo.dockerlab.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import uz.demo.dockerlab.product.ProductQueryService;

import java.util.List;

@Service
public class ProductMaintenanceService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ProductMaintenanceService.class
            );

    private final ProductQueryService queryService;

    public ProductMaintenanceService(
            ProductQueryService queryService
    ) {
        this.queryService = queryService;
    }

    public WarmupResult warmCache(List<Long> ids) {

        int succeeded = 0;
        int missing = 0;
        int failed = 0;

        for (Long id : ids) {

            try {
                queryService.findById(id);

                succeeded++;

                log.info(
                        "WARMUP READ SUCCESS: productId={}",
                        id
                );

            } catch (ResponseStatusException e) {

                if (e.getStatusCode().value() == 404) {

                    missing++;

                    log.info(
                            "WARMUP PRODUCT MISSING: productId={}",
                            id
                    );

                } else {

                    failed++;

                    log.warn(
                            "WARMUP READ FAILED: productId={}, status={}",
                            id,
                            e.getStatusCode()
                    );
                }

            } catch (RuntimeException e) {

                failed++;

                log.error(
                        "WARMUP ERROR: productId={}",
                        id,
                        e
                );
            }
        }

        return new WarmupResult(
                succeeded,
                missing,
                failed
        );
    }

    public record WarmupResult(
            int succeeded,
            int missing,
            int failed
    ) {}
}

