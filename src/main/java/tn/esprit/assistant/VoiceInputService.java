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
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private static final Set<String> QUICK_AMBIGUOUS_TOKENS = Set.of("much", "one", "on");

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

    private static final String WHISPER_PROMPT_BASE = "Sport Insight, Sport Insight News, Real Madrid, Bayern Munich, UEFA Champions League, Equipes, Joueurs, Matchs, Football News, Entrainements, Sponsors, Store, player profile, match details, scorer, assists, cards, lineups, standings, statistics.";
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
    private final AssistantEntityLexicon entityLexicon = AssistantEntityLexicon.getInstance();

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
        startRecording(null);
    }

    public synchronized void startRecording(Consumer<VoiceCaptureUpdate> updateConsumer) throws LineUnavailableException {
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
        Recognizer recognizer = tryCreateRealtimeRecognizer();
        line.start();
        Consumer<VoiceCaptureUpdate> safeUpdateConsumer = updateConsumer == null ? ignored -> { } : updateConsumer;
        Future<?> captureFuture = executor.submit(() -> captureAudio(line, output, running, recognizer, safeUpdateConsumer));
        activeSession = new RecordingSession(line, output, running, captureFuture, recognizer, safeUpdateConsumer);
        safeUpdateConsumer.accept(VoiceCaptureUpdate.listening());
    }

    public CompletableFuture<VoiceCaptureResult> stopRecordingAndTranscribe(Consumer<String> statusConsumer) {
        RecordingSession session;
        synchronized (this) {
            session = activeSession;
            activeSession = null;
        }
        if (session == null) {
            return CompletableFuture.completedFuture(VoiceCaptureResult.empty());
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
                closeRecognizer(session.recognizer());
                return VoiceCaptureResult.empty();
            }

            double audioSeconds = audioDurationSeconds(audio);
            QuickRecognition quickRecognition = QuickRecognition.empty();
            try {
                status.accept("Running the quick recognizer...");
                quickRecognition = readQuickRecognition(session, audio);
                if (shouldUseQuickTranscript(quickRecognition, audioSeconds)) {
                    VoiceCaptureResult quickResult = buildCaptureResult(quickRecognition.text(), quickRecognition.confidence(), false);
                    session.updateConsumer().accept(VoiceCaptureUpdate.finalText(quickResult.transcript(), quickResult.confidence()));
                    if (quickResult.clarificationNeeded()) {
                        status.accept("Low-confidence voice capture. Please confirm the transcript before sending.");
                    }
                    return quickResult;
                }
                status.accept(quickRecognition.text().isBlank()
                        ? "Quick recognizer missed that. Refining the transcript..."
                        : "Refining the transcript for accuracy...");
            } catch (Exception ignored) {
                closeRecognizer(session.recognizer());
                status.accept("Quick recognizer unavailable. Refining the transcript...");
            }

            try {
                String transcript = transcribeWithWhisper(audio);
                if (!transcript.isBlank()) {
                    VoiceCaptureResult whisperResult = buildCaptureResult(transcript, Math.max(quickRecognition.confidence(), 0.82), true);
                    session.updateConsumer().accept(VoiceCaptureUpdate.finalText(whisperResult.transcript(), whisperResult.confidence()));
                    return whisperResult;
                }
                status.accept("Whisper heard very little. Falling back to the quick recognizer...");
            } catch (Exception ignored) {
                status.accept("Whisper needs a backup pass. Returning to the quick recognizer...");
            }

            if (!quickRecognition.text().isBlank()) {
                VoiceCaptureResult quickResult = buildCaptureResult(quickRecognition.text(), quickRecognition.confidence(), false);
                session.updateConsumer().accept(VoiceCaptureUpdate.finalText(quickResult.transcript(), quickResult.confidence()));
                if (quickResult.clarificationNeeded()) {
                    status.accept("Low-confidence voice capture. Please confirm the transcript before sending.");
                }
                return quickResult;
            }

            try {
                Path voskModelDirectory = ensureVoskModelInstalled();
                status.accept("Running the backup recognizer...");
                QuickRecognition backupRecognition = transcribeWithVoskDetailed(voskModelDirectory, audio);
                VoiceCaptureResult backupResult = buildCaptureResult(backupRecognition.text(), backupRecognition.confidence(), false);
                session.updateConsumer().accept(VoiceCaptureUpdate.finalText(backupResult.transcript(), backupResult.confidence()));
                return backupResult;
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }, executor);
    }

    private void captureAudio(
            TargetDataLine line,
            ByteArrayOutputStream output,
            AtomicBoolean running,
            Recognizer recognizer,
            Consumer<VoiceCaptureUpdate> updateConsumer
    ) {
        byte[] buffer = new byte[4_096];
        String lastPartial = "";
        try {
            while (running.get()) {
                int read = line.read(buffer, 0, buffer.length);
                if (read > 0) {
                    output.write(buffer, 0, read);
                    if (recognizer != null) {
                        boolean utteranceComplete = recognizer.acceptWaveForm(buffer, read);
                        String realtimeJson = utteranceComplete ? recognizer.getResult() : recognizer.getPartialResult();
                        QuickRecognition realtimeRecognition = parseRecognitionJson(realtimeJson);
                        if (realtimeRecognition != null && !realtimeRecognition.text().isBlank()) {
                            String partialText = finalizeTranscript(realtimeRecognition.text());
                            if (!partialText.isBlank() && !partialText.equals(lastPartial)) {
                                lastPartial = partialText;
                                updateConsumer.accept(utteranceComplete
                                        ? VoiceCaptureUpdate.finalText(partialText, realtimeRecognition.confidence())
                                        : VoiceCaptureUpdate.partial(partialText, realtimeRecognition.confidence()));
                            }
                        }
                    }
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

    private Recognizer tryCreateRealtimeRecognizer() {
        try {
            Path modelDirectory = ensureVoskModelInstalled();
            Model model = loadVoskModel(modelDirectory);
            Recognizer recognizer = new Recognizer(model, SAMPLE_RATE);
            recognizer.setWords(true);
            recognizer.setPartialWords(true);
            return recognizer;
        } catch (Exception ignored) {
            return null;
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
                    buildWhisperPrompt()
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

    private QuickRecognition transcribeWithVoskDetailed(Path modelDirectory, byte[] audio) throws IOException {
        Model model = loadVoskModel(modelDirectory);
        try (Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {
            recognizer.setWords(true);
            int offset = 0;
            while (offset < audio.length) {
                int chunkSize = Math.min(4_096, audio.length - offset);
                byte[] chunk = new byte[chunkSize];
                System.arraycopy(audio, offset, chunk, 0, chunkSize);
                recognizer.acceptWaveForm(chunk, chunkSize);
                offset += chunkSize;
            }
            QuickRecognition recognition = parseRecognitionJson(recognizer.getFinalResult());
            return recognition == null ? QuickRecognition.empty() : recognition;
        } catch (Exception ex) {
            throw new IOException("The offline voice engine could not transcribe the recording.", ex);
        }
    }

    private QuickRecognition readQuickRecognition(RecordingSession session, byte[] audio) throws IOException, InterruptedException {
        Recognizer sessionRecognizer = session.recognizer();
        if (sessionRecognizer != null) {
            try {
                QuickRecognition recognition = parseRecognitionJson(sessionRecognizer.getFinalResult());
                return recognition == null ? QuickRecognition.empty() : recognition;
            } finally {
                closeRecognizer(sessionRecognizer);
            }
        }

        Path voskModelDirectory = ensureVoskModelInstalled();
        return transcribeWithVoskDetailed(voskModelDirectory, audio);
    }

    private boolean shouldUseQuickTranscript(QuickRecognition recognition, double audioSeconds) {
        String normalized = AssistantService.normalize(recognition.text());
        if (normalized.isBlank()) {
            return false;
        }
        String corrected = entityLexicon.correctTranscriptEntities(normalized);

        int wordCount = normalized.split("\\s+").length;
        if (looksLikeAmbiguousQuickTranscript(normalized) && recognition.confidence() < 0.82) {
            return false;
        }
        if (!corrected.equals(normalized) && recognition.confidence() < 0.90) {
            return false;
        }
        if (looksLikeEntityHeavyCommand(corrected) && recognition.confidence() < 0.84) {
            return false;
        }

        if (recognition.confidence() >= 0.86 && wordCount >= 2) {
            return true;
        }

        if (wordCount >= 3 && normalized.length() >= 10 && recognition.confidence() >= 0.55) {
            return true;
        }

        if (containsAny(normalized,
                "open", "show", "go to", "goto", "search", "find", "explain", "tell me",
                "match", "matches", "details", "detail", "score", "mvp", "lineup", "lineups",
                "teams", "players", "joueurs", "equipes", "league", "table", "standings",
                "real madrid", "bayern", "premier league", "champions league", "sport insight news", "football news", "headlines", "settings",
                "store", "sponsors", "admin", "home")) {
            return true;
        }

        if (audioSeconds <= QUICK_AUDIO_SECONDS_LIMIT && wordCount >= QUICK_MIN_WORDS) {
            return true;
        }

        return audioSeconds <= 3.0 && wordCount >= 1 && normalized.length() >= 5;
    }

    static boolean shouldRequestClarification(String normalizedTranscript, double confidence, boolean refinedTranscript) {
        if (normalizedTranscript == null || normalizedTranscript.isBlank()) {
            return false;
        }

        String normalized = AssistantService.normalize(normalizedTranscript);
        int wordCount = normalized.isBlank() ? 0 : normalized.split("\\s+").length;
        if (normalized.contains("unk")) {
            return true;
        }
        if (!refinedTranscript && confidence > 0.0 && confidence < 0.58) {
            return true;
        }
        if (!refinedTranscript && confidence == 0.0 && wordCount <= 2) {
            return true;
        }
        if (!refinedTranscript && wordCount <= 2 && confidence > 0.0 && confidence < 0.72) {
            return true;
        }
        return false;
    }

    private static boolean looksLikeAmbiguousQuickTranscript(String normalized) {
        List<String> tokens = List.of(normalized.split("\\s+"));
        return tokens.stream().anyMatch(QUICK_AMBIGUOUS_TOKENS::contains);
    }

    private String finalizeTranscript(String transcript) {
        return entityLexicon.correctTranscriptEntities(AssistantService.normalize(transcript));
    }

    private VoiceCaptureResult buildCaptureResult(String transcript, double confidence, boolean refinedTranscript) {
        String finalizedTranscript = finalizeTranscript(transcript);
        if (finalizedTranscript.isBlank()) {
            return VoiceCaptureResult.empty();
        }

        boolean clarificationNeeded = shouldRequestClarification(finalizedTranscript, confidence, refinedTranscript);
        String clarificationPrompt = clarificationNeeded
                ? "I heard \"" + finalizedTranscript + "\". Press Send if that's right, or try again."
                : "";
        return new VoiceCaptureResult(
                finalizedTranscript,
                confidence,
                clarificationNeeded,
                clarificationPrompt,
                refinedTranscript
        );
    }

    private QuickRecognition parseRecognitionJson(String recognitionJson) {
        if (recognitionJson == null || recognitionJson.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(recognitionJson);
            String text = root.path("partial").asText("").trim();
            JsonNode wordsNode = root.path("partial_result");
            if (text.isBlank()) {
                text = root.path("text").asText("").trim();
                wordsNode = root.path("result");
            }
            if (text.isBlank()) {
                return null;
            }
            return new QuickRecognition(text, averageConfidence(wordsNode));
        } catch (Exception ignored) {
            return null;
        }
    }

    private double averageConfidence(JsonNode wordsNode) {
        if (wordsNode == null || !wordsNode.isArray() || wordsNode.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        int count = 0;
        for (JsonNode wordNode : wordsNode) {
            if (wordNode == null || !wordNode.has("conf")) {
                continue;
            }
            sum += wordNode.path("conf").asDouble(0.0);
            count++;
        }
        return count == 0 ? 0.0 : sum / count;
    }

    private void closeRecognizer(Recognizer recognizer) {
        if (recognizer == null) {
            return;
        }
        try {
            recognizer.close();
        } catch (Exception ignored) {
            // Recognizer shutdown is best-effort.
        }
    }

    private double audioDurationSeconds(byte[] audio) {
        if (audio == null || audio.length == 0) {
            return 0.0;
        }
        return audio.length / (double) AUDIO_FORMAT.getFrameSize() / SAMPLE_RATE;
    }

    private String buildWhisperPrompt() {
        String lexiconHint = entityLexicon.buildSpeechPromptHint();
        String prompt = lexiconHint.isBlank() ? WHISPER_PROMPT_BASE : WHISPER_PROMPT_BASE + ", " + lexiconHint;
        return prompt.length() <= 420 ? prompt : prompt.substring(0, 420);
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

    private static boolean looksLikeEntityHeavyCommand(String normalized) {
        return containsAny(normalized,
                "profile", "player", "players", "joueur", "joueurs",
                "team", "teams", "club", "league", "competition",
                "champions", "premier", "bundesliga", "laliga", "la liga");
    }

    private record RecordingSession(
            TargetDataLine line,
            ByteArrayOutputStream audio,
            AtomicBoolean running,
            Future<?> captureFuture,
            Recognizer recognizer,
            Consumer<VoiceCaptureUpdate> updateConsumer
    ) {
    }

    private record QuickRecognition(String text, double confidence) {
        private static QuickRecognition empty() {
            return new QuickRecognition("", 0.0);
        }
    }
}
