package tn.esprit.tools;

public final class FaceIdConfig {
    public static final String API_BASE_PROPERTY = "face.id.api.base";
    public static final String API_BASE_ENV = "FACE_ID_API_BASE";
    public static final String DEFAULT_API_BASE = "http://127.0.0.1:8000";

    private FaceIdConfig() {
    }

    public static String resolveApiBase() {
        String propertyValue = sanitize(System.getProperty(API_BASE_PROPERTY));
        if (propertyValue != null) {
            return propertyValue;
        }

        String environmentValue = sanitize(System.getenv(API_BASE_ENV));
        if (environmentValue != null) {
            return environmentValue;
        }
        return DEFAULT_API_BASE;
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

