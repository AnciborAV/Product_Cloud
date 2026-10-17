package ru.aav.model;

import java.math.BigDecimal;

/**
 * DTO (объект передачи данных) для отображения товара вне слоя хранения.
 * record автоматически предоставляет конструктор, методы id()/name()/price(),
 * equals(), hashCode() и toString(). Компоненты нельзя переназначить после создания.
 * Шаблон получает готовые значения, а не управляемую Hibernate сущность.
 */
public record ProductView(int id, String name, BigDecimal price) {
    /**
     * Преобразует сохранённую сущность в снимок данных для контроллера.
     * Вызывается после назначения id: распаковка Integer в int требует ненулевого id.
     */
    public static ProductView from(Product product) {
        return new ProductView(product.getId(), product.getName(), product.getPrice());
    }
}
