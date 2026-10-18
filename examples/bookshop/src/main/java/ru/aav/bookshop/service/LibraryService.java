package ru.aav.bookshop.service;

import ru.aav.bookshop.repository.LibraryRepository;
import ru.aav.bookshop.model.LibraryDocument;
import java.sql.SQLException;
import java.util.*;

/** Проверка метаданных и предела загрузки до записи в PostgreSQL. */
public final class LibraryService {
    public static final int MAX_PDF_BYTES = 20 * 1024 * 1024;
    private final LibraryRepository repository;
    public LibraryService(LibraryRepository repository) { this.repository = repository; }
    public List<LibraryRepository.Shelf> shelves() throws SQLException { return repository.shelves(); }
    public List<LibraryDocument> documents() throws SQLException { return repository.documents(); }
    public long createShelf(String name) throws SQLException { return repository.createShelf(text(name, 120)); }
    public UUID upload(long shelf, String title, String author, String filename, byte[] content) throws SQLException {
        if (content.length < 5 || content.length > MAX_PDF_BYTES || content[0] != '%' || content[1] != 'P' ||
            content[2] != 'D' || content[3] != 'F' || content[4] != '-')
            throw new IllegalArgumentException("Выберите PDF размером не более 20 МБ.");
        requireShelf(shelf);
        return repository.save(shelf, text(title, 200), text(author, 150), text(filename, 255), content);
    }
    public Optional<byte[]> content(UUID id) throws SQLException { return repository.content(id); }
    public boolean move(UUID id, long shelf) throws SQLException { requireShelf(shelf); return repository.move(id, shelf); }
    private void requireShelf(long id) throws SQLException {
        if (shelves().stream().noneMatch(shelf -> shelf.id() == id)) throw new IllegalArgumentException("Выберите существующую полку.");
    }
    private String text(String value, int maximum) {
        if (value == null || value.isBlank() || value.strip().length() > maximum)
            throw new IllegalArgumentException("Заполните название, автора и полку; проверьте длину полей.");
        return value.strip();
    }
}
