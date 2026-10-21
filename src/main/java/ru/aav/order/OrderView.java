package ru.aav.order;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** DTO фиксирует все ленивые данные внутри транзакции до рендеринга Thymeleaf. */
public record OrderView(
        /** Идентификатор записи. */
        long id,
        /** Дата, подготовленная для показа в часовом поясе Москвы. */
        String date,
        /** Получатель заказа. */
        String recipient,
        /** Контактный телефон. */
        String phone,
        /** Контактный адрес. */
        String address,
        /** Комментарий к заказу. */
        String comment,
        /** Неизменяемый список позиций. */
        List<Line> lines,
        /** Общая стоимость всех позиций. */
        BigDecimal total) {
    /**
     * Создаёт защитную неизменяемую копию списка, чтобы внешние изменения коллекции не меняли
     * снимок.
     */
    public OrderView {
        lines = List.copyOf(lines);
    }

    /** Потокобезопасный форматтер даты заказа в часовом поясе Москвы. */
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.of("Europe/Moscow"));

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
     * Создаёт снимок заказа для HTML, форматирует дату и материализует строки до закрытия
     * транзакции.
     */
    public static OrderView from(CustomerOrder order) {
        return new OrderView(
                order.getId(),
                DATE.format(order.getCreatedAt()),
                order.getRecipient(),
                order.getPhone(),
                order.getAddress(),
                order.getComment(),
                order.getLines().stream()
                        .map(
                                l ->
                                        new Line(
                                                l.getProductId(),
                                                l.getName(),
                                                l.getQuantity(),
                                                l.getPrice(),
                                                l.getSubtotal()))
                        .toList(),
                order.getTotal());
    }
}
