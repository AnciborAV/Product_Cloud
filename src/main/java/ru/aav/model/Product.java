package ru.aav.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "PRODUCTS")
public class Product {

    @Id
    @GeneratedValue
    private int id;
    private String name;
    private double price;

    public Product() {}

    public Product(String name) {
        this.name = name;
    }

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }
}
