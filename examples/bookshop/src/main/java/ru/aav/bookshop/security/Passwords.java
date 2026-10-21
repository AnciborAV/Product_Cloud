package ru.aav.bookshop.security;

import java.security.*;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2 замедляет подбор; случайная соль делает хеши одинаковых паролей разными. */
public final class Passwords {
    /** Количество итераций PBKDF2: увеличивает стоимость перебора паролей. */
    private static final int ITERATIONS = 600_000;

    /**
     * Запрещает создавать экземпляры служебного класса, содержащего только статические операции.
     */
    private Passwords() {}

    /** Создаёт случайную соль и сохраняет число итераций, соль и PBKDF2-хеш в строковом формате. */
    public static String hash(String password) {
        // Случайная соль: одинаковые пароли получают разные сохранённые хеши.
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return ITERATIONS
                + ":"
                + Base64.getEncoder().encodeToString(salt)
                + ":"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    /**
     * Пересчитывает PBKDF2 по сохранённым параметрам и сравнивает байты через
     * MessageDigest.isEqual.
     */
    public static boolean verify(String password, String stored) {
        // Части компактного строкового формата; используются для восстановления значений.
        String[] parts = stored.split(":");
        return MessageDigest.isEqual(
                Base64.getDecoder().decode(parts[2]),
                derive(password, Base64.getDecoder().decode(parts[1]), Integer.parseInt(parts[0])));
    }

    /** Получает ключ PBKDF2WithHmacSHA256 и очищает копию пароля в PBEKeySpec после вычисления. */
    private static byte[] derive(String password, byte[] salt, int iterations) {
        // Параметры PBKDF2 и временная копия символов пароля; очищаются после расчёта.
        var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        } finally {
            spec.clearPassword();
        }
    }
}
