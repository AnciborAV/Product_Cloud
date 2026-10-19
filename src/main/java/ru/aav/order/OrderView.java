package ru.aav.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** DTO фиксирует все ленивые данные внутри транзакции до рендеринга Thymeleaf. */
public record OrderView(long id, String date, String recipient, String phone, String address,
                        String comment, List<Line> lines, BigDecimal total) {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.of("Europe/Moscow"));
    public record Line(int productId, String name, int quantity, BigDecimal price, BigDecimal subtotal) { }
    public static OrderView from(CustomerOrder order) {
        return new OrderView(order.getId(), DATE.format(order.getCreatedAt()), order.getRecipient(), order.getPhone(),
                order.getAddress(), order.getComment(), order.getLines().stream().map(l ->
                new Line(l.getProductId(), l.getName(), l.getQuantity(), l.getPrice(), l.getSubtotal())).toList(), order.getTotal());
    }
}
