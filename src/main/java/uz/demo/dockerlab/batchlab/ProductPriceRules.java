package uz.demo.dockerlab.batchlab;

import java.math.BigDecimal;

public final class ProductPriceRules {

    private ProductPriceRules() {
    }

    public static boolean isValid(BigDecimal price) {
        return price != null
                && price.compareTo(BigDecimal.ZERO) > 0;
    }
}