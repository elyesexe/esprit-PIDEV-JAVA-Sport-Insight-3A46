package tn.esprit.security;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;

public final class PasswordSupport {
    private static final int DEFAULT_COST = 13;

    private PasswordSupport() {
    }

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(DEFAULT_COST));
    }

    public static boolean matches(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            try {
                return BCrypt.checkpw(plainPassword, normalizePrefix(storedPassword));
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        return Objects.equals(plainPassword, storedPassword);
    }

    public static String prepareForStorage(String rawPasswordOrHash) {
        if (rawPasswordOrHash == null || rawPasswordOrHash.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        return isBcryptHash(rawPasswordOrHash) ? rawPasswordOrHash : hashPassword(rawPasswordOrHash);
    }

    public static boolean isBcryptHash(String value) {
        if (value == null) {
            return false;
        }
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    private static String normalizePrefix(String hash) {
        return hash.startsWith("$2y$") ? "$2a$" + hash.substring(4) : hash;
    }
}
