package ru.aav.bookshop.repository;

import ru.aav.bookshop.model.Order;
import java.sql.*;
import java.util.*;

/** PostgreSQL хранит заголовок заказа и его позиции в двух связанных таблицах. */
public final class OrderRepository {
    private final Database database;
    public OrderRepository(Database database) { this.database = database; }

    /** Транзакция сохраняет весь заказ целиком или откатывает его при любой ошибке. */
    public void save(Order order) throws SQLException {
        try (var connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.prepareStatement("INSERT INTO orders(id, created_at) VALUES (?, ?)")) {
                    statement.setObject(1, order.id());
                    statement.setTimestamp(2, Timestamp.from(order.createdAt()));
                    statement.executeUpdate();
                }
                try (var statement = connection.prepareStatement(
                    "INSERT INTO order_items(order_id, book_id, title, price, quantity) VALUES (?, ?, ?, ?, ?)")) {
                    for (var line : order.lines()) {
                        statement.setObject(1, order.id()); statement.setLong(2, line.bookId());
                        statement.setString(3, line.title()); statement.setBigDecimal(4, line.price());
                        statement.setInt(5, line.quantity()); statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
            } catch (SQLException failure) { connection.rollback(); throw failure; }
        }
    }

    /** Загружает чек по непредсказуемому UUID; суммы восстанавливаются из снимка заказа. */
    public Optional<Order> findById(UUID id) throws SQLException {
        try (var connection = database.connect();
             var statement = connection.prepareStatement(
                 "SELECT o.created_at, i.book_id, i.title, i.price, i.quantity FROM orders o " +
                 "JOIN order_items i ON i.order_id = o.id WHERE o.id = ? ORDER BY i.book_id")) {
            statement.setObject(1, id);
            try (var rows = statement.executeQuery()) {
                List<Order.Line> lines = new ArrayList<>();
                java.time.Instant createdAt = null;
                while (rows.next()) {
                    createdAt = rows.getTimestamp("created_at").toInstant();
                    lines.add(new Order.Line(rows.getLong("book_id"), rows.getString("title"),
                        rows.getBigDecimal("price"), rows.getInt("quantity")));
                }
                return lines.isEmpty() ? Optional.empty() : Optional.of(new Order(id, createdAt, lines));
            }
        }
    }
}
