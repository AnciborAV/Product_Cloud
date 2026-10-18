package ru.aav.bookshop.view;

import ru.aav.bookshop.model.Book;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Модель представления: добавляет форматированную цену, не меняя доменную модель книги. */
public record BookView(Book book, String priceLabel) {
    public static BookView from(Book book) { return new BookView(book, money(book.price())); }
    public static String money(BigDecimal price) {
        var format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU"));
        format.setMaximumFractionDigits(0);
        return format.format(price);
    }
}
