package ru.aav.bookshop.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.Base64;

/** PBKDF2 замедляет подбор; случайная соль делает хеши одинаковых паролей разными. */
public final class Passwords {
    private static final int ITERATIONS = 600_000;
    private Passwords() {}
    public static String hash(String password) {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" +
            Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }
    public static boolean verify(String password, String stored) {
        String[] parts = stored.split(":");
        return MessageDigest.isEqual(Base64.getDecoder().decode(parts[2]),
            derive(password, Base64.getDecoder().decode(parts[1]), Integer.parseInt(parts[0])));
    }
    private static byte[] derive(String password, byte[] salt, int iterations) {
        var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException exception) { throw new IllegalStateException(exception); }
        finally { spec.clearPassword(); }
    }
}
