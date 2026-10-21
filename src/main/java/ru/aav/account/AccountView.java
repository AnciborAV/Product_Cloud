package ru.aav.account;

import java.util.Locale;

/** Безопасный снимок профиля для HTML: хеш пароля и бинарное фото сюда не попадают. */
public record AccountView(
        /** Идентификатор записи. */
        long id,
        /** Email для входа; проверка формата не подтверждает владение ящиком. */
        String email,
        /** Имя пользователя. */
        String displayName,
        /** Контактный телефон. */
        String phone,
        /** Контактный адрес. */
        String address,
        /** Признак наличия фотографии. */
        boolean hasAvatar,
        /** Инициалы для отображения вместо фотографии. */
        String initials) {
    /**
     * Создаёт безопасное представление профиля и инициалы по Unicode-кодовым точкам, не обрезая
     * суррогатную пару символа.
     */
    public static AccountView from(CustomerAccount account) {
        // Слова имени после удаления крайних пробелов; нужны для первой и последней инициалов.
        String[] words = account.getDisplayName().strip().split("\\s+");
        // Инициалы пользователя для замены отсутствующего фото.
        String initials = words[0].substring(0, words[0].offsetByCodePoints(0, 1));
        if (words.length > 1)
            initials +=
                    words[words.length - 1].substring(
                            0, words[words.length - 1].offsetByCodePoints(0, 1));
        return new AccountView(
                account.getId(),
                account.getEmail(),
                account.getDisplayName(),
                account.getPhone(),
                account.getAddress(),
                account.hasAvatar(),
                initials.toUpperCase(Locale.ROOT));
    }
}
