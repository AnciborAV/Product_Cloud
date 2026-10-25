package org.example.aav.model;

import jakarta.persistence.*;

/** Этап 2: связываем автора, товар, оценку и текст для хранения отзывов. Учебная последовательность. */
@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class Review extends BaseEntity {
  // Учётная запись пользователя.
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private UserAccount user;

  // Товар, с которым выполняется действие.
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id")
  private ProductEntity product;

  // Оценка товара от 1 до 5.
  @Column(nullable = false)
  private int rating;

  // Текст отзыва или сообщения.
  @Column(nullable = false, length = 2000)
  private String text;

  /**
   * Пустой конструктор нужен JPA для загрузки записи из базы.
   */
  protected Review() {}

  /**
   * Создаёт объект и заполняет его начальными данными.
   * @param user учётная запись пользователя
   * @param product товар, с которым выполняется действие
   * @param rating оценка товара от 1 до 5
   * @param text текст отзыва или сообщения
   */
  public Review(UserAccount user, ProductEntity product, int rating, String text) {
    this.user = user;
    this.product = product;
    update(rating, text);
  }

  /**
   * Меняет оценку и текст существующего отзыва.
   * @param rating оценка товара от 1 до 5
   * @param text текст отзыва или сообщения
   */
  public void update(int rating, String text) {
    this.rating = rating;
    this.text = text;
  }

  /**
   * Возвращает учётную запись пользователя.
   */
  public UserAccount getUser() {
    return user;
  }

  /**
   * Возвращает товар, связанный с этой записью.
   */
  public ProductEntity getProduct() {
    return product;
  }

  /**
   * Возвращает оценку товара от 1 до 5.
   */
  public int getRating() {
    return rating;
  }

  /**
   * Возвращает текст отзыва или сообщения.
   */
  public String getText() {
    return text;
  }
}
