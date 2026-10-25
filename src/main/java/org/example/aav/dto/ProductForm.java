package org.example.aav.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.example.aav.model.Category;

/** Этап 3: описываем поля формы товара и правила проверки пользовательского ввода. Учебная последовательность. */
public class ProductForm {
  // Название товара.
  @NotBlank(message = "Введите название товара")
  @Size(max = 100, message = "Название — не более 100 символов")
  private String name;

  // Цена товара; bigdecimal позволяет точно хранить копейки.
  @NotNull(message = "Введите цену")
  @DecimalMin(value = "0.01", message = "Цена должна быть не меньше 0,01 ₽")
  @DecimalMax(value = "999999.99", message = "Цена должна быть не больше 999 999,99 ₽")
  @Digits(integer = 6, fraction = 2, message = "Укажите цену с точностью до копеек")
  private BigDecimal price;

  // Категория товара.
  @NotNull(message = "Выберите категорию")
  private Category category;

  // Единица продажи, например 1 кг или 1 шт..
  @NotBlank(message = "Укажите единицу продажи")
  @Size(max = 30, message = "Единица продажи — не более 30 символов")
  private String unit = "1 шт.";

  /**
   * Возвращает название товара.
   */
  public String getName() {
    return name;
  }

  /**
   * Сохраняет новое название товара в объекте.
   * @param name название товара
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Возвращает цену товара; BigDecimal позволяет точно хранить копейки.
   */
  public BigDecimal getPrice() {
    return price;
  }

  /**
   * Записывает цену товара; BigDecimal позволяет точно хранить копейки.
   * @param price цена товара; BigDecimal позволяет точно хранить копейки
   */
  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  /**
   * Возвращает категорию товара.
   */
  public Category getCategory() {
    return category;
  }

  /**
   * Записывает категорию товара.
   * @param category категория товара
   */
  public void setCategory(Category category) {
    this.category = category;
  }

  /**
   * Возвращает единицу продажи, например 1 кг или 1 шт..
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Записывает единицу продажи, например 1 кг или 1 шт..
   * @param unit единица продажи, например 1 кг или 1 шт.
   */
  public void setUnit(String unit) {
    this.unit = unit;
  }
}
