package ru.aav.bookshop.service;

import ru.aav.bookshop.model.*;
import ru.aav.bookshop.repository.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/** Бизнес-логика не зависит от HTTP и HTML, поэтому её можно проверять отдельно. */
public final class ShopService {
    /** Репозиторий каталога книг. */
    private final BookRepository books;

    /** Зависимость для работы с заказами. */
    private final OrderRepository orders;

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public ShopService(BookRepository books, OrderRepository orders) {
        this.books = books;
        this.orders = orders;
    }

    /** Возвращает каталог книг из PostgreSQL. */
    public List<Book> catalog() throws SQLException {
        return books.findAll();
    }

    /** Браузер передаёт только идентификаторы и количества; цены всегда берём из каталога. */
    public Order checkout(Map<Long, Integer> quantities) throws SQLException {
        if (quantities.isEmpty() || quantities.size() > 20)
            throw new IllegalArgumentException("Выберите от 1 до 20 разных книг.");
        // Подготовленные строки будущего DTO или снимка заказа.
        List<Order.Line> lines = new ArrayList<>();
        for (var entry : quantities.entrySet()) {
            // Количество из запроса, проверяемое перед расчётом стоимости.
            int quantity = entry.getValue();
            if (quantity < 1 || quantity > 10)
                throw new IllegalArgumentException("Количество каждой книги — от 1 до 10.");
            // Книга из серверного каталога; цена не принимается из браузера.
            Book book = books.findById(entry.getKey());
            lines.add(new Order.Line(book.id(), book.title(), book.price(), quantity));
        }
        // Созданный или найденный заказ; дальнейший ответ содержит его идентификатор или
        // представление.
        Order order = new Order(UUID.randomUUID(), Instant.now(), lines);
        orders.save(order);
        return order;
    }

    /** Находит сохранённый заказ по UUID; доступ к HTTP-маршруту контролирует авторизация. */
    public Optional<Order> findOrder(UUID id) throws SQLException {
        return orders.findById(id);
    }
}
