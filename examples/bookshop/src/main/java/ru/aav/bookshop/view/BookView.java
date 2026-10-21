package ru.aav.bookshop.view;

import ru.aav.bookshop.model.Book;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Модель представления: добавляет форматированную цену, не меняя доменную модель книги. */
public record BookView(
        /** Исходная модель книги. */
        Book book,
        /** Цена в формате для отображения пользователю. */
        String priceLabel) {
    /** Добавляет к модели книги форматированную цену для HTML. */
    public static BookView from(Book book) {
        return new BookView(book, money(book.price()));
    }

    /**
     * Форматирует сумму в рублях для русской локали; экземпляр NumberFormat создаётся для одного
     * вызова, поскольку он изменяемый.
     */
    public static String money(BigDecimal price) {
        // Формат изображения либо форматтер представления цены для текущей операции.
        var format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU"));
        format.setMaximumFractionDigits(0);
        return format.format(price);
    }
}
