package org.example.aav.service;

import org.example.aav.model.Category;
import org.example.aav.model.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ProductService {
    private final List<Product> products = new CopyOnWriteArrayList<>();

    public ProductService() {
        addProduct("Авокадо Хасс", new BigDecimal("189.00"), Category.VEGETABLES, "1 шт.");
        addProduct("Томаты на ветке", new BigDecimal("249.90"), Category.VEGETABLES, "500 г");
        addProduct("Молоко фермерское 3,2%", new BigDecimal("119.00"), Category.DAIRY, "1 л");
        addProduct("Круассан сливочный", new BigDecimal("95.00"), Category.BAKERY, "1 шт.");
        addProduct("Яблоки Гала", new BigDecimal("169.00"), Category.VEGETABLES, "1 кг");
        addProduct("Йогурт натуральный", new BigDecimal("89.00"), Category.DAIRY, "180 г");
        addProduct("Хлеб на закваске", new BigDecimal("159.00"), Category.BAKERY, "400 г");
        addProduct("Мёд цветочный", new BigDecimal("429.00"), Category.PANTRY, "250 г");
    }

    public Product addProduct(String name, BigDecimal price, Category category, String unit) {
        var product = new Product(UUID.randomUUID(), name, price, category, unit);
        products.add(product);
        return product;
    }

    public List<Product> findAll() {
        return List.copyOf(products);
    }

    public List<Product> search(String query, Category category) {
        var normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return products.stream()
                .filter(product -> category == null || product.category() == category)
                .filter(product -> product.name().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }
}
