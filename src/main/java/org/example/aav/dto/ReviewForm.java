package org.example.aav.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

/** Этап 3: описываем форму отзыва, чтобы проверить товар, оценку и текст. Учебная последовательность. */
public class ReviewForm {
  // Уникальный номер товара.
  @NotNull private UUID productId;

  // Оценка товара от 1 до 5.
  @Min(1)
  @Max(5)
  private int rating = 5;

  // Текст отзыва или сообщения.
  @NotBlank(message = "Напишите отзыв")
  @Size(max = 2000, message = "Отзыв: не более 2000 символов")
  private String text;

  /**
   * Возвращает уникальный номер товара.
   */
  public UUID getProductId() {
    return productId;
  }

  /**
   * Записывает уникальный номер товара.
   * @param v номер выбранного товара
   */
  public void setProductId(UUID v) {
    productId = v;
  }

  /**
   * Возвращает оценку товара от 1 до 5.
   */
  public int getRating() {
    return rating;
  }

  /**
   * Записывает оценку товара от 1 до 5.
   * @param v новая оценка товара
   */
  public void setRating(int v) {
    rating = v;
  }

  /**
   * Возвращает текст отзыва или сообщения.
   */
  public String getText() {
    return text;
  }

  /**
   * Записывает текст отзыва или сообщения.
   * @param v новый текст отзыва
   */
  public void setText(String v) {
    text = v;
  }
}
