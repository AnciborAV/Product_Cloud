package ru.aav.cart;

import java.math.BigDecimal;
import java.util.List;

/** Снимок корзины для представления. Денежные расчёты всегда идут через BigDecimal. */
public record CartView(List<Line> lines, int quantity, BigDecimal total) {
    public record Line(int productId, String name, int quantity, BigDecimal price, BigDecimal subtotal) { }
    public static CartView from(List<CartItem> items) {
        List<Line> lines = items.stream().map(i -> new Line(i.getProduct().getId(), i.getProduct().getName(),
                i.getQuantity(), i.getProduct().getPrice(), i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))).toList();
        return new CartView(lines, lines.stream().mapToInt(Line::quantity).sum(),
                lines.stream().map(Line::subtotal).reduce(new BigDecimal("0.00"), BigDecimal::add));
    }
}
