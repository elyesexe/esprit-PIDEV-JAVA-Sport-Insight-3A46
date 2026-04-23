package tn.esprit.face;

import javafx.application.Platform;
import javafx.scene.image.*;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.bytedeco.opencv.global.opencv_core.flip;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * Wraps JavaCV's {@link OpenCVFrameGrabber} and pumps BGR frames to both:
 *   1. An optional per-frame callback (for detection / capture logic)
 *   2. A JavaFX {@link ImageView} (for the live preview)
 *
 * <pre>
 *   WebcamService cam = new WebcamService(previewImageView);
 *   cam.onFrame(mat -> faceService.drawFaceBoxes(mat, faceService.detectFaces(mat)));
 *   cam.start();
 *   // … later …
 *   cam.stop();
 * </pre>
 *
 * The capture loop runs on a daemon thread — it will not prevent JVM shutdown.
 */
public class WebcamService {

    private static final Logger log = LoggerFactory.getLogger(WebcamService.class);

    private static final int CAMERA_INDEX = 0;
    private static final int TARGET_FPS   = 30;
    private static final int FRAME_MS     = 1000 / TARGET_FPS;

    private final ImageView               preview;
    private final OpenCVFrameGrabber      grabber;
    private final OpenCVFrameConverter.ToMat converter;

    private ExecutorService  executor;
    private volatile boolean running = false;
    private Consumer<Mat>    frameCallback;

    public WebcamService(ImageView preview) {
        this.preview   = preview;
        this.grabber   = new OpenCVFrameGrabber(CAMERA_INDEX);
        this.converter = new OpenCVFrameConverter.ToMat();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Optional hook: called for every BGR frame before it is displayed. */
    public void onFrame(Consumer<Mat> callback) { this.frameCallback = callback; }

    public void start() {
        if (running) return;
        running  = true;
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "webcam-thread");
            t.setDaemon(true);
            return t;
        });
        executor.submit(this::loop);
    }

    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
            try { executor.awaitTermination(2, TimeUnit.SECONDS); }
            catch (InterruptedException ignored) {}
        }
        try { grabber.stop(); } catch (Exception ignored) {}
    }

    /** Grab a single frame without the continuous loop — for snapshots. */
    public Mat snapshot() {
        try {
            Frame f = grabber.grab();
            if (f == null) return null;
            Mat m = converter.convert(f);
            flip(m, m, 1);
            return m;
        } catch (Exception e) {
            log.error("Snapshot failed", e);
            return null;
        }
    }

    // ── Capture loop ──────────────────────────────────────────────────────────

    private void loop() {
        try {
            grabber.setImageWidth(640);
            grabber.setImageHeight(480);
            grabber.start();
            log.info("Webcam started");

            while (running && !Thread.currentThread().isInterrupted()) {
                long t0 = System.currentTimeMillis();

                Frame frame = grabber.grab();
                if (frame == null || frame.image == null) continue;

                Mat bgr = converter.convert(frame);
                flip(bgr, bgr, 1);   // mirror for selfie feel

                if (frameCallback != null) frameCallback.accept(bgr);

                // BGR → RGB for JavaFX display
                Mat rgb = new Mat();
                cvtColor(bgr, rgb, COLOR_BGR2RGB);
                WritableImage fx = toFxImage(rgb);
                Platform.runLater(() -> preview.setImage(fx));

                long elapsed = System.currentTimeMillis() - t0;
                if (elapsed < FRAME_MS) Thread.sleep(FRAME_MS - elapsed);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Webcam loop error", e);
            Platform.runLater(() -> preview.setImage(null));
        } finally {
            try { grabber.stop(); } catch (Exception ignored) {}
        }
    }

    // ── Mat → JavaFX Image ────────────────────────────────────────────────────

    private WritableImage toFxImage(Mat rgb) {
        int w = rgb.cols(), h = rgb.rows(), ch = rgb.channels();
        byte[] buf = new byte[w * h * ch];
        rgb.data().get(buf);
        WritableImage img = new WritableImage(w, h);
        PixelWriter   pw  = img.getPixelWriter();
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                int idx = (row * w + col) * ch;
                int r = buf[idx]   & 0xff;
                int g = buf[idx+1] & 0xff;
                int b = buf[idx+2] & 0xff;
                pw.setArgb(col, row, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }
}
