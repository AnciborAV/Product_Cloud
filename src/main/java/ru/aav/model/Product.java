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

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_ids")
    @SequenceGenerator(name = "product_ids", sequenceName = "products_seq", allocationSize = 50)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    protected Product() {
    }

    public Product(String name, BigDecimal price) {
        update(name, price);
    }

    public void update(String name, BigDecimal price) {
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new IllegalArgumentException("Name must contain 1–255 characters");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Price must not be negative");
        }
        final BigDecimal normalized;
        try {
            normalized = price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Price must have at most two decimal places", exception);
        }
        if (normalized.precision() > 19) {
            throw new IllegalArgumentException("Price is too large");
        }
        this.name = name.strip();
        this.price = normalized;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
