package tn.esprit.gui;

import javafx.scene.image.Image;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ProductImageResolver {
    private static final String SOURCE_PUBLIC_PROPERTY = "sportinsight.source.public";
    private static final String SOURCE_PUBLIC_ENV = "SPORT_INSIGHT_SOURCE_PUBLIC";
    private static final String DEFAULT_SOURCE_PUBLIC = "C:/final/sport_insight_final/public";
    private static final String DEFAULT_FALLBACK_RELATIVE_PATH = "api/football_ball.png";
    private static final Path WORKSPACE_ROOT = Path.of(System.getProperty("user.dir"));
    private static final List<Path> LOCAL_SEARCH_ROOTS = List.of(
            WORKSPACE_ROOT.resolve("src/main/resources/tn/esprit/images"),
            WORKSPACE_ROOT.resolve("image")
    );

    private ProductImageResolver() {
    }

    public static Image loadImage(Class<?> owner, String imagePath) {
        Image resolved = resolveImage(owner, imagePath);
        return resolved != null ? resolved : resolveImage(owner, DEFAULT_FALLBACK_RELATIVE_PATH);
    }

    private static Image resolveImage(Class<?> owner, String imagePath) {
        String normalizedPath = trimToNull(imagePath);
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

            Path localPath = resolveLocalPath(normalizedPath);
            if (localPath != null) {
                return new Image(localPath.toUri().toString(), true);
            }

            String resourcePath = normalizedPath.startsWith("/") ? normalizedPath : "/tn/esprit/images/" + normalizedPath;
            URL resource = owner.getResource(resourcePath);
            if (resource != null) {
                return new Image(resource.toExternalForm(), true);
            }

            File sourcePublicRoot = resolveSourcePublicRoot();
            if (sourcePublicRoot != null) {
                File sourceFile = new File(sourcePublicRoot, normalizedPath.replace('/', File.separatorChar));
                if (sourceFile.exists()) {
                    return new Image(sourceFile.toURI().toString(), true);
                }
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

    private static File resolveSourcePublicRoot() {
        String configuredPath = trimToNull(System.getProperty(SOURCE_PUBLIC_PROPERTY));
        if (configuredPath == null) {
            configuredPath = trimToNull(System.getenv(SOURCE_PUBLIC_ENV));
        }
        if (configuredPath == null) {
            configuredPath = DEFAULT_SOURCE_PUBLIC;
        }

        File directory = new File(configuredPath);
        return directory.exists() ? directory : null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
