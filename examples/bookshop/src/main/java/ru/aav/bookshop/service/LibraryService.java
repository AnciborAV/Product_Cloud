package ru.aav.bookshop.service;

import ru.aav.bookshop.model.LibraryDocument;
import ru.aav.bookshop.repository.LibraryRepository;

import java.sql.SQLException;
import java.util.*;

/** Проверка метаданных и предела загрузки до записи в PostgreSQL. */
public final class LibraryService {
    /** Верхняя граница размера загружаемого PDF: 20 МБ. */
    public static final int MAX_PDF_BYTES = 20 * 1024 * 1024;

    /** Репозиторий, изолирующий хранение данных от вызывающего слоя. */
    private final LibraryRepository repository;

    /**
     * Получает зависимости через конструктор и сохраняет их в полях; объект использует переданные
     * компоненты вместо скрытого создания новых.
     */
    public LibraryService(LibraryRepository repository) {
        this.repository = repository;
    }

    /** Возвращает полки личной библиотеки. */
    public List<LibraryRepository.Shelf> shelves() throws SQLException {
        return repository.shelves();
    }

    /** Возвращает метаданные документов без загрузки тяжёлых PDF. */
    public List<LibraryDocument> documents() throws SQLException {
        return repository.documents();
    }

    /** Проверяет название полки и создаёт её либо возвращает уже существующую. */
    public long createShelf(String name) throws SQLException {
        return repository.createShelf(text(name, 120));
    }

    /**
     * Проверяет лимит, сигнатуру PDF, полку и метаданные, затем сохраняет файл; сигнатура не
     * является антивирусной проверкой.
     */
    public UUID upload(long shelf, String title, String author, String filename, byte[] content)
            throws SQLException {
        if (content.length < 5
                || content.length > MAX_PDF_BYTES
                || content[0] != '%'
                || content[1] != 'P'
                || content[2] != 'D'
                || content[3] != 'F'
                || content[4] != '-')
            throw new IllegalArgumentException("Выберите PDF размером не более 20 МБ.");
        requireShelf(shelf);
        return repository.save(
                shelf, text(title, 200), text(author, 150), text(filename, 255), content);
    }

    /** Получает содержимое PDF по UUID или пустой Optional. */
    public Optional<byte[]> content(UUID id) throws SQLException {
        return repository.content(id);
    }

    /** Проверяет целевую полку и переносит документ, сохраняя его содержимое. */
    public boolean move(UUID id, long shelf) throws SQLException {
        requireShelf(shelf);
        return repository.move(id, shelf);
    }

    /** Отклоняет действие, если выбранной полки не существует. */
    private void requireShelf(long id) throws SQLException {
        if (shelves().stream().noneMatch(shelf -> shelf.id() == id))
            throw new IllegalArgumentException("Выберите существующую полку.");
    }

    /** Проверяет обязательный текст и его максимальную длину, затем удаляет крайние пробелы. */
    private String text(String value, int maximum) {
        if (value == null || value.isBlank() || value.strip().length() > maximum)
            throw new IllegalArgumentException(
                    "Заполните название, автора и полку; проверьте длину полей.");
        return value.strip();
    }
}
