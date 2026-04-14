package tn.esprit.tools;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public final class SponsorAssets {
    private static final Path WORKSPACE_ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path IMAGE_ROOT = WORKSPACE_ROOT.resolve("image");
    private static final Path SPONSOR_IMAGE_ROOT = IMAGE_ROOT.resolve("sponsors");
    private static final Path RESOURCE_IMAGE_ROOT = WORKSPACE_ROOT.resolve("src/main/resources/tn/esprit/images");
    private static final Path RESOURCE_SPONSOR_IMAGE_ROOT = RESOURCE_IMAGE_ROOT.resolve("sponsors");
    private static final Path REFERENCE_PROJECT_ROOT = Path.of("C:/final/sport_insight_final");
    private static final List<Path> EXTERNAL_LOGO_ROOTS = List.of(
            REFERENCE_PROJECT_ROOT.resolve("public/uploads/logos"),
            REFERENCE_PROJECT_ROOT.resolve("public/uploads/sponsors"),
            REFERENCE_PROJECT_ROOT.resolve("public/images/logos")
    );
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp");
    private static final String DEFAULT_LOGO_RESOURCE = "/tn/esprit/images/sport-insight-logo.png";

    private SponsorAssets() {
    }

    public static String importLogo(Path sourceFile) throws IOException {
        if (sourceFile == null || !Files.exists(sourceFile)) {
            throw new IOException("Logo source file does not exist.");
        }

        Files.createDirectories(SPONSOR_IMAGE_ROOT);

        String originalName = sourceFile.getFileName() == null ? "logo" : sourceFile.getFileName().toString();
        String extension = "";
        int extensionIndex = originalName.lastIndexOf('.');
        if (extensionIndex >= 0) {
            extension = originalName.substring(extensionIndex);
            originalName = originalName.substring(0, extensionIndex);
        }

        String safeBase = slugify(originalName);
        if (safeBase.isBlank()) {
            safeBase = "sponsor-logo";
        }
        String storedName = safeBase + "-" + System.currentTimeMillis() + extension.toLowerCase(Locale.ROOT);
        Path target = SPONSOR_IMAGE_ROOT.resolve(storedName);
        Files.copy(sourceFile, target, StandardCopyOption.REPLACE_EXISTING);
        return "sponsors/" + storedName;
    }

    public static String resolveLogoUrl(String logoReference) {
        Path filePath = resolveLogoPath(logoReference);
        if (filePath != null) {
            return filePath.toUri().toString();
        }

        if (logoReference == null || logoReference.isBlank()) {
            return null;
        }

        String normalized = logoReference.replace('\\', '/');
        String candidateName = normalized.contains("/")
                ? normalized.substring(normalized.lastIndexOf('/') + 1)
                : normalized;

        URL directResource = SponsorAssets.class.getResource("/tn/esprit/images/" + normalized);
        if (directResource != null) {
            return directResource.toExternalForm();
        }

        URL scopedResource = SponsorAssets.class.getResource("/tn/esprit/images/sponsors/" + candidateName);
        if (scopedResource != null) {
            return scopedResource.toExternalForm();
        }

        URL sponsorResource = SponsorAssets.class.getResource("/tn/esprit/images/" + candidateName);
        if (sponsorResource != null) {
            return sponsorResource.toExternalForm();
        }

        return null;
    }

    public static String resolveDisplayLogoUrl(String logoReference) {
        String logoUrl = resolveLogoUrl(logoReference);
        return logoUrl != null ? logoUrl : resolvePlaceholderLogoUrl();
    }

    public static String resolvePlaceholderLogoUrl() {
        URL resource = SponsorAssets.class.getResource(DEFAULT_LOGO_RESOURCE);
        return resource == null ? null : resource.toExternalForm();
    }

    public static Path resolveLogoPath(String logoReference) {
        if (logoReference == null || logoReference.isBlank()) {
            return null;
        }

        Path directPath = parsePath(logoReference);
        if (directPath != null) {
            if (directPath.isAbsolute()) {
                return isDisplayableImage(directPath) ? directPath : null;
            }
            Path workspaceRelative = WORKSPACE_ROOT.resolve(directPath).normalize();
            if (isDisplayableImage(workspaceRelative)) {
                return workspaceRelative;
            }
        }

        String normalized = logoReference.replace('\\', '/');
        String candidateName = normalized.contains("/")
                ? normalized.substring(normalized.lastIndexOf('/') + 1)
                : normalized;

        for (Path root : buildSearchRoots()) {
            Path normalizedCandidate = root.resolve(normalized).normalize();
            if (isDisplayableImage(normalizedCandidate)) {
                return normalizedCandidate;
            }

            Path fileNameCandidate = root.resolve(candidateName).normalize();
            if (isDisplayableImage(fileNameCandidate)) {
                return fileNameCandidate;
            }

            Path stemCandidate = findByStem(root, candidateName);
            if (stemCandidate != null) {
                return stemCandidate;
            }
        }

        return null;
    }

    private static List<Path> buildSearchRoots() {
        List<Path> roots = new ArrayList<>();
        roots.add(IMAGE_ROOT);
        roots.add(SPONSOR_IMAGE_ROOT);
        roots.add(RESOURCE_IMAGE_ROOT);
        roots.add(RESOURCE_SPONSOR_IMAGE_ROOT);
        roots.addAll(EXTERNAL_LOGO_ROOTS);
        return roots;
    }

    private static Path findByStem(Path directory, String candidateName) {
        if (directory == null || !Files.isDirectory(directory)) {
            return null;
        }

        String safeCandidate = candidateName == null ? "" : candidateName.trim();
        if (safeCandidate.isBlank()) {
            return null;
        }

        String stem = stripExtension(safeCandidate);
        if (stem.isBlank()) {
            return null;
        }

        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(SponsorAssets::isDisplayableImage)
                    .filter(path -> stem.equalsIgnoreCase(stripExtension(path.getFileName().toString())))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .findFirst()
                    .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static boolean isDisplayableImage(Path path) {
        if (path == null || !Files.exists(path) || Files.isDirectory(path)) {
            return false;
        }

        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        return IMAGE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    private static String stripExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;
    }

    private static Path parsePath(String raw) {
        try {
            return Path.of(raw);
        } catch (InvalidPathException ex) {
            return null;
        }
    }

    private static String slugify(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return slug.replaceAll("(^-+|-+$)", "");
    }
}
