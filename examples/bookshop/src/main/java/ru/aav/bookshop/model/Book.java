package ru.aav.bookshop.model;

import java.math.BigDecimal;

/** Неизменяемая модель книги. BigDecimal хранит цену без ошибок двоичной арифметики. */
public record Book(long id, String title, String author, String category,
                   BigDecimal price, String color, String description) {}
