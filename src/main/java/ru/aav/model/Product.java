package ru.aav.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * JPA-сущность: один объект соответствует одной строке таблицы products.
 * JPA задаёт правила отображения объектов на таблицы, Hibernate реализует эти правила.
 * Аннотации стоят на полях: Hibernate читает и заполняет их напрямую, без сеттеров.
 * Изменение товара проходит через update(), чтобы нельзя было обойти проверки.
 */
@Entity
@Table(name = "products")
public class Product {
    // Первичный ключ. Integer допускает null до назначения идентификатора.
    // SEQUENCE использует последовательность products_seq в базе.
    // allocationSize=50 позволяет Hibernate выделять идентификаторы блоками;
    // шаг последовательности должен соответствовать настройке. Пропуски id нормальны.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_ids")
    @SequenceGenerator(name = "product_ids", sequenceName = "products_seq", allocationSize = 50)
    private Integer id;

    // Ограничения схемы дополняют проверки Java; сами по себе они не валидируют форму.
    @Column(nullable = false, length = 255)
    private String name;

    // BigDecimal хранит десятичные значения без погрешностей двоичного double.
    // precision=19 — всего цифр, scale=2 — цифр после запятой (до 17 перед ней).
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    /**
     * Конструктор без аргументов нужен JPA для восстановления объекта из базы.
     * protected оставляет его доступным ORM, но не делает основным способом создания товара.
     */
    protected Product() {
    }

    /**
     * Создаёт новый товар через общие проверки; id назначит механизм сохранения.
     * Цена передаётся как BigDecimal, например new BigDecimal("19.90").
     */
    public Product(String name, BigDecimal price) {
        update(name, price);
    }

    /**
     * Проверяет название и цену, затем меняет оба поля.
     * Проверки нужны и здесь, поскольку сервис можно вызвать без HTML-формы.
     * Все проверки выполняются до присваиваний: при ошибке объект остаётся прежним.
     * @throws IllegalArgumentException если название или цена недопустимы
     */
    public void update(String name, BigDecimal price) {
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new IllegalArgumentException("Name must contain 1–255 characters");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Price must not be negative");
        }
        // Финальная локальная переменная получает цену в едином формате с двумя знаками.
        final BigDecimal normalized;
        try {
            // UNNECESSARY запрещает потерю точности: 1.001 отклоняется, а 1.000 даёт 1.00.
            normalized = price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Price must have at most two decimal places", exception);
        }
        if (normalized.precision() > 19) {
            throw new IllegalArgumentException("Price is too large");
        }
        // strip() удаляет пробельные символы по краям после проверки длины исходной строки.
        this.name = name.strip();
        this.price = normalized;
    }

    /**
     * Возвращает идентификатор; до назначения ORM он может быть null.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Возвращает название товара для чтения, не предоставляя отдельного сеттера.
     */
    public String getName() {
        return name;
    }

    /**
     * Возвращает цену. BigDecimal неизменяем, поэтому защитная копия не требуется.
     */
    public BigDecimal getPrice() {
        return price;
    }
}
