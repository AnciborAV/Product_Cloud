package ru.aav.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductForm(
        @NotBlank(message = "Введите название")
        @Size(max = 255, message = "Не более 255 символов") String name,
        @NotNull(message = "Введите цену")
        @DecimalMin(value = "0.00", message = "Цена не может быть отрицательной")
        @Digits(integer = 17, fraction = 2, message = "До 17 цифр и двух знаков после запятой") BigDecimal price) {
}
