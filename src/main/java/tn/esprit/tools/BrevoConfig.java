package tn.esprit.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class BrevoConfig {
    public static final String BASE_URL = "https://api.brevo.com/v3/smtp/email";
    public static final String API_KEY_PROPERTY = "brevo.api.key";
    public static final String FROM_PROPERTY = "brevo.from";
    public static final String API_KEY_ENV = "BREVO_API_KEY";
    public static final String FROM_ENV = "BREVO_FROM";

    private static final Path LOCAL_PROPERTIES_PATH = Path.of("brevo.local.properties");

    private BrevoConfig() {
    }

    public static String resolveApiKey() {
        return firstNonBlank(
                sanitize(System.getProperty(API_KEY_PROPERTY)),
                sanitize(System.getenv(API_KEY_ENV)),
                loadFromFile(API_KEY_PROPERTY),
                loadFromClasspath("/brevo.properties", API_KEY_PROPERTY)
        );
    }

    public static String resolveFromAddress() {
        return firstNonBlank(
                sanitize(System.getProperty(FROM_PROPERTY)),
                sanitize(System.getenv(FROM_ENV)),
                loadFromFile(FROM_PROPERTY),
                loadFromClasspath("/brevo.properties", FROM_PROPERTY)
        );
    }

    public static boolean isConfigured() {
        return resolveApiKey() != null && resolveFromAddress() != null;
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
        try (InputStream inputStream = BrevoConfig.class.getResourceAsStream(resourcePath)) {
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
        if (normalized.contains("your_brevo_key")
                || normalized.contains("your-brevo-key")
                || normalized.contains("xkeysib-your")
                || normalized.contains("orders@example.com")) {
            return null;
        }
        return trimmed;
    }
}
