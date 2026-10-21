package ru.aav.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Данные входящей HTML-формы, отделённые от JPA-сущности. Spring MVC связывает параметры name/price
 * с компонентами record через конструктор. Поля id здесь нет: клиент не может подменить
 * идентификатор при создании товара. Jakarta Bean Validation проверяет ограничения при
 * использовании @Valid. NotBlank исключает пустое/пробельное название; Size ограничивает длину.
 * NotNull требует цену, DecimalMin допускает ноль, Digits ограничивает число цифр. Ошибка
 * преобразования текста в BigDecimal тоже попадает в BindingResult. Форма ограничивает дробную
 * часть двумя знаками; модель дополнительно проверяет точность для вызовов сервиса в обход формы.
 * {@code @NotBlank} запрещает null, пустую строку и строку из пробелов. {@code @Size} ограничивает
 * длину строки или размер коллекции. {@code @NotNull} требует значение, но допускает пустую строку.
 * {@code @DecimalMin} задаёт нижнюю границу цены. {@code @Digits} ограничивает количество целых и
 * дробных цифр.
 */
public record ProductForm(
        /** Название продукта или полки. */
        @NotBlank(message = "Введите название") @Size(max = 255, message = "Не более 255 символов")
                String name,
        /** Цена за единицу в BigDecimal. */
        @NotNull(message = "Введите цену")
                @DecimalMin(value = "0.00", message = "Цена не может быть отрицательной")
                @Digits(
                        integer = 17,
                        fraction = 2,
                        message = "До 17 цифр и двух знаков после запятой")
                BigDecimal price) {}
