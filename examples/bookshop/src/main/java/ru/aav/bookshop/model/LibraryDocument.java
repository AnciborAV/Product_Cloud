package ru.aav.bookshop.model;

import java.util.UUID;

/** Метаданные PDF: тяжёлое содержимое загружается только при открытии документа. */
public record LibraryDocument(
        /** Идентификатор записи. */
        UUID id,
        /** Идентификатор полки. */
        long shelfId,
        /** Название полки. */
        String shelf,
        /** Название книги или документа. */
        String title,
        /** Автор произведения. */
        String author,
        /** Размер PDF в байтах. */
        long size) {
    /** Форматирует размер в мегабайтах с одним десятичным знаком и фиксированной локалью. */
    public String sizeLabel() {
        return String.format(java.util.Locale.ROOT, "%.1f МБ", size / 1048576.0);
    }
}
