package ru.aav.bookshop.repository;

import ru.aav.bookshop.model.LibraryDocument;

import java.sql.*;
import java.util.*;

/** SQL-операции личной библиотеки; PDF хранится в BYTEA и входит в резервную копию PostgreSQL. */
public final class LibraryRepository {
    /** Настройки и фабрика JDBC-соединений. */
    private final Database database;

    /** Неизменяемая пара идентификатора и названия полки. */
    public record Shelf(
            /** Идентификатор записи. */
            long id,
            /** Название продукта или полки. */
            String name) {}

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public LibraryRepository(Database database) {
        this.database = database;
    }

    /** Читает идентификаторы и названия полок в алфавитном порядке. */
    public List<Shelf> shelves() throws SQLException {
        try (var connection = database.connect();
                var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT id,name FROM shelves ORDER BY name")) {
            // Коллекция результатов, заполняемая данными текущего запроса.
            var result = new ArrayList<Shelf>();
            while (rows.next()) result.add(new Shelf(rows.getLong(1), rows.getString(2)));
            return result;
        }
    }

    /** Вставляет полку или возвращает id существующей через PostgreSQL ON CONFLICT. */
    public long createShelf(String name) throws SQLException {
        try (var connection = database.connect();
                var statement =
                        connection.prepareStatement(
                                "INSERT INTO shelves(name) VALUES (?) ON CONFLICT(name) DO UPDATE"
                                        + " SET name=EXCLUDED.name RETURNING id")) {
            statement.setString(1, name);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getLong(1);
            }
        }
    }

    /** Читает метаданные и размер файлов; сами бинарные PDF не передаются в этот запрос. */
    public List<LibraryDocument> documents() throws SQLException {
        try (var connection = database.connect();
                var statement = connection.createStatement();
                var rows =
                        statement.executeQuery(
                                "SELECT"
                                    + " d.id,d.shelf_id,s.name,d.title,d.author,octet_length(d.content)"
                                    + " size FROM documents d JOIN shelves s ON s.id=d.shelf_id"
                                    + " ORDER BY d.uploaded_at DESC")) {
            // Коллекция результатов, заполняемая данными текущего запроса.
            var result = new ArrayList<LibraryDocument>();
            while (rows.next())
                result.add(
                        new LibraryDocument(
                                rows.getObject("id", UUID.class),
                                rows.getLong("shelf_id"),
                                rows.getString("name"),
                                rows.getString("title"),
                                rows.getString("author"),
                                rows.getLong("size")));
            return result;
        }
    }

    /** Генерирует UUID и записывает метаданные и содержимое PDF параметризованным запросом. */
    public UUID save(long shelfId, String title, String author, String filename, byte[] content)
            throws SQLException {
        // Идентификатор документа, используемый для точного обращения к записи.
        UUID id = UUID.randomUUID();
        try (var connection = database.connect();
                var statement =
                        connection.prepareStatement(
                                "INSERT INTO documents(id,shelf_id,title,author,file_name,content)"
                                        + " VALUES (?,?,?,?,?,?)")) {
            statement.setObject(1, id);
            statement.setLong(2, shelfId);
            statement.setString(3, title);
            statement.setString(4, author);
            statement.setString(5, filename);
            statement.setBytes(6, content);
            statement.executeUpdate();
            return id;
        }
    }

    /** Читает байты одного PDF; отсутствие документа выражено Optional.empty(). */
    public Optional<byte[]> content(UUID id) throws SQLException {
        try (var connection = database.connect();
                var statement =
                        connection.prepareStatement("SELECT content FROM documents WHERE id=?")) {
            statement.setObject(1, id);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(rows.getBytes(1)) : Optional.empty();
            }
        }
    }

    /** Обновляет полку документа и сообщает, существовала ли указанная запись. */
    public boolean move(UUID id, long shelf) throws SQLException {
        try (var connection = database.connect();
                var statement =
                        connection.prepareStatement("UPDATE documents SET shelf_id=? WHERE id=?")) {
            statement.setLong(1, shelf);
            statement.setObject(2, id);
            return statement.executeUpdate() == 1;
        }
    }
}
