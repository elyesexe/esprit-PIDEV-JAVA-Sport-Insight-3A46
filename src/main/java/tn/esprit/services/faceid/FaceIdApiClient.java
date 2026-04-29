package tn.esprit.services.faceid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.tools.MyConnection;
import tn.esprit.tools.FaceIdConfig;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;

public class FaceIdApiClient {
    private static final String FACE_ID_SCRIPT_PROPERTY = "face.id.api.script.dir";
    private static final String FACE_ID_SCRIPT_ENV = "FACE_ID_API_SCRIPT_DIR";
    private static final int HEALTH_WAIT_ATTEMPTS = 16;
    private static final long AUTO_START_RETRY_DELAY_MS = 10_000L;
    private static final double LOCAL_HASH_DISTANCE_THRESHOLD = 0.22;
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);
    private static final Object START_LOCK = new Object();
    private static volatile Process localApiProcess;
    private static volatile long lastAutoStartAttemptEpochMs;
    private static volatile String autoStartDetail;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiBase;

    public FaceIdApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiBase = FaceIdConfig.resolveApiBase();
    }

    public FaceVerificationResult verify(int userId, Path imagePath) throws IOException, InterruptedException {
        try {
            HttpResponse<String> response = sendMultipart("/face/verify", userId, imagePath);
            JsonNode payload = safeReadBody(response.body());
            int statusCode = response.statusCode();

            if (statusCode == 200) {
                return new FaceVerificationResult(
                        payload.path("verified").asBoolean(false),
                        false,
                        textOrFallback(payload, "message", "Face ID verification completed."),
                        payload.has("distance") && payload.path("distance").isNumber() ? payload.path("distance").asDouble() : null,
                        statusCode
                );
            }
            if (statusCode == 404) {
                return new FaceVerificationResult(false, true, readErrorMessage(payload, response.body()), null, statusCode);
            }
            throw new IOException("Face ID verify failed (" + statusCode + "): " + readErrorMessage(payload, response.body()));
        } catch (IOException apiFailure) {
            return verifyLocal(userId, imagePath, apiFailure);
        }
    }

    public FaceEnrollResult enroll(int userId, Path imagePath) throws IOException, InterruptedException {
        try {
            HttpResponse<String> response = sendMultipart("/face/enroll", userId, imagePath);
            JsonNode payload = safeReadBody(response.body());
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return new FaceEnrollResult(true, textOrFallback(payload, "message", "Face profile enrolled."), statusCode);
            }
            throw new IOException("Face ID enroll failed (" + statusCode + "): " + readErrorMessage(payload, response.body()));
        } catch (IOException apiFailure) {
            return enrollLocal(userId, imagePath, apiFailure);
        }
    }

    public String getApiBase() {
        return apiBase;
    }

    private HttpResponse<String> sendMultipart(String path, int userId, Path imagePath) throws IOException, InterruptedException {
        if (!isServiceHealthy() && !ensureServiceAvailable()) {
            String detail = sanitize(autoStartDetail);
            String suffix = detail == null ? "" : " Detail: " + detail;
            throw new IOException("Face ID service is not reachable at " + apiBase + "." + suffix);
        }

        try {
            return sendMultipartOnce(path, userId, imagePath);
        } catch (IOException firstFailure) {
            if (!ensureServiceAvailable()) {
                throw firstFailure;
            }
            return sendMultipartOnce(path, userId, imagePath);
        }
    }

    private HttpResponse<String> sendMultipartOnce(String path, int userId, Path imagePath) throws IOException, InterruptedException {
        String boundary = "----FaceBoundary" + UUID.randomUUID();
        byte[] payload = buildMultipartPayload(boundary, userId, imagePath);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private boolean ensureServiceAvailable() {
        if (isServiceHealthy()) {
            return true;
        }
        if (!isLocalApiBase()) {
            return false;
        }

        synchronized (START_LOCK) {
            if (isServiceHealthy()) {
                return true;
            }
            long now = System.currentTimeMillis();
            boolean processStopped = localApiProcess == null || !localApiProcess.isAlive();
            boolean retryWindowElapsed = now - lastAutoStartAttemptEpochMs >= AUTO_START_RETRY_DELAY_MS;
            if (processStopped && retryWindowElapsed) {
                lastAutoStartAttemptEpochMs = now;
                startLocalApiProcess();
            }
        }
        return waitForHealthyService();
    }

    private boolean waitForHealthyService() {
        for (int i = 0; i < HEALTH_WAIT_ATTEMPTS; i++) {
            if (isServiceHealthy()) {
                return true;
            }
            try {
                Thread.sleep(350);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean isServiceHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBase + "/health"))
                    .header("Accept", "application/json")
                    .timeout(HEALTH_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isLocalApiBase() {
        try {
            URI uri = new URI(apiBase);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            String normalized = host.toLowerCase(Locale.ROOT);
            return "127.0.0.1".equals(normalized) || "localhost".equals(normalized);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private void startLocalApiProcess() {
        Path scriptDir = resolveFaceIdScriptDir();
        if (scriptDir == null || !Files.exists(scriptDir.resolve("main.py"))) {
            autoStartDetail = "tools/faceid-api/main.py introuvable.";
            return;
        }

        autoStartDetail = "Tentative de demarrage automatique dans " + scriptDir.toAbsolutePath() + ".";
        List<List<String>> commandCandidates = buildStartCommands(scriptDir);
        for (List<String> command : commandCandidates) {
            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(scriptDir.toFile());
                builder.redirectErrorStream(true);
                builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                localApiProcess = builder.start();
                if (waitForHealthyService()) {
                    autoStartDetail = null;
                    return;
                }
                if (localApiProcess != null && !localApiProcess.isAlive()) {
                    autoStartDetail = "Processus lance mais arrete: " + String.join(" ", command);
                    localApiProcess = null;
                }
            } catch (Exception e) {
                autoStartDetail = "Echec commande: " + String.join(" ", command) + " (" + fallbackError(e) + ")";
            }
        }
        if (sanitize(autoStartDetail) == null) {
            autoStartDetail = "Aucune commande de demarrage n'a fonctionne.";
        }
    }

    private List<List<String>> buildStartCommands(Path scriptDir) {
        List<List<String>> candidates = new ArrayList<>();

        Path venvPython = scriptDir.resolve(".venv").resolve("Scripts").resolve("python.exe");
        if (Files.exists(venvPython)) {
            candidates.add(Arrays.asList(
                    venvPython.toAbsolutePath().toString(),
                    "-m",
                    "uvicorn",
                    "main:app",
                    "--host",
                    "127.0.0.1",
                    "--port",
                    "8000"
            ));
        }

        candidates.add(Arrays.asList("python", "-m", "uvicorn", "main:app", "--host", "127.0.0.1", "--port", "8000"));
        candidates.add(Arrays.asList("py", "-3", "-m", "uvicorn", "main:app", "--host", "127.0.0.1", "--port", "8000"));
        return candidates;
    }

    private Path resolveFaceIdScriptDir() {
        String byProperty = sanitize(System.getProperty(FACE_ID_SCRIPT_PROPERTY));
        if (byProperty != null) {
            return Path.of(byProperty);
        }

        String byEnv = sanitize(System.getenv(FACE_ID_SCRIPT_ENV));
        if (byEnv != null) {
            return Path.of(byEnv);
        }

        Path workingDir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        List<Path> roots = new ArrayList<>();
        roots.add(workingDir);
        if (workingDir.getParent() != null) {
            roots.add(workingDir.getParent());
        }
        roots.add(workingDir.resolve("esprit-PIDEV-JAVA-Sport-Insight-3A46-integration"));

        for (Path root : roots) {
            if (root == null) {
                continue;
            }
            Path candidate = root.resolve("tools").resolve("faceid-api");
            if (Files.exists(candidate.resolve("main.py"))) {
                return candidate;
            }
        }
        return null;
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String fallbackError(Exception exception) {
        if (exception == null) {
            return "erreur inconnue";
        }
        String message = sanitize(exception.getMessage());
        return message == null ? exception.getClass().getSimpleName() : message;
    }

    private FaceVerificationResult verifyLocal(int userId, Path imagePath, IOException apiFailure) throws IOException {
        try {
            Connection connection = MyConnection.getInstance().getConnection();
            String probeHash = computePerceptualHash(imagePath);

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT embedding_json, model_name FROM face_profile WHERE user_id = ? LIMIT 1")) {
                statement.setInt(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return new FaceVerificationResult(
                                false,
                                true,
                                "Aucun profil Face ID local. Enrollement requis.",
                                null,
                                404
                        );
                    }

                    String storedEmbedding = resultSet.getString("embedding_json");
                    String modelName = sanitize(resultSet.getString("model_name"));
                    String storedHash = extractLocalHash(storedEmbedding, modelName);
                    if (storedHash == null) {
                        return new FaceVerificationResult(
                                false,
                                true,
                                "Profil Face ID incompatible hors-ligne. Reenrollez localement.",
                                null,
                                404
                        );
                    }

                    double distance = normalizedHammingDistance(storedHash, probeHash);
                    boolean verified = distance <= LOCAL_HASH_DISTANCE_THRESHOLD;
                    String message = verified
                            ? "Face ID verifiee (mode local Java)."
                            : "Verification Face ID echouee (mode local Java).";
                    return new FaceVerificationResult(verified, false, message, distance, 200);
                }
            }
        } catch (SQLException sqlException) {
            String detail = fallbackError(apiFailure);
            throw new IOException("Face ID indisponible (API: " + detail + ", DB locale: " + fallbackError(sqlException) + ").", sqlException);
        }
    }

    private FaceEnrollResult enrollLocal(int userId, Path imagePath, IOException apiFailure) throws IOException {
        try {
            Connection connection = MyConnection.getInstance().getConnection();
            String hash = computePerceptualHash(imagePath);
            String payload = objectMapper.createObjectNode()
                    .put("hash", hash)
                    .put("algorithm", "java_ahash_8x8")
                    .toString();

            try (PreparedStatement ensureUser = connection.prepareStatement("SELECT id FROM `user` WHERE id = ? LIMIT 1")) {
                ensureUser.setInt(1, userId);
                try (ResultSet resultSet = ensureUser.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IOException("Utilisateur introuvable pour l'enrollement Face ID.");
                    }
                }
            }

            try (PreparedStatement upsert = connection.prepareStatement("""
                    INSERT INTO face_profile (user_id, embedding_json, model_name)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        embedding_json = VALUES(embedding_json),
                        model_name = VALUES(model_name)
                    """)) {
                upsert.setInt(1, userId);
                upsert.setString(2, payload);
                upsert.setString(3, "java_ahash_8x8");
                upsert.executeUpdate();
            }
            return new FaceEnrollResult(true, "Profil Face ID enregistre (mode local Java).", 200);
        } catch (SQLException sqlException) {
            String detail = fallbackError(apiFailure);
            throw new IOException("Enrollement Face ID indisponible (API: " + detail + ", DB locale: " + fallbackError(sqlException) + ").", sqlException);
        }
    }

    private String computePerceptualHash(Path imagePath) throws IOException {
        BufferedImage input = ImageIO.read(imagePath.toFile());
        if (input == null) {
            throw new IOException("Image invalide pour la verification Face ID.");
        }

        int sourceWidth = input.getWidth();
        int sourceHeight = input.getHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            throw new IOException("Image vide pour la verification Face ID.");
        }

        int square = Math.min(sourceWidth, sourceHeight);
        int offsetX = (sourceWidth - square) / 2;
        int offsetY = (sourceHeight - square) / 2;
        BufferedImage cropped = input.getSubimage(offsetX, offsetY, square, square);

        BufferedImage resized = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(cropped, 0, 0, 8, 8, null);
        } finally {
            graphics.dispose();
        }

        int[] values = new int[64];
        int sum = 0;
        int index = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int rgb = resized.getRGB(x, y);
                int gray = rgb & 0xFF;
                values[index++] = gray;
                sum += gray;
            }
        }
        int average = sum / values.length;

        StringBuilder bits = new StringBuilder(64);
        for (int value : values) {
            bits.append(value >= average ? '1' : '0');
        }
        return bits.toString();
    }

    private String extractLocalHash(String embeddingJson, String modelName) {
        String normalizedModel = modelName == null ? "" : modelName.toLowerCase(Locale.ROOT);
        if (!normalizedModel.isBlank() && !normalizedModel.startsWith("java_")) {
            return null;
        }

        if (embeddingJson == null || embeddingJson.isBlank()) {
            return null;
        }

        try {
            JsonNode node = objectMapper.readTree(embeddingJson);
            if (node.isObject()) {
                String hash = sanitize(node.path("hash").asText(null));
                if (hash != null && hash.length() == 64) {
                    return hash;
                }
            } else if (node.isTextual()) {
                String hash = sanitize(node.asText());
                if (hash != null && hash.length() == 64) {
                    return hash;
                }
            }
        } catch (Exception ignored) {
            String hash = sanitize(embeddingJson);
            if (hash != null && hash.length() == 64 && hash.chars().allMatch(ch -> ch == '0' || ch == '1')) {
                return hash;
            }
        }
        return null;
    }

    private double normalizedHammingDistance(String a, String b) {
        if (a == null || b == null || a.length() != b.length() || a.isEmpty()) {
            return 1.0;
        }
        int differences = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                differences++;
            }
        }
        return (double) differences / (double) a.length();
    }

    private byte[] buildMultipartPayload(String boundary, int userId, Path imagePath) throws IOException {
        String filename = imagePath.getFileName() == null ? "face-image.jpg" : imagePath.getFileName().toString();
        byte[] fileBytes = Files.readAllBytes(imagePath);
        String lineBreak = "\r\n";
        StringBuilder builder = new StringBuilder();

        builder.append("--").append(boundary).append(lineBreak);
        builder.append("Content-Disposition: form-data; name=\"user_id\"").append(lineBreak).append(lineBreak);
        builder.append(userId).append(lineBreak);

        builder.append("--").append(boundary).append(lineBreak);
        builder.append("Content-Disposition: form-data; name=\"image\"; filename=\"").append(filename).append("\"").append(lineBreak);
        builder.append("Content-Type: application/octet-stream").append(lineBreak).append(lineBreak);

        byte[] headerBytes = builder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = (lineBreak + "--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8);

        byte[] payload = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, payload, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, payload, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, payload, headerBytes.length + fileBytes.length, footerBytes.length);
        return payload;
    }

    private JsonNode safeReadBody(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private String textOrFallback(JsonNode payload, String field, String fallback) {
        String value = payload.path(field).asText(null);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String readErrorMessage(JsonNode payload, String fallbackBody) {
        String detail = payload.path("detail").asText(null);
        if (detail != null && !detail.isBlank()) {
            return detail;
        }
        String message = payload.path("message").asText(null);
        if (message != null && !message.isBlank()) {
            return message;
        }
        if (fallbackBody == null) {
            return "Unknown Face ID API error.";
        }
        String normalized = fallbackBody.replaceAll("\\s+", " ").trim();
        return normalized.length() > 200 ? normalized.substring(0, 200) + "..." : normalized;
    }

    public record FaceVerificationResult(
            boolean verified,
            boolean profileMissing,
            String message,
            Double distance,
            int statusCode
    ) {
    }

    public record FaceEnrollResult(
            boolean enrolled,
            String message,
            int statusCode
    ) {
    }
}
