package ru.aav.account;

/** Безопасный снимок профиля для HTML: хеш пароля и бинарное фото сюда не попадают. */
public record AccountView(long id, String email, String displayName, String phone, String address,
                          boolean hasAvatar, String initials) {
    public static AccountView from(CustomerAccount a) {
        String[] words = a.getDisplayName().strip().split("\\s+");
        String initials = words[0].substring(0, words[0].offsetByCodePoints(0, 1));
        if (words.length > 1) initials += words[words.length - 1].substring(0, words[words.length - 1].offsetByCodePoints(0, 1));
        return new AccountView(a.getId(), a.getEmail(), a.getDisplayName(), a.getPhone(), a.getAddress(),
                a.getAvatar() != null, initials.toUpperCase(java.util.Locale.ROOT));
    }
}
