package ru.aav.bookshop.repository;

import ru.aav.bookshop.model.Book;
import java.sql.*;
import java.util.*;

/** Репозиторий изолирует SQL: остальные слои оперируют объектами Book. */
public final class BookRepository {
    private final Database database;
    public BookRepository(Database database) { this.database = database; }
    public List<Book> findAll() throws SQLException {
        try (var connection = database.connect();
             var statement = connection.prepareStatement("SELECT * FROM books ORDER BY id");
             var rows = statement.executeQuery()) {
            List<Book> books = new ArrayList<>();
            while (rows.next()) books.add(read(rows));
            return List.copyOf(books);
        }
    }
    /** PreparedStatement передаёт значение отдельно от SQL, исключая SQL-инъекцию. */
    public Book findById(long id) throws SQLException {
        try (var connection = database.connect();
             var statement = connection.prepareStatement("SELECT * FROM books WHERE id = ?")) {
            statement.setLong(1, id);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) throw new IllegalArgumentException("Книга не найдена.");
                return read(rows);
            }
        }
    }
    private Book read(ResultSet row) throws SQLException {
        return new Book(row.getLong("id"), row.getString("title"), row.getString("author"),
            row.getString("category"), row.getBigDecimal("price"), row.getString("color"), row.getString("description"));
    }
}
