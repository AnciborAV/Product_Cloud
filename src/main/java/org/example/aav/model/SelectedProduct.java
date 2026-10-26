package org.example.aav.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "selected_products",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class SelectedProduct extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id")
  private ProductEntity product;

  @Column(nullable = false, length = 100)
  private String productName;

  public String getProductName() { return productName; }

  protected SelectedProduct() {}

  public SelectedProduct(UserAccount user, ProductEntity product) {
    this.user = user;
    this.product = product;
    this.productName = product.getName();
  }

  public ProductEntity getProduct() {
    return product;
  }
}
