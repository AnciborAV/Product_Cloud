package org.example.aav.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Этап 8: добавляем проверки правил модели товара, чтобы замечать ошибки при изменениях. Учебная последовательность. */
class ProductTest {
    /**
     * Проверяет удаление лишних пробелов и точное хранение цены.
     */
    @Test
    void normalizesTextAndKeepsExactPrice() {
        // Товар, с которым выполняется действие.
        var product = new Product(UUID.randomUUID(), "  Молоко  ", new BigDecimal("119.9"), Category.DAIRY, " 1 л ");
        assertThat(product.name()).isEqualTo("Молоко");
        assertThat(product.unit()).isEqualTo("1 л");
        assertThat(product.price()).isEqualTo(new BigDecimal("119.90"));
    }

    /**
     * Проверяет отказ для недопустимых цен.
     * @param price цена товара; BigDecimal позволяет точно хранить копейки
     */
    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1000000", "1.001"})
    void rejectsInvalidPrices(String price) {
        assertThatThrownBy(() -> new Product(UUID.randomUUID(), "Хлеб", new BigDecimal(price), Category.BAKERY, "1 шт."))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Проверяет отказ для пустого названия.
     */
    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Product(UUID.randomUUID(), "  ", BigDecimal.ONE, Category.BAKERY, "1 шт."));
    }
}
