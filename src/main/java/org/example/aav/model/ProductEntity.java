package org.example.aav.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Этап 2: связываем товар с таблицей products, чтобы сохранять его через JPA. Учебная последовательность. */
@Entity
@Table(name = "products")
public class ProductEntity extends BaseEntity {
  // Название товара.
  @Column(nullable = false, length = 100)
  private String name;

  // Название в нижнем регистре для поиска без учёта регистра.
  @Column(nullable = false, length = 100)
  private String searchName;

  // Цена товара; bigdecimal позволяет точно хранить копейки.
  @Column(nullable = false, precision = 8, scale = 2)
  private BigDecimal price;

  // Категория товара.
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Category category;

  // Единица продажи, например 1 кг или 1 шт..
  @Column(nullable = false, length = 30)
  private String unit;

  /**
   * Пустой конструктор нужен JPA для загрузки записи из базы.
   */
  protected ProductEntity() {}

  /**
   * Создаёт объект и заполняет его начальными данными.
   * @param p товар для сохранения
   */
  public ProductEntity(Product p) {
    id = p.id();
    name = p.name();
    searchName = name.toLowerCase(java.util.Locale.ROOT);
    price = p.price();
    category = p.category();
    unit = p.unit();
  }

  /**
   * Преобразует запись базы в неизменяемый объект товара для остального приложения.
   */
  public Product toProduct() {
    return new Product(id, name, price, category, unit);
  }

  /**
   * Возвращает название товара.
   */
  public String getName() {
    return name;
  }
}
