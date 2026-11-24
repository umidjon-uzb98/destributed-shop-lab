package uz.demo.dockerlab.batchlab;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductPriceRulesTest {

    @Test
    void positivePriceShouldBeValid() {
        boolean result = ProductPriceRules.isValid(
                new BigDecimal("150.50")
        );

        assertTrue(result);
    }

    @Test
    void zeroPriceShouldBeInvalid() {
        boolean result = ProductPriceRules.isValid(
                BigDecimal.ZERO
        );

        assertFalse(result);
    }

    @Test
    void negativePriceShouldBeInvalid() {
        boolean result = ProductPriceRules.isValid(
                new BigDecimal("-10.00")
        );

        assertFalse(result);
    }

    @Test
    void nullPriceShouldBeInvalid() {
        assertFalse(ProductPriceRules.isValid(null));
    }
}