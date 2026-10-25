package org.example.aav.dto;

import jakarta.validation.constraints.*;

/** Этап 3: описываем неизменяемую форму контактов для проверки имени, телефона и email. Учебная последовательность.
 *
 * @param displayName имя пользователя для показа на странице
 * @param lastName фамилия пользователя
 * @param phone контактный телефон
 * @param email адрес электронной почты
 */
public record ContactForm(
    // Имя для показа в профиле.
    @NotBlank @Size(max=100) String displayName,
    // Фамилия для контактов магазина.
    @NotBlank @Size(max=100) String lastName,
    // Телефон; аннотации проверяют допустимые символы и длину.
    @NotBlank @Pattern(regexp="[+0-9() -]{7,30}") String phone,
    // Почта для отправки заказа и подтверждения смены пароля.
    @NotBlank @Email @Size(max=254) String email) {}
