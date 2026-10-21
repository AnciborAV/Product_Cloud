package ru.aav.order;

import jakarta.validation.constraints.*;

/**
 * Контакты для заказа. Цены, сумма, владелец и статус не принимаются от браузера. {@code @NotBlank}
 * запрещает null, пустую строку и строку из пробелов. {@code @Size} ограничивает длину строки или
 * размер коллекции. {@code @NotNull} требует значение, но допускает пустую строку. {@code @Pattern}
 * проверяет соответствие регулярному выражению.
 */
public record CheckoutForm(
        /** Получатель заказа. */
        @NotBlank(message = "Укажите получателя") @Size(max = 80) String recipient,
        /** Контактный телефон. */
        @NotBlank(message = "Укажите телефон") @Size(max = 40) String phone,
        /** Контактный адрес. */
        @NotBlank(message = "Укажите адрес") @Size(max = 500) String address,
        /** Комментарий к заказу. */
        @NotNull @Size(max = 1000, message = "Комментарий — до 1000 символов") String comment,
        /** Токен оформления, который должен принадлежать текущей сессии. */
        @NotBlank @Pattern(regexp = "[a-f0-9-]{36}") String token) {}
