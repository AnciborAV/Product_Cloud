package ru.aav.bookshop.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Снимок покупки: названия и цены сохраняются даже при изменении каталога. */
public record Order(UUID id, Instant createdAt, List<Line> lines) {
    public Order { lines = List.copyOf(lines); }

    /** Общая стоимость вычисляется на сервере из сохранённых позиций. */
    public BigDecimal total() {
        return lines.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Одна позиция заказа; количество проверяет сервис до сохранения. */
    public record Line(long bookId, String title, BigDecimal price, int quantity) {
        public BigDecimal subtotal() { return price.multiply(BigDecimal.valueOf(quantity)); }
    }
}
