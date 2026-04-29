package tn.esprit.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class OpenAiConfig {
    public static final String BASE_URL = "https://api.openai.com/v1/responses";
    public static final String API_KEY_PROPERTY = "openai.api.key";
    public static final String MODEL_PROPERTY = "openai.model";
    public static final String API_KEY_ENV = "OPENAI_API_KEY";
    public static final String MODEL_ENV = "OPENAI_MODEL";
    public static final String DEFAULT_MODEL = "gpt-5";

    private static final Path LOCAL_PROPERTIES_PATH = Path.of("openai.local.properties");

    private OpenAiConfig() {
    }

    public static String resolveApiKey() {
        return firstNonBlank(
                sanitize(System.getProperty(API_KEY_PROPERTY)),
                sanitize(System.getenv(API_KEY_ENV)),
                loadFromFile(API_KEY_PROPERTY),
                loadFromClasspath("/openai.properties", API_KEY_PROPERTY)
        );
    }

    public static String resolveModel() {
        return firstNonBlank(
                sanitize(System.getProperty(MODEL_PROPERTY)),
                sanitize(System.getenv(MODEL_ENV)),
                loadFromFile(MODEL_PROPERTY),
                loadFromClasspath("/openai.properties", MODEL_PROPERTY),
                DEFAULT_MODEL
        );
    }

    public static boolean isConfigured() {
        return resolveApiKey() != null;
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
        try (InputStream inputStream = OpenAiConfig.class.getResourceAsStream(resourcePath)) {
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
        if (normalized.contains("your-openai-key") || normalized.startsWith("sk-your-")) {
            return null;
        }
        return trimmed;
    }
}
