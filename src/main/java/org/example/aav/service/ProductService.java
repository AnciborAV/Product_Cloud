package org.example.aav.service;

import java.math.BigDecimal;
import java.util.*;
import org.example.aav.model.*;
import org.example.aav.repository.ProductRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {
  private final ProductRepository products;

  public ProductService(ProductRepository products) {
    this.products = products;
  }

  @Transactional
  public Product addProduct(String name, BigDecimal price, Category category, String unit) {
    return products
        .save(new ProductEntity(new Product(UUID.randomUUID(), name, price, category, unit)))
        .toProduct();
  }

  public List<Product> findAll() {
    return products.findAll(Sort.by("name")).stream().map(ProductEntity::toProduct).toList();
  }

  public List<Product> search(String query, Category category) {
    return searchPage(query, category, 0).getContent();
  }

  public Page<Product> searchPage(String query, Category category, int page) {
    String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
    q = q.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    return products
        .search(q, category, PageRequest.of(Math.max(0, page), 12, Sort.by("name")))
        .map(ProductEntity::toProduct);
  }
}
