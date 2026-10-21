package ru.aav.cart;

import java.math.BigDecimal;
import java.util.List;

/** Снимок корзины для представления. Денежные расчёты всегда идут через BigDecimal. */
public record CartView(
        /** Неизменяемый список позиций. */
        List<Line> lines,
        /** Количество единиц. */
        int quantity,
        /** Общая стоимость всех позиций. */
        BigDecimal total) {
    /**
     * Создаёт защитную неизменяемую копию списка, чтобы внешние изменения коллекции не меняли
     * снимок.
     */
    public CartView {
        lines = List.copyOf(lines);
    }

    /**
     * Неизменяемая строка представления: идентификатор, название, количество, цена и рассчитанная
     * стоимость.
     */
    public record Line(
            /** Идентификатор продукта. */
            int productId,
            /** Название продукта или полки. */
            String name,
            /** Количество единиц. */
            int quantity,
            /** Цена за единицу в BigDecimal. */
            BigDecimal price,
            /** Стоимость позиции: цена × количество. */
            BigDecimal subtotal) {}

    /**
     * Преобразует сущности в строки DTO, суммирует количество и стоимость через BigDecimal без
     * двоичных погрешностей.
     */
    public static CartView from(List<CartItem> items) {
        // Подготовленные строки будущего DTO или снимка заказа.
        List<Line> lines =
                items.stream()
                        .map(
                                i ->
                                        new Line(
                                                i.getProduct().getId(),
                                                i.getProduct().getName(),
                                                i.getQuantity(),
                                                i.getProduct().getPrice(),
                                                i.getProduct()
                                                        .getPrice()
                                                        .multiply(
                                                                BigDecimal.valueOf(
                                                                        i.getQuantity()))))
                        .toList();
        return new CartView(
                lines,
                lines.stream().mapToInt(Line::quantity).sum(),
                lines.stream().map(Line::subtotal).reduce(new BigDecimal("0.00"), BigDecimal::add));
    }
}
