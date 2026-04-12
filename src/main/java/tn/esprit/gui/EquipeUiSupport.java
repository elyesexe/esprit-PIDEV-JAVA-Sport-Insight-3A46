package tn.esprit.gui;

import javafx.scene.image.Image;

import java.io.File;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EquipeUiSupport {
    private static final Path SYMFONY_UPLOADS_DIRECTORY =
            Path.of("C:", "final", "sport_insight_final", "public", "uploads", "equipes");
    private static final Map<String, Optional<Image>> IMAGE_CACHE = new ConcurrentHashMap<>();

    private EquipeUiSupport() {
    }

    public static Image loadEquipeImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String normalizedPath = imagePath.trim();
        Optional<Image> cachedImage = IMAGE_CACHE.get(normalizedPath);
        if (cachedImage != null) {
            return cachedImage.orElse(null);
        }

        Image resolvedImage = resolveEquipeImage(normalizedPath);
        IMAGE_CACHE.put(normalizedPath, Optional.ofNullable(resolvedImage));
        return resolvedImage;
    }

    public static Image loadResourceImage(Class<?> anchor, String resourcePath) {
        if (anchor == null || resourcePath == null || resourcePath.isBlank()) {
            return null;
        }

        URL resource = anchor.getResource(resourcePath);
        if (resource == null) {
            return null;
        }

        return createImage(resource.toExternalForm());
    }

    public static String buildInitials(String teamName, String fallback) {
        String normalizedName = emptyToNull(teamName);
        if (normalizedName == null) {
            return fallback == null || fallback.isBlank() ? "SI" : fallback;
        }

        String[] parts = normalizedName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
            if (initials.length() == 2) {
                break;
            }
        }

        if (initials.length() == 0) {
            return fallback == null || fallback.isBlank() ? "SI" : fallback;
        }

        return initials.toString();
    }

    public static void clearImageCache() {
        IMAGE_CACHE.clear();
    }

    private static Image resolveEquipeImage(String normalizedPath) {
        Image image = loadImageFromUri(normalizedPath);
        if (image != null) {
            return image;
        }

        Path directPath = toPathIfValid(normalizedPath);
        if (directPath != null && directPath.isAbsolute()) {
            image = loadImageFromFile(directPath);
            if (image != null) {
                return image;
            }
        }

        URL resource = resolveResource(normalizedPath);
        if (resource != null) {
            return createImage(resource.toExternalForm());
        }

        if (directPath != null && !directPath.isAbsolute()) {
            image = loadImageFromFile(directPath);
            if (image != null) {
                return image;
            }
        }

        for (Path candidate : buildRelativeCandidates(normalizedPath)) {
            image = loadImageFromFile(candidate);
            if (image != null) {
                return image;
            }
        }

        return null;
    }

    private static URL resolveResource(String imagePath) {
        String[] resourceCandidates = {
                imagePath.startsWith("/") ? imagePath : "/" + imagePath,
                "/tn/esprit/" + imagePath,
                "/tn/esprit/images/" + imagePath,
                "/tn/esprit/uploads/equipes/" + imagePath,
                "/uploads/equipes/" + imagePath
        };

        for (String candidate : resourceCandidates) {
            URL resource = EquipeUiSupport.class.getResource(candidate);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    private static List<Path> buildRelativeCandidates(String imagePath) {
        List<Path> candidates = new ArrayList<>();
        appendCandidate(candidates, Path.of("uploads", "equipes"), imagePath);
        appendCandidate(candidates, Path.of("public", "uploads", "equipes"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources", "tn", "esprit"), imagePath);
        appendCandidate(candidates, Path.of("src", "main", "resources", "tn", "esprit", "images"), imagePath);
        appendCandidate(candidates, SYMFONY_UPLOADS_DIRECTORY, imagePath);
        return candidates;
    }

    private static void appendCandidate(List<Path> candidates, Path base, String imagePath) {
        Path childPath = toPathIfValid(imagePath);
        if (childPath == null || childPath.isAbsolute()) {
            return;
        }

        candidates.add(base.resolve(childPath));
    }

    private static Image loadImageFromUri(String imagePath) {
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://") || imagePath.startsWith("file:/")) {
            return createImage(imagePath);
        }

        return null;
    }

    private static Path toPathIfValid(String pathValue) {
        try {
            return Path.of(pathValue);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private static Image loadImageFromFile(Path path) {
        File file = path.toFile();
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        return createImage(file.toURI().toString());
    }

    private static Image createImage(String imageSource) {
        try {
            Image image = new Image(imageSource, false);
            return image.isError() ? null : image;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
