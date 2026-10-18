package ru.aav.bookshop.repository;

import ru.aav.bookshop.model.LibraryDocument;
import java.sql.*;
import java.util.*;

/** SQL-операции личной библиотеки; PDF хранится в BYTEA и входит в резервную копию PostgreSQL. */
public final class LibraryRepository {
    private final Database database;
    public record Shelf(long id, String name) {}
    public LibraryRepository(Database database) { this.database = database; }
    public List<Shelf> shelves() throws SQLException {
        try (var connection = database.connect(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT id,name FROM shelves ORDER BY name")) {
            var result = new ArrayList<Shelf>();
            while (rows.next()) result.add(new Shelf(rows.getLong(1), rows.getString(2)));
            return result;
        }
    }
    public long createShelf(String name) throws SQLException {
        try (var connection = database.connect();
             var statement = connection.prepareStatement("INSERT INTO shelves(name) VALUES (?) ON CONFLICT(name) DO UPDATE SET name=EXCLUDED.name RETURNING id")) {
            statement.setString(1, name);
            try (var rows = statement.executeQuery()) { rows.next(); return rows.getLong(1); }
        }
    }
    public List<LibraryDocument> documents() throws SQLException {
        try (var connection = database.connect(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT d.id,d.shelf_id,s.name,d.title,d.author,octet_length(d.content) size FROM documents d JOIN shelves s ON s.id=d.shelf_id ORDER BY d.uploaded_at DESC")) {
            var result = new ArrayList<LibraryDocument>();
            while (rows.next()) result.add(new LibraryDocument(rows.getObject("id", UUID.class), rows.getLong("shelf_id"),
                rows.getString("name"), rows.getString("title"), rows.getString("author"), rows.getLong("size")));
            return result;
        }
    }
    public UUID save(long shelfId, String title, String author, String filename, byte[] content) throws SQLException {
        UUID id = UUID.randomUUID();
        try (var connection = database.connect(); var statement = connection.prepareStatement(
            "INSERT INTO documents(id,shelf_id,title,author,file_name,content) VALUES (?,?,?,?,?,?)")) {
            statement.setObject(1, id); statement.setLong(2, shelfId); statement.setString(3, title);
            statement.setString(4, author); statement.setString(5, filename); statement.setBytes(6, content);
            statement.executeUpdate(); return id;
        }
    }
    public Optional<byte[]> content(UUID id) throws SQLException {
        try (var connection = database.connect(); var statement = connection.prepareStatement("SELECT content FROM documents WHERE id=?")) {
            statement.setObject(1, id);
            try (var rows = statement.executeQuery()) { return rows.next() ? Optional.of(rows.getBytes(1)) : Optional.empty(); }
        }
    }
    public boolean move(UUID id, long shelf) throws SQLException {
        try (var connection = database.connect(); var statement = connection.prepareStatement("UPDATE documents SET shelf_id=? WHERE id=?")) {
            statement.setLong(1, shelf); statement.setObject(2, id); return statement.executeUpdate() == 1;
        }
    }
}
