package tn.esprit.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class FootballDataConfig {
    public static final String SOURCE = "football-data.org";
    public static final String BASE_URL = "https://api.football-data.org/v4";
    public static final String API_KEY_PROPERTY = "football.data.api.key";
    public static final String API_KEY_ENV = "FOOTBALL_DATA_API_KEY";

    private static final Path LOCAL_PROPERTIES_PATH = Path.of("football-data.local.properties");
    private static final String[] PROPERTY_KEYS = {"api.key", API_KEY_PROPERTY};

    private FootballDataConfig() {
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

        String classpathValue = loadFromClasspath("/football-data.properties");
        if (classpathValue != null) {
            return classpathValue;
        }

        throw new IllegalStateException(
                "Aucune cle API football-data.org n'a ete trouvee. " +
                        "Configurez " + API_KEY_ENV + ", la propriete JVM " + API_KEY_PROPERTY +
                        " ou le fichier football-data.local.properties."
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
        try (InputStream inputStream = FootballDataConfig.class.getResourceAsStream(resourcePath)) {
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
