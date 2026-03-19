package ru.sbertech.phonebook.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.nio.charset.StandardCharsets;

/**
 * Утилита хеширования паролей.
 *
 * Новый формат хранения: "hexSalt:hexHash" (PBKDF2WithHmacSHA256, 260 000 итераций, 32-байтовый ключ).
 * Устаревший формат (только 64-символьный hex без ":") — SHA-256 без соли — поддерживается
 * при верификации для обратной совместимости с существующими записями.
 */
public class PasswordUtil {

    private static final int ITERATIONS  = 260_000;
    private static final int KEY_BITS    = 256;
    private static final int SALT_BYTES  = 16;

    private PasswordUtil() {}

    /**
     * Создаёт новый хеш с случайной солью.
     * Возвращает строку вида "hexSalt:hexHash".
     */
    public static String hash(String password) {
        try {
            SecureRandom rng = new SecureRandom();
            byte[] salt = new byte[SALT_BYTES];
            rng.nextBytes(salt);
            byte[] hash = pbkdf2(password, salt);
            return bytesToHex(salt) + ":" + bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка хеширования пароля", e);
        }
    }

    /**
     * Проверяет пароль против сохранённого хеша.
     * Поддерживает оба формата:
     *   — "hexSalt:hexHash" (PBKDF2, новый формат)
     *   — 64-символьный hex без ":" (SHA-256 без соли, устаревший формат)
     */
    public static boolean verify(String password, String stored) {
        if (stored == null || password == null) return false;
        if (stored.contains(":")) {
            try {
                String[] parts = stored.split(":", 2);
                byte[] salt         = hexToBytes(parts[0]);
                byte[] expectedHash = hexToBytes(parts[1]);
                byte[] actualHash   = pbkdf2(password, salt);
                return MessageDigest.isEqual(actualHash, expectedHash);
            } catch (Exception e) {
                return false;
            }
        }
        // Обратная совместимость: устаревший SHA-256 без соли
        return sha256hex(password).equals(stored);
    }

    // ── внутренние вспомогательные методы ────────────────────────

    private static byte[] pbkdf2(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        try {
            return factory.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    static String sha256hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return bytesToHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 недоступен", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return out;
    }
}
