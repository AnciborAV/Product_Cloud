package ru.aav.bookshop.model;

import java.math.BigDecimal;

/** Неизменяемая модель книги. BigDecimal хранит цену без ошибок двоичной арифметики. */
public record Book(
        /** Идентификатор записи. */
        long id,
        /** Название книги или документа. */
        String title,
        /** Автор произведения. */
        String author,
        /** Категория книги. */
        String category,
        /** Цена за единицу в BigDecimal. */
        BigDecimal price,
        /** Имя темы оформления обложки. */
        String color,
        /** Описание книги. */
        String description) {}
