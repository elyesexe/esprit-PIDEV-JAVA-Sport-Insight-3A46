package tn.esprit.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class StripeConfig {
    public static final String SECRET_KEY_PROPERTY = "stripe.secret.key";
    public static final String SECRET_KEY_ENV = "STRIPE_SECRET_KEY";
    public static final String API_KEY_ENV = "STRIPE_API_KEY";

    private static final Path LOCAL_PROPERTIES_PATH = Path.of("stripe.local.properties");

    private StripeConfig() {
    }

    public static String resolveSecretKey() {
        return firstNonBlank(
                sanitize(System.getProperty(SECRET_KEY_PROPERTY)),
                sanitize(System.getenv(SECRET_KEY_ENV)),
                sanitize(System.getenv(API_KEY_ENV)),
                loadFromFile(SECRET_KEY_PROPERTY),
                loadFromClasspath("/stripe.properties", SECRET_KEY_PROPERTY)
        );
    }

    public static boolean isConfigured() {
        return resolveSecretKey() != null;
    }

    private static String loadFromFile(String key) {
        if (!Files.exists(LOCAL_PROPERTIES_PATH)) {
            return null;
        }
        try (InputStream inputStream = Files.newInputStream(LOCAL_PROPERTIES_PATH)) {
            return loadFromStream(inputStream, key);
        } catch (IOException e) {
            return null;
        }
    }

    private static String loadFromClasspath(String resourcePath, String key) {
        try (InputStream inputStream = StripeConfig.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return null;
            }
            return loadFromStream(inputStream, key);
        } catch (IOException e) {
            return null;
        }
    }

    private static String loadFromStream(InputStream inputStream, String key) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        return sanitize(properties.getProperty(key));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = sanitize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String normalized = trimmed.toLowerCase();
        if (normalized.contains("your-stripe-secret")
                || normalized.contains("your_stripe_secret")
                || normalized.startsWith("sk_test_your")
                || normalized.startsWith("sk_live_your")) {
            return null;
        }
        return trimmed;
    }
}
