package uz.demo.dockerlab.batchlab;

import java.math.BigDecimal;

public record ImportProduct(
        long id,
        String name,
        BigDecimal price
) {
}