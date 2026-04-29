package tn.esprit.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

public class CommentCvStorageService {
    private static final Path STORAGE_DIRECTORY = Path.of(System.getProperty("user.dir"), "uploads", "comment-cvs");

    public String store(Path sourceFile) throws IOException {
        if (sourceFile == null || !Files.exists(sourceFile) || Files.isDirectory(sourceFile)) {
            throw new IOException("The selected CV file does not exist.");
        }

        String originalName = sourceFile.getFileName().toString();
        String sanitizedName = sanitizeFileName(originalName);
        if (!sanitizedName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IOException("Only PDF CV files are supported.");
        }

        Files.createDirectories(STORAGE_DIRECTORY);
        String storedName = UUID.randomUUID() + "-" + sanitizedName;
        Path target = STORAGE_DIRECTORY.resolve(storedName);
        Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);
        return Path.of("uploads", "comment-cvs", storedName).toString().replace('\\', '/');
    }

    public Path resolve(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        Path path = Path.of(storedPath);
        if (path.isAbsolute()) {
            return path;
        }
        return Path.of(System.getProperty("user.dir")).resolve(path).normalize();
    }

    public void deleteQuietly(String storedPath) {
        Path resolved = resolve(storedPath);
        if (resolved == null) {
            return;
        }

        try {
            if (resolved.startsWith(STORAGE_DIRECTORY) && Files.exists(resolved)) {
                Files.deleteIfExists(resolved);
            }
        } catch (IOException ignored) {
            // Cleanup must not block comment actions.
        }
    }

    private String sanitizeFileName(String fileName) {
        String normalized = Normalizer.normalize(fileName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9._-]", "_");
        if (normalized.isBlank()) {
            return "cv.pdf";
        }
        return normalized;
    }
}
