package tn.esprit.assistant;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class VoiceOutputService {
    private static final String PIPER_RELEASE_TAG = "2023.11.14-2";
    private static final URI PIPER_WINDOWS_URI = URI.create(
            "https://github.com/rhasspy/piper/releases/download/" + PIPER_RELEASE_TAG + "/piper_windows_amd64.zip"
    );
    private static final String VOICE_ID = "en_US-ryan-low";
    private static final String VOICE_LABEL = "Fast local voice";
    private static final int REALTIME_SPEECH_CHAR_LIMIT = 170;
    private static final URI VOICE_MODEL_URI = URI.create(
            "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/ryan/low/en_US-ryan-low.onnx?download=true"
    );
    private static final URI VOICE_CONFIG_URI = URI.create(
            "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/ryan/low/en_US-ryan-low.onnx.json?download=true"
    );
    private static final Path INSTALL_ROOT = Path.of(System.getProperty("user.home"), ".sport-insight", "assistant", "voice");
    private static final Path PIPER_DIR = INSTALL_ROOT.resolve("piper");
    private static final Path PIPER_EXE = PIPER_DIR.resolve("piper.exe");
    private static final Path VOICE_DIR = INSTALL_ROOT.resolve("voices");
    private static final Path VOICE_MODEL = VOICE_DIR.resolve(VOICE_ID + ".onnx");
    private static final Path VOICE_CONFIG = VOICE_DIR.resolve(VOICE_ID + ".onnx.json");

    private final ExecutorService executor = Executors.newSingleThreadExecutor(daemonFactory("assistant-voice-output"));
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final Object installLock = new Object();
    private final Object playbackLock = new Object();

    private volatile Process activeProcess;
    private volatile SourceDataLine activeLine;

    public boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public String voiceLabel() {
        return VOICE_LABEL;
    }

    public String statusSummary() {
        if (!isSupported()) {
            return "Voice replies are limited on this operating system.";
        }
        if (isReady()) {
            return "Short replies use low-latency local speech and longer replies use the Piper Ryan neural voice.";
        }
        return "Voice replies use low-latency local speech and Piper Ryan for longer answers. Piper downloads on first use.";
    }

    public void prepareFastReplyAsync() {
        if (!isSupported()) {
            return;
        }
        executor.execute(() -> {
            try {
                ensurePiperInstalled();
                ensureVoiceInstalled();
            } catch (Exception ignored) {
                // Best effort warm-up only.
            }
        });
    }

    public void speakAsync(String rawText) {
        if (!isSupported()) {
            return;
        }

        String text = sanitize(rawText);
        if (text.isBlank()) {
            return;
        }

        executor.execute(() -> {
            stop();
            try {
                if (shouldUseRealtimeSpeech(text)) {
                    speakWithWindowsSpeech(text);
                    return;
                }

                ensurePiperInstalled();
                ensureVoiceInstalled();

                Path outputDirectory = INSTALL_ROOT.resolve("output");
                Files.createDirectories(outputDirectory);
                Path wavPath = Files.createTempFile(outputDirectory, "assistant-", ".wav");
                try {
                    synthesizeWithPiper(text, wavPath);
                    if (!playWavWithWindows(wavPath)) {
                        playWav(wavPath);
                    }
                } finally {
                    Files.deleteIfExists(wavPath);
                }
            } catch (Exception ignored) {
                speakWithWindowsSpeech(text);
            }
        });
    }

    public void stop() {
        Process process = activeProcess;
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        activeProcess = null;

        synchronized (playbackLock) {
            if (activeLine != null) {
                try {
                    activeLine.stop();
                    activeLine.flush();
                } catch (Exception ignored) {
                    // Best effort only.
                }
                try {
                    activeLine.close();
                } catch (Exception ignored) {
                    // Best effort only.
                }
            }
            activeLine = null;
        }
    }

    private boolean isReady() {
        return Files.isRegularFile(PIPER_EXE)
                && Files.isDirectory(PIPER_DIR.resolve("espeak-ng-data"))
                && Files.isRegularFile(VOICE_MODEL)
                && Files.isRegularFile(VOICE_CONFIG);
    }

    private void ensurePiperInstalled() throws IOException, InterruptedException {
        if (Files.isRegularFile(PIPER_EXE) && Files.isDirectory(PIPER_DIR.resolve("espeak-ng-data"))) {
            return;
        }

        synchronized (installLock) {
            if (Files.isRegularFile(PIPER_EXE) && Files.isDirectory(PIPER_DIR.resolve("espeak-ng-data"))) {
                return;
            }

            Files.createDirectories(INSTALL_ROOT);
            Path zipPath = INSTALL_ROOT.resolve("piper_windows_amd64.zip");
            downloadToFile(PIPER_WINDOWS_URI, zipPath);
            unzip(zipPath, INSTALL_ROOT);
            Files.deleteIfExists(zipPath);
        }
    }

    private void ensureVoiceInstalled() throws IOException, InterruptedException {
        if (Files.isRegularFile(VOICE_MODEL) && Files.isRegularFile(VOICE_CONFIG)) {
            return;
        }

        synchronized (installLock) {
            if (Files.isRegularFile(VOICE_MODEL) && Files.isRegularFile(VOICE_CONFIG)) {
                return;
            }

            Files.createDirectories(VOICE_DIR);
            downloadToFile(VOICE_MODEL_URI, VOICE_MODEL);
            downloadToFile(VOICE_CONFIG_URI, VOICE_CONFIG);
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
            throw new IOException("Voice asset download failed with HTTP " + response.statusCode() + ".");
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

    private void synthesizeWithPiper(String text, Path wavPath) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                PIPER_EXE.toString(),
                "--model",
                VOICE_MODEL.toString(),
                "--output_file",
                wavPath.toString()
        )
                .directory(PIPER_DIR.toFile())
                .redirectErrorStream(true)
                .start();

        activeProcess = process;
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(text);
            writer.newLine();
        }

        String processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        activeProcess = null;
        if (exitCode != 0 || !Files.isRegularFile(wavPath)) {
            throw new IOException("Piper synthesis failed. " + processOutput.trim());
        }
    }

    private boolean playWavWithWindows(Path wavPath) {
        try {
            String encodedPath = Base64.getEncoder().encodeToString(wavPath.toString().getBytes(StandardCharsets.UTF_8));
            String script = "$p=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('" + encodedPath + "'));"
                    + "$player=New-Object System.Media.SoundPlayer $p;"
                    + "$player.Load();"
                    + "$player.PlaySync();";
            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script)
                    .redirectErrorStream(true)
                    .start();
            activeProcess = process;
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception ignored) {
            return false;
        } finally {
            activeProcess = null;
        }
    }

    private void playWav(Path wavPath) throws Exception {
        try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(wavPath.toFile())) {
            AudioInputStream playbackStream = sourceStream;
            AudioFormat sourceFormat = sourceStream.getFormat();
            if (!AudioFormat.Encoding.PCM_SIGNED.equals(sourceFormat.getEncoding())
                    || sourceFormat.getSampleSizeInBits() <= 0) {
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        sourceFormat.getSampleRate(),
                        16,
                        sourceFormat.getChannels(),
                        Math.max(1, sourceFormat.getChannels()) * 2,
                        sourceFormat.getSampleRate(),
                        false
                );
                playbackStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
            }

            try (AudioInputStream audioInputStream = playbackStream) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioInputStream.getFormat());
                if (!AudioSystem.isLineSupported(info)) {
                    throw new IOException("No supported output line for format " + audioInputStream.getFormat() + ".");
                }

                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                synchronized (playbackLock) {
                    activeLine = line;
                }

                try {
                    line.open(audioInputStream.getFormat());
                    line.start();

                    byte[] buffer = new byte[8_192];
                    int bytesRead;
                    while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                        if (!line.isOpen()) {
                            break;
                        }
                        line.write(buffer, 0, bytesRead);
                    }

                    if (line.isOpen()) {
                        line.drain();
                        line.stop();
                    }
                } finally {
                    try {
                        line.close();
                    } catch (Exception ignored) {
                        // Best effort only.
                    }
                    synchronized (playbackLock) {
                        if (activeLine == line) {
                            activeLine = null;
                        }
                    }
                }
            }
        }
    }

    private void speakWithWindowsSpeech(String text) {
        try {
            String encoded = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
            String script = "$t=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('" + encoded + "'));"
                    + "Add-Type -AssemblyName System.Speech;"
                    + "$s=New-Object System.Speech.Synthesis.SpeechSynthesizer;"
                    + "$s.SetOutputToDefaultAudioDevice();"
                    + "$s.Rate=1;"
                    + "$s.Volume=100;"
                    + "$s.Speak($t);";
            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script)
                    .redirectErrorStream(true)
                    .start();
            activeProcess = process;
            process.waitFor();
        } catch (Exception ignored) {
            // Voice output is best-effort only.
        } finally {
            activeProcess = null;
        }
    }

    private boolean shouldUseRealtimeSpeech(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        int sentenceCount = text.split("[.!?]+").length;
        return text.length() <= REALTIME_SPEECH_CHAR_LIMIT && sentenceCount <= 2;
    }

    private String sanitize(String rawText) {
        if (rawText == null) {
            return "";
        }
        String plain = rawText
                .replace('`', ' ')
                .replace('*', ' ')
                .replace('#', ' ')
                .replaceAll("\\[(.*?)\\]\\((.*?)\\)", "$1")
                .replaceAll("\\s+", " ")
                .trim();
        return plain.length() <= 360 ? plain : plain.substring(0, 360);
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
