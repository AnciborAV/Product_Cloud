package org.example.aav.model;

import jakarta.persistence.*;

/** Этап 2: связываем пользователя с выбранным товаром для его личного списка. Учебная последовательность. */
@Entity
@Table(
    name = "selected_products",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class SelectedProduct extends BaseEntity {
  // Учётная запись пользователя.
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  // Товар, с которым выполняется действие.
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id")
  private ProductEntity product;

  // Название товара на момент добавления в список.
  @Column(nullable = false, length = 100)
  private String productName;

  /**
   * Возвращает название товара на момент добавления в список.
   */
  public String getProductName() { return productName; }

  /**
   * Пустой конструктор нужен JPA для загрузки записи из базы.
   */
  protected SelectedProduct() {}

  /**
   * Создаёт объект и заполняет его начальными данными.
   * @param user учётная запись пользователя
   * @param product товар, с которым выполняется действие
   */
  public SelectedProduct(UserAccount user, ProductEntity product) {
    this.user = user;
    this.product = product;
    this.productName = product.getName();
  }

  /**
   * Возвращает товар, связанный с этой записью.
   */
  public ProductEntity getProduct() {
    return product;
  }
}
