package ru.aav.bookshop.model;

import java.util.UUID;

/** Метаданные PDF: тяжёлое содержимое загружается только при открытии документа. */
public record LibraryDocument(UUID id, long shelfId, String shelf, String title, String author, long size) {
    public String sizeLabel() { return String.format(java.util.Locale.ROOT, "%.1f МБ", size / 1048576.0); }
}
