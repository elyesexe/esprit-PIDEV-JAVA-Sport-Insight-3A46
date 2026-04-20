package tn.esprit.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class VoiceInputService {
    private static final float SAMPLE_RATE = 16_000.0f;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final double QUICK_AUDIO_SECONDS_LIMIT = 6.5;
    private static final int QUICK_MIN_WORDS = 2;

    private static final String WHISPER_RELEASE_TAG = "v1.8.4";
    private static final URI WHISPER_RUNTIME_URI = URI.create(
            "https://github.com/ggml-org/whisper.cpp/releases/download/" + WHISPER_RELEASE_TAG + "/whisper-bin-x64.zip"
    );
    private static final String WHISPER_MODEL_NAME = "ggml-base.bin";
    private static final URI WHISPER_MODEL_URI = URI.create(
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/" + WHISPER_MODEL_NAME + "?download=true"
    );
    private static final String VOSK_MODEL_NAME = "vosk-model-small-en-us-0.15";
    private static final URI VOSK_MODEL_DOWNLOAD_URI = URI.create("https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip");

    private static final String WHISPER_PROMPT = "Sport Insight, Real Madrid, Bayern Munchen, Bayern Munich, UEFA Champions League, Equipes, Joueurs, Matchs, Entrainements, Sponsors, Store.";
    private static final Path INSTALL_ROOT = Path.of(System.getProperty("user.home"), ".sport-insight", "assistant", "stt");
    private static final Path WHISPER_RUNTIME_DIR = INSTALL_ROOT.resolve("whisper");
    private static final Path WHISPER_EXE = WHISPER_RUNTIME_DIR.resolve("Release").resolve("whisper-cli.exe");
    private static final Path WHISPER_MODEL_DIR = INSTALL_ROOT.resolve("models");
    private static final Path WHISPER_MODEL = WHISPER_MODEL_DIR.resolve(WHISPER_MODEL_NAME);
    private static final Path VOSK_BASE_DIR = INSTALL_ROOT.resolve("vosk");

    private final ExecutorService executor = Executors.newCachedThreadPool(daemonFactory("assistant-voice-input"));
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Object modelLock = new Object();

    private volatile RecordingSession activeSession;
    private volatile Model cachedModel;

    public synchronized boolean isRecording() {
        return activeSession != null;
    }

    public void prepareRealtimeRecognitionAsync() {
        executor.execute(() -> {
            try {
                Path modelDirectory = ensureVoskModelInstalled();
                loadVoskModel(modelDirectory);
            } catch (Exception ignored) {
                // Best effort warm-up only.
            }
        });
    }

    public synchronized void startRecording() throws LineUnavailableException {
        if (activeSession != null) {
            return;
        }

        prepareRealtimeRecognitionAsync();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("No compatible microphone line is available.");
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(AUDIO_FORMAT);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AtomicBoolean running = new AtomicBoolean(true);
        line.start();
        Future<?> captureFuture = executor.submit(() -> captureAudio(line, output, running));
        activeSession = new RecordingSession(line, output, running, captureFuture);
    }

    public CompletableFuture<String> stopRecordingAndTranscribe(Consumer<String> statusConsumer) {
        RecordingSession session;
        synchronized (this) {
            session = activeSession;
            activeSession = null;
        }
        if (session == null) {
            return CompletableFuture.completedFuture("");
        }

        session.running().set(false);
        session.line().stop();
        session.line().close();

        Consumer<String> status = statusConsumer == null ? ignored -> { } : statusConsumer;
        return CompletableFuture.supplyAsync(() -> {
            joinCapture(session.captureFuture());
            byte[] audio = session.audio().toByteArray();
            if (audio.length < 6_400) {
                status.accept("I did not catch enough audio. Try speaking a little longer and a little closer to the microphone.");
                return "";
            }

            double audioSeconds = audioDurationSeconds(audio);
            String quickTranscript = "";
            try {
                status.accept("Running the quick recognizer...");
                Path voskModelDirectory = ensureVoskModelInstalled();
                quickTranscript = transcribeWithVosk(voskModelDirectory, audio);
                if (shouldUseQuickTranscript(quickTranscript, audioSeconds)) {
                    return quickTranscript;
                }
                status.accept(quickTranscript.isBlank()
                        ? "Quick recognizer missed that. Refining the transcript..."
                        : "Refining the transcript for accuracy...");
            } catch (Exception ignored) {
                status.accept("Quick recognizer unavailable. Refining the transcript...");
            }

            try {
                String transcript = transcribeWithWhisper(audio);
                if (!transcript.isBlank()) {
                    return transcript;
                }
                status.accept("Whisper heard very little. Falling back to the quick recognizer...");
            } catch (Exception ignored) {
                status.accept("Whisper needs a backup pass. Returning to the quick recognizer...");
            }

            if (!quickTranscript.isBlank()) {
                return quickTranscript;
            }

            try {
                Path voskModelDirectory = ensureVoskModelInstalled();
                status.accept("Running the backup recognizer...");
                return transcribeWithVosk(voskModelDirectory, audio);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }, executor);
    }

    private void captureAudio(TargetDataLine line, ByteArrayOutputStream output, AtomicBoolean running) {
        byte[] buffer = new byte[4_096];
        try {
            while (running.get()) {
                int read = line.read(buffer, 0, buffer.length);
                if (read > 0) {
                    output.write(buffer, 0, read);
                }
            }
        } catch (Exception ignored) {
            // Microphone capture is best-effort.
        }
    }

    private void joinCapture(Future<?> captureFuture) {
        try {
            captureFuture.get();
        } catch (Exception ignored) {
            // Capture may stop abruptly when the line closes.
        }
    }

    private String transcribeWithWhisper(byte[] audio) throws IOException, InterruptedException {
        ensureWhisperRuntimeInstalled();
        ensureWhisperModelInstalled();

        Files.createDirectories(INSTALL_ROOT.resolve("temp"));
        Path wavPath = Files.createTempFile(INSTALL_ROOT.resolve("temp"), "assistant-input-", ".wav");
        Path outputPrefix = Files.createTempFile(INSTALL_ROOT.resolve("temp"), "assistant-output-", "");
        Files.deleteIfExists(outputPrefix);
        Path transcriptPath = Path.of(outputPrefix.toString() + ".txt");

        try {
            writeWaveFile(audio, wavPath);
            Process process = new ProcessBuilder(
                    WHISPER_EXE.toString(),
                    "-m",
                    WHISPER_MODEL.toString(),
                    "-f",
                    wavPath.toString(),
                    "-l",
                    "auto",
                    "-otxt",
                    "-of",
                    outputPrefix.toString(),
                    "-nt",
                    "-np",
                    "-ng",
                    "--prompt",
                    WHISPER_PROMPT
            )
                    .directory(WHISPER_EXE.getParent().toFile())
                    .redirectErrorStream(true)
                    .start();

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Whisper transcription failed. " + output.trim());
            }

            if (!Files.isRegularFile(transcriptPath)) {
                return "";
            }

            return Files.readString(transcriptPath, StandardCharsets.UTF_8).trim();
        } finally {
            Files.deleteIfExists(wavPath);
            Files.deleteIfExists(transcriptPath);
        }
    }

    private void writeWaveFile(byte[] audio, Path wavPath) throws IOException {
        try (AudioInputStream audioInputStream = new AudioInputStream(
                new ByteArrayInputStream(audio),
                AUDIO_FORMAT,
                audio.length / AUDIO_FORMAT.getFrameSize()
        )) {
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavPath.toFile());
        }
    }

    private void ensureWhisperRuntimeInstalled() throws IOException, InterruptedException {
        if (Files.isRegularFile(WHISPER_EXE)) {
            return;
        }

        synchronized (modelLock) {
            if (Files.isRegularFile(WHISPER_EXE)) {
                return;
            }

            Files.createDirectories(WHISPER_RUNTIME_DIR);
            Path zipPath = WHISPER_RUNTIME_DIR.resolve("whisper-bin-x64.zip");
            downloadToFile(WHISPER_RUNTIME_URI, zipPath);
            unzip(zipPath, WHISPER_RUNTIME_DIR);
            Files.deleteIfExists(zipPath);
        }
    }

    private void ensureWhisperModelInstalled() throws IOException, InterruptedException {
        if (Files.isRegularFile(WHISPER_MODEL)) {
            return;
        }

        synchronized (modelLock) {
            if (Files.isRegularFile(WHISPER_MODEL)) {
                return;
            }

            Files.createDirectories(WHISPER_MODEL_DIR);
            downloadToFile(WHISPER_MODEL_URI, WHISPER_MODEL);
        }
    }

    private Path ensureVoskModelInstalled() throws IOException, InterruptedException {
        Files.createDirectories(VOSK_BASE_DIR);
        Path modelDirectory = VOSK_BASE_DIR.resolve(VOSK_MODEL_NAME);
        if (Files.isDirectory(modelDirectory.resolve("am"))) {
            return modelDirectory;
        }

        synchronized (modelLock) {
            if (Files.isDirectory(modelDirectory.resolve("am"))) {
                return modelDirectory;
            }

            Path zipPath = VOSK_BASE_DIR.resolve(VOSK_MODEL_NAME + ".zip");
            downloadToFile(VOSK_MODEL_DOWNLOAD_URI, zipPath);
            unzip(zipPath, VOSK_BASE_DIR);
            Files.deleteIfExists(zipPath);
            return modelDirectory;
        }
    }

    private String transcribeWithVosk(Path modelDirectory, byte[] audio) throws IOException {
        Model model = loadVoskModel(modelDirectory);
        try (Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {
            int offset = 0;
            while (offset < audio.length) {
                int chunkSize = Math.min(4_096, audio.length - offset);
                byte[] chunk = new byte[chunkSize];
                System.arraycopy(audio, offset, chunk, 0, chunkSize);
                recognizer.acceptWaveForm(chunk, chunkSize);
                offset += chunkSize;
            }
            JsonNode resultNode = objectMapper.readTree(recognizer.getFinalResult());
            return resultNode.path("text").asText("").trim();
        } catch (Exception ex) {
            throw new IOException("The offline voice engine could not transcribe the recording.", ex);
        }
    }

    private boolean shouldUseQuickTranscript(String transcript, double audioSeconds) {
        String normalized = AssistantService.normalize(transcript);
        if (normalized.isBlank()) {
            return false;
        }

        int wordCount = normalized.split("\\s+").length;
        if (wordCount >= 2 && normalized.length() >= 6) {
            return true;
        }

        if (containsAny(normalized,
                "open", "show", "go to", "goto", "search", "find", "explain", "tell me",
                "match", "matches", "details", "detail", "score", "mvp", "lineup", "lineups",
                "teams", "players", "joueurs", "equipes", "league", "table", "standings",
                "real madrid", "bayern", "premier league", "champions league", "settings",
                "store", "sponsors", "admin", "home")) {
            return true;
        }

        if (audioSeconds <= QUICK_AUDIO_SECONDS_LIMIT && wordCount >= QUICK_MIN_WORDS) {
            return true;
        }

        return audioSeconds <= 3.0 && wordCount >= 1 && normalized.length() >= 5;
    }

    private double audioDurationSeconds(byte[] audio) {
        if (audio == null || audio.length == 0) {
            return 0.0;
        }
        return audio.length / (double) AUDIO_FORMAT.getFrameSize() / SAMPLE_RATE;
    }

    private Model loadVoskModel(Path modelDirectory) throws IOException {
        if (cachedModel != null) {
            return cachedModel;
        }
        synchronized (modelLock) {
            if (cachedModel == null) {
                cachedModel = new Model(modelDirectory.toString());
            }
            return cachedModel;
        }
    }

    private void downloadToFile(URI uri, Path target) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMinutes(10))
                .GET()
                .build();
        HttpResponse<Path> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofFile(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Voice model download failed with HTTP " + response.statusCode() + ".");
        }
    }

    private void unzip(Path zipPath, Path outputDirectory) throws IOException {
        try (InputStream inputStream = Files.newInputStream(zipPath);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path target = outputDirectory.resolve(entry.getName()).normalize();
                if (!target.startsWith(outputDirectory)) {
                    throw new IOException("Blocked unsafe archive path: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(Objects.requireNonNull(target.getParent()));
                    Files.copy(zipInputStream, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                zipInputStream.closeEntry();
            }
        }
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }

    private static boolean containsAny(String source, String... terms) {
        if (source == null || source.isBlank()) {
            return false;
        }
        for (String term : terms) {
            if (source.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private record RecordingSession(
            TargetDataLine line,
            ByteArrayOutputStream audio,
            AtomicBoolean running,
            Future<?> captureFuture
    ) {
    }
}
