package ru.aav.order;

import jakarta.persistence.*;

import ru.aav.cart.CartItem;

import java.math.BigDecimal;

/**
 * Неизменяемый снимок покупки: последующее изменение каталога не меняет историю.
 * {@code @Embeddable} объявляет составное значение, жизненным циклом которого управляет владелец.
 */
@Embeddable
public class OrderLine {
    /**
     * Идентификатор продукта; в истории это снимок, а не внешняя ссылка на живой товар.
     * {@code @Column} задаёт имя, допустимость NULL, длину или точность столбца; это не проверка
     * HTML-формы.
     */
    @Column(nullable = false)
    private Integer productId;

    /**
     * Название, которое увидит пользователь. {@code @Column} задаёт имя, допустимость NULL, длину
     * или точность столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Цена единицы в десятичном формате BigDecimal. {@code @Column} задаёт имя, допустимость NULL,
     * длину или точность столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    /**
     * Количество единиц продукта в позиции. {@code @Column} задаёт имя, допустимость NULL, длину
     * или точность столбца; это не проверка HTML-формы.
     */
    @Column(nullable = false)
    private int quantity;

    /** Конструктор для JPA: ORM создаёт экземпляр и заполняет поля при чтении из базы. */
    protected OrderLine() {}

    /**
     * Копирует название, цену и количество из корзины, отделяя историю от будущих изменений
     * продукта.
     */
    public OrderLine(CartItem item) {
        productId = item.getProduct().getId();
        name = item.getProduct().getName();
        price = item.getProduct().getPrice();
        quantity = item.getQuantity();
    }

    /** Возвращает исходный идентификатор продукта в снимке заказа. */
    public Integer getProductId() {
        return productId;
    }

    /** Возвращает название продукта. */
    public String getName() {
        return name;
    }

    /** Возвращает цену единицы; BigDecimal неизменяем. */
    public BigDecimal getPrice() {
        return price;
    }

    /** Возвращает количество единиц. */
    public int getQuantity() {
        return quantity;
    }

    /** Возвращает стоимость позиции: цена × количество. */
    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
