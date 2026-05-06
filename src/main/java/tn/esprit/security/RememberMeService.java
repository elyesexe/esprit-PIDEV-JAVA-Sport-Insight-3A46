package tn.esprit.security;

import tn.esprit.entities.User;
import tn.esprit.services.UserService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class RememberMeService {
    private static final String PREF_NODE = "tn/esprit/sport-insight/auth";
    private static final String KEY_DEVICE_SECRET = "deviceSecret";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_EXPIRES_AT = "expiresAt";
    private static final String KEY_NONCE = "nonce";
    private static final String KEY_SIGNATURE = "signature";
    private static final String TOKEN_VERSION = "v1";
    private static final Duration TOKEN_TTL = Duration.ofDays(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private RememberMeService() {
    }

    public static void remember(User user) {
        if (user == null || user.getId() == null || isBlank(user.getEmail())) {
            forget();
            return;
        }

        Preferences preferences = preferences();
        long expiresAt = Instant.now().plus(TOKEN_TTL).getEpochSecond();
        String nonce = randomUrlSafeBytes(18);
        String email = normalizeEmail(user.getEmail());
        String payload = payload(user.getId(), email, expiresAt, nonce);
        preferences.putInt(KEY_USER_ID, user.getId());
        preferences.put(KEY_EMAIL, email);
        preferences.putLong(KEY_EXPIRES_AT, expiresAt);
        preferences.put(KEY_NONCE, nonce);
        preferences.put(KEY_SIGNATURE, sign(payload, deviceSecret(preferences)));
        flush(preferences);
    }

    public static User restore() {
        if (!hasStoredToken(preferences())) {
            return null;
        }
        try {
            return restore(new UserService());
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    public static User restore(UserService userService) {
        if (userService == null) {
            return null;
        }

        Preferences preferences = preferences();
        int userId = preferences.getInt(KEY_USER_ID, -1);
        String email = preferences.get(KEY_EMAIL, "");
        long expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L);
        String nonce = preferences.get(KEY_NONCE, "");
        String signature = preferences.get(KEY_SIGNATURE, "");

        if (userId <= 0 || isBlank(email) || expiresAt <= 0 || isBlank(nonce) || isBlank(signature)) {
            return null;
        }
        if (Instant.now().getEpochSecond() >= expiresAt) {
            forget();
            return null;
        }

        String normalizedEmail = normalizeEmail(email);
        String expectedSignature = sign(payload(userId, normalizedEmail, expiresAt, nonce), deviceSecret(preferences));
        if (!constantTimeEquals(signature, expectedSignature)) {
            forget();
            return null;
        }

        try {
            User user = userService.getById(userId);
            if (user == null || !user.isActiveAccount() || !Objects.equals(normalizedEmail, normalizeEmail(user.getEmail()))) {
                forget();
                return null;
            }
            return user;
        } catch (SQLException ex) {
            return null;
        }
    }

    public static void forget() {
        Preferences preferences = preferences();
        preferences.remove(KEY_USER_ID);
        preferences.remove(KEY_EMAIL);
        preferences.remove(KEY_EXPIRES_AT);
        preferences.remove(KEY_NONCE);
        preferences.remove(KEY_SIGNATURE);
        flush(preferences);
    }

    private static String payload(int userId, String email, long expiresAt, String nonce) {
        return TOKEN_VERSION + "\n" + userId + "\n" + email + "\n" + expiresAt + "\n" + nonce;
    }

    private static String deviceSecret(Preferences preferences) {
        String secret = preferences.get(KEY_DEVICE_SECRET, "");
        if (!isBlank(secret)) {
            return secret;
        }
        secret = randomUrlSafeBytes(32);
        preferences.put(KEY_DEVICE_SECRET, secret);
        flush(preferences);
        return secret;
    }

    private static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign remember-me token.", ex);
        }
    }

    private static String randomUrlSafeBytes(int byteCount) {
        byte[] bytes = new byte[byteCount];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(leftBytes, rightBytes);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean hasStoredToken(Preferences preferences) {
        return preferences.getInt(KEY_USER_ID, -1) > 0
                && !isBlank(preferences.get(KEY_EMAIL, ""))
                && preferences.getLong(KEY_EXPIRES_AT, 0L) > 0
                && !isBlank(preferences.get(KEY_NONCE, ""))
                && !isBlank(preferences.get(KEY_SIGNATURE, ""));
    }

    private static Preferences preferences() {
        return Preferences.userRoot().node(PREF_NODE);
    }

    private static void flush(Preferences preferences) {
        try {
            preferences.flush();
        } catch (BackingStoreException ignored) {
            // Preferences will retry persistence on the next update.
        }
    }
}
