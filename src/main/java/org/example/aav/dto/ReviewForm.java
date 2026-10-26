package org.example.aav.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public class ReviewForm {
  @NotNull private UUID productId;

  @Min(1)
  @Max(5)
  private int rating = 5;

  @NotBlank(message = "Напишите отзыв")
  @Size(max = 2000, message = "Отзыв: не более 2000 символов")
  private String text;

  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID v) {
    productId = v;
  }

  public int getRating() {
    return rating;
  }

  public void setRating(int v) {
    rating = v;
  }

  public String getText() {
    return text;
  }

  public void setText(String v) {
    text = v;
  }
}
