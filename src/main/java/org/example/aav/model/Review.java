package org.example.aav.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class Review extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id")
  private ProductEntity product;

  @Column(nullable = false)
  private int rating;

  @Column(nullable = false, length = 2000)
  private String text;

  protected Review() {}

  public Review(UserAccount user, ProductEntity product, int rating, String text) {
    this.user = user;
    this.product = product;
    update(rating, text);
  }

  public void update(int rating, String text) {
    this.rating = rating;
    this.text = text;
  }

  public UserAccount getUser() {
    return user;
  }

  public ProductEntity getProduct() {
    return product;
  }

  public int getRating() {
    return rating;
  }

  public String getText() {
    return text;
  }
}
