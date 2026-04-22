package tn.esprit.assistant;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class AssistantClapWakeService {
    private static final float SAMPLE_RATE = 16_000.0f;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(daemonFactory("assistant-clap-wake"));
    private final Object lifecycleLock = new Object();
    private final AssistantClapPatternDetector detector = new AssistantClapPatternDetector();

    private volatile Consumer<AssistantWakeSignal> wakeListener;
    private volatile MonitoringSession activeSession;
    private volatile String statusLabel = "Double clap standby";

    public void setWakeListener(Consumer<AssistantWakeSignal> listener) {
        wakeListener = listener;
        if (listener == null) {
            pauseMonitoring();
            statusLabel = "Double clap off";
            return;
        }
        resumeMonitoring();
    }

    public String statusLabel() {
        return statusLabel;
    }

    public void pauseMonitoring() {
        MonitoringSession session;
        synchronized (lifecycleLock) {
            session = activeSession;
            activeSession = null;
        }
        if (session == null) {
            return;
        }

        session.running().set(false);
        try {
            session.line().stop();
        } catch (Exception ignored) {
            // Best effort only.
        }
        try {
            session.line().close();
        } catch (Exception ignored) {
            // Best effort only.
        }
    }

    public void resumeMonitoring() {
        if (wakeListener == null) {
            return;
        }
        synchronized (lifecycleLock) {
            if (activeSession != null) {
                return;
            }

            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
                if (!AudioSystem.isLineSupported(info)) {
                    statusLabel = "Double clap unavailable";
                    return;
                }

                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(AUDIO_FORMAT);
                line.start();
                AtomicBoolean running = new AtomicBoolean(true);
                Future<?> future = executor.submit(() -> monitor(line, running));
                activeSession = new MonitoringSession(line, running, future);
                statusLabel = "Double clap armed";
            } catch (Exception ignored) {
                statusLabel = "Double clap unavailable";
            }
        }
    }

    private void monitor(TargetDataLine line, AtomicBoolean running) {
        byte[] buffer = new byte[256];
        while (running.get()) {
            int read = line.read(buffer, 0, buffer.length);
            if (read <= 0) {
                continue;
            }

            AudioEnergy energy = measureEnergy(buffer, read);
            if (!detector.accept(energy.rmsLevel(), energy.peakLevel(), System.currentTimeMillis())) {
                continue;
            }

            Consumer<AssistantWakeSignal> listener = wakeListener;
            if (listener != null) {
                listener.accept(AssistantWakeSignal.doubleClap(clampConfidence(energy.peakLevel())));
            }
        }
    }

    private AudioEnergy measureEnergy(byte[] buffer, int read) {
        double sumSquares = 0.0;
        double peak = 0.0;
        int samples = 0;
        for (int index = 0; index + 1 < read; index += 2) {
            int sample = (buffer[index + 1] << 8) | (buffer[index] & 0xff);
            double normalized = Math.abs(sample / 32768.0);
            sumSquares += normalized * normalized;
            peak = Math.max(peak, normalized);
            samples++;
        }
        double rms = samples == 0 ? 0.0 : Math.sqrt(sumSquares / samples);
        return new AudioEnergy(rms, peak);
    }

    private double clampConfidence(double peakLevel) {
        return Math.max(0.0, Math.min(1.0, peakLevel));
    }

    private static ThreadFactory daemonFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }

    private record MonitoringSession(
            TargetDataLine line,
            AtomicBoolean running,
            Future<?> future
    ) {
    }

    private record AudioEnergy(
            double rmsLevel,
            double peakLevel
    ) {
    }
}
