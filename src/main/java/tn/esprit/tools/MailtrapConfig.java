package tn.esprit.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class MailtrapConfig {
    public static final String SANDBOX_BASE_URL = "https://sandbox.api.mailtrap.io/api/send/";
    public static final String API_KEY_PROPERTY = "mailtrap.api.key";
    public static final String INBOX_ID_PROPERTY = "mailtrap.inbox.id";
    public static final String FROM_PROPERTY = "mailtrap.from";
    public static final String API_KEY_ENV = "MAILTRAP_API_KEY";
    public static final String INBOX_ID_ENV = "MAILTRAP_INBOX_ID";
    public static final String FROM_ENV = "MAILTRAP_FROM";
    public static final String DEFAULT_FROM = "notifications@sport-insight.local";

    private static final Path LOCAL_PROPERTIES_PATH = Path.of("mailtrap.local.properties");

    private MailtrapConfig() {
    }

    public static String resolveApiKey() {
        return firstNonBlank(
                sanitize(System.getProperty(API_KEY_PROPERTY)),
                sanitize(System.getenv(API_KEY_ENV)),
                loadFromFile(API_KEY_PROPERTY),
                loadFromClasspath("/mailtrap.properties", API_KEY_PROPERTY)
        );
    }

    public static Integer resolveInboxId() {
        String rawValue = firstNonBlank(
                sanitize(System.getProperty(INBOX_ID_PROPERTY)),
                sanitize(System.getenv(INBOX_ID_ENV)),
                loadFromFile(INBOX_ID_PROPERTY),
                loadFromClasspath("/mailtrap.properties", INBOX_ID_PROPERTY)
        );
        if (rawValue == null) {
            return null;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static String resolveFromAddress() {
        return firstNonBlank(
                sanitize(System.getProperty(FROM_PROPERTY)),
                sanitize(System.getenv(FROM_ENV)),
                loadFromFile(FROM_PROPERTY),
                loadFromClasspath("/mailtrap.properties", FROM_PROPERTY),
                DEFAULT_FROM
        );
    }

    public static boolean isConfigured() {
        return resolveApiKey() != null && resolveInboxId() != null;
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
        try (InputStream inputStream = MailtrapConfig.class.getResourceAsStream(resourcePath)) {
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
        if (normalized.contains("your_mailtrap_key")
                || normalized.contains("your-mailtrap-key")
                || normalized.contains("your_mailtrap_inbox")
                || normalized.contains("your-mailtrap-inbox")) {
            return null;
        }
        return trimmed;
    }
}
