package ru.aav.model;

import java.math.BigDecimal;

/** A detached snapshot for the presentation layer. */
public record ProductView(int id, String name, BigDecimal price) {
    public static ProductView from(Product product) {
        return new ProductView(product.getId(), product.getName(), product.getPrice());
    }
}
