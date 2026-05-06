package tn.esprit.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class JoueurUiSupport {
    private static final Path SYMFONY_UPLOADS_DIRECTORY =
            Path.of("C:", "final", "sport_insight_final", "public", "uploads", "joueurs");
    private static final Map<String, Optional<Image>> IMAGE_CACHE = new ConcurrentHashMap<>();

    private JoueurUiSupport() {
    }

    public static Image loadJoueurImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String normalizedPath = imagePath.trim();
        Optional<Image> cachedImage = IMAGE_CACHE.get(normalizedPath);
        if (cachedImage != null) {
            return cachedImage.orElse(null);
        }

        Image resolvedImage = resolveJoueurImage(normalizedPath);
        IMAGE_CACHE.put(normalizedPath, Optional.ofNullable(resolvedImage));
        return resolvedImage;
    }

    public static String buildInitials(String prenom, String nom, String fallback) {
        String sanitizedFallback = fallback == null || fallback.isBlank() ? "J" : fallback;
        StringBuilder initials = new StringBuilder();

        appendInitial(initials, prenom);
        appendInitial(initials, nom);

        return initials.isEmpty() ? sanitizedFallback : initials.toString();
    }

    public static void clearImageCache() {
        IMAGE_CACHE.clear();
    }

    private static void appendInitial(StringBuilder initials, String value) {
        String cleaned = emptyToNull(value);
        if (cleaned == null) {
            return;
        }
        initials.append(Character.toUpperCase(cleaned.charAt(0)));
    }

    private static Image resolveJoueurImage(String normalizedPath) {
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
                "/tn/esprit/uploads/joueurs/" + imagePath,
                "/uploads/joueurs/" + imagePath
        };

        for (String candidate : resourceCandidates) {
            URL resource = JoueurUiSupport.class.getResource(candidate);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }

    private static List<Path> buildRelativeCandidates(String imagePath) {
        List<Path> candidates = new ArrayList<>();
        appendCandidate(candidates, Path.of("uploads", "joueurs"), imagePath);
        appendCandidate(candidates, Path.of("public", "uploads", "joueurs"), imagePath);
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
            return createImage(imagePath, imagePath.startsWith("http://") || imagePath.startsWith("https://"));
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

        Image image = createImage(file.toURI().toString());
        if (image != null) {
            return image;
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            return bufferedImage == null ? null : SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            return null;
        }
    }

    private static Image createImage(String imageSource) {
        return createImage(imageSource, false);
    }

    private static Image createImage(String imageSource, boolean backgroundLoading) {
        try {
            Image image = new Image(imageSource, 320, 320, true, true, backgroundLoading);
            return image.isError() ? null : image;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
