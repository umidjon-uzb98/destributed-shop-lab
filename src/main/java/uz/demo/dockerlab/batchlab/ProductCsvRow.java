package uz.demo.dockerlab.batchlab;

import java.math.BigDecimal;

public record ProductCsvRow(
        long id,
        String name,
        BigDecimal price
) {
}