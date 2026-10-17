package ru.aav.service;

public final class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(int id) {
        super("Product not found: " + id);
    }
}
