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
    private static final String DEFAULT_FALLBACK_RELATIVE_PATH = "store.png";
    private static final String USER_HOME = System.getProperty("user.home");
    private static final Path WORKSPACE_ROOT = Path.of(System.getProperty("user.dir"));
    private static final List<Path> LOCAL_SEARCH_ROOTS = List.of(
            WORKSPACE_ROOT.resolve("src/main/resources/tn/esprit/images"),
            WORKSPACE_ROOT.resolve("image")
    );
    private static final List<Path> DEFAULT_SOURCE_PUBLIC_CANDIDATES = List.of(
            Path.of(USER_HOME, "Downloads", "Esprit-PIDEV-3A46-2526-sport_insight-final (1)",
                    "Esprit-PIDEV-3A46-2526-sport_insight-final", "public"),
            Path.of(USER_HOME, "Downloads", "sport_insight-integration-v1.1", "public"),
            Path.of(USER_HOME, "OneDrive", "Desktop", "projects", "final",
                    "Esprit-PIDEV-3A46-2526-sport_insight", "public"),
            Path.of(USER_HOME, "OneDrive", "Documents", "GitHub", "sport_insight", "public")
    );

    private ProductImageResolver() {
    }

    public static Image loadImage(Class<?> owner, String imagePath) {
        Image resolved = resolveImage(owner, imagePath);
        return resolved != null ? resolved : resolveImage(owner, DEFAULT_FALLBACK_RELATIVE_PATH);
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
        } catch (Exception ignored) {
        }

        return null;
    }

    private static Path resolveLocalPath(String normalizedPath) {
        for (Path root : LOCAL_SEARCH_ROOTS) {
            Path candidate = root.resolve(normalizedPath).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }

            Path fileNameCandidate = root.resolve(Path.of(normalizedPath).getFileName() == null
                    ? normalizedPath
                    : Path.of(normalizedPath).getFileName().toString()).normalize();
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
        return normalized.replace('\\', '/');
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
