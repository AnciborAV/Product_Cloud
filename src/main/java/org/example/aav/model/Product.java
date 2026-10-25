package org.example.aav.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/** Неизменяемый товар; денежные значения хранятся без ошибок double. */
public record Product(UUID id, String name, BigDecimal price, Category category, String unit) {
    public Product {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(unit, "unit");
        name = name.strip();
        unit = unit.strip();
        if (name.isBlank() || name.length() > 100 || unit.isBlank() || unit.length() > 30) {
            throw new IllegalArgumentException("Некорректное название или единица продажи");
        }
        if (price.signum() <= 0 || price.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new IllegalArgumentException("Цена должна быть от 0.01 до 999999.99");
        }
        price = price.setScale(2, RoundingMode.UNNECESSARY);
    }
}
