package ru.aav.order;

import jakarta.validation.constraints.*;

/** Контакты для заказа. Цены, сумма, владелец и статус не принимаются от браузера. */
public record CheckoutForm(
        @NotBlank(message = "Укажите получателя") @Size(max = 80) String recipient,
        @NotBlank(message = "Укажите телефон") @Size(max = 40) String phone,
        @NotBlank(message = "Укажите адрес") @Size(max = 500) String address,
        @NotNull @Size(max = 1000, message = "Комментарий — до 1000 символов") String comment,
        @NotBlank @Pattern(regexp = "[a-f0-9-]{36}") String token) { }
