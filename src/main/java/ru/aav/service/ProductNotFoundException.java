package ru.aav.service;

/**
 * Явная ошибка предметной области: товар с указанным id не найден.
 * RuntimeException по умолчанию приводит к откату транзакции Spring.
 * Сама по себе эта ошибка не означает HTTP 404: для HTTP-методов поиска
 * потребовалось бы отдельно настроить её преобразование в ответ контроллера.
 */
public final class ProductNotFoundException extends RuntimeException {
    /**
     * Включает искомый id в диагностическое сообщение.
     */
    public ProductNotFoundException(int id) {
        super("Product not found: " + id);
    }
}
