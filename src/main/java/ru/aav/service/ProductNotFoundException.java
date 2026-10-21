package ru.aav.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Явная ошибка предметной области: товар с указанным id не найден. RuntimeException по умолчанию
 * приводит к откату транзакции Spring. Для веб-запроса Spring преобразует её в HTTP 404, не
 * раскрывая чужой продукт. {@code @ResponseStatus} задаёт HTTP-статус для этого исключения.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public final class ProductNotFoundException extends RuntimeException {
    /** Включает искомый id в диагностическое сообщение. */
    public ProductNotFoundException(int id) {
        super("Product not found: " + id);
    }
}
