package org.example.aav.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.example.aav.model.Category;

public class ProductForm {
  @NotBlank(message = "Введите название товара")
  @Size(max = 100, message = "Название — не более 100 символов")
  private String name;

  @NotNull(message = "Введите цену")
  @DecimalMin(value = "0.01", message = "Цена должна быть не меньше 0,01 ₽")
  @DecimalMax(value = "999999.99", message = "Цена должна быть не больше 999 999,99 ₽")
  @Digits(integer = 6, fraction = 2, message = "Укажите цену с точностью до копеек")
  private BigDecimal price;

  @NotNull(message = "Выберите категорию")
  private Category category;

  @NotBlank(message = "Укажите единицу продажи")
  @Size(max = 30, message = "Единица продажи — не более 30 символов")
  private String unit = "1 шт.";

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }
}
