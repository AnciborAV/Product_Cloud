package ru.aav.bookshop.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Снимок покупки: названия и цены сохраняются даже при изменении каталога. */
public record Order(
        /** Идентификатор записи. */
        UUID id,
        /** Момент создания заказа. */
        Instant createdAt,
        /** Неизменяемый список позиций. */
        List<Line> lines) {
    /**
     * Создаёт защитную неизменяемую копию списка, чтобы внешние изменения коллекции не меняли
     * снимок.
     */
    public Order {
        lines = List.copyOf(lines);
    }

    /** Общая стоимость вычисляется на сервере из сохранённых позиций. */
    public BigDecimal total() {
        return lines.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Одна позиция заказа; количество проверяет сервис до сохранения. */
    public record Line(
            /** Идентификатор исходной книги. */
            long bookId,
            /** Название книги или документа. */
            String title,
            /** Цена за единицу в BigDecimal. */
            BigDecimal price,
            /** Количество единиц. */
            int quantity) {
        /** Вычисляет стоимость одной строки: цена книги, умноженная на количество. */
        public BigDecimal subtotal() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
