package tn.esprit.gui;

import javafx.scene.image.Image;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ProductImageResolver {
    private static final String SOURCE_PUBLIC_PROPERTY = "sportinsight.source.public";
    private static final String SOURCE_PUBLIC_ENV = "SPORT_INSIGHT_SOURCE_PUBLIC";
    private static final String API_BASE_PROPERTY = "sportinsight.api.base";
    private static final String API_BASE_ENV = "SPORT_INSIGHT_API_BASE";
    private static final String DEFAULT_FALLBACK_RELATIVE_PATH = "store.png";
    private static final String USER_HOME = System.getProperty("user.home");
    private static final Path WORKSPACE_ROOT = Path.of(System.getProperty("user.dir"));
    private static final List<Path> LOCAL_SEARCH_ROOTS = List.of(
            WORKSPACE_ROOT.resolve("src/main/resources/tn/esprit/images"),
            WORKSPACE_ROOT.resolve("src/main/resources/tn/esprit/images/api"),
            WORKSPACE_ROOT.resolve("src/main/resources/tn/esprit/images/products"),
            WORKSPACE_ROOT.resolve("target/classes/tn/esprit/images"),
            WORKSPACE_ROOT.resolve("public"),
            WORKSPACE_ROOT.resolve("public/api"),
            WORKSPACE_ROOT.resolve("public/uploads"),
            WORKSPACE_ROOT.resolve("uploads"),
            WORKSPACE_ROOT.resolve("uploads/products"),
            WORKSPACE_ROOT.resolve("image")
    );
    private static final List<String> DEFAULT_API_BASE_URLS = List.of(
            "http://127.0.0.1:8000",
            "http://localhost:8000"
    );
    private static final List<Path> DEFAULT_SOURCE_PUBLIC_CANDIDATES = List.of(
            Path.of("C:", "sport insight", "sport_insight", "public"),
            Path.of("C:", "sport insight", "sport_insight", "sport_insight-gestion-produit-orders", "public"),
            Path.of("C:", "sport insight", "sport_insight", "sport_insight-gestion-produit-orders",
                    "sport_insight-gestion-produit-orders", "public"),
            Path.of(USER_HOME, "Downloads", "Esprit-PIDEV-3A46-2526-sport_insight-final (1)",
                    "Esprit-PIDEV-3A46-2526-sport_insight-final", "public"),
            Path.of(USER_HOME, "Downloads", "sport_insight-integration-v1.1", "public"),
            Path.of(USER_HOME, "OneDrive", "Desktop", "projects", "final",
                    "Esprit-PIDEV-3A46-2526-sport_insight", "public"),
            Path.of(USER_HOME, "OneDrive", "Documents", "GitHub", "sport_insight", "public")
    );
    private static boolean apiBaseDetectionComplete;
    private static String detectedApiBaseUrl;

    private ProductImageResolver() {
    }

    public static Image loadImage(Class<?> owner, String imagePath) {
        String normalizedPath = normalizePath(trimToNull(imagePath));
        if (normalizedPath == null) {
            return resolveImage(owner, DEFAULT_FALLBACK_RELATIVE_PATH);
        }
        return resolveImage(owner, normalizedPath);
    }

    private static Image resolveImage(Class<?> owner, String imagePath) {
        String normalizedPath = normalizePath(trimToNull(imagePath));
        if (owner == null || normalizedPath == null) {
            return null;
        }

        try {
            if (normalizedPath.startsWith("http://")
                    || normalizedPath.startsWith("https://")
                    || normalizedPath.startsWith("file:")) {
                return new Image(normalizedPath, true);
            }

            String apiUrl = resolveApiUrl(normalizedPath);
            if (apiUrl != null) {
                return new Image(apiUrl, true);
            }

            File directFile = new File(normalizedPath);
            if (directFile.exists()) {
                return new Image(directFile.toURI().toString(), true);
            }

            Path sourcePublicPath = resolveSourcePublicPath(normalizedPath);
            if (sourcePublicPath != null) {
                return new Image(sourcePublicPath.toUri().toString(), true);
            }

            Path localPath = resolveLocalPath(normalizedPath);
            if (localPath != null) {
                return new Image(localPath.toUri().toString(), true);
            }

            String resourcePath = normalizedPath.startsWith("/") ? normalizedPath : "/tn/esprit/images/" + normalizedPath;
            URL resource = owner.getResource(resourcePath);
            if (resource != null) {
                return new Image(resource.toExternalForm(), true);
            }

            String fileName = fileNameOf(normalizedPath);
            if (fileName != null && !fileName.equals(normalizedPath)) {
                String imageResourcePath = "/tn/esprit/images/" + fileName;
                URL imageResource = owner.getResource(imageResourcePath);
                if (imageResource != null) {
                    return new Image(imageResource.toExternalForm(), true);
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static String resolveApiUrl(String normalizedPath) {
        if (normalizedPath == null || !normalizedPath.startsWith("api/")) {
            return null;
        }

        String configuredBaseUrl = trimToNull(System.getProperty(API_BASE_PROPERTY));
        if (configuredBaseUrl == null) {
            configuredBaseUrl = trimToNull(System.getenv(API_BASE_ENV));
        }
        if (configuredBaseUrl != null) {
            return joinUrl(configuredBaseUrl, normalizedPath);
        }

        String detectedBaseUrl = detectApiBaseUrl(normalizedPath);
        return detectedBaseUrl == null ? null : joinUrl(detectedBaseUrl, normalizedPath);
    }

    private static synchronized String detectApiBaseUrl(String normalizedPath) {
        if (!apiBaseDetectionComplete) {
            for (String candidate : DEFAULT_API_BASE_URLS) {
                if (isApiServerReachable(candidate, normalizedPath)) {
                    detectedApiBaseUrl = candidate;
                    break;
                }
            }
            apiBaseDetectionComplete = true;
        }
        return detectedApiBaseUrl;
    }

    private static boolean isApiServerReachable(String baseUrl, String normalizedPath) {
        try {
            URL url = new URL(joinUrl(baseUrl, normalizedPath));
            var connection = url.openConnection();
            connection.setConnectTimeout(700);
            connection.setReadTimeout(700);
            connection.connect();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String joinUrl(String baseUrl, String normalizedPath) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = normalizedPath.startsWith("/") ? normalizedPath : "/" + normalizedPath;
        return base + path;
    }

    private static Path resolveLocalPath(String normalizedPath) {
        for (Path root : LOCAL_SEARCH_ROOTS) {
            Path candidate = root.resolve(normalizedPath).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }

            String fileName = fileNameOf(normalizedPath);
            Path fileNameCandidate = root.resolve(fileName == null ? normalizedPath : fileName).normalize();
            if (Files.exists(fileNameCandidate)) {
                return fileNameCandidate;
            }
        }
        return null;
    }

    private static Path resolveSourcePublicPath(String normalizedPath) {
        if (normalizedPath == null) {
            return null;
        }
        for (Path root : resolveSourcePublicRoots()) {
            Path candidate = root.resolve(normalizedPath).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }

            String fileName = fileNameOf(normalizedPath);
            if (fileName != null) {
                Path fileNameCandidate = root.resolve(fileName).normalize();
                if (Files.exists(fileNameCandidate)) {
                    return fileNameCandidate;
                }
            }
        }
        return null;
    }

    private static List<Path> resolveSourcePublicRoots() {
        Set<Path> roots = new LinkedHashSet<>();
        String configuredPath = trimToNull(System.getProperty(SOURCE_PUBLIC_PROPERTY));
        if (configuredPath == null) {
            configuredPath = trimToNull(System.getenv(SOURCE_PUBLIC_ENV));
        }
        if (configuredPath != null) {
            Path configuredRoot = Path.of(configuredPath).normalize();
            if (Files.exists(configuredRoot)) {
                roots.add(configuredRoot);
            }
        }

        for (Path candidate : DEFAULT_SOURCE_PUBLIC_CANDIDATES) {
            if (Files.exists(candidate)) {
                roots.add(candidate.normalize());
            }
        }

        return new ArrayList<>(roots);
    }

    private static String normalizePath(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized.startsWith("/") && !normalized.matches("^[A-Za-z]:/.*")
                ? normalized.substring(1)
                : normalized;
    }

    private static String fileNameOf(String normalizedPath) {
        if (normalizedPath == null || normalizedPath.isBlank()) {
            return null;
        }
        try {
            Path fileName = Path.of(normalizedPath).getFileName();
            return fileName == null ? null : fileName.toString();
        } catch (Exception ignored) {
            int slashIndex = normalizedPath.lastIndexOf('/');
            return slashIndex >= 0 ? normalizedPath.substring(slashIndex + 1) : normalizedPath;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
