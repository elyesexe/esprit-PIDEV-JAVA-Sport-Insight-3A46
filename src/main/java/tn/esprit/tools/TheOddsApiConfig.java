package tn.esprit.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class TheOddsApiConfig {
    public static final String SOURCE = "the-odds-api";
    public static final String BASE_URL = "https://api.the-odds-api.com/v4";
    public static final String API_KEY_PROPERTY = "the.odds.api.key";
    public static final String API_KEY_ENV = "THE_ODDS_API_KEY";

    private static final Path LOCAL_PROPERTIES_PATH = Path.of("the-odds-api.local.properties");
    private static final Path SHARED_PROPERTIES_PATH = Path.of("football-data.local.properties");
    private static final String[] PROPERTY_KEYS = {
            API_KEY_PROPERTY,
            "the-odds-api.key",
            "theOddsApi.key"
    };

    private TheOddsApiConfig() {
    }

    public static String resolveApiKey() {
        String systemProperty = sanitize(System.getProperty(API_KEY_PROPERTY));
        if (systemProperty != null) {
            return systemProperty;
        }

        String environmentValue = sanitize(System.getenv(API_KEY_ENV));
        if (environmentValue != null) {
            return environmentValue;
        }

        String localValue = loadFromFile(LOCAL_PROPERTIES_PATH);
        if (localValue != null) {
            return localValue;
        }

        String sharedFileValue = loadFromFile(SHARED_PROPERTIES_PATH);
        if (sharedFileValue != null) {
            return sharedFileValue;
        }

        String classpathValue = loadFromClasspath("/the-odds-api.properties");
        if (classpathValue != null) {
            return classpathValue;
        }

        throw new IllegalStateException(
                "Aucune cle The Odds API n'a ete trouvee. Configurez " + API_KEY_ENV
                        + ", la propriete JVM " + API_KEY_PROPERTY
                        + " ou le fichier the-odds-api.local.properties."
        );
    }

    private static String loadFromFile(Path path) {
        if (!Files.exists(path)) {
            return null;
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            return loadFromStream(inputStream);
        } catch (IOException e) {
            return null;
        }
    }

    private static String loadFromClasspath(String resourcePath) {
        try (InputStream inputStream = TheOddsApiConfig.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return null;
            }
            return loadFromStream(inputStream);
        } catch (IOException e) {
            return null;
        }
    }

    private static String loadFromStream(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        for (String key : PROPERTY_KEYS) {
            String value = sanitize(properties.getProperty(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
