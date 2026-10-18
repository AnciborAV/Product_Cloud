package ru.aav.bookshop.service;

import ru.aav.bookshop.model.*;
import ru.aav.bookshop.repository.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/** Бизнес-логика не зависит от HTTP и HTML, поэтому её можно проверять отдельно. */
public final class ShopService {
    private final BookRepository books;
    private final OrderRepository orders;
    public ShopService(BookRepository books, OrderRepository orders) {
        this.books = books; this.orders = orders;
    }
    public List<Book> catalog() throws SQLException { return books.findAll(); }

    /** Браузер передаёт только идентификаторы и количества; цены всегда берём из каталога. */
    public Order checkout(Map<Long, Integer> quantities) throws SQLException {
        if (quantities.isEmpty() || quantities.size() > 20)
            throw new IllegalArgumentException("Выберите от 1 до 20 разных книг.");
        List<Order.Line> lines = new ArrayList<>();
        for (var entry : quantities.entrySet()) {
            int quantity = entry.getValue();
            if (quantity < 1 || quantity > 10)
                throw new IllegalArgumentException("Количество каждой книги — от 1 до 10.");
            Book book = books.findById(entry.getKey());
            lines.add(new Order.Line(book.id(), book.title(), book.price(), quantity));
        }
        Order order = new Order(UUID.randomUUID(), Instant.now(), lines);
        orders.save(order);
        return order;
    }
    public Optional<Order> findOrder(UUID id) throws SQLException { return orders.findById(id); }
}
