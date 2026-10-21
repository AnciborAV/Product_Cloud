package ru.aav.account;

import jakarta.validation.constraints.*;

/** Отдельные формы не позволяют клиенту менять id, хеш пароля и владельца заказов. */
public final class AccountForms {
    /**
     * Запрещает создавать экземпляры класса-контейнера: в нём сгруппированы типы форм, а
     * собственное состояние отсутствует.
     */
    private AccountForms() {}

    /**
     * Форма регистрации: содержит только разрешённые для ввода поля; пароль исключён из toString().
     * {@code @NotBlank} запрещает null, пустую строку и строку из пробелов. {@code @Size}
     * ограничивает длину строки или размер коллекции. {@code @Email} проверяет форму адреса;
     * существование ящика не подтверждает.
     */
    public record Registration(
            /** Имя пользователя. */
            @NotBlank(message = "Укажите имя") @Size(max = 80, message = "До 80 символов")
                    String displayName,
            /** Email для входа; проверка формата не подтверждает владение ящиком. */
            @NotBlank(message = "Укажите email")
                    @Email(message = "Проверьте email")
                    @Size(max = 254, message = "До 254 символов")
                    String email,
            /** Пароль для хеширования; не сохраняется в открытом виде. */
            @NotBlank(message = "Введите пароль")
                    @Size(min = 8, max = 128, message = "От 8 до 128 символов")
                    String password,
            /** Повтор пароля для проверки совпадения. */
            @NotBlank(message = "Повторите пароль") String confirmation) {
        /**
         * Возвращает безопасное диагностическое описание без паролей и их подтверждения.
         * {@code @Override} позволяет компилятору проверить, что метод переопределяет существующий
         * контракт.
         */
        @Override
        public String toString() {
            return "Registration[credentials redacted]";
        }
    }

    /**
     * Форма изменения имени, телефона и адреса; логин и права доступа через неё изменить нельзя.
     * {@code @NotBlank} запрещает null, пустую строку и строку из пробелов. {@code @Size}
     * ограничивает длину строки или размер коллекции. {@code @NotNull} требует значение, но
     * допускает пустую строку.
     */
    public record Profile(
            /** Имя пользователя. */
            @NotBlank(message = "Укажите имя") @Size(max = 80, message = "До 80 символов")
                    String displayName,
            /** Контактный телефон. */
            @NotNull @Size(max = 40, message = "До 40 символов") String phone,
            /** Контактный адрес. */
            @NotNull @Size(max = 500, message = "До 500 символов") String address) {}

    /**
     * Форма смены пароля; секреты исключены из toString(). {@code @NotBlank} запрещает null, пустую
     * строку и строку из пробелов. {@code @Size} ограничивает длину строки или размер коллекции.
     */
    public record Password(
            /** Текущий пароль для подтверждения смены. */
            @NotBlank(message = "Введите текущий пароль") @Size(max = 128) String currentPassword,
            /** Новый пароль, который будет хеширован. */
            @NotBlank(message = "Введите новый пароль")
                    @Size(min = 8, max = 128, message = "От 8 до 128 символов")
                    String newPassword,
            /** Повтор пароля для проверки совпадения. */
            @NotBlank(message = "Повторите новый пароль") String confirmation) {
        /**
         * Возвращает безопасное диагностическое описание без паролей и их подтверждения.
         * {@code @Override} позволяет компилятору проверить, что метод переопределяет существующий
         * контракт.
         */
        @Override
        public String toString() {
            return "Password[credentials redacted]";
        }
    }
}
