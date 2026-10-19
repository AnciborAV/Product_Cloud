package ru.aav.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import ru.aav.cart.CartItem;

/** Неизменяемый снимок покупки: последующее изменение каталога не меняет историю. */
@Embeddable
public class OrderLine {
    @Column(nullable = false) private Integer productId;
    @Column(nullable = false, length = 255) private String name;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal price;
    @Column(nullable = false) private int quantity;
    protected OrderLine() { }
    public OrderLine(CartItem item) {
        productId = item.getProduct().getId(); name = item.getProduct().getName();
        price = item.getProduct().getPrice(); quantity = item.getQuantity();
    }
    public Integer getProductId() { return productId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public BigDecimal getSubtotal() { return price.multiply(BigDecimal.valueOf(quantity)); }
}
