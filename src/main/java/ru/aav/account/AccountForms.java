package ru.aav.account;

import jakarta.validation.constraints.*;

/** Отдельные формы не позволяют клиенту менять id, хеш пароля и владельца заказов. */
public final class AccountForms {
    private AccountForms() { }
    public record Registration(
            @NotBlank(message = "Укажите имя") @Size(max = 80, message = "До 80 символов") String displayName,
            @NotBlank(message = "Укажите email") @Email(message = "Проверьте email")
            @Size(max = 254, message = "До 254 символов") String email,
            @NotBlank(message = "Введите пароль") @Size(min = 8, max = 128, message = "От 8 до 128 символов") String password,
            @NotBlank(message = "Повторите пароль") String confirmation) {
        @Override public String toString() { return "Registration[credentials redacted]"; }
    }
    public record Profile(
            @NotBlank(message = "Укажите имя") @Size(max = 80, message = "До 80 символов") String displayName,
            @NotNull @Size(max = 40, message = "До 40 символов") String phone,
            @NotNull @Size(max = 500, message = "До 500 символов") String address) { }
    public record Password(
            @NotBlank(message = "Введите текущий пароль") @Size(max = 128) String currentPassword,
            @NotBlank(message = "Введите новый пароль") @Size(min = 8, max = 128, message = "От 8 до 128 символов") String newPassword,
            @NotBlank(message = "Повторите новый пароль") String confirmation) {
        @Override public String toString() { return "Password[credentials redacted]"; }
    }
}
