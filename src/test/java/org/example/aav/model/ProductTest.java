package org.example.aav.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class ProductTest {
    @Test
    void normalizesTextAndKeepsExactPrice() {
        var product = new Product(UUID.randomUUID(), "  Молоко  ", new BigDecimal("119.9"), Category.DAIRY, " 1 л ");
        assertThat(product.name()).isEqualTo("Молоко");
        assertThat(product.unit()).isEqualTo("1 л");
        assertThat(product.price()).isEqualTo(new BigDecimal("119.90"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1000000", "1.001"})
    void rejectsInvalidPrices(String price) {
        assertThatThrownBy(() -> new Product(UUID.randomUUID(), "Хлеб", new BigDecimal(price), Category.BAKERY, "1 шт."))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Product(UUID.randomUUID(), "  ", BigDecimal.ONE, Category.BAKERY, "1 шт."));
    }
}
