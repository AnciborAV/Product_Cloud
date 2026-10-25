package org.example.aav.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class ProductEntity extends BaseEntity {
  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 100)
  private String searchName;

  @Column(nullable = false, precision = 8, scale = 2)
  private BigDecimal price;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Category category;

  @Column(nullable = false, length = 30)
  private String unit;

  protected ProductEntity() {}

  public ProductEntity(Product p) {
    id = p.id();
    name = p.name();
    searchName = name.toLowerCase(java.util.Locale.ROOT);
    price = p.price();
    category = p.category();
    unit = p.unit();
  }

  public Product toProduct() {
    return new Product(id, name, price, category, unit);
  }

  public String getName() {
    return name;
  }
}
